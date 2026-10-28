package id.shisen.app.data.subtitle;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SubtitleManagerIsolationTest {

    private SubtitleManager manager;

    @Before
    public void setUp() {
        // SubtitleManager constructor accepts null context in unit testing environment
        manager = new SubtitleManager(null);
    }

    @Test
    public void testSetModeAndEngineIsolation() {
        manager.setMode("id", "smart_merge");
        assertEquals("id", manager.getActiveLanguage());
        assertEquals("smart_merge", manager.getActiveEngine());

        manager.setMode("id", "ai");
        assertEquals("id", manager.getActiveLanguage());
        assertEquals("ai", manager.getActiveEngine());

        manager.setMode("en", "raw");
        assertEquals("en", manager.getActiveLanguage());
        assertEquals("raw", manager.getActiveEngine());

        manager.setMode("ja", "raw");
        assertEquals("ja", manager.getActiveLanguage());
        assertEquals("raw", manager.getActiveEngine());
    }

    @Test
    public void testClearCuesAtomicReset() {
        List<SubtitleCue> dummyCues = new ArrayList<>();
        dummyCues.add(new SubtitleCue(1000, 3000, "Halo Dunia"));
        dummyCues.add(new SubtitleCue(4000, 6000, "Sampai Jumpa"));

        manager.setCustomCues(dummyCues, "Test Track");
        assertTrue(manager.getActiveCuesCount() > 0);
        assertEquals("Halo Dunia", manager.getCurrentText(2000));

        manager.clearCues();
        assertEquals(0, manager.getActiveCuesCount());
        assertEquals("", manager.getCurrentText(2000));
    }

    @Test
    public void testSetCustomCuesIncrementsLoadIdAndIsolatesState() {
        int initialLoadId = manager.getCurrentLoadId();

        List<SubtitleCue> cues = new ArrayList<>();
        cues.add(new SubtitleCue(500, 1500, "Custom Sub"));

        manager.setCustomCues(cues, "Custom User File");

        assertTrue("setCustomCues must increment currentLoadId to cancel inflight AI jobs",
                manager.getCurrentLoadId() > initialLoadId);
        assertEquals("custom", manager.getActiveEngine());
        assertEquals("custom", manager.getActiveLanguage());
        assertEquals("Custom User File", manager.getActiveLabel());
        assertEquals("Custom Sub", manager.getCurrentText(1000));
    }

    @Test
    public void testDisablingSubtitlesIncrementsLoadIdAndClearsCues() {
        List<SubtitleCue> cues = new ArrayList<>();
        cues.add(new SubtitleCue(1000, 2000, "Active Subtitle"));
        manager.setCustomCues(cues, "Active");

        int loadIdBefore = manager.getCurrentLoadId();
        assertTrue(manager.isSubEnabled());
        assertEquals("Active Subtitle", manager.getCurrentText(1500));

        // Disable subtitles
        manager.setSubEnabled(false);

        assertFalse(manager.isSubEnabled());
        assertTrue("Disabling subtitles must increment currentLoadId to cancel background jobs",
                manager.getCurrentLoadId() > loadIdBefore);
        assertEquals("getCurrentText must return empty when disabled", "", manager.getCurrentText(1500));
        assertEquals(0, manager.getActiveCuesCount());
    }

    @Test
    public void testStaleInflightAiJobRejectedByLoadIdGuard() {
        int simulatedAiJobLoadId = manager.getCurrentLoadId();

        // User switches to Raw Track or disables subtitles while AI is translating
        manager.setMode("en", "raw");
        manager.setSubEnabled(false); // Increments currentLoadId

        int activeLoadId = manager.getCurrentLoadId();
        assertNotEquals("Active load ID must differ from simulated AI job", simulatedAiJobLoadId, activeLoadId);

        // Simulated AI translation callback finishing late
        boolean isStale = (simulatedAiJobLoadId != manager.getCurrentLoadId());
        assertTrue("Late AI translation must be identified as stale and discarded", isStale);
    }
}
