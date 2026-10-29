package id.shisen.app.data.service;

import org.junit.Test;

import static org.junit.Assert.*;

public class NetworkConfigTest {

    @Test
    public void testMegaCdnWhitelistDetection() {
        assertTrue(NetworkConfig.isMegaCdnHost("megaplay.buzz", "/subtitles/1.vtt", "https://megaplay.buzz/subtitles/1.vtt"));
        assertTrue(NetworkConfig.isMegaCdnHost("rabbitstream.net", "/embed-4/track", "https://rabbitstream.net/embed-4/track"));
        assertTrue(NetworkConfig.isMegaCdnHost("sub.dokicloud.one", "/track/vtt", "https://sub.dokicloud.one/track/vtt"));
        assertTrue(NetworkConfig.isMegaCdnHost("rapid-cloud.ru", "/ajax/get_link", "https://rapid-cloud.ru/ajax/get_link"));
    }

    @Test
    public void testExcludedHostsNeverTreatedAsMegaCdn() {
        // API dan domain eksternal penting TIDAK BOLEH dianggap Mega CDN
        assertFalse(NetworkConfig.isMegaCdnHost("api.github.com", "/repos/asfaraguest/shisen/releases", "https://api.github.com/repos/asfaraguest/shisen/releases"));
        assertFalse(NetworkConfig.isMegaCdnHost("generativelanguage.googleapis.com", "/v1beta/models", "https://generativelanguage.googleapis.com/v1beta/models"));
        assertFalse(NetworkConfig.isMegaCdnHost("api.groq.com", "/openai/v1/chat/completions", "https://api.groq.com/openai/v1/chat/completions"));
        assertFalse(NetworkConfig.isMegaCdnHost("api.openai.com", "/v1/chat/completions", "https://api.openai.com/v1/chat/completions"));
        assertFalse(NetworkConfig.isMegaCdnHost("graphql.anilist.co", "/", "https://graphql.anilist.co/"));

        // Host provider resmi tidak disuntik referer megaplay
        assertFalse(NetworkConfig.isMegaCdnHost("anichan.to", "/anime/watch", "https://anichan.to/anime/watch"));
        assertFalse(NetworkConfig.isMegaCdnHost("ww2.otakudesu.biz", "/episode/1", "https://ww2.otakudesu.biz/episode/1"));
        assertFalse(NetworkConfig.isMegaCdnHost("animetosho.net", "/series/1", "https://animetosho.net/series/1"));
        assertFalse(NetworkConfig.isMegaCdnHost("opensubtitles.org", "/api/v1", "https://opensubtitles.org/api/v1"));

        // Domain acak dengan TLD .to / .xyz yang bukan CDN
        assertFalse(NetworkConfig.isMegaCdnHost("random-site.to", "/page", "https://random-site.to/page"));
        assertFalse(NetworkConfig.isMegaCdnHost("example.xyz", "/index.html", "https://example.xyz/index.html"));
    }
}
