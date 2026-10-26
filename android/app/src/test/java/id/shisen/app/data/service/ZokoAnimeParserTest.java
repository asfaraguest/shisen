package id.shisen.app.data.service;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import id.shisen.app.data.model.StreamSource;
import id.shisen.app.data.model.SubtitleTrack;

import static org.junit.Assert.*;

public class ZokoAnimeParserTest {

    /**
     * Helper to create an obfuscated window.__P blob using the otaku-embed-v1 XOR key.
     */
    private static String createObfuscatedBlob(String jsonConfig) {
        byte[] raw = jsonConfig.getBytes(StandardCharsets.UTF_8);
        byte[] xored = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            xored[i] = (byte) (raw[i] ^ ZokoAnimeParser.XOR_KEY[i % ZokoAnimeParser.XOR_KEY.length]);
        }
        return java.util.Base64.getEncoder().encodeToString(xored);
    }

    @Test
    public void testZokoAnimeEmbedParsingFromHtmlFixture() throws Exception {
        String jsonConfig = "{\n" +
                "  \"src\": \"https://hls.dramahot.top/anime/master.m3u8\",\n" +
                "  \"subtitles\": [\n" +
                "    {\"src\": \"https://hls.dramahot.top/sub/eng.vtt\", \"label\": \"English\", \"default\": true},\n" +
                "    {\"src\": \"https://hls.dramahot.top/sub/ind.vtt\", \"label\": \"Indonesian\", \"default\": false}\n" +
                "  ],\n" +
                "  \"intro\": {\"start\": 90, \"end\": 180},\n" +
                "  \"outro\": {\"start\": 1350, \"end\": 1440}\n" +
                "}";

        String blob = createObfuscatedBlob(jsonConfig);

        String htmlFixture = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><title>ZokoAnime Video Player</title></head>\n" +
                "<body>\n" +
                "<div id=\"player\"></div>\n" +
                "<script>\n" +
                "  window.__P = \"" + blob + "\";\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";

        StreamSource source = ZokoAnimeParser.parseEmbed(htmlFixture, "https://zokoanime.video/v/embed-12345");

        assertNotNull(source);
        assertEquals("https://hls.dramahot.top/anime/master.m3u8", source.url);
        assertEquals("ZokoAnime", source.provider);
        assertEquals("https://zokoanime.video/", source.headers.get("Referer"));
        assertEquals("https://zokoanime.video", source.headers.get("Origin"));

        assertEquals(2, source.subtitles.size());
        SubtitleTrack eng = source.subtitles.get(0);
        assertEquals("https://hls.dramahot.top/sub/eng.vtt", eng.url);
        assertEquals("English", eng.label);
        assertEquals("en", eng.lang);
        assertTrue(eng.isDefault);

        SubtitleTrack ind = source.subtitles.get(1);
        assertEquals("https://hls.dramahot.top/sub/ind.vtt", ind.url);
        assertEquals("Indonesian", ind.label);
        assertEquals("id", ind.lang);
        assertFalse(ind.isDefault);
    }

    @Test
    public void testDetect404HtmlPageThrowsException() {
        String notFoundHtml = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><title>Player</title></head>\n" +
                "<body>\n" +
                "<h1>Not found.</h1>\n" +
                "<p>The requested video does not exist on this cluster.</p>\n" +
                "</body>\n" +
                "</html>";

        try {
            ZokoAnimeParser.parseEmbed(notFoundHtml, "https://zokoanime.video/v/not-found-ep");
            fail("404 page must throw IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("404 Not Found"));
        } catch (Exception e) {
            fail("Expected IOException, got: " + e);
        }
    }

    @Test
    public void testParseDemuxedAudioTracksFromHlsMasterPlaylist() {
        String masterM3u8Fixture = "#EXTM3U\n" +
                "#EXT-X-VERSION:4\n" +
                "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio-main\",NAME=\"Japanese\",DEFAULT=YES,AUTOSELECT=YES,LANGUAGE=\"ja\",URI=\"audio/ja.m3u8\"\n" +
                "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio-main\",NAME=\"English Dub\",DEFAULT=NO,AUTOSELECT=NO,LANGUAGE=\"en\",URI=\"https://hls.dramahot.top/audio/en.m3u8\"\n" +
                "#EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1920x1080,AUDIO=\"audio-main\"\n" +
                "video/1080p.m3u8\n";

        String masterUrl = "https://hls.dramahot.top/hls/series/master.m3u8";

        List<ZokoAnimeParser.AudioTrack> tracks = ZokoAnimeParser.parseDemuxedAudioTracks(masterM3u8Fixture, masterUrl);

        assertEquals(2, tracks.size());

        ZokoAnimeParser.AudioTrack ja = tracks.get(0);
        assertEquals("Japanese", ja.name);
        assertEquals("ja", ja.language);
        // Relative URI resolved against masterUrl base
        assertEquals("https://hls.dramahot.top/hls/series/audio/ja.m3u8", ja.uri);
        assertTrue(ja.isDefault);

        ZokoAnimeParser.AudioTrack en = tracks.get(1);
        assertEquals("English Dub", en.name);
        assertEquals("en", en.language);
        // Absolute URI maintained
        assertEquals("https://hls.dramahot.top/audio/en.m3u8", en.uri);
        assertFalse(en.isDefault);
    }
}
