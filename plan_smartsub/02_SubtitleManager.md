# 02. SubtitleManager.java — Set Mode Eksplisit & Terjemah Gap

**File**: `android/app/src/main/java/id/menatap/app/data/subtitle/SubtitleManager.java`

## Bug

### Bug A: `loadFromTracks()` Cek State Lama

Baris 114-126 — method `loadFromTracks()` cek `activeEngine` dan `activeLanguage` untuk menentukan jalur:
```java
if (idTrack != null && "raw".equals(activeEngine)) {   // baris 114
    loadRawTrack(idTrack.url, ...);
    return;
}
if ("en".equals(activeLanguage)) {                      // baris 119
    loadRawTrack(enTrack.url, ...);
    return;
}
```

State `activeEngine`/`activeLanguage` adalah sisa dari pilihan sebelumnya → bocor.

### Bug B: Premature English Display

Baris 144-149 — sambil menunggu terjemahan, English mentah langsung ditampilkan:
```java
if (activeCues.isEmpty()) {
    activeCues = new ArrayList<>(enCues);   // ← English mentah tampil!
    activeLabel = "English [Menerjemahkan ke ID...]";
}
```

### Bug C: Gap Cues Tidak Diterjemahkan

Di callback merge (AnimeTosho & OpenSubtitles), `SubtitleMergeEngine.merge()` dipanggil tanpa `translatedGaps`.
Hasilnya gap English mentah masuk ke subtitle Indonesia.

**Tatap reference** (`main.py#L2830-L2850`):
```python
# Tatap selalu terjemahkan gap SEBELUM merge
unmatched_idx, unmatched_cues = find_unmatched_builtin_cues(builtin_cues, ext_cues, offset=offset)
if unmatched_cues and len(unmatched_cues) <= 180:
    tr_gaps = await call_gtx_translate(unmatched_cues, src, "id", mode="gtx+")
    for idx_b, tr_c in zip(unmatched_idx, tr_gaps):
        translated_gaps[idx_b] = tr_c.get("text", "")
merged_cues, stats = merge_opensubtitles_with_builtin(..., translated_gaps=translated_gaps, ...)
```

## Perubahan

### A. Tambah method `setMode()`

Tambahkan setelah baris 64 (setelah `getActiveLabel()`):

```java
/**
 * Set mode subtitle secara eksplisit sebelum load.
 * Dipanggil oleh PlayerActivity saat user pilih opsi subtitle.
 */
public void setMode(String language, String engine) {
    this.activeLanguage = language;
    this.activeEngine = engine;
}
```

### B. Hapus cek state lama di `loadFromTracks()`

**HAPUS** baris 114-126 (blok `if (idTrack != null && "raw".equals(activeEngine))` dan `if ("en".equals(activeLanguage))`):

```java
// HAPUS SELURUH BLOK INI:
if (idTrack != null && "raw".equals(activeEngine)) {
    loadRawTrack(idTrack.url, "Bahasa Indonesia (Asli)", "id", idTrack.referer);
    return;
}

if ("en".equals(activeLanguage)) {
    if (enTrack != null) {
        loadRawTrack(enTrack.url, "English (Original)", "en", enTrack.referer);
    } else {
        loadRawTrack(tracks.get(0).url, tracks.get(0).label, tracks.get(0).lang, tracks.get(0).referer);
    }
    return;
}
```

`loadFromTracks()` sekarang **SELALU** jalur Smart Merge ID. Opsi lain (English raw, ID raw) dipanggil langsung via `loadRawTrack()` dari `PlayerActivity`.

### C. Ganti premature English display dengan loading indicator

**SEBELUM** (baris 144-149):
```java
if (activeCues.isEmpty()) {
    activeCues = new ArrayList<>(enCues);
    activeLabel = "English [Menerjemahkan ke ID...]";
    if (listener != null) listener.onSubtitleLoaded(activeLabel, activeCues.size());
}
```

**SESUDAH**:
```java
if (activeCues.isEmpty()) {
    activeLabel = "Menerjemahkan ke Bahasa Indonesia...";
    if (listener != null) listener.onSubtitleLoading(activeLabel);
}
```

### D. Terjemahkan gap cues sebelum merge

Di callback `onSuccess` AnimeTosho (baris ~168-190) dan OpenSubtitles (baris ~223-250), tambahkan step terjemah gap **sebelum** panggil `SubtitleMergeEngine.merge()`.

**SEBELUM** (AnimeTosho callback):
```java
if (!finalEnCues.isEmpty()) {
    long offset = SubtitleMergeEngine.estimateOptimalOffset(finalEnCues, atCues);
    SubtitleMergeEngine.MergeResult mr = SubtitleMergeEngine.merge(finalEnCues, atCues, offset);
    // ...
}
```

**SESUDAH**:
```java
if (!finalEnCues.isEmpty()) {
    long offset = SubtitleMergeEngine.estimateOptimalOffset(finalEnCues, atCues);

    // Terjemahkan gap cues (Signs/OP/ED) sebelum merge
    // Ref: main.py#L2834-L2841
    Map<Integer, String> translatedGaps = new HashMap<>();
    List<int[]> unmatched = SubtitleMergeEngine.findUnmatchedBuiltinIndices(finalEnCues, atCues, offset);
    if (!unmatched.isEmpty() && unmatched.size() <= 180) {
        List<SubtitleCue> gapCues = new ArrayList<>();
        List<Integer> gapIndices = new ArrayList<>();
        for (int[] pair : unmatched) {
            gapIndices.add(pair[0]);
            gapCues.add(finalEnCues.get(pair[0]));
        }
        List<String> trTexts = GtxTranslator.translateBatchSync(context, gapCues);
        for (int g = 0; g < gapIndices.size(); g++) {
            if (g < trTexts.size() && !trTexts.get(g).equals(gapCues.get(g).text)) {
                translatedGaps.put(gapIndices.get(g), trTexts.get(g));
            }
        }
    }

    SubtitleMergeEngine.MergeResult mr = SubtitleMergeEngine.merge(finalEnCues, atCues, offset, translatedGaps);
    // ... sisa kode sama
}
```

Tambah import di atas file:
```java
import java.util.HashMap;
import java.util.Map;
```

**Lakukan hal yang sama** untuk callback OpenSubtitles (`runOpenSubtitlesFallback` baris ~226-228).

## Logic Flow Sesudah Fix

```
loadFromTracks(tracks, title, ep, forceRefresh):
  ├─ Cari enTrack dari tracks
  ├─ Download English cues
  ├─ (TIDAK tampilkan English mentah — hanya loading indicator)
  ├─ Cari fansub di AnimeTosho
  │   ├─ Ditemukan → terjemah gap → merge(en, fansub, offset, translatedGaps)
  │   └─ Tidak → cari di OpenSubtitles
  │       ├─ Ditemukan → terjemah gap → merge(en, os, offset, translatedGaps)
  │       └─ Tidak → GTX translate seluruh English
  └─ Set activeCues = hasil Indonesia
```
