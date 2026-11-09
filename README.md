# Shisen (視線) — Android Native Anime Streaming App

<div align="center">
  <img src="./shisen_icon_512.png" width="112" height="112" alt="Shisen Logo">
  <h1>Shisen (視線)</h1>
  <p>Aplikasi pemutar video anime native Android. Tanpa login, tanpa telemetri, tanpa iklan.</p>

  <p>
    <a href="https://github.com/asfaraguest/shisen/releases/latest"><img src="https://img.shields.io/github/v/release/asfaraguest/shisen?style=flat-square&color=00dbeb&label=Rilis" alt="Release"></a>
    <a href="https://github.com/asfaraguest/shisen/stargazers"><img src="https://img.shields.io/github/stars/asfaraguest/shisen?style=flat-square&logo=github&color=e3b341&label=Stars" alt="Stars"></a>
    <a href="https://github.com/asfaraguest/shisen/network/members"><img src="https://img.shields.io/github/forks/asfaraguest/shisen?style=flat-square&logo=github&color=58a6ff&label=Forks" alt="Forks"></a>
    <a href="LICENSE"><img src="https://img.shields.io/badge/Lisensi-GPL--3.0-blue.svg?style=flat-square" alt="License"></a>
  </p>

  <p>
    <a href="https://github.com/asfaraguest/shisen/releases/download/v1.0.1/shisen-release-v1.0.1.apk"><img src="https://img.shields.io/badge/Download-Shisen%20Mobile%20(Stable)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Download Shisen Mobile Stable APK"></a>
    <br>
    <a href="https://github.com/asfaraguest/shisen/releases/download/v1.1.0-beta.27/shisen-tv-release-v1.1.0-beta.27.apk"><img src="https://img.shields.io/badge/Download%20Beta-Shisen%20TV%20(Android%20TV)-00B4D8?style=for-the-badge&logo=androidtv&logoColor=white" alt="Download Shisen TV Beta APK"></a>
  </p>

  <p>
    <a href="https://trakteer.id/bagus_azizul" target="_blank"><img src="https://img.shields.io/badge/Trakteer-Dukung%20Karya-be1e2d?style=flat-square" alt="Trakteer"></a>
    <a href="https://ko-fi.com/L5J028GNTA" target="_blank"><img src="https://img.shields.io/badge/Ko--fi-Beli%20Kopi-FF5E5B?style=flat-square&logo=ko-fi&logoColor=white" alt="Ko-fi"></a>
  </p>
</div>

---

## Tentang

Shisen adalah aplikasi streaming anime native Android yang dirancang sederhana dan ringan. Dibangun dengan 100% Pure Native Java (tanpa Python runtime), menggunakan Media3 ExoPlayer, OkHttp, dan SQLite.

App bersifat **guest-only**: tidak perlu login, tidak ada tracking, dan tidak menyimpan data pengguna di server.

---

## Menu Utama

- **Sekarang** — daftar anime rilis musiman
- **Sedang Tayang** — anime yang tayang saat ini
- **Jadwal Rilis** — jadwal episode airing
- **Riwayat** — riwayat tontonan
- **Favorit** — anime yang disimpan
- **Katalog** — daftar anime lengkap
- **Genre** — filter genre anime

---

## Sumber Konten

Aplikasi mencoba beberapa sumber streaming anime secara berjenjang untuk mendapatkan ketersediaan stream yang stabil:

- HiAnime
- AniChan
- Otakudesu

Integrasi backend mengikuti paritas menu dari [Tatap](https://github.com/asfaraguest/tatap). Kode backend tersebut **tidak dimodifikasi**.

---

## Unduh

APK rilis resmi (signed, production keystore):

- [GitHub Releases](https://github.com/asfaraguest/shisen/releases):
  - **Mobile (Stable):** [`shisen-release-v1.0.1.apk`](https://github.com/asfaraguest/shisen/releases/download/v1.0.1/shisen-release-v1.0.1.apk)
  - **Android TV (Beta):** [`shisen-tv-release-v1.1.0-beta.27.apk`](https://github.com/asfaraguest/shisen/releases/download/v1.1.0-beta.27/shisen-tv-release-v1.1.0-beta.27.apk)

> Asset rilis resmi: `shisen-release-*.apk` dan `shisen-tv-release-*.apk`.

---

## Build Lokal

```bash
cd android
# Build kedua APK (Mobile & TV):
./gradlew assembleRelease

# Atau build salah satu varian:
./gradlew assembleMobileRelease
./gradlew assembleTvRelease
```

Output akan tersedia di:
- `android/app/build/outputs/apk/mobile/release/shisen-release-v1.1.0-beta.3.apk`
- `android/app/build/outputs/apk/tv/release/shisen-tv-release-v1.1.0-beta.27.apk`

---

## Bahasa Dukungan

- Indonesia (default)
- English
- 日本語
- Español (baru)

---

## Topik

`android` `java` `anime` `streaming` `media3` `native` `kotlin-free`

---

## Dukungan

- [Trakteer](https://trakteer.id/bagus_azizul)
- [Ko-fi](https://ko-fi.com/L5J028GNTA)

---

## Lisensi

[GPL-3.0](LICENSE)

---
