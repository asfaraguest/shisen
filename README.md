# Menatap — Pure Native Java Mobile Anime Streaming

**Menatap** adalah aplikasi pemutar dan agregator anime mandiri yang dirancang khusus untuk platform **Android Mobile**. Dibangun dengan arsitektur **100% Pure Native Java** (tanpa dependensi Python runtime atau server lokal), Menatap menghadirkan performa secepat kilat, efisiensi konsumsi daya baterai, dan ukuran unduhan yang sangat ringkas (< 8 MB).

---

## ❄ Karakteristik Utama

- **100% Pure Native Java Stack:** Menghilangkan overhead runtime Python Chaquopy, memangkas penggunaan RAM hingga 80% (~50MB) dan mempercepat cold start (< 180ms).
- **Cold-Frost & Deep Obsidian Design:** Antarmuka modern yang tenang dan elegan dengan kanvas obsidian dalam (`#080C14`), permukaan kaca es (`#0F1726`), aksen sian glasial (`#00F2FE` -> `#4FACFE`), dan pendaran lavender es (`#818CF8`).
- **Ikon Aplikasi Khas Hyouka:** Visual tatapan anime ekspresif dengan color grading Kyoto Animation (kilau pupil kristal Amethyst & Emerald dengan pencahayaan sinematik lembut).
- **AniList GraphQL Integration:** Katalog anime komprehensif (Trending, Sedang Tayang, Terpopuler, Jadwal Rilis Mingguan WIB).
- **Native Otakudesu & Stream Resolver:** Parsing HTML berbasis Jsoup berkecepatan tinggi dengan dekripsi native Java AES-256-GCM.
- **Auto-Skip Intro/Outro (AniSkip v2):** Deteksi dan lewati opening/ending secara otomatis atau via floating frosted button.
- **Media3 ExoPlayer High-Performance Video:** Dukungan streaming HLS, gesture swipe volume/brightness dengan float accumulator, Watch Portrait View, dan Picture-in-Picture (PiP).
- **Zero Public Server / Standalone Client:** Berjalan mandiri langsung di perangkat tanpa memerlukan server perantara publik.

---

## 📱 Unduhan Rilis Resmi (Android Mobile Only)

Unduh berkas rilis resmi bertanda tangan dari [Halaman Rilis GitHub](https://github.com/asfaraguest/menatap/releases):
- 📱 **`menatap-release-v1.0.0.apk`** (Official Signed Mobile APK via production Keystore)

---

## 🛠 Panduan Build Lokal

```bash
cd android
./gradlew assembleRelease
```
Berkas APK bertanda tangan akan tersedia di:
`android/app/build/outputs/apk/release/menatap-release-v1.0.0.apk`
