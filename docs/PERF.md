# PERF — Catatan & Template Pengukuran Performa Shisen

Alat ukur:
- `tools/bench.sh` (di HP, via adb) — cold start, meminfo, batterystats. Ikuti protokol di header skrip (Wi-Fi stabil, kecerahan tetap, episode sama, 30 menit pemutaran).
- Tag log `SHISEN_PERF` (build debug) — cold start→`reportFullyDrawn`, tap episode→`onRenderedFirstFrame`, dropped frames, rebuffering, byte jaringan, puncak memori.

## Baseline 0.1.0-beta.3 (dapat diukur tanpa perangkat)

| Metrik | Nilai |
|---|---|
| Ukuran APK release (`shisen-release-v0.1.0-beta.3.apk`) | 5.161.172 byte |
| Jumlah method dex (release, R8, classes.dex) | 43.978 |
| Cold start median (TotalTime, `bench.sh` 5x) | *(diisi dari HP)* |
| Meminfo idle katalog (TOTAL PSS) | *(diisi dari HP)* |
| Meminfo setelah playback 30 menit (TOTAL PSS) | *(diisi dari HP)* |
| Wake lock sesi 30 menit (batterystats) | *(diisi dari HP)* |
| `SHISEN_PERF: cold_start_fully_drawn_ms` | *(diisi dari HP)* |
| `SHISEN_PERF: first_frame_after_tap_ms` | *(diisi dari HP)* |
| `SHISEN_PERF: dropped_frames_cumulative` (akhir sesi) | *(diisi dari HP)* |
| `SHISEN_PERF: rebuffer_events / total_ms` | *(diisi dari HP)* |
| `SHISEN_PERF: network_bytes` (akhir sesi) | *(diisi dari HP)* |
| `SHISEN_PERF: peak_memory_kb` | *(diisi dari HP)* |

## Template sesi perbandingan build

| Metrik | Build A | Build B | Delta | Keterangan |
|---|---|---|---|---|
| Cold start median (ms) | | | | `bench.sh` bagian (b) |
| Fully drawn (ms) | | | | log `SHISEN_PERF` |
| PSS idle (KB) | | | | `bench.sh` bagian (c) |
| PSS playback 30m (KB) | | | | `bench.sh` bagian (c-d) |
| Wake lock sesi | | | | `bench.sh` bagian (e) |
| Dropped frames | | | | log `SHISEN_PERF` |
| Rebuffer events / ms | | | | log `SHISEN_PERF` |
| Byte jaringan sesi | | | | log `SHISEN_PERF` |
| Ukuran APK (byte) | | | | `ls -l` |
| Method dex | | | | header dex offset 0x58 |

Aturan optimasi Fase 2: setiap perubahan diukur sebelum/sesudah; bila perbaikan tidak terukur (>5%), di-revert dan dicatat sebagai "tidak berguna".
