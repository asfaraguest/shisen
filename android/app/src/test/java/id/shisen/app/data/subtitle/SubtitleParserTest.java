package id.shisen.app.data.subtitle;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SubtitleParserTest {

    @Test
    public void testParseAssStandardDialogue() {
        String ass = "[Events]\n"
                + "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n"
                + "Dialogue: 0,0:00:01.00,0:00:03.50,Default,,0,0,0,,Halo dunia... Ini *spesial*!\n"
                + "Dialogue: 1,0:00:04.20,0:00:06.80,Alt,Actor,0,0,0,Fx,Halo, teman, apa kabar?\n";

        List<SubtitleCue> cues = SubtitleParser.parse(ass);
        assertEquals(2, cues.size());

        SubtitleCue cue1 = cues.get(0);
        assertEquals(1000L, cue1.startMs);
        assertEquals(3500L, cue1.endMs);
        assertEquals("Halo dunia... Ini *spesial*!", cue1.text);

        SubtitleCue cue2 = cues.get(1);
        assertEquals(4200L, cue2.startMs);
        assertEquals(6800L, cue2.endMs);
        assertEquals("Halo, teman, apa kabar?", cue2.text);
    }

    @Test
    public void testParseAssWithOverrideTags() {
        String ass = "[Events]\n"
                + "Dialogue: 0,0:01:00.00,0:01:05.00,Default,,0,0,0,,{\\b1}Teks Tebal{\\b0}\\NBaris Kedua\n";

        List<SubtitleCue> cues = SubtitleParser.parse(ass);
        assertEquals(1, cues.size());
        assertEquals(60000L, cues.get(0).startMs);
        assertEquals(65000L, cues.get(0).endMs);
        assertEquals("Teks Tebal\nBaris Kedua", cues.get(0).text);
    }

    @Test
    public void testParseSrtFormat() {
        String srt = "1\n"
                + "00:00:01,000 --> 00:00:04,000\n"
                + "Halo dari SRT\n\n"
                + "2\n"
                + "00:00:05,000 --> 00:00:08,000\n"
                + "Baris kedua SRT\n";

        List<SubtitleCue> cues = SubtitleParser.parse(srt);
        assertEquals(2, cues.size());
        assertEquals(1000L, cues.get(0).startMs);
        assertEquals(4000L, cues.get(0).endMs);
        assertEquals("Halo dari SRT", cues.get(0).text);
    }
}
