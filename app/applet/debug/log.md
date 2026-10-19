# LOGGING DEBUG — SRMusic

Dokumen ini adalah aturan tetap untuk menambahkan log diagnostik pada SRMusic. Baca sebelum menambahkan atau mengubah logging.

## Tujuan

Membuat log yang bisa dipakai untuk mengisolasi satu masalah pada subsistem yang diminta, tanpa refactor atau mengubah perilaku aplikasi.

## Aturan utama

1. Baca source aktual terlebih dahulu dan telusuri alur yang benar-benar dipakai runtime. Jangan mengarang class, function, field, state, API, atau hasil yang tidak ada di source.
2. Tambahkan log hanya untuk subsistem dan file yang relevan dengan permintaan saat ini. Jangan memasang logging menyeluruh di seluruh aplikasi.
3. Logging bersifat observasional: tidak mengubah urutan operasi, lifecycle, threading, buffer, nilai audio, pemilihan backend, error handling, maupun fallback.
4. Jangan menambahkan fitur debug otomatis (misalnya runner, timer, coroutine, polling loop, test volume otomatis, atau UI) kecuali diminta secara eksplisit.
5. Jangan mencatat judul lagu, nama artis, path file/media, identifier pengguna, atau informasi privat bila tidak dibutuhkan untuk diagnosis.
6. Jangan menganggap log sukses sebagai bukti fitur bekerja. Catat hasil aktual, error, state, dan efek runtime yang relevan.
7. Hindari log berlebihan. Jangan menulis log per-frame/per-sample atau melakukan logging, alokasi, mutex lock, atau operasi blocking di callback audio realtime.
8. Pertahankan log/error handling yang sudah ada. Hapus atau ubah log lama hanya jika diminta atau terbukti menduplikasi/mengaburkan diagnosis.

## Konvensi tag

- Periksa apakah subsistem sudah memiliki tag. Gunakan tag yang sudah ada untuk menjaga filter Logcat tetap kompatibel.
- Pada kode audio native SRMusic yang saat ini menggunakan `SRMusicAudioDiag`, pertahankan tag tersebut untuk diagnostik audio yang berkaitan.
- Untuk subsistem lain, gunakan tag yang sudah ada jika tersedia. Jika belum ada, pilih satu tag stabil dan konsisten, misalnya `SRMusicBitPerfect`, lalu gunakan tag yang sama untuk semua log di subsistem itu.
- Kotlin/Android: gunakan `android.util.Log` dengan level yang sesuai (`d`, `i`, `w`, `e`). Native C++: gunakan wrapper logging yang sudah tersedia (misalnya `LOGI`, `LOGW`, `LOGE` di `sroboe.cpp`) dan jangan membuat wrapper kedua jika tidak perlu.
- Pesan log harus menyebut tahap/kejadian dan nilai aktual, misalnya `CREATE`, `START`, `PAUSE`, `FLUSH`, `RELEASE`, `FALLBACK`, `ERROR`.

## Jika targetnya Audio Output / Oboe

Periksa terlebih dahulu file aktual yang terkait, yang pada source saat ini mencakup:

- `app/src/main/java/com/example/core/audio/OboeAudioOutput.kt`
- `app/src/main/java/com/example/core/audio/OboeAudioOutputProvider.kt`
- `app/src/main/java/com/example/core/audio/OboeEngine.kt`
- `app/src/main/java/com/example/core/playback/MusicService.kt`
- `app/src/main/cpp/sroboe.cpp`

Gunakan hanya file yang benar-benar dibutuhkan. Jika relevan, catat:

- API yang diminta dibanding API aktual.
- Format PCM yang aktual: sample rate, channel count, encoding/format, serta audio session ID jika tersedia.
- Hasil create/start/pause/flush/release beserta kode error/state aktual.
- Hasil write ke buffer dibanding frame yang benar-benar dikonsumsi.
- Underrun, callback stall, kegagalan native, dan alasan fallback.
- Tahap rebuild player serta state/posisi playback yang relevan.

Jangan menyimpulkan backend aktif hanya dari pilihan UI. Pastikan log berasal dari stream/native API aktual dan konsumsi frame benar-benar terjadi.

## Jika targetnya Bit Perfect / DSP / EQ

Telusuri jalur audio aktual sebelum menambahkan log. Tambahkan hanya log yang dapat membuktikan keputusan atau transformasi yang memang ada di kode, misalnya:

- Status Bit Perfect yang diminta dan status yang benar-benar aktif setelah semua kondisi/eligibility diperiksa.
- Apakah EQ, preamp gain, software volume, resampling, format conversion, channel conversion, atau pemrosesan DSP lain aktif atau dilewati — hanya jika keadaan tersebut tersedia dari source.
- Format PCM masuk dan keluar dari tahap yang sedang diperiksa, bila benar-benar diketahui.
- Alasan Bit Perfect tidak aktif atau jalur mengalami fallback.

Jangan menyatakan output bit-perfect hanya karena toggle aktif atau karena sample rate cocok. Jangan menambahkan pemrosesan baru demi logging. Jangan mengubah jalur audio.

## Saat menerima perintah “log [subsistem]”

1. Tentukan subsistem dari permintaan terbaru atau konteks langsung yang jelas.
2. Baca file dan fungsi yang terlibat sebelum melakukan edit.
3. Tambahkan log minimal pada awal/akhir operasi penting dan jalur error/fallback.
4. Pastikan logging tidak membuat side effect atau mengubah timing kritis.
5. Periksa diff agar hanya berisi file logging yang diperlukan.
6. Berikan tag Logcat serta perintah filter yang benar untuk tag itu.

Jika target logging tidak bisa ditentukan dari perintah maupun konteks, tanyakan subsistem apa yang ingin diperiksa daripada menyebarkan log ke banyak file.

## Perintah gabungan

Jika pengguna berkata “ubah tipe build debug dan log [subsistem]”:

1. Baca `debug/debug.md` dan ikuti aturan konfigurasi build debug.
2. Baca `debug/log.md` dan tambahkan logging hanya pada subsistem yang disebut atau yang jelas dari konteks langsung.
3. Pertahankan perubahan terpisah: Gradle untuk build mode, source runtime hanya untuk log yang diminta.
4. Jangan menambahkan fitur debug runtime baru kecuali diminta.
5. Jangan mengubah `release` atau subsistem lain.
