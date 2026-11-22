# PROJECT_MAP.md — Shisen (視線)

> Indeks topologi & simbol. Baca setelah AGENTS.md.

## Struktur Direktori

```
menatap/
├── android/                          # Android Studio project (pure Java)
│   ├── app/src/main/java/id/menatap/app/
│   │   ├── MenatapApplication.java
│   │   ├── data/
│   │   │   ├── db/MenatapDatabase.java
│   │   │   ├── model/ (Anime, Episode, StreamSource, SubtitleTrack)
│   │   │   ├── service/ (AniListClient, HiAnimeClient, AniChanClient, OtakudesuScraper, StreamResolver, SubtitleService, HttpClientProvider, ...)
│   │   │   └── subtitle/ (SubtitleManager, SubtitleMergeEngine, GtxTranslator, SmartLexicon, AnimeToshoClient, OpenSubtitlesClient, AiSubtitleService, ...)
│   │   ├── ui/
│   │   │   ├── home/MainActivity.java
│   │   │   ├── detail/DetailActivity.java
│   │   │   ├── player/PlayerActivity.java, PlaybackService.java, PlaybackDataHolder.java
│   │   │   ├── adapter/
│   │   │   └── dialog/SettingsDialog.java
│   │   └── util/ (AppSettings, LocaleHelper, SystemUiHelper, ImageLoader, ...)
│   ├── app/src/main/res/
│   └── app/build.gradle
├── backend_reference/                # Tatap Python reference (jangan diubah, hanya rujukan)
│   ├── backend/ (main.py, api/...)
│   └── frontend/js/app.js
├── plan_smartsub/                    # Rencana perbaikan SmartSub
├── tools/bump_version.py
├── AGENTS.md
├── PROJECT_MAP.md
├── README.md
├── CHANGELOG.md
└── VERSION
```

## Peta Simbol Utama

### Subtitle
- `SubtitleManager.loadFromTracks(...)` — jalur Smart Merge ID (selalu)
- `SubtitleManager.loadRawTrack(url,label,lang,referer)` — jalur raw
- `SubtitleManager.setMode(lang,engine)` — set state eksplisit
- `SubtitleMergeEngine.merge(builtin,external,offset,translatedGaps)` — merge + gap fill (skip jika no translation)
- `SubtitleMergeEngine.findUnmatchedBuiltinIndices(...)` — cari gap cue
- `SubtitleMergeEngine.estimateOptimalOffset(...)`
- `GtxTranslator.translateAsync(...)` / `translateBatchSync(...)`
- `SmartLexicon.applyUniversalLexicon(...)`, `sanitizeCuesWithContext(...)`

### Streaming
- `StreamResolver` — orkestrasi sumber
- `HiAnimeClient`, `AniChanClient`, `OtakudesuScraper`, `ZokoAnimeParser`
- `AniListClient` — metadata/anilist
- `AniSkipClient` — skip intro/outro

### UI
- `MainActivity` — home/katalog
- `DetailActivity` — detail anime
- `PlayerActivity` — player + dialog subtitle
- `SettingsDialog`

### Util
- `AppSettings`, `LocaleHelper` (trilingual), `SystemUiHelper` (edge-to-edge), `ImageLoader`

## Konvensi
- Komentar/dokumentasi: Indonesia; nama variabel/fungsi: Inggris.
- Tanpa framework build di frontend (N/A; native Java).
- Release asset: hanya `shisen-release-*.apk`.
