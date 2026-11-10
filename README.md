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
    <a href="https://github.com/asfaraguest/shisen/releases/download/v1.1.0/shisen-release-v1.1.0.apk"><img src="https://img.shields.io/badge/Download-Shisen%20Mobile%20(Stable)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Download Shisen Mobile Stable APK"></a>
    <br>
    <a href="https://github.com/asfaraguest/shisen/releases/download/v1.1.0/shisen-tv-release-v1.1.0.apk"><img src="https://img.shields.io/badge/Download-Shisen%20TV%20(Stable)-00B4D8?style=for-the-badge&logo=androidtv&logoColor=white" alt="Download Shisen TV Stable APK"></a>
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
  - **Mobile (Stable):** [`shisen-release-v1.1.0.apk`](https://github.com/asfaraguest/shisen/releases/download/v1.1.0/shisen-release-v1.1.0.apk)
  - **Android TV (Stable):** [`shisen-tv-release-v1.1.0.apk`](https://github.com/asfaraguest/shisen/releases/download/v1.1.0/shisen-tv-release-v1.1.0.apk)

> Asset rilis resmi: `shisen-release-*.apk` dan `shisen-tv-release-*.apk`.

---

## Panduan Lengkap Instalasi di Android TV

Karena aplikasi Shisen didistribusikan langsung dalam bentuk berkas APK (sideload), berikut panduan terperinci langkah demi langkah untuk memasang Shisen pada perangkat Android TV, Google TV, atau Android TV Box (seperti Coocaa, Xiaomi TV, Mi TV Stick, Chromecast with Google TV, Realme TV, dsb).

### Langkah 1: Mengaktifkan Izin Instalasi Sumber Tidak Dikenal (Wajib)
Secara bawaan, Android TV membatasi pemasangan aplikasi dari luar Google Play Store untuk keamanan. Anda perlu mengizinkan aplikasi perantara (File Manager atau Downloader) agar dapat memasang berkas APK:
1. Nyalakan TV Anda dan buka menu **Setelan (Settings)** (ikon roda gigi di pojok kanan atas).
2. Masuk ke:
   - **Android TV standar (versi 9 / 10 / 11):** Pilih **Preferensi Perangkat (Device Preferences)** > **Keamanan & Pembatasan (Security & Restrictions)** > **Sumber Tidak Dikenal (Unknown Sources)** atau **Instal aplikasi yang tidak dikenal (Install unknown apps)**.
   - **Google TV / Android 12+:** Pilih **Aplikasi (Apps)** > **Akses aplikasi khusus (Special app access)** > **Instal aplikasi tidak dikenal (Install unknown apps)**.
3. Cari nama aplikasi yang akan Anda gunakan untuk memasang APK (misal: *Send Files to TV*, *File Commander*, *AnExplorer*, atau *Downloader*), lalu geser sakelar ke posisi **Aktif (Izinkan / Allowed)**.

---

### Metode A: Menggunakan Aplikasi "Send Files to TV" (Paling Mudah & Cepat)
Metode ini direkomendasikan jika Anda memiliki smartphone Android atau laptop yang terhubung ke jaringan Wi-Fi yang sama dengan TV.

#### 1. Persiapan di Smartphone / Komputer:
- Unduh berkas APK khusus TV: [`shisen-tv-release-v1.1.0.apk`](https://github.com/asfaraguest/shisen/releases/download/v1.1.0/shisen-tv-release-v1.1.0.apk).
- Pasang aplikasi **Send Files to TV (SFTV)** dari Google Play Store di smartphone Anda.

#### 2. Persiapan di Android TV:
- Buka **Google Play Store** di Android TV Anda.
- Cari dan pasang **Send Files to TV**.
- Pasang juga satu aplikasi pengelola berkas jika TV belum memilikinya, contohnya: **File Commander**, **AnExplorer**, atau **FX File Explorer**.

#### 3. Proses Pengiriman & Pemasangan:
1. Pastikan smartphone dan Android TV terhubung ke **jaringan Wi-Fi yang sama**.
2. Buka aplikasi *Send Files to TV* di TV, lalu pilih opsi **RECEIVE** (layar TV akan menunggu kiriman berkas).
3. Buka aplikasi *Send Files to TV* di smartphone, pilih opsi **SEND**, lalu navigasikan ke folder unduhan dan pilih berkas `shisen-tv-release-v1.1.0.apk`.
4. Pilih nama Android TV Anda dari daftar perangkat yang muncul di layar HP.
5. Tunggu proses transfer selesai (biasanya hanya memakan waktu 5–10 detik).
6. Buka aplikasi **File Manager** di TV Anda, masuk ke memori internal > folder **Download**.
7. Klik pada berkas `shisen-tv-release-v1.1.0.apk`, lalu tekan tombol **Instal (Install)**.
8. Setelah selesai, pilih **Buka (Open)** untuk langsung menikmati Shisen TV.

---

### Metode B: Menggunakan Aplikasi "Downloader" (Langsung di TV Tanpa HP)
Metode ini cocok jika Anda tidak ingin menggunakan smartphone atau flashdisk dan ingin mengunduh langsung dari TV.

1. Buka **Google Play Store** di Android TV, cari aplikasi bernama **Downloader by AFTVnews**, lalu pasang.
2. Buka aplikasi Downloader, berikan izin akses penyimpanan saat diminta.
3. Masuk ke menu **Home** atau **Browser** pada bilah samping kiri Downloader.
4. Pada kolom isian URL, ketik alamat halaman rilis Shisen:
   ```text
   https://github.com/asfaraguest/shisen/releases
   ```
5. Navigasikan kursor ke bagian rilis terbaru (`v1.1.0`), lalu klik pada tautan berkas **`shisen-tv-release-v1.1.0.apk`**.
6. Aplikasi Downloader akan mulai mengunduh file APK.
7. Setelah unduhan 100% selesai, jendela konfirmasi instalasi akan otomatis muncul di layar TV.
8. Pilih tombol **Install**. Jika muncul permintaan izin sumber tidak dikenal, klik *Settings* lalu izinkan Downloader untuk menginstal aplikasi, kemudian tekan tombol kembali di remote dan klik **Install** sekali lagi.
9. Tekan **Done** atau **Open**. Anda bisa memilih opsi *Delete* pada Downloader untuk menghapus berkas installer APK agar menghemat memori internal TV.

---

### Metode C: Menggunakan Flashdisk USB (Tanpa Jaringan Internet Eksternal)
Metode ini praktis jika Anda lebih nyaman mengunduh lewat PC/laptop atau koneksi Wi-Fi TV lambat.

1. Unduh berkas [`shisen-tv-release-v1.1.0.apk`](https://github.com/asfaraguest/shisen/releases/download/v1.1.0/shisen-tv-release-v1.1.0.apk) di komputer atau laptop Anda.
2. Format flashdisk Anda ke sistem berkas **FAT32** atau **exFAT** (format standar agar terbaca oleh Android TV).
3. Salin (*copy-paste*) file APK tersebut ke dalam flashdisk (sebaiknya letakkan di folder luar/root flashdisk agar mudah dicari).
4. Cabut flashdisk dengan aman dari komputer, lalu colokkan ke port USB di Android TV atau Android Box Anda.
5. Pada Android TV, pasang aplikasi **File Manager** (seperti *File Commander* atau *AnExplorer*) dari Play Store jika belum tersedia.
6. Buka File Manager, pilih drive penyimpanan USB Anda.
7. Cari dan klik berkas `shisen-tv-release-v1.1.0.apk`.
8. Tekan tombol **Instal** dan tunggu hingga proses pemasangan selesai.

---

### Metode D: Menggunakan Wireless ADB (Khusus Pengembang / Pengguna Lanjutan)
Jika Anda terbiasa dengan terminal/ADB:
1. Aktifkan **Opsi Pengembang (Developer Options)** di TV:
   - Buka *Setelan > Preferensi Perangkat > Tentang (About)*.
   - Tekan opsi **Build Number** sebanyak 7 kali berturut-turut menggunakan remote hingga muncul teks *"Anda kini seorang pengembang!"*.
2. Masuk ke *Opsi Pengembang*, lalu aktifkan opsi **USB Debugging** dan **Network Debugging / Wireless Debugging**.
3. Cek alamat IP lokal TV Anda pada *Setelan > Jaringan & Internet > Wi-Fi terhubung* (contoh: `192.168.1.50`).
4. Buka terminal di PC/laptop Anda (pastikan berada di jaringan Wi-Fi yang sama) dan jalankan:
   ```bash
   adb connect 192.168.1.50:5555
   adb install -r shisen-tv-release-v1.1.0.apk
   ```
5. Aplikasi Shisen TV akan langsung terpasang dan muncul di layar utama TV Anda.

---

### Tips Tambahan & Pemecahan Masalah
- **Penting — Gunakan Varian TV:** Pastikan berkas APK yang Anda pasang memiliki kata `tv` pada namanya (`shisen-tv-release-*.apk`). Varian mobile (`shisen-release-*.apk`) ditujukan untuk layar sentuh ponsel dan tidak memiliki tata letak navigasi remote D-Pad Android TV.
- **Icon Aplikasi di Home Screen:** Shisen TV sudah dilengkapi dengan Leanback Launcher Banner resmi resolusi tinggi. Setelah dipasang, ikon Shisen TV akan otomatis muncul di baris aplikasi utama TV Anda. Jika belum terlihat, cari di daftar *Semua Aplikasi (All Apps)* atau tambahkan secara manual ke baris favorit (*Add to favorites*).
- **Tombol Remote TV:** Pemutar video Shisen TV dirancang khusus untuk remote fisik TV:
  - **D-Pad Tengah / Enter:** Membuka overlay kontrol atau Play/Pause.
  - **D-Pad Kiri / Kanan:** Langsung melompat (seek) mundur/maju 10 detik.
  - **Tombol Back:** Langsung menutup dialog atau kembali ke halaman detail anime.
- **Pembaruan Aplikasi Berikutnya:** Shisen TV dilengkapi dengan fitur *In-App Update Checker*. Jika terdapat versi rilis baru di masa mendatang, aplikasi akan menampilkan dialog pembaruan otomatis yang bisa diunduh langsung tanpa perlu instal manual ulang.

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
- `android/app/build/outputs/apk/mobile/release/shisen-release-v1.1.0.apk`
- `android/app/build/outputs/apk/tv/release/shisen-tv-release-v1.1.0.apk`

---

## Bahasa Dukungan

- Indonesia (default)
- English
- 日本語
- Español

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
