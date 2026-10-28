package id.shisen.app.data.subtitle;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import id.shisen.app.data.model.SubtitleTrack;

import static org.junit.Assert.*;

public class SubtitleTrackLanguageInferenceTest {

    @Test
    public void testLanguageInferenceFromVariousLabels() {
        assertEquals("ar", SubtitleTrack.inferLanguage("Arabic", ""));
        assertEquals("ar", SubtitleTrack.inferLanguage("ara", ""));
        assertEquals("en", SubtitleTrack.inferLanguage("English", ""));
        assertEquals("en", SubtitleTrack.inferLanguage("English [Signs & Songs]", ""));
        assertEquals("en", SubtitleTrack.inferLanguage("eng", ""));
        assertEquals("id", SubtitleTrack.inferLanguage("Indonesian", ""));
        assertEquals("id", SubtitleTrack.inferLanguage("Bahasa Indonesia", ""));
        assertEquals("id", SubtitleTrack.inferLanguage("ind", ""));
        assertEquals("ja", SubtitleTrack.inferLanguage("Japanese", ""));
        assertEquals("es", SubtitleTrack.inferLanguage("Spanish (Latin)", ""));
        assertEquals("fr", SubtitleTrack.inferLanguage("French", ""));
        assertEquals("de", SubtitleTrack.inferLanguage("German", ""));
        assertEquals("pt", SubtitleTrack.inferLanguage("Portuguese (Brazil)", ""));
        assertEquals("ru", SubtitleTrack.inferLanguage("Russian", ""));
        assertEquals("ko", SubtitleTrack.inferLanguage("Korean", ""));
        assertEquals("zh", SubtitleTrack.inferLanguage("Chinese", ""));

        // Dummy "lang: en" dari CDN upstream tidak boleh membajak bahasa non-Inggris
        assertEquals("ar", SubtitleTrack.inferLanguage("Arabic", "en"));
        assertEquals("es", SubtitleTrack.inferLanguage("Spanish", "en"));
        assertEquals("fr", SubtitleTrack.inferLanguage("French", "en"));
    }

    @Test
    public void testEnglishTrackSelectionIgnoresArabicFirst() {
        List<SubtitleTrack> tracks = new ArrayList<>();
        // Skenario Megacloud/Megaplay: Arabic di indeks 0 karena abjad
        tracks.add(new SubtitleTrack("https://cdn.example.com/ara.vtt", "ar", "Arabic", false));
        tracks.add(new SubtitleTrack("https://cdn.example.com/eng.vtt", "en", "English", true));
        tracks.add(new SubtitleTrack("https://cdn.example.com/spa.vtt", "es", "Spanish", false));
        tracks.add(new SubtitleTrack("https://cdn.example.com/fre.vtt", "fr", "French", false));

        SubtitleTrack selected = SubtitleTrack.findEnglishTrack(tracks);
        assertNotNull(selected);
        assertEquals("English", selected.label);
        assertEquals("en", selected.lang);
        assertEquals("https://cdn.example.com/eng.vtt", selected.url);
        assertFalse("English selection must NEVER be Arabic", SubtitleTrack.isArabic(selected));
    }

    @Test
    public void testEnglishTrackSelectionWithSignsAndFullTracks() {
        List<SubtitleTrack> tracks = new ArrayList<>();
        tracks.add(new SubtitleTrack("https://cdn.example.com/ara.vtt", "ar", "Arabic", false));
        tracks.add(new SubtitleTrack("https://cdn.example.com/eng-signs.vtt", "en", "English - Signs", false));
        tracks.add(new SubtitleTrack("https://cdn.example.com/eng-full.vtt", "en", "English", true));

        SubtitleTrack selected = SubtitleTrack.findEnglishTrack(tracks);
        assertNotNull(selected);
        // Prioritas 1 adalah exact "English" label
        assertEquals("English", selected.label);
        assertEquals("https://cdn.example.com/eng-full.vtt", selected.url);
    }

    @Test
    public void testFallbackWhenNoEnglishExists() {
        List<SubtitleTrack> tracks = new ArrayList<>();
        tracks.add(new SubtitleTrack("https://cdn.example.com/ara.vtt", "ar", "Arabic", false));
        tracks.add(new SubtitleTrack("https://cdn.example.com/jpn.vtt", "ja", "Japanese", false));

        SubtitleTrack selected = SubtitleTrack.findEnglishTrack(tracks);
        assertNotNull(selected);
        // Memilih track non-Arabic pertama jika tidak ada track English
        assertEquals("Japanese", selected.label);
    }

    @Test
    public void testContainsDominantArabicScriptDetector() {
        List<SubtitleCue> normalIndo = new ArrayList<>();
        normalIndo.add(new SubtitleCue(1000, 2000, "Halo teman-teman! Apa kabar?"));
        normalIndo.add(new SubtitleCue(2500, 4000, "Ayo berangkat sekarang juga!"));
        assertFalse(AiSubtitleService.containsDominantArabicScript(normalIndo));

        List<SubtitleCue> arabicScriptCues = new ArrayList<>();
        arabicScriptCues.add(new SubtitleCue(1000, 2000, "مرحبا كيف حالك اليوم؟"));
        arabicScriptCues.add(new SubtitleCue(2500, 4000, "أنا ذاهب إلى المدرسة الآن"));
        assertTrue(AiSubtitleService.containsDominantArabicScript(arabicScriptCues));

        // Mixed teks dengan sedikit aksara asing tidak boleh salah positif
        List<SubtitleCue> mixed = new ArrayList<>();
        mixed.add(new SubtitleCue(1000, 2000, "Karakter itu bernama Al-Quds (القدس) dalam sejarah"));
        assertFalse(AiSubtitleService.containsDominantArabicScript(mixed));
    }
}
