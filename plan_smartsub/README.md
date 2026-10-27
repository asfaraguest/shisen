# Plan: Perbaikan Subtitle SmartSub Indonesia

## Ringkasan

3 bug menyebabkan subtitle English muncul saat user pilih SmartSub Indonesia:

1. **State bocor antar opsi** — PlayerActivity: Option "Smart ID" dan "English" pakai method sama (`loadFromTracks`), state dari pilihan sebelumnya bocor
2. **Gap-fill inject English mentah** — SubtitleMergeEngine: Cue Signs/OP/ED dari subtitle bawaan (English) masuk tanpa terjemahan
3. **Cache simpan English gagal** — GtxTranslator: Jika GTX gagal terjemah, English mentah ter-cache permanen

## Referensi Tatap

| Komponen Android | Referensi Tatap | File |
|---|---|---|
| PlayerActivity (dialog subtitle) | app.js `renderSubtitles` + `onchange` | `backend_reference/frontend/js/app.js#L1527-L1707` |
| SubtitleManager (load logic) | app.js `pickSubtitleUrl` + `switchSubtitleTrack` | `backend_reference/frontend/js/app.js#L1460-L1500` |
| SubtitleMergeEngine (merge + gap) | translate.py `merge_opensubtitles_with_builtin` | `backend_reference/backend/api/translate.py#L248-L369` |
| Gap translation | main.py `_opensubtitles_merge_sub_job` | `backend_reference/backend/main.py#L2830-L2850` |
| GtxTranslator (translate batch) | translate.py `call_gtx_translate` | `backend_reference/backend/api/translate.py#L2619` |

## File Guide

| # | File | Guide |
|---|---|---|
| 1 | `PlayerActivity.java` | [01_PlayerActivity.md](./01_PlayerActivity.md) |
| 2 | `SubtitleManager.java` | [02_SubtitleManager.md](./02_SubtitleManager.md) |
| 3 | `SubtitleMergeEngine.java` | [03_SubtitleMergeEngine.md](./03_SubtitleMergeEngine.md) |
| 4 | `GtxTranslator.java` | [04_GtxTranslator.md](./04_GtxTranslator.md) |

## Urutan Implementasi

```
1. SubtitleMergeEngine.java  — tambah findUnmatchedBuiltinIndices + parameter translatedGaps
2. GtxTranslator.java        — tambah translateBatchSync + validasi cache
3. SubtitleManager.java       — tambah setMode, perbaiki loadFromTracks, terjemah gap
4. PlayerActivity.java        — pisahkan jalur tiap opsi dialog
```

## Verifikasi

```
./gradlew assembleRelease
```

Test manual:
- Pilih SmartSub ID → harus Indonesia
- Ganti English → ganti balik SmartSub ID → harus tetap Indonesia
- Gap-fill (Signs/OP/ED) → harus Indonesia bukan English
