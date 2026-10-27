# 04. GtxTranslator.java — Sync Translate & Validasi Cache

**File**: `android/app/src/main/java/id/menatap/app/data/subtitle/GtxTranslator.java`

## Bug

### Bug A: Tidak ada method sync untuk translate gap

`SubtitleManager` perlu terjemahkan gap cues secara sinkron (sudah di background thread).
Saat ini `translateAsync` hanya async dengan callback. Perlu tambah `translateBatchSync()`.

### Bug B: Cache menyimpan English gagal terjemah

Baris 83-86 di `translateBatch()`:
```java
// Jika translate gagal, fallback ke teks asli (English)
res.add(text != null && !text.isEmpty() ? text : batch.get(i).text);
```

Lalu baris 98-101, hasil (termasuk English fallback) disimpan ke cache:
```java
String jsonOut = serializeCues(sanitizedCues);
writeFile(cacheFile, jsonOut);  // ← English mentah ter-cache permanen!
```

Akibat: play berikutnya selalu return English dari cache.

## Perubahan

### A. Tambah `translateBatchSync()`

Tambahkan method public static baru:

```java
/**
 * Terjemahkan batch cue secara sinkron. Panggil dari background thread saja.
 * Dipakai oleh SubtitleManager untuk terjemah gap cues (Signs/OP/ED) sebelum merge.
 *
 * @return List<String> teks terjemahan, urutan sama dengan input cues.
 *         Jika gagal, kembalikan teks asli (English) — caller harus cek kesamaan.
 */
public static List<String> translateBatchSync(Context context, List<SubtitleCue> cues) {
    if (cues == null || cues.isEmpty()) return new ArrayList<>();
    OkHttpClient client = HttpClientProvider.getClient();
    List<String> results = new ArrayList<>();
    for (int i = 0; i < cues.size(); i += BATCH_SIZE) {
        int end = Math.min(i + BATCH_SIZE, cues.size());
        List<SubtitleCue> batch = cues.subList(i, end);
        List<String> tr = translateBatch(client, batch);
        results.addAll(tr);
    }
    return results;
}
```

`translateBatch()` sudah `private static` — tidak perlu ubah visibility karena `translateBatchSync` ada di class yang sama.

### B. Validasi cache — jangan simpan jika gagal terjemah

**SEBELUM** (baris 97-101):
```java
// 2. Simpan Hasil ke Disk Cache
try {
    String jsonOut = serializeCues(sanitizedCues);
    writeFile(cacheFile, jsonOut);
} catch (Exception ignored) {}
```

**SESUDAH**:
```java
// 2. Simpan Hasil ke Disk Cache — HANYA jika terjemahan berhasil
// Deteksi gagal: jika >80% cue sama persis dengan English input, berarti GTX gagal
int sameCount = 0;
for (int ci = 0; ci < sanitizedCues.size() && ci < cues.size(); ci++) {
    if (sanitizedCues.get(ci).text.equals(cues.get(ci).text)) {
        sameCount++;
    }
}
boolean translationSuccessful = cues.isEmpty()
        || (sameCount * 100 / cues.size()) <= 80;

if (translationSuccessful) {
    try {
        String jsonOut = serializeCues(sanitizedCues);
        writeFile(cacheFile, jsonOut);
    } catch (Exception ignored) {}
}
```

## Logic Flow

```
translateAsync(sourceId, englishCues, forceRefresh, callback):
  ├─ Cek cache → ada & valid? → return dari cache
  ├─ Loop batch (25 cue per request):
  │   └─ translateBatch(client, batch) → Google GTX API
  ├─ Sanitize dengan SmartLexicon
  ├─ Validasi: >80% sama dengan English? → JANGAN cache
  └─ callback.onSuccess(sanitizedCues)

translateBatchSync(context, gapCues):  [BARU]
  ├─ Loop batch (25 cue per request):
  │   └─ translateBatch(client, batch) → Google GTX API
  └─ Return List<String> teks terjemahan
```

## Catatan

`translateBatchSync` **tidak pakai cache** karena:
- Hanya untuk gap cues kecil (≤180 cue, biasanya <20)
- Hasilnya masuk ke `SubtitleMergeEngine.merge()` yang sudah di-cache di level `SubtitleManager`
- Menghindari complexity cache key tambahan
