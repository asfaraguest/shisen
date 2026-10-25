# Menatap — Pure Native Java Mobile Anime Streaming

**Menatap** adalah aplikasi pemutar dan agregator anime mandiri yang dirancang khusus untuk platform **Android Mobile**. Dibangun dengan arsitektur **100% Pure Native Java** (tanpa dependensi Python runtime atau server lokal), Menatap menghadirkan performa secepat kilat, efisiensi konsumsi daya baterai, dan ukuran unduhan yang sangat ringkas (< 9 MB).

---

## ❄ Karakteristik Utama

- **100% Pure Native Java Stack:** Menghilangkan overhead runtime Python Chaquopy, memangkas penggunaan RAM hingga 80% dan mempercepat cold start (< 180ms).
- **Paritas Penuh Backend Tatap:** Mengadopsi seluruh 7 menu utama Tatap (*Musim Ini, Sedang Tayang, Jadwal Rilis 7 Hari WIB, Riwayat, Favorit, Katalog Lengkap, dan 14 Kategori Genre*).
- **Multi-Source Streaming & Smart Fallback 3-Tier:**
  1. 🥇 **HiAnime (`hianime.at`):** Master HLS CDN kualitas tinggi dengan dekripsi AES-128-CBC Megaplay dan multi-subtitle.
  2. 🥈 **AniChan (`anichan.to`):** Direct HLS Stream resolver menggunakan AniList ID + dekripsi AES-GCM.
  3. 🥉 **Otakudesu (`otakudesu.cloud`):** Hardsub Indonesia via dekripsi Putarin AES-256-GCM sebagai safety net cadangan.
- **Pencegahan Bug Episode On-going:** Menampilkan daftar episode riil yang sudah tayang, dibatasi oleh `nextAiringEpisode.episode - 1` dari AniList (zero un-aired phantom episodes).
- **Cold-Frost & Deep Obsidian Design:** Antarmuka modern yang tenang dan elegan dengan kanvas obsidian dalam (`#080C14`), permukaan kaca es (`#0F1726`), aksen sian glasial (`#00F2FE` -> `#4FACFE`), dan pendaran lavender es (`#818CF8`).
- **Ikon Aplikasi Khas Hyouka:** Visual tatapan anime ekspresif dengan color grading Kyoto Animation (kilau pupil kristal Amethyst & Emerald dengan pencahayaan sinematik lembut).
- **Auto-Skip Intro/Outro (AniSkip v2):** Deteksi dan lewati opening/ending secara otomatis atau via floating frosted button.
- **Media3 ExoPlayer High-Performance Video:** Dukungan streaming HLS, gesture swipe volume/brightness dengan float accumulator, Watch Portrait View, dan Picture-in-Picture (PiP).

---

## 📱 Unduhan Rilis Resmi (Android Mobile Only)

Unduh berkas rilis resmi bertanda tangan dari [Halaman Rilis GitHub](https://github.com/asfaraguest/menatap/releases):
- 📱 **`menatap-release-v0.1.0-beta.1.apk`** (Official Signed Mobile APK via production Keystore)

---

## 🛠 Panduan Build Lokal

```bash
cd android
./gradlew assembleRelease
```
Berkas APK bertanda tangan akan tersedia di:
`android/app/build/outputs/apk/release/menatap-release-v0.1.0-beta.1.apk`
