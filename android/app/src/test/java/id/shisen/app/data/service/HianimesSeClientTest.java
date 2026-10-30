package id.shisen.app.data.service;

import org.junit.Test;

import id.shisen.app.data.model.StreamSource;

import static org.junit.Assert.*;

public class HianimesSeClientTest {

    @Test(expected = IllegalArgumentException.class)
    public void testResolveByMalIdInvalidMalId() throws Exception {
        HianimesSeClient.resolveByMalIdSync(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveByMalIdInvalidEpisode() throws Exception {
        HianimesSeClient.resolveByMalIdSync(57334, 0);
    }

    @Test
    public void testStreamSourceSkipTimesModel() {
        StreamSource source = new StreamSource("https://example.com/stream.m3u8", "1080p", "HiAnimes (Megaplay)", true);
        assertNull(source.skipTimes);

        AniSkipClient.SkipResult skips = new AniSkipClient.SkipResult();
        skips.intro = new AniSkipClient.SkipInterval(10.5, 95.0, "op");
        skips.outro = new AniSkipClient.SkipInterval(1300.0, 1390.0, "ed");
        source.skipTimes = skips;

        assertNotNull(source.skipTimes);
        assertEquals(10.5, source.skipTimes.intro.startTime, 0.001);
        assertEquals(95.0, source.skipTimes.intro.endTime, 0.001);
        assertEquals("op", source.skipTimes.intro.type);
        assertEquals(1300.0, source.skipTimes.outro.startTime, 0.001);
        assertEquals(1390.0, source.skipTimes.outro.endTime, 0.001);
        assertEquals("ed", source.skipTimes.outro.type);
    }
}
