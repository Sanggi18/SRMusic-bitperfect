#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>
#include <vector>
#include <atomic>
#include <cstring>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <limits>

#define LOG_TAG "SRMusicAudioDiag"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * Lock-free Single-Producer Single-Consumer (SPSC) Ring Buffer.
 * Frame-aligned read and write operations.
 */
class SpscRingBuffer {
private:
    std::vector<uint8_t> buffer;
    size_t capacity;
    size_t bytesPerFrame;
    std::atomic<size_t> head{0}; // write index
    std::atomic<size_t> tail{0}; // read index

public:
    explicit SpscRingBuffer(size_t size, size_t frameSize)
        : buffer(size), capacity(size), bytesPerFrame(frameSize) {}

    size_t getCapacity() const { return capacity; }

    size_t write(const uint8_t* data, size_t count) {
        size_t currentHead = head.load(std::memory_order_relaxed);
        size_t currentTail = tail.load(std::memory_order_acquire);

        size_t freeSpace;
        if (currentHead >= currentTail) {
            freeSpace = capacity - (currentHead - currentTail) - 1;
        } else {
            freeSpace = currentTail - currentHead - 1;
        }

        size_t toWrite = std::min(count, freeSpace);
        toWrite -= toWrite % bytesPerFrame;
        if (toWrite == 0) return 0;

        size_t firstPart = std::min(toWrite, capacity - currentHead);
        std::memcpy(buffer.data() + currentHead, data, firstPart);

        size_t secondPart = toWrite - firstPart;
        if (secondPart > 0) {
            std::memcpy(buffer.data(), data + firstPart, secondPart);
        }

        size_t newHead = (currentHead + toWrite) % capacity;
        head.store(newHead, std::memory_order_release);
        return toWrite;
    }

    size_t read(uint8_t* out, size_t count) {
        size_t currentHead = head.load(std::memory_order_acquire);
        size_t currentTail = tail.load(std::memory_order_relaxed);

        size_t available;
        if (currentHead >= currentTail) {
            available = currentHead - currentTail;
        } else {
            available = capacity - (currentTail - currentHead);
        }

        size_t toRead = std::min(count, available);
        toRead -= toRead % bytesPerFrame;
        if (toRead == 0) return 0;

        size_t firstPart = std::min(toRead, capacity - currentTail);
        std::memcpy(out, buffer.data() + currentTail, firstPart);

        size_t secondPart = toRead - firstPart;
        if (secondPart > 0) {
            std::memcpy(out + firstPart, buffer.data(), secondPart);
        }

        size_t newTail = (currentTail + toRead) % capacity;
        tail.store(newTail, std::memory_order_release);
        return toRead;
    }

    void reset() {
        head.store(0, std::memory_order_relaxed);
        tail.store(0, std::memory_order_relaxed);
    }
};

struct EngineContext : public oboe::AudioStreamDataCallback, public oboe::AudioStreamErrorCallback {
    oboe::AudioStream *stream = nullptr;
    SpscRingBuffer ringBuffer;
    int32_t sampleRate = 48000;
    int32_t channelCount = 2;
    int32_t bytesPerFrame = 4;
    oboe::AudioApi requestedApi = oboe::AudioApi::Unspecified;
    oboe::AudioFormat audioFormat = oboe::AudioFormat::I16;
    std::atomic<int64_t> totalFramesConsumed{0};
    std::atomic<bool> isPlaying{false};
    std::atomic<int64_t> callbackCount{0};
    std::atomic<int64_t> underrunCount{0};

    // setVolume() updates this atomic from a non-realtime thread.
    // The remaining ramp state is accessed only by the audio callback.
    std::atomic<float> targetVolume{1.0f};
    float callbackVolume = 1.0f;
    float callbackTargetVolume = 1.0f;
    float callbackVolumeStep = 0.0f;
    int32_t callbackVolumeRampRemaining = 0;

    EngineContext(
        int32_t sr,
        int32_t ch,
        int32_t bpf,
        oboe::AudioApi api,
        oboe::AudioFormat format,
        size_t bufCapacity)
        : ringBuffer(bufCapacity, static_cast<size_t>(bpf)),
          sampleRate(sr),
          channelCount(ch),
          bytesPerFrame(bpf),
          requestedApi(api),
          audioFormat(format) {}

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream *audioStream,
        void *audioData,
        int32_t numFrames) override {

        callbackCount.fetch_add(1, std::memory_order_relaxed);

        auto *output = static_cast<uint8_t*>(audioData);
        size_t bytesNeeded = static_cast<size_t>(numFrames * bytesPerFrame);

        size_t bytesRead = ringBuffer.read(output, bytesNeeded);
        if (bytesRead > 0) {
            const float requestedVolume = targetVolume.load(std::memory_order_relaxed);
            if (requestedVolume != callbackTargetVolume) {
                callbackTargetVolume = requestedVolume;
                // Smooth volume transitions over 5 ms to avoid abrupt gain steps.
                callbackVolumeRampRemaining = std::max<int32_t>(1, sampleRate / 200);
                callbackVolumeStep =
                    (callbackTargetVolume - callbackVolume) /
                    static_cast<float>(callbackVolumeRampRemaining);
            }

            const bool unityGainFastPath =
                callbackVolume == 1.0f &&
                callbackTargetVolume == 1.0f &&
                callbackVolumeRampRemaining == 0;

            // At unity gain, leave the PCM bytes untouched (bit-exact path).
            if (!unityGainFastPath) {
                const size_t framesRead = bytesRead / static_cast<size_t>(bytesPerFrame);
                for (size_t frame = 0; frame < framesRead; ++frame) {
                    if (callbackVolumeRampRemaining > 0) {
                        callbackVolume += callbackVolumeStep;
                        --callbackVolumeRampRemaining;
                        if (callbackVolumeRampRemaining == 0) {
                            callbackVolume = callbackTargetVolume;
                        }
                    }

                    const size_t sampleOffset = frame * static_cast<size_t>(channelCount);
                    if (audioFormat == oboe::AudioFormat::Float) {
                        auto *samples = reinterpret_cast<float *>(output);
                        for (int32_t channel = 0; channel < channelCount; ++channel) {
                            const size_t index = sampleOffset + static_cast<size_t>(channel);
                            samples[index] *= callbackVolume;
                        }
                    } else {
                        auto *samples = reinterpret_cast<int16_t *>(output);
                        for (int32_t channel = 0; channel < channelCount; ++channel) {
                            const size_t index = sampleOffset + static_cast<size_t>(channel);
                            const float scaled = static_cast<float>(samples[index]) * callbackVolume;
                            const long rounded = std::lrintf(scaled);
                            const long bounded = std::max<long>(
                                std::numeric_limits<int16_t>::min(),
                                std::min<long>(std::numeric_limits<int16_t>::max(), rounded));
                            samples[index] = static_cast<int16_t>(bounded);
                        }
                    }
                }
            }

            const int64_t framesRead = static_cast<int64_t>(bytesRead / bytesPerFrame);
            totalFramesConsumed.fetch_add(framesRead, std::memory_order_relaxed);
        }

        if (bytesRead < bytesNeeded) {
            // Fill remainder with silence (zero-allocation, non-blocking)
            std::memset(output + bytesRead, 0, bytesNeeded - bytesRead);
            underrunCount.fetch_add(1, std::memory_order_relaxed);
        }

        return oboe::DataCallbackResult::Continue;
    }

    void onErrorBeforeClose(oboe::AudioStream *oboeStream, oboe::Result error) override {
        LOGE("NATIVE_ERROR_BEFORE_CLOSE: error=%s (code=%d)", oboe::convertToText(error), (int)error);
    }

    void onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error) override {
        LOGE("NATIVE_ERROR_AFTER_CLOSE: error=%s (code=%d)", oboe::convertToText(error), (int)error);
    }
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_core_audio_OboeEngine_nativeCreate(
    JNIEnv *env, jobject thiz,
    jint apiOrdinal, jint sampleRate, jint channelCount, jint encoding, jint sessionId) {

    oboe::AudioApi api = oboe::AudioApi::Unspecified;
    if (apiOrdinal == 0) {
        api = oboe::AudioApi::AAudio;
    } else if (apiOrdinal == 1) {
        api = oboe::AudioApi::OpenSLES;
    } else {
        LOGE("NATIVE_CREATE failed: Unsupported API ordinal %d", apiOrdinal);
        return 0;
    }

    oboe::AudioFormat audioFormat = (encoding == 4 /* C.ENCODING_PCM_FLOAT */)
        ? oboe::AudioFormat::Float
        : oboe::AudioFormat::I16;

    int32_t bytesPerSample = (audioFormat == oboe::AudioFormat::Float) ? 4 : 2;
    int32_t bpf = channelCount * bytesPerSample;

    size_t ringBufferSize = std::max(static_cast<size_t>(16384 * bpf), static_cast<size_t>(65536));

    auto *ctx = new EngineContext(sampleRate, channelCount, bpf, api, audioFormat, ringBufferSize);

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Shared)
        ->setFormat(audioFormat)
        ->setChannelCount(channelCount)
        ->setSampleRate(sampleRate)
        ->setAudioApi(api)
        ->setDataCallback(ctx)
        ->setErrorCallback(ctx);

    if (sessionId > 0) {
        builder.setSessionId(static_cast<oboe::SessionId>(sessionId));
    }

    oboe::Result result = builder.openStream(&ctx->stream);
    if (result != oboe::Result::OK || !ctx->stream) {
        LOGE("NATIVE_CREATE openStream failed: result=%s (code=%d)", oboe::convertToText(result), (int)result);
        delete ctx;
        return 0;
    }

    // Validate that actual stream format matches requested configuration
    if (ctx->stream->getAudioApi() != api ||
        ctx->stream->getSampleRate() != sampleRate ||
        ctx->stream->getChannelCount() != channelCount ||
        ctx->stream->getFormat() != audioFormat) {

        LOGE("NATIVE_CREATE parameter mismatch: requested [api=%d sr=%d ch=%d fmt=%d] vs actual [api=%d sr=%d ch=%d fmt=%d]",
             (int)api, sampleRate, channelCount, (int)audioFormat,
             (int)ctx->stream->getAudioApi(), ctx->stream->getSampleRate(),
             ctx->stream->getChannelCount(), (int)ctx->stream->getFormat());

        ctx->stream->close();
        delete ctx->stream;
        delete ctx;
        return 0;
    }

    LOGI("Oboe stream opened: requestedApi=%d actualApi=%d sampleRate=%d channels=%d format=%d sessionId=%d",
         apiOrdinal,
         (int)ctx->stream->getAudioApi(),
         ctx->stream->getSampleRate(),
         ctx->stream->getChannelCount(),
         (int)ctx->stream->getFormat(),
         (int)ctx->stream->getSessionId()
    );

    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jboolean JNICALL
Java_com_example_core_audio_OboeEngine_nativeStart(JNIEnv *env, jobject thiz, jlong handle) {
    auto *ctx = reinterpret_cast<EngineContext*>(handle);
    if (!ctx || !ctx->stream) return JNI_FALSE;

    oboe::StreamState stateBefore = ctx->stream->getState();
    if (stateBefore == oboe::StreamState::Started) {
        ctx->isPlaying.store(true, std::memory_order_relaxed);
        LOGI("NATIVE_START already Started: stateBefore=%s", oboe::convertToText(stateBefore));
        return JNI_TRUE;
    }

    oboe::Result res = ctx->stream->requestStart();
    oboe::StreamState stateAfter = ctx->stream->getState();

    LOGI("NATIVE_START: stateBefore=%s res=%s (code=%d) stateAfter=%s",
         oboe::convertToText(stateBefore), oboe::convertToText(res), (int)res, oboe::convertToText(stateAfter));

    if (res != oboe::Result::OK) {
        ctx->isPlaying.store(false, std::memory_order_relaxed);
        LOGE("NATIVE_START requestStart failed: result=%s (code=%d) stateAfter=%s",
             oboe::convertToText(res), (int)res, oboe::convertToText(stateAfter));
        return JNI_FALSE;
    }

    ctx->isPlaying.store(true, std::memory_order_relaxed);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_example_core_audio_OboeEngine_nativePause(JNIEnv *env, jobject thiz, jlong handle) {
    auto *ctx = reinterpret_cast<EngineContext*>(handle);
    if (!ctx || !ctx->stream) return;

    oboe::StreamState stateBefore = ctx->stream->getState();
    oboe::Result res = ctx->stream->requestPause();
    oboe::StreamState stateAfter = ctx->stream->getState();

    ctx->isPlaying.store(false, std::memory_order_relaxed);
    LOGI("NATIVE_PAUSE: stateBefore=%s res=%s (code=%d) stateAfter=%s",
         oboe::convertToText(stateBefore), oboe::convertToText(res), (int)res, oboe::convertToText(stateAfter));

    if (res != oboe::Result::OK) {
        LOGE("NATIVE_PAUSE requestPause failed: result=%s (code=%d) stateAfter=%s",
             oboe::convertToText(res), (int)res, oboe::convertToText(stateAfter));
    }
}

JNIEXPORT void JNICALL
Java_com_example_core_audio_OboeEngine_nativeStop(JNIEnv *env, jobject thiz, jlong handle) {
    auto *ctx = reinterpret_cast<EngineContext*>(handle);
    if (!ctx || !ctx->stream) return;

    oboe::StreamState stateBefore = ctx->stream->getState();
    oboe::Result res = ctx->stream->requestStop();
    oboe::StreamState stateAfter = ctx->stream->getState();

    ctx->isPlaying.store(false, std::memory_order_relaxed);
    LOGI("NATIVE_STOP: stateBefore=%s res=%s (code=%d) stateAfter=%s",
         oboe::convertToText(stateBefore), oboe::convertToText(res), (int)res, oboe::convertToText(stateAfter));

    if (res != oboe::Result::OK) {
        LOGE("NATIVE_STOP requestStop failed: result=%s (code=%d) stateAfter=%s",
             oboe::convertToText(res), (int)res, oboe::convertToText(stateAfter));
    }
}

JNIEXPORT void JNICALL
Java_com_example_core_audio_OboeEngine_nativeFlush(JNIEnv *env, jobject thiz, jlong handle) {
    auto *ctx = reinterpret_cast<EngineContext*>(handle);
    if (!ctx) return;

    int64_t underruns = ctx->underrunCount.load(std::memory_order_relaxed);
    int64_t callbacksBefore = ctx->callbackCount.load(std::memory_order_relaxed);
    int64_t consumedBefore = ctx->totalFramesConsumed.load(std::memory_order_relaxed);

    if (ctx->stream) {
        oboe::StreamState stateBefore = ctx->stream->getState();

        // If stream is Starting or Started, pause first before flushing
        if (stateBefore == oboe::StreamState::Starting || stateBefore == oboe::StreamState::Started) {
            oboe::Result pauseRes = ctx->stream->requestPause();
            oboe::StreamState stateAfterPause = ctx->stream->getState();
            LOGI("NATIVE_FLUSH pauseBeforeFlush: stateBefore=%s pauseRes=%s (code=%d) stateAfter=%s",
                 oboe::convertToText(stateBefore), oboe::convertToText(pauseRes), (int)pauseRes,
                 oboe::convertToText(stateAfterPause));
        }

        oboe::StreamState stateBeforeFlush = ctx->stream->getState();
        oboe::Result flushRes = ctx->stream->requestFlush();
        oboe::StreamState stateAfterFlush = ctx->stream->getState();

        LOGI("NATIVE_FLUSH: stateBefore=%s flushRes=%s (code=%d) stateAfter=%s callbacksBefore=%lld underruns=%lld consumedBefore=%lld",
             oboe::convertToText(stateBeforeFlush), oboe::convertToText(flushRes), (int)flushRes,
             oboe::convertToText(stateAfterFlush), (long long)callbacksBefore, (long long)underruns, (long long)consumedBefore);

        if (flushRes != oboe::Result::OK) {
            LOGE("NATIVE_FLUSH requestFlush failed: result=%s (code=%d) state=%s",
                 oboe::convertToText(flushRes), (int)flushRes, oboe::convertToText(stateAfterFlush));
        }
    } else {
        LOGI("NATIVE_FLUSH: stream is null callbacksBefore=%lld underruns=%lld consumedBefore=%lld",
             (long long)callbacksBefore, (long long)underruns, (long long)consumedBefore);
    }

    ctx->ringBuffer.reset();
    ctx->totalFramesConsumed.store(0, std::memory_order_relaxed);
    ctx->isPlaying.store(false, std::memory_order_relaxed);

    int64_t callbacksAfter = ctx->callbackCount.load(std::memory_order_relaxed);
    int64_t consumedAfter = ctx->totalFramesConsumed.load(std::memory_order_relaxed);
    LOGI("NATIVE_FLUSH_DONE: callbacksAfter=%lld consumedAfter=%lld",
         (long long)callbacksAfter, (long long)consumedAfter);
}

JNIEXPORT void JNICALL
Java_com_example_core_audio_OboeEngine_nativeSetVolume(
    JNIEnv *env, jobject thiz, jlong handle, jfloat volume) {
    auto *ctx = reinterpret_cast<EngineContext*>(handle);
    if (!ctx) return;

    const float safeVolume = std::isfinite(volume)
        ? std::min(1.0f, std::max(0.0f, volume))
        : 1.0f;
    ctx->targetVolume.store(safeVolume, std::memory_order_relaxed);
    LOGI("NATIVE_SET_VOLUME targetVolume=%.3f (requested=%.3f)", safeVolume, volume);
}

JNIEXPORT jint JNICALL
Java_com_example_core_audio_OboeEngine_nativeWritePcm(
    JNIEnv *env, jobject thiz, jlong handle, jobject byteBuffer, jint offset, jint byteCount) {
    auto *ctx = reinterpret_cast<EngineContext*>(handle);
    if (!ctx || byteCount <= 0 || offset < 0) return 0;

    auto *data = static_cast<uint8_t*>(env->GetDirectBufferAddress(byteBuffer));
    if (!data) {
        LOGE("NATIVE_WRITE_ERROR: byteBuffer is not a direct buffer or accessible");
        return -1;
    }

    jlong bufferCapacity = env->GetDirectBufferCapacity(byteBuffer);
    if (bufferCapacity >= 0 && (static_cast<jlong>(offset) + byteCount > bufferCapacity)) {
        LOGE("NATIVE_WRITE_ERROR: offset %d + count %d > capacity %lld", offset, byteCount, (long long)bufferCapacity);
        return -1;
    }

    if (byteCount % ctx->bytesPerFrame != 0) {
        LOGE("NATIVE_WRITE_ERROR: byteCount %d is not aligned to frame size %d", byteCount, ctx->bytesPerFrame);
        return -1;
    }

    size_t written = ctx->ringBuffer.write(data + offset, static_cast<size_t>(byteCount));
    return static_cast<jint>(written);
}

JNIEXPORT jlong JNICALL
Java_com_example_core_audio_OboeEngine_nativeGetConsumedFrames(JNIEnv *env, jobject thiz, jlong handle) {
    auto *ctx = reinterpret_cast<EngineContext*>(handle);
    if (!ctx) return 0;
    return static_cast<jlong>(ctx->totalFramesConsumed.load(std::memory_order_relaxed));
}

JNIEXPORT jlong JNICALL
Java_com_example_core_audio_OboeEngine_nativeGetBufferSizeInFrames(JNIEnv *env, jobject thiz, jlong handle) {
    auto *ctx = reinterpret_cast<EngineContext*>(handle);
    if (!ctx || !ctx->stream) return 0;
    return static_cast<jlong>(ctx->stream->getBufferSizeInFrames());
}

JNIEXPORT jint JNICALL
Java_com_example_core_audio_OboeEngine_nativeGetAudioSessionId(JNIEnv *env, jobject thiz, jlong handle) {
    auto *ctx = reinterpret_cast<EngineContext*>(handle);
    if (!ctx || !ctx->stream) return 0;
    return static_cast<jint>(ctx->stream->getSessionId());
}

JNIEXPORT jint JNICALL
Java_com_example_core_audio_OboeEngine_nativeGetActualApi(JNIEnv *env, jobject thiz, jlong handle) {
    auto *ctx = reinterpret_cast<EngineContext*>(handle);
    if (!ctx || !ctx->stream) return 2; // Default to AudioTrack ordinal

    oboe::AudioApi api = ctx->stream->getAudioApi();
    if (api == oboe::AudioApi::AAudio) {
        return 0; // AAUDIO
    } else if (api == oboe::AudioApi::OpenSLES) {
        return 1; // OPENSL_ES
    } else {
        return 2; // AUDIOTRACK
    }
}

JNIEXPORT void JNICALL
Java_com_example_core_audio_OboeEngine_nativeRelease(JNIEnv *env, jobject thiz, jlong handle) {
    auto *ctx = reinterpret_cast<EngineContext*>(handle);
    if (ctx) {
        int64_t underruns = ctx->underrunCount.load(std::memory_order_relaxed);
        int64_t callbacks = ctx->callbackCount.load(std::memory_order_relaxed);
        int64_t consumed = ctx->totalFramesConsumed.load(std::memory_order_relaxed);

        if (ctx->stream) {
            oboe::StreamState stateBefore = ctx->stream->getState();
            ctx->stream->stop();
            ctx->stream->close();
            delete ctx->stream;
            ctx->stream = nullptr;
            LOGI("NATIVE_RELEASE: stateBefore=%s totalCallbacks=%lld totalUnderruns=%lld finalConsumedFrames=%lld",
                 oboe::convertToText(stateBefore), (long long)callbacks, (long long)underruns, (long long)consumed);
        } else {
            LOGI("NATIVE_RELEASE: stream was null totalCallbacks=%lld totalUnderruns=%lld finalConsumedFrames=%lld",
                 (long long)callbacks, (long long)underruns, (long long)consumed);
        }
        delete ctx;
    }
}

} // extern "C"
