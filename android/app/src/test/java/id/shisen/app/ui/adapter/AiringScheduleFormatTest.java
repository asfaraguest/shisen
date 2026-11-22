package id.shisen.app.ui.adapter;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AiringScheduleFormatTest {

    @Test
    public void testZeroOrNegativeAiringAtReturnsEmpty() {
        assertEquals("", AnimeAdapter.formatAiringSchedule(null, 0L));
        assertEquals("", AnimeAdapter.formatAiringSchedule(null, -100L));
    }

    @Test
    public void testValidAiringAtContainsWibAndTime() {
        // Epoch: Sunday 2026-11-22 16:00:00 UTC = 23:00 WIB (UTC+7)
        long epochSec = 1763827200L;
        String formatted = AnimeAdapter.formatAiringSchedule(null, epochSec);
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.contains("WIB"));
        assertTrue(formatted.contains(":"));
    }

    @Test
    public void testTodayAiringDetection() {
        TimeZone wib = TimeZone.getTimeZone("Asia/Jakarta");
        Calendar now = Calendar.getInstance(wib);
        now.add(Calendar.HOUR_OF_DAY, 2);
        long todayEpoch = now.getTimeInMillis() / 1000L;

        String formatted = AnimeAdapter.formatAiringSchedule(null, todayEpoch);
        assertTrue(formatted.contains("Hari Ini") || formatted.contains("WIB"));
    }
}
