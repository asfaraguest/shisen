package id.shisen.app.data.service;

import org.junit.Test;

import java.util.List;

import id.shisen.app.data.model.Episode;

import static org.junit.Assert.*;

public class AdaptiveScrapingTest {

    @Test
    public void testHiAnimeEpisodeParsing_NormalAndReversedAttributes() {
        // Sample HTML dengan urutan data-number dan data-id normal serta terbalik
        String sampleHtml = "<div class=\"ss-list\">"
                + "<a class=\"ssl-item ep-item\" data-number=\"1\" data-id=\"1001\" title=\"Prologue\">1</a>"
                + "<a class=\"ssl-item ep-item\" data-id=\"1002\" data-number=\"2\" title=\"Departure\">2</a>"
                + "<div class=\"ssl-item\"><span data-id=\"1003\" data-number=\"3\">3</span></div>"
                + "</div>";

        List<Episode> episodes = HiAnimeClient.parseEpisodesFromHtml(sampleHtml, "test-anime-1234");
        assertEquals(3, episodes.size());

        assertEquals(1, episodes.get(0).number);
        assertEquals("1001", episodes.get(0).epId);
        assertEquals("Prologue", episodes.get(0).title);

        assertEquals(2, episodes.get(1).number);
        assertEquals("1002", episodes.get(1).epId);
        assertEquals("Departure", episodes.get(1).title);

        assertEquals(3, episodes.get(2).number);
        assertEquals("1003", episodes.get(2).epId);
    }

    @Test
    public void testHiAnimeEpisodeParsing_RegexFallbackReversed() {
        // Sample string tanpa tag kelas Jsoup standar, menguji regex attribute-agnostic fallback
        String rawHtml = "<custom-tag data-id=\"999\" data-number=\"99\"></custom-tag>";
        List<Episode> episodes = HiAnimeClient.parseEpisodesFromHtml(rawHtml, "slug-test-55");
        assertEquals(1, episodes.size());
        assertEquals(99, episodes.get(0).number);
        assertEquals("999", episodes.get(0).epId);
    }

    @Test
    public void testOtakudesuEpisodeNumberParsing_TextVariations() {
        assertEquals(12, OtakudesuScraper.parseEpisodeNumber("Episode 12 Subtitle Indonesia"));
        assertEquals(5, OtakudesuScraper.parseEpisodeNumber("Eps 5 Indo"));
        assertEquals(8, OtakudesuScraper.parseEpisodeNumber("Ep. 8"));
        assertEquals(3, OtakudesuScraper.parseEpisodeNumber("Ke-3"));
        assertEquals(24, OtakudesuScraper.parseEpisodeNumber("24"));
        assertEquals(16, OtakudesuScraper.parseEpisodeNumber("#16"));

        // Batch / Rangkuman tidak boleh dianggap episode biasa
        assertEquals(0, OtakudesuScraper.parseEpisodeNumber("Full Batch 1-12 Subtitle Indonesia"));
        assertEquals(0, OtakudesuScraper.parseEpisodeNumber("Batch Lengkap"));
    }

    @Test
    public void testOtakudesuEpisodeNumberParsing_SlugFallback() {
        // Link yang teksnya generik ("Lihat", "Nonton Anime", "Download") mengandalkan slug URL
        assertEquals(8, OtakudesuScraper.parseEpisodeNumber("Nonton Anime", "/anime/dandadan-episode-8-sub-indo/"));
        assertEquals(10, OtakudesuScraper.parseEpisodeNumber("Lihat", "https://otakudesu.biz/dandadan-eps-10/"));
        assertEquals(4, OtakudesuScraper.parseEpisodeNumber("Download Video", "/dandadan-ep-4/"));
        assertEquals(2, OtakudesuScraper.parseEpisodeNumber("", "https://otakudesu.biz/anime/bleach-ke-2/"));
    }

    @Test
    public void testModernCdnTldDetection() {
        // TLD modern edge CDN (.space, .cfd, .biz, .monster, .quest, .live, .stream, dll)
        assertTrue(NetworkConfig.isMegaCdnHost("edge1.streamcdn.space", "/anime/master.m3u8", "https://edge1.streamcdn.space/anime/master.m3u8"));
        assertTrue(NetworkConfig.isMegaCdnHost("stream-node.cfd", "/chunk.ts", "https://stream-node.cfd/chunk.ts"));
        assertTrue(NetworkConfig.isMegaCdnHost("cluster.play.biz", "/video/track.m4s", "https://cluster.play.biz/video/track.m4s"));
        assertTrue(NetworkConfig.isMegaCdnHost("fast-edge.monster", "/stream.m3u8", "https://fast-edge.monster/stream.m3u8"));
        assertTrue(NetworkConfig.isMegaCdnHost("media.quest", "/playlist.m3u8", "https://media.quest/playlist.m3u8"));
        assertTrue(NetworkConfig.isMegaCdnHost("live-video.live", "/hls/stream.m3u8", "https://live-video.live/hls/stream.m3u8"));
    }

    @Test
    public void testResolveAppropriateReferer() {
        assertEquals("https://ww2.otakudesu.biz/", NetworkConfig.resolveAppropriateReferer("putarin.top", "otakudesu", "https://putarin.top/stream/123.m3u8"));
        assertEquals("https://anichan.to/", NetworkConfig.resolveAppropriateReferer("cdn.anichan.to", "anichan", "https://cdn.anichan.to/ep.m3u8"));
        assertEquals("https://zokoanime.video/", NetworkConfig.resolveAppropriateReferer("edge.zokoanime.video", "zokoanime", "https://edge.zokoanime.video/hls.m3u8"));
        assertEquals("https://zokoanime.video/", NetworkConfig.resolveAppropriateReferer("hls1.drama1.cfd", "hianimes_se", "https://hls1.drama1.cfd/s/seg_00000.ts.jpg"));
        assertEquals("https://zokoanime.video/", NetworkConfig.resolveAppropriateReferer("hls.dramahot.top", "hianimes_se", "https://hls.dramahot.top/anime/master.m3u8"));
        assertEquals("https://hianimes.se/", NetworkConfig.resolveAppropriateReferer("edge.animehot.cc", "hianimes", "https://edge.animehot.cc/stream.m3u8"));
        assertEquals("https://megaplay.buzz/", NetworkConfig.resolveAppropriateReferer("dokicloud.one", "hianime", "https://dokicloud.one/ep1.m3u8"));
    }

    @Test
    public void testDnsFallbackResolverKnownIps() throws Exception {
        id.shisen.app.util.DnsFallbackResolver resolver = new id.shisen.app.util.DnsFallbackResolver();
        List<java.net.InetAddress> ipsZoko = resolver.lookup("zokoanime.video");
        assertNotNull(ipsZoko);
        assertFalse(ipsZoko.isEmpty());

        List<java.net.InetAddress> ipsDrama1 = resolver.lookup("hls1.drama1.cfd");
        assertNotNull(ipsDrama1);
        assertFalse(ipsDrama1.isEmpty());
    }
}
