# Status Tugas 429 Rate Limiting (Parsial)

## ✅ Selesai (6/8)

### TUGAS 1 — Logger Request AniList
- PerfLogger.anilistRequest() dan anilistCacheHit()
- Counter DEBUG-ONLY di SHISEN_PERF
- Commit: 1c34b49

### TUGAS 2 — AniListGateway Integrasi
- AniListClient.executePostCustom pakai Gateway
- HTTP call dengan penanganan 429 (Retry-After, X-RateLimit-Reset)
- Body 429 HTML/kosong tidak crash
- Commit: 26a5e62

### TUGAS 3 — Penanganan 429 (sudah di TUGAS 2)
- Parse Retry-After (detik) dan X-RateLimit-Reset
- Cooldown global persisten (anilist_blocked_until di DB)
- Cooldown bertahap: 60→120→300 detik, max
- Reset setelah satu sukses
- Tidak ada retry otomatis

### TUGAS 4 — Cache Persisten Stale-While-Revalidate
- getFromCacheStale() ambil cache expired
- execute() cek fresh → stale → fail
- Background revalidate (1 detik delay) jika bucket tersedia
- PREFETCH tidak trigger refresh
- Commit: b9376a9

### TUGAS 5 — Kurangi Request
- Pencarian: debounce 500ms, minimal 3 karakter
- Batching alias GetMasihTayangAll (sudah ada)
- Dedup in-flight via Gateway
- Commit: d034ba3

### TUGAS 6 — Pemetaan Slug→ID untuk Playback
- AniChanClient.getEpisodesWithFallback(context, slug, hint)
- AniSkipClient.getSkipTimesWithFallback(context, slug, hint)
- DB mapping: MenatapDatabase.getAniListId(), getMalId()
- TTL 30 hari (schema sudah ada)
- Commit: 7c74820

## ❌ Belum (2/8)

### TUGAS 7 — UX 429
- Banner countdown "AniList sedang membatasi..."
- State kosong + tombol "Coba lagi" aktif setelah cooldown
- Toggle DEBUG "Simulasikan 429 (60 detik)"

### TUGAS 8 — Tes Perilaku
- MockWebServer + FakeClock
- 10 skenario (a-j): 429+Retry-After, cooldown persisten, burst, stale, dedup, dll

## Konstanta Gateway

Token bucket:
- BURST_CAP = 5
- REFILL_PER_MINUTE = 24 (80% dari 30)
- MAX_PARALLEL = 2
- MIN_GAP_MS = 300

Cooldown 429:
- INITIAL_COOLDOWN_MS = 60000 (60 detik)
- MAX_COOLDOWN_MS = 300000 (300 detik)
- JITTER_MS = 3000 (0-3 detik)

TTL cache (ms):
- Detail: 24 jam
- Search: 1 jam
- Relations: 7 hari
- Airing/seasonal: 6 jam
- Mapping slug→ID: 30 hari

## Build

- Version: 0.1.0-beta.6 (versionCode 10006)
- APK: 5.0 MB
- Test: testDebugUnitTest passed
- Release: assembleRelease sukses

## Risiko Tersisa

1. **Pengukuran baseline belum:** Home cold start, Detail, search belum diukur
2. **UX 429 belum:** User tidak tahu sedang cooldown
3. **Tes otomatis belum:** Perubahan gateway belum diverifikasi dengan tes perilaku
4. **Cleanup cache 14 hari:** Belum dijadwalkan otomatis
5. **Batching terbatas:** Home masih panggil beberapa endpoint terpisah
6. **Lazy load belum:** Section home dimuat semua di awal
7. **Mapping tidak auto-resolve:** Jika slug baru dan belum ada mapping, AniChan gagal (tidak fallback ke AniList)

## Hal yang Sengaja Tidak Disentuh

- TitleMatcher tidak diubah
- Blacklist hentai tidak diubah
- Logika desync tidak diubah
- Keystore, applicationId, signing config tidak diubah
- Server eksternal tidak ditambah
- Dependensi jaringan baru tidak ditambah

## Uji Manual di HP

1. Cold start dari fresh install → cek log SHISEN_PERF request count
2. Buka Detail anime → cek log request count
3. Ketik 10 karakter di pencarian → cek log request count (harus 1-2, bukan 10)
4. Ganti tab Home → cek cache hit
5. Scroll 5 halaman → cek request count

(TUGAS 7 toggle simulasi 429 belum tersedia)
