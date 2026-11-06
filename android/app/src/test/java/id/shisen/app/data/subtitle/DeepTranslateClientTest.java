package id.shisen.app.data.subtitle;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DeepTranslateClientTest {

    @Test
    public void testBuildSrtString() {
        List<SubtitleCue> cues = new ArrayList<>();
        cues.add(new SubtitleCue(1000, 3000, "Hello, world!"));
        cues.add(new SubtitleCue(3500, 6000, "How are you?"));

        String srt = DeepTranslateClient.buildSrtString(cues, 1);
        assertTrue(srt.contains("1\n00:00:01,000 --> 00:00:03,000\nHello, world!"));
        assertTrue(srt.contains("2\n00:00:03,500 --> 00:00:06,000\nHow are you?"));
    }

    @Test
    public void testParseSrtTranslation() {
        List<SubtitleCue> orig = new ArrayList<>();
        orig.add(new SubtitleCue(1000, 3000, "What are you doing here?"));
        orig.add(new SubtitleCue(3500, 6000, "I am just chilling."));

        String responseWithHeader =
                "[Petunjuk: Terjemahkan dialog English anime berikut...]\n\n" +
                "1\n00:00:01,000 --> 00:00:03,000\nKamu lagi ngapain di sini?\n\n" +
                "2\n00:00:03,500 --> 00:00:06,000\nAku cuma lagi santai aja.\n";

        List<SubtitleCue> parsed = DeepTranslateClient.parseSrtTranslation(orig, responseWithHeader);
        assertNotNull(parsed);
        assertEquals(2, parsed.size());
        assertEquals("Kamu lagi ngapain di sini?", parsed.get(0).text);
        assertEquals(1000, parsed.get(0).startMs);
        assertEquals(3000, parsed.get(0).endMs);
        assertEquals("Aku cuma lagi santai aja.", parsed.get(1).text);
        assertEquals(3500, parsed.get(1).startMs);
        assertEquals(6000, parsed.get(1).endMs);
    }
}
