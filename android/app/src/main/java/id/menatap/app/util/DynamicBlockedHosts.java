package id.menatap.app.util;

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
            "dramahot", "zokoanime", "drama1", "puterin", "putarin", "stream121"
    ));

    private static final Set<String> KNOWN_DPI_HOSTS = new HashSet<>(Arrays.asList(
            "hls.dramahot.top", "dramahot.top", "drama1.cfd", "hls1.drama1.cfd",
            "puterin.biz", "putarin.xyz", "stream121.space", "zokoanime.video"
    ));

    // Bounded thread-safe cache for dynamically learned blocked hosts
    private static final Set<String> DYNAMIC_BLOCKED_HOSTS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private DynamicBlockedHosts() {}

    /**
     * Checks if a host is subject to DPI blocking and requires TLS ClientHello desynchronization.
     */
    public static boolean isBlocked(String host) {
        if (host == null || host.trim().isEmpty()) return false;
        String h = host.toLowerCase(Locale.US).trim();

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
     * Dynamically marks a host as blocked when connection resets, timeouts, or SSL handshake failures occur.
     */
    public static void markBlocked(String host) {
        if (host != null && !host.trim().isEmpty()) {
            DYNAMIC_BLOCKED_HOSTS.add(host.toLowerCase(Locale.US).trim());
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
