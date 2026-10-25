# Changelog

Semua perubahan pada Menatap didokumentasikan di berkas ini mengikuti kaidah [Semantic Versioning 2.0.0](https://semver.org/).

## [0.1.0-beta.1] - 2026-10-25

### Ditambahkan
- **Paritas Katalog Musiman Beranda & Masih Tayang (Dual-Lane)**:
  - **Beranda (Musim Ini)**: Menampilkan daftar anime yang tayang pada musim saat ini (`getCurrentSeasonAiring`) dengan Spotlight Hero anime terpopuler di halaman 1, mengecualikan serial panjang agar Beranda murni anime musiman aktif.
  - **Masih Tayang (Airing)**: Mengadopsi arsitektur Dual-Lane Tatap OG yang memisahkan anime ongoing dari 2 musim sebelumnya (`ANIME 2 MUSIM SEBELUMNYA (ONGOING)`) dan serial panjang berkelanjutan (`SERIAL PANJANG BERKELANJUTAN`) via header pemisah jalur (`item_lane_header`). Dilengkapi sub-filter chip (*Semua Dual-Lane*, *2 Musim Sebelumnya*, *Serial Panjang*).
- **Mode Picture-in-Picture (PiP) Lengkap**:
  - Tombol aksi PiP pada panggung kontrol ExoPlayer (`exo_vr`) dan quick action bar pada Watch Portrait View (`btn_portrait_pip`).
  - Dukungan aspek rasio video native dinamis via `PictureInPictureParams.Builder.setAspectRatio(Rational)` dan animasi halus `setSourceRectHint`.
  - Kontrol remote PiP interaktif: Mundur 10 detik, Putar/Jeda, dan Maju 10 detik via `RemoteAction` & `BroadcastReceiver`.
  - Transisi otomatis ke PiP saat pengguna menekan tombol Home atau berpindah aplikasi (`onUserLeaveHint`).
  - Penanganan lifecycle cerdas di `onPause()`, `onStop()`, dan `onResume()` agar pemutaran video tidak terhenti saat masuk mode PiP.
- **Bilah Paginasi Melayang (Floating Transparent Pagination Bar)**:
  - Bilah navigasi paginasi khas Tatap di bagian tengah bawah (`btn_prev_page`, `tv_page_indicator`, `btn_next_page`).
  - Berfungsi penuh di seluruh tampilan: Musim Ini (Beranda), Masih Tayang (Airing), Riwayat Tontonan, Anime Favorit, Katalog Lengkap, Kategori Genre, dan Hasil Pencarian.
  - Reset otomatis nomor halaman (`currentPage = 1`) saat berganti tab, filter sub-kategori, atau kata kunci pencarian.

### Diubah & Diperbaiki
- **Penyempurnaan Tombol Aksi Watch Portrait View**:
  - Menyamakan proporsi kelima tombol kontrol cepat (`btn_portrait_sub`, `btn_portrait_quality`, `btn_portrait_speed`, `btn_portrait_pip`, `btn_portrait_fullscreen`) dengan distribusi bobot rata (`weight="1"`), menghapus inset default MaterialButton, dan menerapkan tinggi seragam `34dp` berbentuk kapsul presisi.
  - Memperbaiki tombol pengurutan episode `btn_portrait_sort` dengan padding ringkas tanpa pemborosan vertikal.
- **Pembersihan Header ExoPlayer**:
  - Menyembunyikan tombol pil (`btn_aspect`, `btn_quality`, `btn_speed`, `btn_sub_selector`) dari bilah atas (`layout_top_bar`) agar header pemutar video bersih dan tidak berantakan (hanya menampilkan judul episode dan status subtitle), persis seperti Tatap OG.
- **Penyelarasan Ukuran Menu Lipat Melayang (Folded Floating Menu)**:
  - Menyamakan lebar seluruh 8 tombol menu (`layout_width="match_parent"` di dalam kontainer `230dp`) agar simetris, presisi, dan tidak bertingkat-tingkat ukurannya.
- **Desain Ulang Menu Aktif (High-Contrast Solid Frost)**:
  - Mengganti latar belakang semi-transparan (10-17%) pada `bg_folded_menu_pill_active.xml` dengan kartu solid 100% opak `#FF1A2C47` berbingkai sian glasial tebal `1.8dp` (`#38BDF8`), menghasilkan kontras tajam yang elegan dan nyaman di mata.
