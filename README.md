# Shisen (視線) — Android Native Anime Streaming App

**Shisen (視線 • シセン)** adalah aplikasi streaming anime native Android (100% Pure Java) dengan arsitektur modular, multi-source fallback, dan UI minimalis.

---

## Kredit

- **Backend referensi:** [Tatap](https://github.com/asfaraguest/tatap) — untuk struktur menu & logika backend
- **Sumber konten:** HiAnime, AniChan, Otakudesu, AniList, AniSkip, AnimeTosho, OpenSubtitles
- **Library utama:** Media3 ExoPlayer, OkHttp, jsoup, SQLite (Android native)

---

## Fitur Utama

- **Pure Native Java** — tanpa Python runtime/Chaquopy; APK ~5 MB
- **Multi-source 3-tier** — HiAnime → AniChan → Otakudesu (smart fallback)
- **Paritas menu Tatap** — Musim Ini, Sedang Tayang, Jadwal Rilis, Riwayat, Favorit, Katalog, Genre
- **AniList integration** — jadwal rilis, relasi musim/film, auto-skip (AniSkip v2)
- **Guest-only** — tanpa login, tanpa telemetri
- **Edge-to-edge UI** — transparent system bars, insets-aware
- **Trilingual** — Indonesia / English / 日本語 / Español (via LocaleHelper)

---

## Unduhan

APK rilis resmi (signed, production keystore):
- [GitHub Releases](https://github.com/asfaraguest/shisen/releases) — `shisen-release-v0.1.0-beta.6.apk`

> Hanya asset `shisen-release-*.apk` yang dipublikasikan. `menatap-release-*.apk` tidak di-upload.

---

## Build Lokal

```bash
cd android
./gradlew assembleRelease
```

Output: `android/app/build/outputs/apk/release/shisen-release-v0.1.0-beta.6.apk`

---

## Topics

`android` `java` `anime` `streaming` `media3` `exoplayer` `okhttp` `jsoup` `anilist` `native` `kotlin-free`