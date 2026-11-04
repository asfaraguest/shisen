# AGENTS.md — Shisen (視線) Engineering & Collaboration Workflow

> [!IMPORTANT]
> **BACA SEBELUM BERUBAH:** Setiap agen/AI WAJIB membaca `AGENTS.md` ini dahulu, lalu `PROJECT_MAP.md`
> untuk peta topologi + indeks simbol, sebelum merencanakan atau mengubah kode.
> **WAJIB PONYTAIL MAX (ULTRA - ANTI-CEREWET & ANTI-BLOAT):**
> Setiap model AI WAJIB mengaktifkan dan mematuhi skill `ponytail` level **ultra/max** (`.agents/skills/ponytail/SKILL.md`) secara permanen di setiap sesi tanpa perlu diminta.
> 1. **Perubahan Kode Terkecil (*Smallest Working Diff*):** Selesaikan masalah dengan kode seminimal mungkin. Tolak abstraksi, wrapper, opsi ekstra, atau fitur "untuk nanti" (YAGNI). Deletion lebih baik daripada addition.
> 2. **Manfaatkan Apa yang Ada:** Gunakan helper, komponen, atau dependency yang sudah terpasang. Dilarang menambah dependency baru jika bisa diselesaikan native platform/kode yang ada.
> 3. **Tepat Sasaran & Tuntas:** Cari akar masalah via grep/indeks simbol, perbaiki sekali di kode bersama (shared code), dan perbarui semua caller yang terdampak. Jangan potong error handling kritis atau validasi batasan.
> 4. **Respon Super Ringkas:** Dilarang narasi panjang bertele-tele saat riset atau eksekusi. Berikan jawaban padat, to-the-point, dan dapat dipahami dalam sekali baca. Jika ada batas/hal yang diskip, tuliskan dalam 1-2 baris di akhir.

---

## 1. Identitas & Filosofi Proyek

**Shisen (視線)** adalah aplikasi Android native (100% Pure Java) untuk streaming anime, paritas backend Tatap.
Sumber konten: HiAnime, AniChan, Otakudesu, AniList, AniSkip, AnimeTosho, OpenSubtitles.

Prinsip:
- **Pure Native Java** — tanpa Python runtime/Chaquopy; performa tinggi, APK ringkas.
- **Paritas Tatap** — adopsi menu & logika backend Tatap (backend_reference hanya rujukan, jangan diubah).
- **Multi-source 3-tier** — HiAnime → AniChan → Otakudesu.
- **Rilis tunggal** — hanya asset `shisen-release-*.apk` yang dipublikasikan; jangan upload `menatap-release-*.apk`.
- **Guest-only** — tanpa login; hormati batas kualitas.

---

## 2. Struktur Direktori

Lihat `PROJECT_MAP.md`.

---

## 3. Kontrak Teknis Kunci (JANGAN diubah tanpa alasan kuat)

### 3.1 Subtitle Smart Merge (INVARIAN)
- `loadFromTracks()` SELALU jalur Smart Merge ID; opsi lain (English raw, ID raw) lewat `loadRawTrack()`.
- Setiap opsi dialog WAJIB set state sendiri (`setMode`) atau bypass `loadFromTracks` — cegah state bocor.
- Gap-fill (Signs/OP/ED) HANYA masuk jika ada terjemahan; jangan inject English mentah.
- Cache GTX HANYA disimpan jika terjemahan berhasil (>80% cue tidak identik dengan English).
- Premature English display DILARANG; gunakan loading indicator.

### 3.2 Streaming
- Sumber berjenjang HiAnime → AniChan → Otakudesu; jangan hapus fallback.
- AniList `nextAiringEpisode.episode - 1` untuk batas episode tayang.
- Auto-skip AniSkip v2.

### 3.3 UI
- Edge-to-edge transparent system bars; insets awareness.
- Trilingual (id/en/ja) via LocaleHelper.
- Subtitle styling/appearance.

---

## 4. Konvensi Kode

- **Bahasa komentar/dokumentasi:** Indonesia. **Nama variabel/fungsi:** Inggris.
- Tanpa komentar berlebihan — hanya untuk "mengapa" yang tidak jelas.
- Escape teks user sebelum innerHTML (N/A native; gunakan setText).
- I/O jaringan di background thread; jangan blokir UI thread.

---

## 5. Alur Kerja Perubahan

1. Baca `AGENTS.md` → `PROJECT_MAP.md`.
2. Cari simbol via grep/search dahulu.
3. Ubah kode + perbarui catatan commit (jika git aktif).
4. Jalankan verifikasi:
   - `cd android && ./gradlew compileDebugJavaWithJavac`
   - `cd android && ./gradlew assembleRelease`
5. Build & rilis hanya APK Shisen.

---

## 6. Catatan Rilis & Keamanan

- Build: `cd android && ./gradlew assembleRelease` → menghasilkan:
  - Mobile: `android/app/build/outputs/apk/mobile/release/shisen-release-*.apk`
  - Android TV: `android/app/build/outputs/apk/tv/release/shisen-tv-release-*.apk`
- Upload ke GitHub release HANYA `shisen-release-*.apk` dan `shisen-tv-release-*.apk` (gunakan `--clobber`).
- Jangan upload `menatap-release-*.apk`.
- **Aturan Permanen Tombol Download di `README.md` (WAJIB):**
  1. **Shisen Mobile (Stable):** Tombol download Mobile WAJIB selalu mengarah langsung ke file APK rilis stabil terakhir yang dipublikasikan (`https://github.com/asfaraguest/shisen/releases/download/vX.Y.Z/shisen-release-vX.Y.Z.apk`), jangan pernah ditimpa dengan versi beta.
  2. **Shisen TV (Beta):** Selama varian TV belum rilis stabil, tombol download Shisen TV WAJIB selalu mengarah langsung ke file APK beta terbaru (`https://github.com/asfaraguest/shisen/releases/download/vX.Y.Z-beta.N/shisen-tv-release-vX.Y.Z-beta.N.apk`).
- Aplikasi guest-only; tanpa telemetri; tidak membypass langganan berbayar.
- backend_reference adalah rujukan Tatap — jangan dimodifikasi.
