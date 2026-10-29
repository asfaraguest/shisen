package id.shisen.app.data.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Konfigurasi terpusat untuk domain, endpoint resmi, dan CDN video/subtitle Shisen.
 */
public final class NetworkConfig {

    private NetworkConfig() {}

    // --- Official Content & API Providers ---
    public static final String HIANIME_BASE = "https://hianime.at";
    public static final String HIANIME_HOST = "hianime.at";
    public static final String HIANIME_ALT_HOST = "hianime.to";

    public static final String OTAKUDESU_BASE = "https://ww2.otakudesu.biz";
    public static final String OTAKUDESU_HOST = "otakudesu.biz";

    public static final String ANICHAN_BASE = "https://anichan.to";
    public static final String ANICHAN_HOST = "anichan.to";

    public static final String ANIMETOSHO_BASE = "https://animetosho.net";
    public static final String ANIMETOSHO_HOST = "animetosho.net";

    public static final String OPENSUBTITLES_BASE = "https://opensubtitles.org";
    public static final String OPENSUBTITLES_HOST = "opensubtitles.org";

    public static final String ZOKOANIME_BASE = "https://zokoanime.video";

    // --- Subtitle CDN Bypass Referers ---
    public static final java.util.List<String> SUBTITLE_REFERER_CANDIDATES = Collections.unmodifiableList(Arrays.asList(
            "https://megaplay.buzz/",
            "https://hianime.to/",
            "https://hianime.at/",
            "https://zokoanime.video/",
            "https://zoro.to/",
            "https://aniwave.to/",
            "https://kaido.to/",
            "https://animixplay.to/"
    ));

    // --- Known Video & Subtitle Streaming CDNs (Whitelisted for anti-403 referer injection) ---
    private static final Set<String> KNOWN_MEGA_CDN_HOSTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "megaplay.buzz",
            "megacloud.tv",
            "rabbitstream.net",
            "dokicloud.one",
            "rapid-cloud.ru",
            "nexabloom.xyz",
            "ivorysummit.xyz",
            "shiora.xyz",
            "broforgotsave.xyz",
            "eclipseharbor.xyz",
            "emeralddrift.xyz",
            "silverhaze.xyz"
    )));

    /**
     * Memeriksa apakah host merupakan CDN streaming Megaplay/RapidCloud yang membutuhkan
     * header Referer/Origin anti-hotlinking.
     * Mencegah injeksi ke domain sembarang atau API LLM/GitHub.
     */
    public static boolean isMegaCdnHost(String host, String path, String fullUrl) {
        if (host == null) return false;
        String h = host.toLowerCase(java.util.Locale.US);

        // Jangan pernah suntikkan header Megaplay ke provider resmi atau API umum
        if (h.contains("anichan") || h.contains("otakudesu") || h.contains("animetosho")
                || h.contains("opensubtitles") || h.contains("github.com")
                || h.contains("googleapis.com") || h.contains("groq.com")
                || h.contains("openai.com") || h.contains("anilist.co")) {
            return false;
        }

        for (String cdn : KNOWN_MEGA_CDN_HOSTS) {
            if (h.equals(cdn) || h.endsWith("." + cdn)) {
                return true;
            }
        }

        // Host pola CDN video / subtitle spesifik
        return h.contains("megaplay") || h.contains("megacloud")
                || h.contains("rabbitstream") || h.contains("dokicloud")
                || h.contains("rapid-cloud");
    }
}
