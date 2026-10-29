package id.shisen.app.data.db;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlaybackProgressTest {

    @Test
    public void testShouldResumeBelowThresholdReturnsFalse() {
        // Kurang dari 15 detik dianggap buka tidak sengaja / intro awal -> jangan resume
        MenatapDatabase.PlaybackProgress p = new MenatapDatabase.PlaybackProgress(14_999, 1_440_000);
        assertFalse(p.shouldResume());
    }

    @Test
    public void testShouldResumeMidVideoReturnsTrue() {
        // Menit ke-2 dari 24 menit -> harus resume
        MenatapDatabase.PlaybackProgress p = new MenatapDatabase.PlaybackProgress(120_000, 1_440_000);
        assertTrue(p.shouldResume());
    }

    @Test
    public void testShouldResumeNearEndReturnsFalse() {
        // Berada di 30 detik terakhir (kredit/ending) -> anggap selesai, jangan resume
        MenatapDatabase.PlaybackProgress p = new MenatapDatabase.PlaybackProgress(1_420_000, 1_440_000);
        assertFalse(p.shouldResume());
    }

    @Test
    public void testFormattedTime() {
        MenatapDatabase.PlaybackProgress p1 = new MenatapDatabase.PlaybackProgress(65_000, 1_440_000);
        assertEquals("01:05", p1.getFormattedTime());

        MenatapDatabase.PlaybackProgress p2 = new MenatapDatabase.PlaybackProgress(0, 0);
        assertEquals("00:00", p2.getFormattedTime());

        MenatapDatabase.PlaybackProgress p3 = new MenatapDatabase.PlaybackProgress(754_000, 1_440_000);
        assertEquals("12:34", p3.getFormattedTime());
    }
}
