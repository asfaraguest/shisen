# 03. SubtitleMergeEngine.java — Terjemahkan Gap, Jangan Inject English

**File**: `android/app/src/main/java/id/menatap/app/data/subtitle/SubtitleMergeEngine.java`

## Bug

Baris 119-126 — cue bawaan (English) yang tidak cocok dengan fansub Indonesia langsung masuk ke merged **tanpa terjemahan**:

```java
for (int b = 0; b < builtinCues.size(); b++) {
    if (!matchedBuiltinIndices.contains(b)) {
        SubtitleCue bc = builtinCues.get(b);
        if (!bc.text.isEmpty()) {
            merged.add(new SubtitleCue(bc.startMs, bc.endMs, bc.text));  // ← bc.text = ENGLISH
            gapFilledCount++;
        }
    }
}
```

**Tatap reference** (`translate.py#L340-L353`):
```python
for b_idx, bc in enumerate(clean_builtin):
    if b_idx in matched_builtin_indices:
        continue
    orig_b_txt = (bc.get("text") or "").strip()
    if not orig_b_txt:
        continue
    # Gunakan TERJEMAHAN jika ada, bukan English mentah
    tr_b_txt = (translated_gaps.get(b_idx) or orig_b_txt).strip()
    if is_id_lang and tr_b_txt:
        tr_b_txt = apply_universal_lexicon_id(tr_b_txt)
    gap_txt = tr_b_txt
    merged.append({"start": bs, "end": be, "text": gap_txt})
```

Catatan: Tatap masih fallback ke `orig_b_txt` (English) jika terjemahan tidak tersedia.
Di Android kita **skip** saja jika tidak ada terjemahan — lebih aman daripada inject English ke subtitle Indonesia.

## Perubahan

### A. Tambah `findUnmatchedBuiltinIndices()`

Tambahkan method baru (logic dari `translate.py#L221-L245`):

```java
/**
 * Cari indeks cue bawaan yang TIDAK cocok dengan external cues.
 * Ref: translate.py find_unmatched_builtin_cues()
 *
 * @return List of int[] dimana int[0] = index di builtinCues
 */
public static List<int[]> findUnmatchedBuiltinIndices(
        List<SubtitleCue> builtinCues,
        List<SubtitleCue> externalCues,
        long offsetMs) {
    List<int[]> result = new ArrayList<>();
    if (builtinCues == null || externalCues == null) return result;

    for (int b = 0; b < builtinCues.size(); b++) {
        SubtitleCue bc = builtinCues.get(b);
        if (bc.text.isEmpty()) continue;

        boolean matched = false;
        for (SubtitleCue oc : externalCues) {
            long os = Math.max(0, oc.startMs + offsetMs);
            long oe = Math.max(os + 100, oc.endMs + offsetMs);
            long overlap = Math.min(oe, bc.endMs) - Math.max(os, bc.startMs);
            if (overlap >= -250 || Math.abs(os - bc.startMs) <= 650) {
                matched = true;
                break;
            }
        }

        if (!matched) {
            result.add(new int[]{b});
        }
    }
    return result;
}
```

### B. Tambah parameter `translatedGaps` di `merge()`

**SEBELUM**:
```java
public static MergeResult merge(List<SubtitleCue> builtinCues, List<SubtitleCue> externalCues, long offsetMs) {
```

**SESUDAH** — tambah overload baru, pertahankan yang lama:

```java
// Overload lama tetap ada agar kode lain tidak break
public static MergeResult merge(List<SubtitleCue> builtinCues, List<SubtitleCue> externalCues, long offsetMs) {
    return merge(builtinCues, externalCues, offsetMs, null);
}

// Method utama dengan translatedGaps
public static MergeResult merge(List<SubtitleCue> builtinCues, List<SubtitleCue> externalCues,
                                 long offsetMs, Map<Integer, String> translatedGaps) {
```

Pindahkan seluruh isi method lama ke method baru.

### C. Ubah gap-fill section

**SEBELUM** (baris 118-127):
```java
// Isi celah dari subtitle bawaan yang tidak ada di rilis fansub eksternal (Signs, OP/ED)
for (int b = 0; b < builtinCues.size(); b++) {
    if (!matchedBuiltinIndices.contains(b)) {
        SubtitleCue bc = builtinCues.get(b);
        if (!bc.text.isEmpty()) {
            merged.add(new SubtitleCue(bc.startMs, bc.endMs, bc.text));
            gapFilledCount++;
        }
    }
}
```

**SESUDAH**:
```java
// Isi celah dari subtitle bawaan yang tidak ada di rilis fansub eksternal (Signs, OP/ED)
// Ref: translate.py#L340-L353
for (int b = 0; b < builtinCues.size(); b++) {
    if (!matchedBuiltinIndices.contains(b)) {
        SubtitleCue bc = builtinCues.get(b);
        if (!bc.text.isEmpty()) {
            // Gunakan terjemahan jika tersedia
            String gapText = (translatedGaps != null) ? translatedGaps.get(b) : null;
            if (gapText != null && !gapText.isEmpty()) {
                merged.add(new SubtitleCue(bc.startMs, bc.endMs, gapText));
                gapFilledCount++;
            }
            // Jika tidak ada terjemahan → SKIP, jangan inject English mentah
        }
    }
}
```

Tambah import:
```java
import java.util.Map;
```

## Logic Flow Sesudah Fix

```
merge(builtin=English, external=FansubID, offset, translatedGaps):
  ├─ Loop external cues:
  │   ├─ Shift timing + offset
  │   ├─ Snap ke builtin timing jika dekat
  │   ├─ Tandai builtin yg cocok (matchedBuiltinIndices)
  │   └─ Tambah ke merged (teks fansub Indonesia)
  │
  └─ Loop builtin cues yg TIDAK cocok (Signs/OP/ED):
      ├─ Ada terjemahan di translatedGaps? → tambah ke merged (teks Indonesia)
      └─ Tidak ada terjemahan? → SKIP (jangan inject English)
```
