# Changelog

Semua perubahan pada Menatap didokumentasikan di berkas ini mengikuti kaidah [Semantic Versioning 2.0.0](https://semver.org/).

## [0.1.0-beta.2] - 2026-10-25

### Ditambahkan
- **Harmonisasi Pewarnaan System UI & Indikator Sistem Bilah Atas (`SystemUiHelper.java`, `styles.xml`, `AnimeThemeManager.java`, `MainActivity.java`, `DetailActivity.java`, `PlayerActivity.java`, `SettingsDialog.java`)**:
  - **Sinkronisasi Warna Status Bar & Navigation Bar**: Mengintegrasikan utilitas baru `SystemUiHelper` yang secara dinamis menyelaraskan warna status bar dan navigation bar dengan palet warna kanvas tema Shisen yang sedang aktif (Shinkai `#0A0D18`, Sakura `#120B18`, Cyber `#080B14`, Ghibli `#081412`).
  - **Indikator Sistem Putih/Terang Kontras Tinggi (Anti-Blackout)**: Menyetel `WindowInsetsControllerCompat.setAppearanceLightStatusBars(false)` dan `setAppearanceLightNavigationBars(false)` serta konfigurasi tema dasar `<item name="android:windowLightStatusBar">false</item>` dan `<item name="android:windowLightNavigationBar">false</item>`. Menjamin ikon indikator sistem di bilah atas (jam, baterai, sinyal seluler, dan ikon Wi-Fi) selalu berwarna putih/terang yang kontras dan tajam di atas bilah status, mengeliminasi masalah indikator hitam yang tidak terbaca saat perangkat pengguna mengaktifkan Light Mode sistem OS.
  - **Reaktif Real-Time Terhadap Pergantian Tema**: Mengikat `SystemUiHelper.applySystemBarsTheme(activity, selectedTheme)` pada interaksi klik pemilihan tema di `SettingsDialog` dan `AnimeThemeManager`, sehingga status bar dan bilah navigasi langsung berganti warna seketika secara seamless tanpa perlu memuat ulang aplikasi.
  - **Dukungan Menyeluruh di Semua Layar & Pemutar Video**: Diterapkan secara konsisten di `MainActivity`, `DetailActivity`, dan `PlayerActivity` (baik pada mode portrait dengan visual canvas maupun saat transisi pemulihan dari mode lanskap fullscreen).

### Diperbaiki
- **Akurasi Tampilan & Isolasi Kueri Musim Sebelumnya pada Tab Sedang Tayang (`AniListClient.getMasihTayangDualLane`)**:
  - **Kueri Spesifik Musim Sebelumnya (`PREV`)**: Mengubah arsitektur pemanggilan sub-filter "Musim Sebelumnya" dari yang sebelumnya hanya memfilter array kueri global `status: RELEASING` (yang 95%-nya berisi anime musim berjalan sehingga hanya menyisakan 1-2 judul) menjadi kueri langsung `season: prev.season, seasonYear: prev.year, sort: POPULARITY_DESC`.
  - **Penyajian 35 Anime Penuh & Paginasi Bersih**: Pengguna kini disuguhkan 35 anime terpopuler musim sebelumnya per halaman secara penuh (seperti *Mushoku Tensei Season 3*, *Bleach TYBW*, *The Exiled Heavy Knight*, dll.) dengan badge status yang tepat (*Ongoing* untuk anime berlanjut dan *Tamat* untuk anime yang telah selesai).
  - **Kueri Multi-Node 3-Jalur pada Tampilan Semua (`ALL`)**: Mengoptimalkan tampilan default "Semua (3 Jalur)" menggunakan kueri multi-node GraphQL (`cur`, `prev`, `longMedia`) dalam 1 HTTP request sehingga ketiga section terisi lengkap, seimbang, dan bebas dari kebocoran antar-musim.

### Ditambahkan
- **Penyeragaman Lokalisasi Hero CTA & Tags Dinamis (`item_netflix_hero.xml`, `AnimeAdapter.java`, `strings.xml`)**:
  - Menghubungkan seluruh elemen Hero Card di Beranda (`TREN HARI INI` / `TRENDING TODAY` / `本日のトレンド`, `▶ Putar Sekarang` / `▶ Watch Now` / `▶ 今すぐ再生`, `ℹ Info Detail` / `ℹ Details` / `ℹ 詳細情報`, dan jumlah episode) dengan string resources dinamis multi-bahasa.
  - Memberikan ID resmi `tv_hero_trending_tag` pada label tren teratas dan memperbarui method `bindHero()` agar bereaksi seketika terhadap perubahan bahasa aktif.
- **Penyeragaman Lokalisasi Status Episode & Badge Kartu Anime (`AnimeAdapter.java`)**:
  - Mengganti teks status hardcoded bahasa Indonesia (`"Belum Tayang"`, `"Ongoing"`, `"Tamat"`, `"SERIAL PANJANG"`, `"Terakhir diputar: Ep X"`) dengan helper method `getLocalizedBadge()` dan `getLocalizedHeroEps()`.
  - Seluruh badge kartu anime (status penayangan, format serial, progres riwayat tontonan) kini secara otomatis mengikuti preferensi bahasa aktif pengguna (Bahasa Indonesia, English, dan 日本語).
- **Perbaikan Kontras Warna Tinggi WCAG AAA pada Tombol & Chip Pengaturan (`dialog_settings.xml`, `SettingsDialog.java`)**:
  - Memperbaiki kontras chip selektor bahasa aktif (`btn_lang_id`, `btn_lang_en`, `btn_lang_ja`): teks disetel hitam pekat `#080C14` di atas latar belakang sian glasial terang (`@color/accent_glacial`), mengeliminasi bug teks cyan di atas cyan yang tidak terbaca.
  - Memperbaiki tombol aksi simpan AI (`btn_settings_save_ai`): menggunakan teks hitam tebal `#080C14` di atas latar sian glasial.
  - Memperbaiki tombol hapus kunci (`btn_settings_clear_ai`) dan cek pembaruan (`btn_settings_check_update`): menggunakan latar elevated frosted `#161F38` dengan stroke kontras `#233252` dan teks putih cerah `#F1F5F9`.
  - Membersihkan duplikasi penutup tag XML pada tombol periksa pembaruan di `dialog_settings.xml`.
- **Dukungan Multi-Endpoint Update Checker & Fallback Repositori GitHub (`AppUpdateManager.java`)**:
  - Menerapkan rantai endpoint pembaruan terpadu: mencoba repositori utama `asfaraguest/shisen`, dan secara otomatis fallback ke `asfaraguest/menatap` jika endpoint pertama belum tersedia.
  - Memperbarui User-Agent ke `Shisen-Android` dan menyelaraskan seluruh dialog perizinan paket serta notifikasi toast dengan identitas resmi **Shisen**.
- **Sistem Trilingual & Language Switcher di Pengaturan Terpadu (`LocaleHelper.java`, `SettingsDialog.java`, `strings.xml`)**:
  - **Bahasa Indonesia Bersih Sebagai Default**: Menghilangkan istilah dan karakter kanji/kana Jepang yang sebelumnya terpasang di antarmuka default. Pengguna kini disuguhkan antarmuka Bahasa Indonesia yang bersih, alami, dan mudah dipahami.
  - **Selector 3 Bahasa (Trilingual)**: Menyediakan selektor bahasa interaktif di dalam modal Pengaturan Terpadu dengan pilihan:
    1. 🇮🇩 **Bahasa Indonesia** (Default aktif)
    2. 🇬🇧 **English** (Antarmuka bahasa Inggris penuh)
    3. 🇯🇵 **日本語** (Antarmuka bahasa Jepang bagi pengguna yang menginginkannya)
  - **Lokalisasi Menyeluruh**: Seluruh label navigasi melayang, lencana kategori, sub-filter jalur airing, jadwal hari (Senin-Minggu), indikator paginasi, aksi kontekstual, halaman detail anime, kontrol pemutar video, dan dialog pengaturan kini dilokalisasikan secara otomatis via `values/strings.xml`, `values-en/strings.xml`, dan `values-ja/strings.xml`.
  - **Penerapan Seamless & Persisten**: Preferensi bahasa disimpan di `SharedPreferences` (`tatap_player_prefs`) dan langsung diperbarui via `LocaleHelper` tanpa perlu me-restart perangkat.
  - **Nama Resmi Aplikasi**: Tetap konsisten mengusung nama resmi **Shisen (視線)**.
- **Harmonisasi Tampilan Floating Page Navigation Senada Floating Menu Button (`activity_main.xml`, `bg_floating_pagination.xml`)**:
  - **Proporsi & Penjajaran Sejajar (Horizontal Alignment)**: Menyelaraskan tinggi bilah navigasi paginasi melayang menjadi **48dp** dengan margin bawah **20dp** dan elevasi **18dp**, sehingga sejajar horizontal secara presisi dan senada dengan floating action button menu (`fab_menu_trigger`, tinggi 54dp, margin bawah 20dp, elevasi 18dp).
  - **Palet Obsidian Frosted Glass Senada**: Mengubah latar belakang `bg_floating_pagination` menggunakan obsidian glass `#E60F1726` dengan stroke sian glasial `1.5dp #8038BDF8` (identik dengan visual `bg_fab_trigger`).
  - **Ergonomi & Haptic Feedback**: Tombol `‹ Sebelumnya` dan `Berikutnya ›` diperluas dengan padding 16dp, ripple effect sian glasial, teks tebal berukuran 12sp, dan getaran haptic taktil saat ditekan.
  - **Indikator Halaman Kontras**: Menampilkan indikator pill berlatar `#E617253D` dengan stroke `1.2dp #6638BDF8` dan teks aksen sian glasial (`@color/accent_glacial`) yang dinamis menyesuaikan bahasa aktif (`Hal %d` / `Page %d` / `第 %d 頁`).

### Diperbaiki
- **Penghapusan Shortcut Tema di Top Bar (`activity_main.xml`, `MainActivity.java`)**:
  - Menghapus tombol shortcut tema (`btn_theme_trigger` / icon palette) dari header top bar untuk menghilangkan redundansi antarmuka dan memperluas capsule input pencarian (*search trigger capsule*).
  - Penggantian tema anime (Shinkai, Sakura, Cyber, Ghibli) tetap terintegrasi secara seamless melalui modal Pengaturan Terpadu (*SettingsDialog*).
- **Identitas Resmi Baru: Shisen (視線 • シセン)**:
  - Mengubah nama aplikasi menjadi **Shisen** dengan kanji **視線** (artinya *Tatapan / Line of Sight*, padanan kata paling pas dan otentik untuk "Menatap", ringkas 2 suku kata, modern, dan belum pernah digunakan oleh aplikasi anime mana pun).
- **Isolasi Musiman Ketat & Eliminasi Kebocoran Antar-Musim (`AniListClient.getCurrentSeasonAiring`, `AniListClient.getMasihTayangDualLane`)**:
  - **Pencegahan Kebocoran Musim Sebelumnya ke Beranda ("Musim Ini")**: Menambahkan validasi ketat `cur.season.equalsIgnoreCase(a.season) && cur.year == a.year && !isLongRunning(a)` pada `getCurrentSeasonAiring` dan fallback-nya. Menjamin tidak ada satupun anime carry-over dari musim sebelumnya (misal Summer 2024 ke Fall 2024) atau serial panjang tanpa batas yang bocor ke daftar anime musim berjalan di tab "MUSIM INI".
  - **Restrukturisasi 3-Jalur Eksklusif pada Tab "SEDANG TAYANG" (`AIRING`)**: Membagi anime on-going yang sedang tayang menjadi 3 jalur yang saling lepas (*mutually exclusive*):
    1. *Musim Berjalan* (`CUR`): Anime musiman yang tayang perdana pada musim & tahun aktif saat ini.
    2. *Lanjutan Musim Sebelumnya* (`PREV`): Anime cour lanjutan/carry-over yang tayang perdana di musim sebelumnya namun masih berstatus rilis aktif.
    3. *Serial Panjang Berkelanjutan* (`LONG`): Serial raksasa non-musiman (>30 episode atau ongoing tahunan seperti *One Piece*, *Detective Conan*, *Doraemon*).
    - Memperbarui chip filter sub-lane di `MainActivity` menjadi `{"Semua (3 Jalur)", "Musim Berjalan", "Musim Sebelumnya", "Serial Panjang"}` sehingga pengguna dapat menyaring tiap jalur secara terisolasi tanpa tercampur.
  - **Penyempurnaan Heuristik `isLongRunning`**: Mengamankan anime musiman 1-2 cour yang memiliki jumlah episode pasti (`episodesCount <= 30`, misal Bleach TYBW, Re:Zero S3) agar tidak salah terdeteksi sebagai serial panjang berkelanjutan hanya karena nama franchise terkenalnya.
- **Akurasi Jumlah & Daftar Episode yang Sudah Tayang (`AniListClient.parseMediaNode`, `DetailActivity`)**:
  - **Penyelarasan `epsCounter` & `airedEpisodesCount`**: Mengoreksi parsing `nextAiringEpisode` di mana `airedEpisodesCount = nextAiringEpisode - 1`. Jika `nextAiringEpisode == 1` atau status `NOT_YET_RELEASED`, anime ditampilkan secara jujur sebagai `"Belum Tayang"` (`airedEpisodesCount = 0`). Untuk anime yang sedang tayang, counter menampilkan `"Ep X/Y"` (hanya menghitung episode yang sudah tayang di TV).
  - **Penyaringan Episode Scraper Sesuai Broadcast TV (`DetailActivity.filterAccurateEpisodes`)**: Memfilter seluruh respon daftar episode dari penyedia/scraper (HiAnime, AniChan, Otakudesu) agar episode yang belum tayang (`ep.number > maxAired`) tidak muncul di daftar episode yang dapat diputar.
  - **Pencegahan Matching Keliru Antar-Season (`DetailActivity.pickBestSlug`)**: Menghilangkan fallback buta `list.get(0).slug` ketika kecocokan judul dan nomor musim tidak valid, mencegah anime Season 2 yang baru rilis 4 episode keliru mengambil daftar 24 episode milik Season 1 yang sudah tamat.
  - **Penanganan State Kosong & Disabled Button**: Jika anime berstatus `NOT_YET_RELEASED` atau belum memiliki episode tayang (`airedEpisodesCount == 0`), daftar episode dikosongkan dan tombol aksi utama disetel menjadi nonaktif dengan teks `"Belum Tayang"`.
- **Pemunculan Musim & Film Terkait di Detail Anime (`AniListClient.getRelations`)**:
  - **Eliminasi HTTP 500 GraphQL AniList**: Menghapus nesting rekursif ilegal `relations { edges { node { relations { ... } } } }` di dalam `RELATIONS_QUERY` yang memicu respon `HTTP 500 Internal Server Error` dari server AniList untuk seluruh anime, yang sebelumnya menyebabkan kontainer Musim dan Film Terkait disembunyikan permanen (`View.GONE`).
  - **Arsitektur Traversal 2-Hop Berbasis GraphQL Aliases**: Menerapkan pengambilan relasi langsung (Hop 1) yang disajikan seketika ke UI, dilanjutkan dengan pengayaan relasi tingkat 2 (Hop 2) secara asinkron menggunakan multi-node aliases (`m0`, `m1`, `m2`) untuk prekuel/sekuel utama. Menjamin seluruh franchise anime (seperti kelanjutan Season 2, Season 3, film kanon, dan movie spin-off) tampil lengkap dan akurat.
- **Pencegahan Terlempar ke View Sebelumnya & Watchdog Crash Saat Menonton / Idle**:
  - **Eliminasi Fatal Foreground Service Crash (`PlayerActivity`, `PlaybackService`)**: Mengganti `startForegroundService` di `PlayerActivity.setupPlayer()` menjadi `startService` aman dengan `sessionIntent` lengkap, meniadakan crash OS `ForegroundServiceDidNotStartInTimeException` yang mematikan activity saat resolusi stream berlangsung.
  - **Eliminasi UI Freeze & ANR pada Main UI Thread (`PlayerActivity`, `MenatapDatabase`)**: Memindahkan operasi penulisan disk SQLite `saveHistory` ke single-thread executor latar belakang (`dbExecutor`) dengan debouncing 10 detik, serta meniadakan `notifyItemChanged` setiap 1000ms pada thread UI utama yang memicu frame drops dan watchdog termination.
  - **Penghapusan `finish()` Tak Disengaja Saat Layar Mati / Idle (`PlayerActivity.onStop`)**: Menghapus pemanggilan `finish()` prematur di `onStop()` saat layar mati atau berpindah sementara, memastikan video/player tetap utuh saat pengguna kembali ke aplikasi.
  - **Restorasi Status Activity Lengkap Saat Memory Reclamation / Idle Timeout (`DetailActivity`, `PlayerActivity`, `MainActivity`)**: Mengimplementasikan `onSaveInstanceState`, pemulihan `savedInstanceState`, serta in-memory data holder (`PlaybackDataHolder`, `DetailDataHolder`) agar view tidak memanggil `finish()` atau kehilangan data saat proses direklamasi sistem Android.
  - **Optimasi Alokasi Memori & Downsampling Gambar (`ImageLoader`, `MenatapApplication`)**: Menerapkan dynamic downsampling `inSampleSize` saat mendecode gambar web/disk, pembersihan memori pada `onTrimMemory` / `onLowMemory`, dan penambahan `android:largeHeap="true"` untuk meniadakan OOM crash pada penayangan berkelanjutan.
  - **Pencegahan Concurrency Race Condition Subtitle (`SubtitleManager`)**: Penggunaan referensi cue snapshot aman dan penanganan `try-catch` terisolasi pada sinkronisasi subtitle real-time (80ms interval).

### Ditambahkan
- **Fitur Haptic Feedback & Getaran Ergonomis (`AppSettings`, `HapticHelper`, `AndroidManifest.xml`)**:
  - Penambahan izin sistem Android `<uses-permission android:name="android.permission.VIBRATE" />` pada `AndroidManifest.xml`.
  - Preferensi toggle getaran haptic terintegrasi (`AppSettings.isHapticEnabled`, default `true`) yang tersimpan aman secara persisten di `SharedPreferences`.
  - Pembungkusan seluruh pemanggilan getaran sentuhan di `HapticHelper` (`performClick`, `performTick`, `performHeavy`, `performSelection`) dengan validasi preferensi pengguna, memberikan fleksibilitas penuh untuk mematikan getaran tanpa kompromi performa.
- **Fitur Safe Mode Tatap & Proteksi Konten Dewasa (`AppSettings`, `MainActivity`)**:
  - Porting logika proteksi konten Safe Mode dari Tatap OG ke Menatap (`AppSettings.isSafeMode`, default `true` aktif).
  - Mode Aman Aktif (*Default*): Menyaring anime dengan konten dewasa non-hardcore (`ecchi`, `erotica`, `magical-sex-shift`, `sexual-violence`, `nudity`) dari feed beranda (Musim Ini & Sedang Tayang) serta kueri pencarian instan.
  - Mode Aman Nonaktif: Menampilkan kembali konten bernuansa ecchi/erotica bagi pengguna yang menginginkannya.
  - **Zero-Tolerance Blacklist Hentai 100% Permanen**: Konten, genre, slug, maupun kueri pencarian bertema `hentai` diblokir permanen dan mutlak di semua level tanpa memandang status Safe Mode aktif maupun nonaktif.
- **Fitur Pemilih Model AI Subtitle Dinamis (*AI Model Selector*) (`AiSubtitleService`, `SettingsDialog`, `PlayerActivity`)**:
  - Menyediakan opsi model selector adaptif berbasis provider BYOK:
    - *Google Gemini*: `gemini-2.5-flash` (default), `gemini-2.0-flash`, `gemini-1.5-flash`, `gemini-1.5-pro`.
    - *Groq Cloud*: `llama-3.3-70b-versatile` (default), `llama-3.1-8b-instant`, `mixtral-8x7b-32768`, `gemma2-9b-it`.
    - *OpenAI*: `gpt-4o-mini` (default), `gpt-4o`, `gpt-3.5-turbo`.
    - *Ollama*: `llama3.2` (default), `qwen2.5:7b`, `gemma2:9b`, serta input model kustom lokal.
  - Chip pilihan model interaktif dan input model kustom tersimpan aman via hardware-backed `SecureStore` (`KEY_PREF_MODEL`).
  - Unit test komprehensif pada `AiSubtitleServiceTest` menguji default fallback dan injeksi model kustom pada URL Gemini serta body request Groq/OpenAI.
- **Konsolidasi Menu Pengaturan Terpadu (*Unified Settings Dialog*) (`SettingsDialog`, `dialog_settings.xml`)**:
  - Merapikan menu floating nav yang sebelumnya terpisah-pisah (Tema, AI Subtitle, Update) menjadi **1 Menu Terpadu: "Pengaturan • 設定" (`menu_item_settings`)**.
  - Desain modal cyber-anime 5-in-1:
    1. *Tampilan & Tema*: Pemilihan interaktif 4 kanvas atmosferik anime (Shinkai, Sakura, Cyber, Ghibli).
    2. *Proteksi Konten*: Toggle Safe Mode Tatap untuk penyaringan konten dewasa.
    3. *Haptic & Getaran*: Toggle getaran haptic dengan live tactile preview.
    4. *AI Subtitle Fansub BYOK*: Konfigurasi API Key AES-256-GCM, chip model selector dinamis, dan kustomisasi prompt fansub.
    5. *Sistem & Pembaruan*: Informasi versi rilis Menatap, status zero-server standalone runtime, dan tombol "Periksa Pembaruan Rilis" seamless.
- **Fitur Musim & Film Terkait di Detail Anime (`Related Seasons & Movies`)**:
  - **Single GraphQL 2-Hop Traversal Query (`AniListClient.getRelations`)**: Mengambil seluruh relasi franchise (prekuel, sekuel, seri utama, spin-off, alternative, film/movie) hingga kedalaman relasi tingkat 2 dalam 1 kali request efisien tanpa memicu rate-limit HTTP 429 AniList.
  - **Pemisahan Kategori Otomatis (`seasons` & `movies`)**:
    - *Musim & Seri Terkait*: Format serial (`TV`, `TV_SHORT`, `ONA`, `OVA`, `SPECIAL`) dengan badge dinamis kontekstual (*Season X*, *Season 1*, *Prekuel*, *Sekuel*, *Seri Utama*, *Side Story*, *Spin-Off*, *OVA*, *Spesial*).
    - *Film & Movie Terkait*: Format layar lebar (`MOVIE`) dengan badge dinamis (*Movie*, *Movie Part X*, *Prekuel Film*, *Sekuel Film*).
  - **Penyaringan Bersih & Zero-Tolerance Blacklist**: Mengecualikan konten non-anime (`MANGA`, `NOVEL`, `ONE_SHOT`), ID anime yang sedang dibuka, duplikasi franchise (`seenIds`), serta penyaringan mutlak terhadap genre Hentai.
  - **Urutan Kronologis (*Chronological Order*)**: Seluruh seri dan film diurutkan secara menaik berdasarkan tahun rilis (`year`) dan relasi alur cerita agar penonton dapat mengikuti urutan rilis secara alami.
  - **Komponen UI Kartu Relasi (`item_related_card.xml` & `RelatedAnimeAdapter`)**: Tampilan kartu kompak berukuran 105dp x 160dp dengan thumbnail karakter, scrim gradasi gelap, badge pill relasi, judul anime tebal, tahun/format, dan animasi spring touch interaktif.
  - **Integrasi Seamless di `DetailActivity`**: Menampilkan kontainer horizontal `rv_detail_seasons` dan `rv_detail_movies` tepat di bawah kartu sinopsis dengan jumlah item ("X Seri" / "X Film"), serta auto-hide (`View.GONE`) jika tidak memiliki relasi terkait. Mengetuk kartu relasi langsung membuka `DetailActivity` baru dengan haptic feedback.
  - **Unit Testing Komprehensif (`AniListRelationsTest`)**: Pengujian unit test untuk multi-season franchise (Jujutsu Kaisen, Kimetsu no Yaiba), deteksi part film (Girls und Panzer Finale Part 2), eksklusi manga/novel, dan verifikasi blacklist genre hentai.
- **Perbaikan Paginasi Musim Ini & Sedang Tayang (`AniListClient` & `MainActivity`)**:
  - Menyertakan metadata resmi `pageInfo { hasNextPage total currentPage }` pada seluruh kueri GraphQL AniList (`PagedCallback`), membebaskan paginasi dari asumsi naif `currentCount >= pageSize`.
  - Pengecualian anime berseri panjang (`isLongRunning`) pada Beranda Musim Ini tidak lagi mematikan tombol "Berikutnya ›" pada halaman 1 ketika jumlah item tersaring menjadi <35.
  - Merestrukturisasi kueri "Sedang Tayang" (Airing) menjadi aliran kontinu anime berstatus `RELEASING` yang dipisahkan secara dinamis ke dalam jalur musiman dan serial panjang, memungkinkan paginasi antar-halaman berjalan mulus dan tanpa batas.
- **Pemunculan Pengaturan AI Subtitle di Beranda & Player**:
  - Menambahkan menu aksi langsung "✨ AI Subtitle • 字幕AI" pada *Folded Floating Menu* di Beranda (`menu_item_ai_subtitle`).
  - Mempromosikan pintasan konfigurasi AI Subtitle ke baris teratas pemilih track subtitle di Pemutar Video (`PlayerActivity`).
- **Fitur Kustomisasi Prompt Terjemahan Fansub (*Custom Translation Prompt*)**:
  - Menambahkan input multiline pada `dialog_ai_subtitle_settings.xml` (`et_ai_custom_prompt`) untuk mengatur persona, register bahasa, dan aturan terjemahan LLM.
  - Dilengkapi tombol reset instan ke default fansub anime populer (`DEFAULT_CUSTOM_PROMPT`).
  - Menyuntikkan custom prompt ke payload Gemini, Groq (`system`), OpenAI (`system`), dan Ollama (`prompt`).
  - Menyimpan preferensi prompt secara aman dan persisten via `SecureStore` (`KEY_PREF_CUSTOM_PROMPT`).
  - Penambahan unit test verifikasi injeksi custom prompt pada `AiSubtitleServiceTest`.
- **Paritas Fitur Inti & DPI-Bypass (Fase 2)**:
  - **TLS ClientHello Desync DPI-Bypass (`TlsDesyncHelper`, `DesyncSocketWrapper`, `DesyncSSLSocketFactory`)**:
    - Implementasi pemecahan TLS record ClientHello RFC-compliant (1 byte data + micro-pause 30ms) untuk meniadakan tanda tangan SNI utuh dari pemindaian firewall DPI ISP (Internet Positif / Sangfor).
    - Mekanisme pembelajaran dinamis (`DynamicBlockedHosts`) yang secara otomatis menandai dan meng-cache domain saat koneksi mengalami reset, timeout, atau handshake error.
    - Dilengkapi pemecah segmen TCP untuk segmentasi di tingkat soket transport.
  - **Multi-Tier DNS Fallback Chain (`DnsFallbackResolver`)**:
    - Rantai resolusi domain komprehensif: In-Memory Cache ➔ System DNS (dengan filter penyaringan sinkhole/poisoning ISP: 10.x, 127.x, 192.168.x, 118.98.x) ➔ Cloudflare DoH (1.1.1.1) ➔ Google DoH (8.8.8.8) ➔ Hardcoded CDN IP (`hls.dramahot.top`).
  - **MediaSession & Background Playback Service (`media3-session`)**:
    - Integrasi `androidx.media3:media3-session` dan implementasi `PlaybackService` sebagai foreground service `mediaPlayback`.
    - Kontrol pemutaran sistem pada notifikasi dan lockscreen Android lengkap dengan metadata (judul episode, anime, artwork).
  - **Porting Provider ZokoAnime & Demuxed Audio (`ZokoAnimeParser`)**:
    - Mengadopsi parser ZokoAnime (`zokoanime.video` dan CDN `dramahot.top`) dari Tatap OG dengan deobfuskasi XOR `otaku-embed-v1`, deteksi halaman 404, ekstraksi subtitle multi-track, dan penanganan demuxed audio tracks `#EXT-X-MEDIA:TYPE=AUDIO`.
  - **AI Subtitle BYOK LLM Engine (`AiSubtitleService`)**:
    - Dukungan penerjemahan subtitle berbasis LLM mandiri (BYOK) untuk Google Gemini, Groq Cloud, OpenAI-compatible, dan Ollama (lokal/remote).
    - Deteksi otomatis provider berdasarkan format kunci (`AIzaSy...`, `gsk_...`, `sk-...`, `http://...`).
    - Penyimpanan kunci aman via `SecureStore` dengan jaminan zero-logging (tidak pernah dicatat ke logcat).
    - Antarmuka dialog pengaturan terpadu `dialog_ai_subtitle_settings.xml`.
  - **Rangkaian Tes Emas TitleMatcher (`TitleMatcherTest`)**:
    - Rangkaian pengujian unit test tanpa regresi untuk kasus multi-season (`Re:Zero`, `Jujutsu Kaisen`, `Oshi no Ko`), apostrof (`Frieren: Beyond Journey's End`), token penomoran romawi (`Season II/III/IV`), dan isolasi Movie/OVA.
- **Hardware-Backed Secret Storage (`SecureStore`)**:
  - Implementasi penyimpanan rahasia terenkripsi berbasis Android Keystore dengan algoritma authenticated encryption AES-256-GCM (`AES/GCM/NoPadding`, 12-byte random IV, 128-bit authentication tag).
  - Integrasi penyimpanan privat `SharedPreferences` terenkripsi dengan proteksi anti-tampering (menolak ciphertext yang diubah).
  - Factory pengujian terisolasi untuk unit test JVM host.
  - Zero-logging policy: tidak pernah mencatat atau menampilkan API key/secret ke logcat atau antarmuka. Dipersiapkan untuk BYOK LLM AI Subtitle (Fase 2).
- **Network Security Configuration (`network_security_config.xml`)**:
  - Konfigurasi keamanan jaringan ketat dengan `cleartextTrafficPermitted="false"`.
  - Pengamanan aplikasi terhadap transmisi data tidak terenkripsi pada seluruh koneksi scraper, stream CDN, dan update checker.
- **Root Gradle Wrapper (`gradlew`)**:
  - Script wrapper eksekusi di root repositori untuk mempermudah eksekusi `./gradlew` langsung dari direktori utama maupun subdirektori `android/`.
- **Floating Pagination Auto-Fold & Crossed Swords Trigger**:
  - Paginasi mengambang melipat otomatis (`folded`) saat scrolling menjadi pill nomor halaman minimalis ("Hal X", alpha 75%) selebar ~56dp untuk memaksimalkan area pandang konten anime.
  - Kembali melebar (*expand*) mulus ke ukuran normal beserta tombol navigasi ("‹ Sebelumnya" dan "Berikutnya ›") saat pengguliran berhenti (`SCROLL_STATE_IDLE`).
  - Ketukan langsung pada pill dalam kondisi folded langsung mengembalikan status expanded.
  - Ikon FAB menu mengambang berganti secara dinamis menjadi ikon pedang bersilang (`ic_sword_cross`) dengan animasi rotasi & scaling halus saat menu aktif/terbuka, dan kembali ke hamburger (`ic_menu`) saat ditutup.
- **Tema UI Anime & Pemilihan Background Atmosferik (`AnimeThemeManager`)**:
  - Penambahan 4 preset kanvas atmosferik anime: 🌌 *Twilight Shinkai (Default)*, 🌸 *Sakura Blossom*, ⚡ *Cyber Neo-Tokyo*, dan 🍃 *Ghibli Forest Night* yang dapat dipilih interaktif via dialog modal `dialog_anime_theme_selector.xml`.
  - Palet warna anime autentik: Glacial Cyan (`#38BDF8`), Electric Neon (`#00F0FF`), Sakura Blossom Pink (`#FB7185`), Twilight Iris (`#818CF8`), Miku Mint (`#10B981`), dan Star Gold (`#FBBF24`).
  - Tipografi anime: logo topbar dengan aksen kanji `見つめる` dan tag `[ アニメ ]`, serta Floating Folded Menu dengan sub-label kanji/katakana (`ホーム`, `放送中`, `目録`, `ジャンル`, dll.).
- **Cover Detail Anime Berfokus Karakter Utama**:
  - Redesain Hero Section pada Detail Anime dengan menghadirkan `card_detail_poster` (105x155dp, rasio 2:3, border hairline glow) yang memfokuskan visual pada karakter utama anime secara utuh tanpa terpotong `centerCrop` horizontal.
  - Menampilkan judul asli Jepang (`title.native` dari AniList, misal `Sousou no Frieren • 葬送のフリーレン`) mendampingi judul Romaji.
  - Ambient backdrop sinematik di latar belakang (`bg_hero_gradient_scrim`).
- **Safety Screen Lock Fullscreen Landscape (Anti-PiP)**:
  - Proteksi penguncian layar ergonomis yang HANYA dapat diaktifkan pada mode Fullscreen Landscape (`ORIENTATION_LANDSCAPE`).
  - Seluruh sentuhan gesture (volume, brightness, seek, double-tap zoom) dan kontrol player diblokir saat terkunci.
  - Sentuhan saat terkunci memunculkan tombol melayang `[ 🔒 Buka Kunci Layar ]` selama 3.5 detik dengan animasi fade-in dan auto-fadeout.
  - **Bypass Mutlak Mode PiP**: Otomatis membuka kunci layar seketika (`unlockScreen(false)`) jika beralih ke Picture-in-Picture, menjamin zero-lock di mode PiP.
  - Otomatis membuka kunci layar seketika jika perangkat diputar kembali ke posisi Portrait.
  - Tombol fisik Back saat terkunci menampilkan tombol buka kunci tanpa menutup video.

### Diubah & Dioptimalkan
- **Build Release Hardening & R8 Shrinking**:
  - Mengaktifkan optimasi R8 code shrinking (`minifyEnabled true`) dan resource shrinking (`shrinkResources true`) pada build release.
  - Menambahkan aturan ProGuard terperinci (`proguard-rules.pro`) untuk OkHttp, jsoup, Media3 (ExoPlayer & HLS), Kotlin Coroutines, Tukaani XZ, dan model serialisasi data Menatap.
  - Mengecualikan artefak `DebugProbesKt.bin` via blok `packaging { resources { excludes += ['DebugProbesKt.bin'] } }` dengan tetap menjaga kelengkapan seluruh berkas lisensi (`LICENSE`, `NOTICE`).
- **Audit Manifest & Keamanan Jaringan**:
  - Menetapkan `android:allowBackup="false"` untuk mencegah kebocoran data aplikasi melalui Android ADB backup.
  - Menghapus atribut `extractNativeLibs` (aplikasi murni 100% Java/Kotlin bytecode tanpa pustaka C/C++ native).
  - Menetapkan `android:usesCleartextTraffic="false"` dan mengikat `android:networkSecurityConfig="@xml/network_security_config"`.
- **Audit Dex Bytecode**:
  - Memverifikasi asal simbol `127.0.0.1` (dari pustaka Google Guava / Media3 `InetAddresses` dan `HttpClientProvider` untuk filter sinkhole ISP) dan `ServerSocket` (dari utilitas `okhttp3.internal.Util.closeQuietly`).
  - Terbukti 100% bebas dari embedded HTTP server (NanoHTTPD, Ktor, atau ServerSocket listener).
- **Sinkronisasi Versi Otomatis**:
  - Pembaruan versi aplikasi ke `0.1.0-beta.2` (versionCode: 10002).
  - Memperbarui `AppUpdateManager.java` agar membaca `BuildConfig.APP_VERSION_NAME` secara dinamis.

## [0.1.0-beta.1] - 2026-10-25

### Ditambahkan
- **Adopsi Penuh Filter Katalog Tatap (Cross-Filter Modal)**:
  - Mengadopsi dialog modal cross-filter resmi Tatap (`dialog_catalog_filter.xml`) dengan 6 kategori penyaringan lengkap:
    1. *Urutan (Sort)*: Terpopuler, Skor Tertinggi, Terbaru, Trending, A-Z, Favorit.
    2. *Tahun Rilis*: Semua Tahun, 2026, 2025, 2024, 2023, 2022, 2021, 2020, rentang dekade 2015-2019, 2010-2014, 2000-2009, dan klasik &lt;2000.
    3. *Musim Penayangan*: Semua Musim, Musim Semi (Spring), Musim Panas (Summer), Musim Gugur (Fall), dan Musim Dingin (Winter).
    4. *Bulan Rilis*: Semua Bulan, Januari (1) hingga Desember (12).
    5. *Format / Tipe*: Semua Format, TV Series, Movie, OVA, ONA, dan Special.
    6. *Status Penayangan*: Semua Status, Sedang Tayang (Releasing), dan Sudah Tamat (Finished).
  - Tombol aksi terpadu *Hapus Filter* dan *Terapkan Filter*.
  - Indikator ringkasan filter aktif di bilah kontekstual (`KATALOG: TERPOPULER · 2024 · TV SERIES`) dengan tombol pintasan reset/filter ulang.
- **Adopsi Alur & Daftar Genre Lengkap Tatap (90+ Kategori)**:
  - Mengadopsi dialog pemilih genre interaktif (`dialog_genre_selector.xml`) yang langsung terbuka otomatis saat tab/menu Genre dipilih.
  - Kolom pencarian genre instan (*real-time filter*) untuk mencari kategori secara cepat (misal: *isekai*, *romance*, *cyberpunk*, *sports*).
  - Menyertakan daftar 90+ genre resmi Tatap (Action, Adventure, Animation, Boys Love, Cars, CGDCT, Childcare, Combat Sports, Comedy, Crossdressing, Delinquents, Dementia, Demons, Detective, Drama, Educational, Fantasy, Gag Humor, Game, Girls Love, Gore, Gourmet, Harem, High Stakes Game, Historical, Horror, Idols, Isekai, Iyashikei, Josei, Kids, Magic, Mahou Shoujo, Martial Arts, Mecha, Military, Music, Mystery, Mythology, Parody, Pets, Police, Psychological, Racing, Reincarnation, Romance, Samurai, School, Sci-Fi, Seinen, Shoujo, Shounen, Slice of Life, Space, Sports, Supernatural, Super Power, Survival, Suspense, Thriller, Time Travel, Vampire, Video Game, Workplace, dll.).
  - Penanganan multi-sumber di backend: otomatis memetakan genre resmi AniList (`genre: $genre`) dan tags tematik AniList (`tag: $tag`) dengan zero hentai blacklist policy.
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
