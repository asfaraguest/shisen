package id.shisen.app.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DynamicBlockedHosts maintains the set of hostnames that require TLS ClientHello
 * DPI-bypass desynchronization. Hosts are pre-seeded with known Indonesian ISP blocked
 * video CDNs (from Tatap OG parity) and dynamically learned upon connection failures
 * (timeouts, connection resets, or SSL handshake resets).
 */
public final class DynamicBlockedHosts {

    private static final Set<String> KNOWN_DPI_KEYWORDS = new HashSet<>(Arrays.asList(
            "dramahot", "zokoanime", "drama1", "puterin", "putarin", "stream121",
            "mysubsplay", "nexabloom", "emeralddrift", "megacloud", "megaplay",
            "rapid-cloud", "rabbitstream"
    ));

    private static final Set<String> KNOWN_DPI_HOSTS = new HashSet<>(Arrays.asList(
            "hls.dramahot.top", "dramahot.top", "drama1.cfd", "hls1.drama1.cfd",
            "puterin.biz", "putarin.xyz", "stream121.space", "zokoanime.video",
            "megacloud.tv", "megaplay.buzz", "rapid-cloud.co", "rabbitstream.net"
    ));

    private static final Set<String> WHITELISTED_HOSTS = new HashSet<>(Arrays.asList(
            "clients5.google.com", "translate.google.com", "translate.googleapis.com",
            "graphql.anilist.co", "anilist.co", "api.aniskip.com", "raw.githubusercontent.com",
            "hianime.to", "hianime.sx", "hianimes.se", "animetosho.net", "opensubtitles.org",
            "rest.opensubtitles.org", "api.opensubtitles.com"
    ));

    // Bounded thread-safe cache for dynamically learned blocked hosts
    private static final Set<String> DYNAMIC_BLOCKED_HOSTS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private DynamicBlockedHosts() {}

    private static boolean isWhitelisted(String host) {
        if (host == null) return false;
        for (String w : WHITELISTED_HOSTS) {
            if (host.equals(w) || host.endsWith("." + w)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a host is subject to DPI blocking and requires TLS ClientHello desynchronization.
     */
    public static boolean isBlocked(String host) {
        if (host == null || host.trim().isEmpty()) return false;
        String h = host.toLowerCase(Locale.US).trim();

        if (isWhitelisted(h)) return false;
        if (KNOWN_DPI_HOSTS.contains(h)) return true;
        if (DYNAMIC_BLOCKED_HOSTS.contains(h)) return true;

        for (String kw : KNOWN_DPI_KEYWORDS) {
            if (h.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dynamically marks a host as blocked when connection resets or SSL handshake failures occur on CDN endpoints.
     */
    public static void markBlocked(String host) {
        if (host != null && !host.trim().isEmpty()) {
            String h = host.toLowerCase(Locale.US).trim();
            if (!isWhitelisted(h)) {
                DYNAMIC_BLOCKED_HOSTS.add(h);
            }
        }
    }

    public static void unmark(String host) {
        if (host != null) {
            DYNAMIC_BLOCKED_HOSTS.remove(host.toLowerCase(Locale.US).trim());
        }
    }

    public static Set<String> getAllBlockedHosts() {
        Set<String> all = new HashSet<>(KNOWN_DPI_HOSTS);
        all.addAll(DYNAMIC_BLOCKED_HOSTS);
        return Collections.unmodifiableSet(all);
    }

    public static void reset() {
        DYNAMIC_BLOCKED_HOSTS.clear();
    }
}
