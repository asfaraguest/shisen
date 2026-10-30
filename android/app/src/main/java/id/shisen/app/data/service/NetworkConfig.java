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

    public static final String HIANIMES_SE_BASE = "https://hianimes.se";
    public static final String ANIMEHOT_API_BASE = "https://animehot.cc/api";
    public static final String ANITV_API_BASE = "https://anitv.cfd/api";

    // --- Subtitle CDN Bypass Referers ---
    public static final java.util.List<String> SUBTITLE_REFERER_CANDIDATES = Collections.unmodifiableList(Arrays.asList(
            "https://megaplay.buzz/",
            "https://hianimes.se/",
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
            "silverhaze.xyz",
            "velvet.xyz",
            "aurora.xyz"
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
                || h.contains("openai.com") || h.contains("anilist.co")
                || h.contains("animehot") || h.contains("anitv") || h.contains("hianimes.se")) {
            return false;
        }

        for (String cdn : KNOWN_MEGA_CDN_HOSTS) {
            if (h.equals(cdn) || h.endsWith("." + cdn)) {
                return true;
            }
        }

        // Host pola CDN video / subtitle spesifik
        if (h.contains("megaplay") || h.contains("megacloud")
                || h.contains("rabbitstream") || h.contains("dokicloud")
                || h.contains("rapid-cloud") || h.contains("shiora")
                || h.contains("broforgotsave") || h.contains("velvet")
                || h.contains("aurora")) {
            return true;
        }

        // Deteksi potongan media video streaming (HLS .m3u8, segmen .ts/.m4s) atau subtitle (.vtt/.srt)
        // pada domain edge CDN dengan TLD streaming atau subdomain cdn/stream/play/edge
        String p = path != null ? path.toLowerCase(java.util.Locale.US) : "";
        String f = fullUrl != null ? fullUrl.toLowerCase(java.util.Locale.US) : "";
        boolean isMediaOrTrack = isMediaUrl(p, f);

        if (isMediaOrTrack) {
            return h.endsWith(".world") || h.endsWith(".buzz") || h.endsWith(".top")
                    || h.endsWith(".xyz") || h.endsWith(".to") || h.endsWith(".cc")
                    || h.endsWith(".site") || h.endsWith(".space") || h.endsWith(".cfd")
                    || h.endsWith(".biz") || h.endsWith(".monster") || h.endsWith(".quest")
                    || h.endsWith(".live") || h.endsWith(".stream") || h.endsWith(".cloud")
                    || h.endsWith(".press") || h.endsWith(".lat") || h.endsWith(".fun")
                    || h.endsWith(".lol") || h.endsWith(".tech") || h.endsWith(".link")
                    || h.endsWith(".video") || h.endsWith(".vip") || h.endsWith(".tv")
                    || h.contains("cdn") || h.contains("stream")
                    || h.contains("play") || h.contains("edge");
        }

        return false;
    }

    /**
     * Memeriksa apakah path atau URL merupakan request file media streaming atau subtitle.
     */
    public static boolean isMediaUrl(String path, String fullUrl) {
        String p = path != null ? path.toLowerCase(java.util.Locale.US) : "";
        String f = fullUrl != null ? fullUrl.toLowerCase(java.util.Locale.US) : "";
        return p.contains("/anime/") || p.contains("/ep/")
                || p.contains("/tracks/") || p.contains("/subtitles/")
                || p.endsWith(".m3u8") || p.endsWith(".ts") || p.endsWith(".m4s") || p.endsWith(".mp4")
                || p.endsWith(".vtt") || p.endsWith(".srt")
                || f.contains(".m3u8") || f.contains(".ts") || f.contains(".m4s")
                || f.contains(".vtt") || f.contains(".srt");
    }

    /**
     * Menentukan Referer yang tepat berdasarkan host URL, provider, dan endpoint tujuan
     * untuk mencegah respon HTTP 403 Forbidden.
     */
    public static String resolveAppropriateReferer(String host, String provider, String fullUrl) {
        String h = host != null ? host.toLowerCase(java.util.Locale.US) : "";
        String prov = provider != null ? provider.toLowerCase(java.util.Locale.US) : "";
        String f = fullUrl != null ? fullUrl.toLowerCase(java.util.Locale.US) : "";

        if (h.contains("dramahot") || h.contains("zokoanime") || h.contains("drama1") || f.contains("drama1") || prov.contains("zoko")) {
            return ZOKOANIME_BASE + "/";
        }
        if (h.contains("otakudesu") || h.contains("putarin") || prov.contains("otaku")) {
            return OTAKUDESU_BASE + "/";
        }
        if (h.contains("anichan") || prov.contains("anichan")) {
            return ANICHAN_BASE + "/";
        }
        if (h.contains("animehot") || h.contains("anitv") || h.contains("hianimes.se") || prov.contains("hianimes")) {
            return HIANIMES_SE_BASE + "/";
        }
        if (h.contains("animetosho") || prov.contains("animetosho")) {
            return ANIMETOSHO_BASE + "/";
        }
        if (h.contains("opensubtitles") || prov.contains("opensubtitles")) {
            return OPENSUBTITLES_BASE + "/";
        }
        if (h.contains("hianime.to") || h.contains("hianime.at") || prov.contains("hianime")) {
            return "https://megaplay.buzz/";
        }
        return "https://megaplay.buzz/";
    }
}
