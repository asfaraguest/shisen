# Shisen (視線) — Android Native Anime Streaming App

<div align="center">
  <img src="./assets/shisen_icon_512.png" width="112" height="112" alt="Shisen Logo">
  <h1>Shisen (視線)</h1>
  <p>Pemain video anime asli Android, 100% Native Java, non-explosif, tanpa login, tanpa telemetri.</p>

  <p>
    <a href="https://github.com/asfaraguest/shisen/releases/latest"><img src="https://img.shields.io/github/v/release/asfaraguest/shisen?style=flat-square&color=00dbeb&label=Rilis" alt="Release"></a>
    <a href="https://somsubhra.github.io/github-release-stats/?username=asfaraguest&repository=shisen" target="_blank" title="Lihat Total Unduhan Lengkap"><img src="https://img.shields.io/github/downloads/asfaraguest/shisen/total?style=flat-square&color=3fb950&label=Statistik%20Unduhan" alt="Statistik Unduhan"></a>
    <a href="https://github.com/asfaraguest/shisen/stargazers"><img src="https://img.shields.io/github/stars/asfaraguest/shisen?style=flat-square&logo=github&color=e3b341&label=Stars" alt="Stars"></a>
    <a href="https://github.com/asfaraguest/shisen/network/members"><img src="https://img.shields.io/github/forks/asfaraguest/shisen?style=flat-square&logo=github&color=58a6ff&label=Forks" alt="Forks"></a>
    <a href="https://hits.sh/github.com/asfaraguest/shisen/"><img src="https://hits.sh/github.com/asfaraguest/shisen.svg?style=flat-square&label=Kunjungan&color=00dbeb" alt="Views"></a>
    <a href="LICENSE"><img src="https://img.shields.io/badge/Lisensi-GPL--3.0-blue.svg?style=flat-square" alt="License"></a>
  </p>

  <p>
    <a href="https://github.com/asfaraguest/shisen/releases/latest/download/shisen-release-v0.1.0-beta.6.apk"><img src="https://img.shields.io/badge/Download-Android%20Mobile-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Mobile APK"></a>
  </p>

  <p>
    <a href="https://trakteer.id/bagus_azizul" target="_blank"><img src="https://img.shields.io/badge/Trakteer-Dukung%20Karya-be1e2d?style=flat-square" alt="Trakteer"></a>
    <a href="https://ko-fi.com/L5J028GNTA" target="_blank"><img src="https://img.shields.io/badge/Ko--fi-Beli%20Kopi-FF5E5B?style=flat-square&logo=ko-fi&logoColor=white" alt="Ko-fi"></a>
  </p>
</div>

---

## 🎬 Keunggulan Utama

- **100% Pure Native Java Stack:** Tanpa Python runtime, hemat RAM (~80% lebih efisien), cold start < 180ms.
- **Multi-source 3-tier Fallback:** HiAnime → AniChan → Otakudesu (mantan Tatap) untuk pengalaman streaming tanpa putus.
- **Paritas menu Tatap:** Semua 7 menu utama (`Sekarang`, `Sedang Tayang`, `Jadwal Rilis`, `Riwayat`, `Favorit`, `Katalog`, `Genre`) sama persis dengan backend Tatap.
- **Cepat & Ringkas:** Saat ini APK ~5 MB, arsitektur minimalis (native Android, SQLite, OkHttp, jsoup, Media3 ExoPlayer).
- **Guest-only:** Tanpa login, tanpa telemetri, tanpa iklan, tanpa tracking.
- **Transparent UI:** Edge-to-edge dengan insets awareness, tema Obsidian modern (dark mode) & Atom (light mode) dengan kontras tinggi.
- **Auto-skip AniSkip v2:** Deteksi & lewati opening/ending otomatis via tombol `OK` pada remote TV atau keyboard.
- **Intelligent Rate Limiting:** Token bucket (burst 5, refill 24/menit, max 2 parallel, gap 300ms) + cooldown bertahap (60→120→300 detik) pada 429.
- **Stale-While-Revalidate Cache:** Cache SQLite persisten (TTL per jenis), tanpa cache kosong saat cooldown.
- **Playback pemulihan:** Simpan pemetaan slug/judul -> anilistId/MalId saat resolve sukses, AniChan/AniSkip tetap jalan saat cooldown.

---

## 🌟 Fitur

### 🔍 **Penanganan 429 (Rate Limiting)**
- Token bucket: burst 5, refill 24/menit (80% dari 20), max 2 parallel, gap 300ms
- Cooldown bertahap: 60→120→300 detik, reset setelah sukses
- Parse Retry-After & X-RateLimit-Reset dari header 429
- Body 429 HTML/kosong: tidak crash, jangan log query/variabel
- Cache hit/miss: logger SHISEN_PERF (DEBUG-only)

### 📡 **Request Pengurangan**
- Minimal 3 karakter pencarian (debounce 500ms)
- Dedup in-flight: request identik paralel -> 1 request jaringan
- Prefetch dibuang saat bucket kosong
- Batches alias GetMasihTayangAll (sudah ada)

### 💾 **Cache & Pemetaan**
- TTL per jenis: detail 24 jam, search 1 jam, relations 7 hari, mapping 30 hari
- Stale-while-revalidate: tampil cache expired, refresh background
- Pemetaan slug→anilistId/malId untuk playback saat cooldown

### 📺 **Antarmuka**
- Edge-to-edge system bars, insets awareness
- Trilingual: Indonesia (default), English, 日本語, Spanyol (baru!)
- Media3 ExoPlayer, gesture-based controls
- Player bottom sheet, sink, dan floating controls

### 🔄 **Integration Tatap**
- **Paritas menu:** 7 menu yang sama persis dengan backend Tatap
- **Pemetaan:** slug→anilistId/malId (TTL 30 hari)
- **Fallback:** AniChan/AniSkip terus jalan saat cooldown

### 🛡️ **Keamanan & Privasi**
- Tanpa login, tanpa telemetri
- Autentikasi BYOK (Bearer Token)
- Endpoint OTA legal
- No storage requirement

---

## Unduh

**APK rilis resmi (signed, production keystore):**
- [GitHub Releases](https://github.com/asfaraguest/shisen/releases) — `shisen-release-v0.1.Now](https://github.com/asfaraguest/shisen/releases/latest/download/shisen-release-v0.1.0-beta.6.apk)

> **Peringatan:** Hanya asset `shisen-release-*.apk` yang dipublikasikan. `menatap-release-*.apk` tidak di-upload.

**Build Lokal:**
```bash
cd android
./gradlew assembleRelease
```

**Checklist Unduh:** 5.0 MB, versionCode 10006, testDebugUnitTest passed

---

## Topik

`android` `java` `anime` `streaming` `media3` `exoplayer` `okhttp` `jsoup` `anilist` `native` `kotlin-free`

---

## Trakteer & Dukungan

<p align="left">
  <a href="https://trakteer.id/bagus_azizul" target="_blank">
    <img src="https.
    <img src="https://img.shields.io/badge/Trakteer-Dukung%20Karya-be1e2d?style=for-the-badge" height="38" alt="Dukung di Trakteer">
  </a>
  &nbsp;
  <a href="https://ko-fi.com/L5J028GNTA" target="_blank">
    <img src="https://img.shields.io/badge/Ko--fi-Beli%20Kopi-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white" height="38" alt="Support di Ko-fi">
  </a>
</p>

---

## Referensi

- [Tatap](https://github.com/asfaraguest/tatap) — untuk struktur menu & logika backend
- [Media3](https://developer.android.com/topic/libraries/media3) — video streaming
- [OkHttp](https://square.github.io/okhttp/) — HTTP client
- [jsoup](https://jsoup.org/) — HTML parsing
- [SQLite](https://developer.android.com/guide/database) — local persistence

## Catatan Versi

- Sebagian besar fitur dikembangkan di Android (Pure Native Java)
- Integrasi backend Tatap (paritas menu & logika) — **tidak dimodifikasi**
- Rilis APK dirilis atas izin developer Tatap & contributor
- Apresiasi: komunitas pengembang independen, kontributor independen, dan semua pengguna
