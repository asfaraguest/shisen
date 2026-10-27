# 01. PlayerActivity.java — Pisahkan Jalur Opsi Subtitle

**File**: `android/app/src/main/java/id/menatap/app/ui/player/PlayerActivity.java`

## Bug

Option 1 (`[ID] Smart Fansub`) dan Option 3 (`[EN] English Original`) sama-sama panggil `subtitleManager.loadFromTracks(...)`.

`loadFromTracks()` di dalamnya cek `activeLanguage` dan `activeEngine` untuk menentukan mode.
Akibatnya: jika user pernah pilih English (`activeLanguage = "en"`), lalu pilih Smart ID,
`loadFromTracks` melihat `"en".equals(activeLanguage)` → load English lagi.

**Tatap reference** (`app.js#L1669-L1707`):
```javascript
// Setiap opsi punya jalur eksplisit, tidak saling bocor
if (url.indexOf("os_merge:") === 0) {
    // Jalur Smart Merge → set mode, switch track
    cur.translate = false;
    switchSubtitleTrack(cleanTarget, "id");
    cur.subLang = "Indonesian (Smart Fansub & Merge)";
    return;
}
// URL biasa → langsung switch track raw
switchSubtitleTrack(url);
```

## Perubahan

### A. Option 1 — Smart Fansub ID: Set state eksplisit sebelum load

**Lokasi**: baris ~1640-1646

**SEBELUM**:
```java
actions.add(() -> {
    subtitleManager.setSubEnabled(true);
    if (currentStreamSource != null) {
        String safeTitle = (anime.title != null && !anime.title.isEmpty()) ? anime.title : anime.romajiTitle;
        subtitleManager.loadFromTracks(currentStreamSource.subtitles, safeTitle, episodeList.get(currentIndex).number, false);
    }
});
```

**SESUDAH**:
```java
actions.add(() -> {
    subtitleManager.setSubEnabled(true);
    subtitleManager.setMode("id", "smart_merge"); // Set state SEBELUM load
    if (currentStreamSource != null) {
        String safeTitle = (anime.title != null && !anime.title.isEmpty()) ? anime.title : anime.romajiTitle;
        subtitleManager.loadFromTracks(currentStreamSource.subtitles, safeTitle, episodeList.get(currentIndex).number, false);
    }
});
```

### B. Option 3 — English Original: Jangan pakai loadFromTracks, langsung loadRawTrack

**Lokasi**: baris ~1674-1680

**SEBELUM**:
```java
actions.add(() -> {
    subtitleManager.setSubEnabled(true);
    if (currentStreamSource != null) {
        String safeTitle = (anime.title != null && !anime.title.isEmpty()) ? anime.title : anime.romajiTitle;
        subtitleManager.loadFromTracks(currentStreamSource.subtitles, safeTitle, episodeList.get(currentIndex).number, false);
    }
});
```

**SESUDAH**:
```java
actions.add(() -> {
    subtitleManager.setSubEnabled(true);
    if (currentStreamSource != null) {
        SubtitleTrack enTrack = findEnglishTrack(currentStreamSource.subtitles);
        if (enTrack != null) {
            subtitleManager.loadRawTrack(enTrack.url, "English (Original)", "en", enTrack.referer);
        } else if (!currentStreamSource.subtitles.isEmpty()) {
            SubtitleTrack first = currentStreamSource.subtitles.get(0);
            subtitleManager.loadRawTrack(first.url, first.label, first.lang, first.referer);
        }
    }
});
```

### C. Tambah helper `findEnglishTrack`

Tambahkan di `PlayerActivity.java` (di area method private):

```java
private SubtitleTrack findEnglishTrack(List<SubtitleTrack> tracks) {
    if (tracks == null) return null;
    for (SubtitleTrack t : tracks) {
        String l = t.lang.toLowerCase();
        if (l.contains("en") || t.label.toLowerCase().contains("english")) {
            return t;
        }
    }
    return null;
}
```

## Logic Flow Sesudah Fix

```
User pilih "Smart Fansub ID":
  → setMode("id", "smart_merge")
  → loadFromTracks()  ← sekarang SELALU masuk jalur smart merge

User pilih "English Original":
  → loadRawTrack(enTrack.url, "English", "en")  ← langsung raw, tidak lewat loadFromTracks

User pilih "Smart Fansub ID" lagi:
  → setMode("id", "smart_merge")  ← reset state
  → loadFromTracks()  ← masuk smart merge, BUKAN English
```

Tidak ada state yang bocor karena setiap opsi set state sendiri atau bypass `loadFromTracks` sama sekali.
