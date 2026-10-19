# DEBUG BUILD — SRMusic

Dokumen ini adalah aturan tetap untuk mengubah konfigurasi build `debug` SRMusic. Baca sebelum mengedit konfigurasi Gradle untuk pekerjaan debugging.

## Tujuan

Menjaga varian `debug` benar-benar dapat dipakai untuk debugging: aplikasi debuggable, tidak di-minify/obfuscate oleh R8, dan resource tidak di-shrink. Konfigurasi `release` tidak boleh ikut berubah.

## File dalam scope

- `app/build.gradle.kts`
- `gradle.properties` hanya jika ada alasan teknis yang terbukti dan perubahan tersebut memang diminta. Secara default, jangan mengubahnya.

## Konfigurasi yang diharapkan

Periksa blok `android.buildTypes.debug` di `app/build.gradle.kts`. Pastikan setidaknya kondisi berikut terpenuhi, dengan mempertahankan signing config yang sudah dipakai proyek:

```kotlin
debug {
    isDebuggable = true
    isMinifyEnabled = false
    isShrinkResources = false
    signingConfig = signingConfigs.getByName("debugConfig")
}
```

- Jangan menambahkan `proguardFiles(...)` khusus pada blok `debug`.
- Jangan menghapus atau mengubah konfigurasi `release`.
- Pertahankan konfigurasi CMake, ABI filters, Prefab, BuildConfig, dependencies, SDK, application ID, versioning, dan signing yang tidak terkait.
- Jika proyek saat ini sudah memenuhi kondisi tersebut, jangan melakukan edit ulang yang tidak diperlukan. Cukup konfirmasi bahwa konfigurasi debug sudah sesuai.

## R8 dan `gradle.properties`

- `isMinifyEnabled = false` pada build type `debug` menonaktifkan proses minification/obfuscation R8 untuk varian tersebut.
- Jangan menambahkan `android.enableR8.fullMode=false` atau mengubah `android.enableR8.fullMode=true` secara global hanya untuk mendapatkan log debug. Pengaturan global dapat memengaruhi build lain, termasuk `release`.
- Jangan mencoba menonaktifkan R8 untuk seluruh proyek sebagai pengganti pengaturan build type `debug`.
- Jangan mengubah aturan `app/proguard-rules.pro` untuk menyelesaikan masalah log pada debug; perbaiki konfigurasi build debug terlebih dahulu.

## Batasan perubahan

Saat pengguna meminta “ubah tipe build debug”, ubah hanya konfigurasi yang diperlukan untuk varian `debug`. Jangan menambah fitur, UI, logging, test runner, coroutine, timer, atau kode runtime kecuali pengguna juga meminta logging atau perilaku tersebut secara eksplisit.

## Pemeriksaan setelah perubahan

1. Periksa ulang blok `debug` dan pastikan `release` tetap sama.
2. Pastikan tidak ada `proguardFiles(...)` khusus di blok `debug`.
3. Build varian `debug` jika lingkungan build tersedia.
4. Laporkan dengan jujur jika build tidak dijalankan atau gagal; jangan menyamakan pemeriksaan source dengan build yang sukses.
