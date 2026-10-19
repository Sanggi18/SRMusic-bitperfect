# SRMusic — AGENTS.md

Kontrak engineering untuk SRMusic. Wajib dibaca sebelum mengubah kode — oleh manusia maupun AI coding agent (termasuk Google AI Studio).

> **BACA FILE INI SEBELUM MENGUBAH SRMUSIC.**
>
> SRMusic adalah aplikasi yang sudah ada, bukan template kosong. Jaga perilaku yang ada kecuali developer meminta perubahan perilaku secara eksplisit.
>
> Jika ragu, kerjakan lebih sedikit. Jangan mengarang infrastruktur atau fitur.
>
> **Jangan berimprovisasi di luar instruksi yang diberikan.**

---

## 1. IDENTITAS PRODUK

| Item | Nilai saat ini |
|---|---|
| Aplikasi | `SRMusic` |
| Package | `com.example` (BELUM diganti — wajib diganti sebelum rilis Play Store) |
| Bahasa | Kotlin |
| UI | Jetpack Compose, Material 3 Expressive |
| Playback | Media3 / ExoPlayer + MediaSession |
| Database | Room (Song, Playlist, Favorite, PlayCount) |
| Preferences | SharedPreferences manual |
| DI | Manual via `SRMusicApp` |
| minSdk | 24 |

---

## 2. ATURAN NON-NEGOSIABLE

Aturan arsitektur, bukan saran.

### 2.1 No fake data

Jangan pernah mengarang data:

- bitrate / sample rate / bit depth / channel palsu;
- tanggal rilis palsu;
- genre palsu.

Jika datanya tidak ada, tampilkan kosong atau sembunyikan elemennya. Jangan tampilkan `"320 kbps"`, `"Stereo"`, `"24-bit"`, `"44.1 kHz"` hasil karangan.

### 2.2 Satu operasi = satu query

Dilarang N+1 query (query di dalam loop per lagu). Satu pemindaian library = tepat **1 query** MediaStore.

### 2.3 Nol file I/O di path scan

Jangan buka file audio (`MediaMetadataRetriever`, Jaudiotagger, TagLib) saat scan library. File hanya boleh dibuka on-demand untuk lagu yang sedang diputar/dilihat, dan hasilnya wajib di-cache.

### 2.4 Satu file = satu tanggung jawab

Satu file, satu konsep. Jika sebuah file mulai mencampur 2+ tanggung jawab, pecah. Dilarang package yang hanya berisi satu file tanpa alasan.

### 2.5 Jangan duplikasi logika

Jika blok kode yang sama muncul 2×, ekstrak jadi satu fungsi/komponen shared. Jangan copy-paste lalu modifikasi sedikit.

---

## 3. STRUKTUR PROYEK

```
com.example/
├── SRMusicApp.kt / AppRoot.kt / MainActivity.kt   ← composition root, manual DI
├── core/
│   ├── model/          ← domain models (Song, enums UI)
│   ├── util/           ← pure helpers (TimeUtils, FileSizeUtils, dst.)
│   ├── playback/       ← PlaybackManager, MusicService
│   └── data/
│       ├── mediastore/   ← scan/ (1 query, nol I/O) · sync/ · tags/ (zona boleh buka file)
│       ├── database/     ← Room: entities, DAOs
│       ├── repository/   ← repository implementations
│       └── preferences/  ← SharedPreferences wrappers
├── lyrics/             ← parsing lirik (LrcParser, readers)
└── ui/
    ├── player/         ← PlayerHost (mini player), NowPlayingScreen, style/
    ├── library/        ← song/album/artist/folder
    ├── sidebar/        ← drawer, playlist, history, favorites
    ├── home/           ← HomeScreen + sections
    ├── settings/       ← semua layar pengaturan
    ├── navigation/     ← NavigationState, AppNavigation
    ├── equalizer/      ← (catatan: belum wiring ke audiofx)
    └── common/         ← komponen & theme shared
```

---

## 4. KONVENSI UI

### 4.1 Settings rows = kartu terpisah ⭐

Aturan paling penting untuk layar pengaturan:

- Setiap baris pengaturan = **SATU card mandiri yang terpisah**.
- Antar card diberi **gap/jarak kecil**.
- **DILARANG** satu card besar berisi banyak baris + divider.
- Card: corner radius **20.dp**, padding **16.dp**.
- Acuan visual: `AppThemeScreen.kt`.

```
✅ BENAR:  [ Card baris 1 ]  ← gap →  [ Card baris 2 ]  ← gap →  [ Card baris 3 ]
❌ SALAH:  [ Card besar berisi baris 1 ─── divider ─── baris 2 ─── divider ─── baris 3 ]
```

### 4.2 Material 3 Expressive

Gunakan token `MaterialTheme` (colorScheme, typography). Dilarang hardcode `Color(0x...)` untuk warna tema dan dilarang teks hardcode tanpa `stringResource` pada kode baru.

### 4.3 Bahasa UI

Bahasa Indonesia untuk seluruh teks yang tampil ke pengguna.

---

## 5. ATURAN KERJA DENGAN AI STUDIO

- Setiap perubahan spesifikasi diberikan dalam **SATU blok**: `files` / `perubahan` / `kode` / `efek`.
- Terapkan **persis seperti tertulis**. Jangan menambah, mengurangi, atau mengimprovisasi di luar instruksi.
- Jika beberapa spec berkaitan, gabungkan jadi satu blok prompt — jangan dipecah satu-satu.
- Jika sebuah item spec tidak bisa diterapkan (anchor tidak ketemu / ada referensi aktif), **lewati item itu** dan lanjut — jangan mengarang pengganti.
