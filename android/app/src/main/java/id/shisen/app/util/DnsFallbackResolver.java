package id.shisen.app.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;

/**
 * DnsFallbackResolver implements the multi-tier DNS resolution chain from Tatap OG:
 * 1. Memory Cache ->
 * 2. System DNS (filtered against Indonesian ISP DNS sinkholes: 10.x, 127.x, 192.168.x, 118.98.x) ->
 * 3. Cloudflare DoH (1.1.1.1 / 1.0.0.1) ->
 * 4. Google DoH (8.8.8.8 / 8.8.4.4) ->
 * 5. Known CDN Hardcoded IPs (e.g. hls.dramahot.top).
 */
public class DnsFallbackResolver implements Dns {

    @FunctionalInterface
    public interface TierResolver {
        List<InetAddress> resolve(String hostname) throws Exception;
    }

    private final Map<String, List<InetAddress>> memoryCache = new ConcurrentHashMap<>();
    private final TierResolver systemResolver;
    private final TierResolver cloudflareResolver;
    private final TierResolver googleResolver;
    private final Map<String, String> knownIps;

    public DnsFallbackResolver() {
        this.knownIps = new HashMap<>();
        this.knownIps.put("hls.dramahot.top", "93.123.109.210");
        this.knownIps.put("dramahot.top", "93.123.109.210");
        this.knownIps.put("zokoanime.video", "5.253.86.249");
        this.knownIps.put("hls1.drama1.cfd", "104.21.21.54");
        this.knownIps.put("drama1.cfd", "104.21.21.54");

        // Real system DNS resolver
        this.systemResolver = hostname -> Dns.SYSTEM.lookup(hostname);

        // Real DoH Cloudflare
        TierResolver cf;
        TierResolver gg;
        try {
            List<InetAddress> bootstrapCf = new ArrayList<>();
            bootstrapCf.add(InetAddress.getByName("1.1.1.1"));
            bootstrapCf.add(InetAddress.getByName("1.0.0.1"));

            List<InetAddress> bootstrapGg = new ArrayList<>();
            bootstrapGg.add(InetAddress.getByName("8.8.8.8"));
            bootstrapGg.add(InetAddress.getByName("8.8.4.4"));

            OkHttpClient dohClient = new OkHttpClient.Builder()
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(4, TimeUnit.SECONDS)
                    .build();

            DnsOverHttps dohCloudflare = new DnsOverHttps.Builder()
                    .client(dohClient)
                    .url(HttpUrl.get("https://cloudflare-dns.com/dns-query"))
                    .bootstrapDnsHosts(bootstrapCf)
                    .includeIPv6(false)
                    .build();

            DnsOverHttps dohGoogle = new DnsOverHttps.Builder()
                    .client(dohClient)
                    .url(HttpUrl.get("https://dns.google/dns-query"))
                    .bootstrapDnsHosts(bootstrapGg)
                    .includeIPv6(false)
                    .build();

            cf = hostname -> dohCloudflare.lookup(hostname);
            gg = hostname -> dohGoogle.lookup(hostname);
        } catch (Exception e) {
            cf = null;
            gg = null;
        }
        this.cloudflareResolver = cf;
        this.googleResolver = gg;
    }

    public DnsFallbackResolver(TierResolver systemResolver,
                               TierResolver cloudflareResolver,
                               TierResolver googleResolver,
                               Map<String, String> knownIps) {
        this.systemResolver = systemResolver;
        this.cloudflareResolver = cloudflareResolver;
        this.googleResolver = googleResolver;
        this.knownIps = (knownIps != null) ? new HashMap<>(knownIps) : new HashMap<>();
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        if (hostname == null || hostname.trim().isEmpty()) {
            throw new UnknownHostException("Hostname cannot be null or empty");
        }
        String h = hostname.trim().toLowerCase(java.util.Locale.US);

        // Tier 1: In-Memory Cache
        if (memoryCache.containsKey(h)) {
            return memoryCache.get(h);
        }

        // Direct IP check
        if (isNumericIp(h)) {
            try {
                List<InetAddress> direct = Collections.singletonList(InetAddress.getByName(h));
                memoryCache.put(h, direct);
                return direct;
            } catch (Exception ignored) {}
        }

        // Tier 2: System DNS with Anti-Poison Sinkhole Filter
        if (systemResolver != null) {
            try {
                List<InetAddress> rawSys = systemResolver.resolve(h);
                List<InetAddress> cleanSys = filterPoisonedIps(rawSys);
                if (!cleanSys.isEmpty()) {
                    memoryCache.put(h, cleanSys);
                    return cleanSys;
                }
            } catch (Exception ignored) {}
        }

        // Tier 3: DoH Cloudflare (1.1.1.1)
        if (cloudflareResolver != null) {
            try {
                List<InetAddress> rawCf = cloudflareResolver.resolve(h);
                List<InetAddress> cleanCf = filterPoisonedIps(rawCf);
                if (!cleanCf.isEmpty()) {
                    memoryCache.put(h, cleanCf);
                    return cleanCf;
                }
            } catch (Exception ignored) {}
        }

        // Tier 4: DoH Google (8.8.8.8)
        if (googleResolver != null) {
            try {
                List<InetAddress> rawGg = googleResolver.resolve(h);
                List<InetAddress> cleanGg = filterPoisonedIps(rawGg);
                if (!cleanGg.isEmpty()) {
                    memoryCache.put(h, cleanGg);
                    return cleanGg;
                }
            } catch (Exception ignored) {}
        }

        // Tier 5: Known CDN Hardcoded IPs
        if (knownIps != null && knownIps.containsKey(h)) {
            try {
                List<InetAddress> fallback = Collections.singletonList(InetAddress.getByName(knownIps.get(h)));
                memoryCache.put(h, fallback);
                return fallback;
            } catch (Exception ignored) {}
        }

        throw new UnknownHostException("Unable to resolve host via cache, system, DoH, or fallback: " + h);
    }

    public static List<InetAddress> filterPoisonedIps(List<InetAddress> list) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<InetAddress> clean = new ArrayList<>();
        for (InetAddress addr : list) {
            if (addr == null) continue;
            // Saring wildcard local, loopback (127.x, ::1), link-local, dan site-local (10.x, 192.168.x)
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()
                    || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
                continue;
            }
            String ip = addr.getHostAddress();
            if (ip == null) continue;
            // Saring Indonesian ISP poisoned DNS addresses (118.98.x Mercusuar/Internet Positif) dan sinkhole tambahan
            if (ip.startsWith("118.98.") || ip.equals("0.0.0.0") || ip.equals("::") || ip.equals("::1")) {
                continue;
            }
            clean.add(addr);
        }
        return clean;
    }

    private static boolean isNumericIp(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4) return false;
        for (String p : parts) {
            if (p.isEmpty()) return false;
            for (int i = 0; i < p.length(); i++) {
                if (!Character.isDigit(p.charAt(i))) return false;
            }
        }
        return true;
    }

    public void clearCache() {
        memoryCache.clear();
    }
}
