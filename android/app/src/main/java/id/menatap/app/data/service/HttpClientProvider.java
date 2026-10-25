package id.menatap.app.data.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.dnsoverhttps.DnsOverHttps;

public class HttpClientProvider {
    private static volatile OkHttpClient client;
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    // Known fallback CDN IP mappings (Tatap OG parity)
    private static final Map<String, String> KNOWN_IPS = new HashMap<String, String>() {{
        put("hls.dramahot.top", "93.123.109.210");
        put("dramahot.top", "93.123.109.210");
    }};

    public static OkHttpClient getClient() {
        if (client == null) {
            synchronized (HttpClientProvider.class) {
                if (client == null) {
                    client = buildClient();
                }
            }
        }
        return client;
    }

    private static OkHttpClient buildClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(25, 60, TimeUnit.SECONDS))
                .followRedirects(true)
                .followSslRedirects(true);

        // Anti-403 & Header Normalization Interceptor
        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request.Builder rb = original.newBuilder();

                // User-Agent enforcement
                if (original.header("User-Agent") == null) {
                    rb.header("User-Agent", USER_AGENT);
                }

                String host = original.url().host().toLowerCase(java.util.Locale.US);

                // Anti-403 CDN Referer/Origin Injection
                if (host.contains("megaplay") || host.contains("megap.") || host.contains("nexabloom") || host.contains("ivorysummit") || host.contains("shiora")) {
                    if (original.header("Referer") == null || original.header("Referer").isEmpty()) {
                        rb.header("Referer", "https://megaplay.buzz/");
                    }
                    if (original.header("Origin") == null || original.header("Origin").isEmpty()) {
                        rb.header("Origin", "https://megaplay.buzz");
                    }
                } else if (host.contains("hianime")) {
                    if (original.header("Referer") == null) {
                        rb.header("Referer", "https://hianime.at/");
                    }
                } else if (host.contains("anichan")) {
                    if (original.header("Referer") == null) {
                        rb.header("Referer", "https://anichan.to/");
                    }
                } else if (host.contains("otakudesu")) {
                    if (original.header("Referer") == null) {
                        rb.header("Referer", "https://ww2.otakudesu.biz/");
                    }
                }

                if (original.header("Accept-Language") == null) {
                    rb.header("Accept-Language", "en-US,en;q=0.9,id;q=0.8");
                }

                return chain.proceed(rb.build());
            }
        });

        // Robust Anti-ISP DNS Blocking (Bypass via DoH Cloudflare & Google with Raw IP Bootstrap)
        final Map<String, List<InetAddress>> dnsMemoryCache = new ConcurrentHashMap<>();

        try {
            List<InetAddress> bootstrapHosts = new ArrayList<>();
            bootstrapHosts.add(InetAddress.getByName("1.1.1.1"));
            bootstrapHosts.add(InetAddress.getByName("1.0.0.1"));
            bootstrapHosts.add(InetAddress.getByName("8.8.8.8"));
            bootstrapHosts.add(InetAddress.getByName("8.8.4.4"));

            OkHttpClient dohBootstrapClient = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build();

            DnsOverHttps dohCloudflare = new DnsOverHttps.Builder()
                    .client(dohBootstrapClient)
                    .url(HttpUrl.get("https://cloudflare-dns.com/dns-query"))
                    .bootstrapDnsHosts(bootstrapHosts)
                    .includeIPv6(false)
                    .build();

            builder.dns(hostname -> {
                if (dnsMemoryCache.containsKey(hostname)) {
                    return dnsMemoryCache.get(hostname);
                }

                // Check Known IPs first
                if (KNOWN_IPS.containsKey(hostname)) {
                    try {
                        List<InetAddress> known = Collections.singletonList(InetAddress.getByName(KNOWN_IPS.get(hostname)));
                        dnsMemoryCache.put(hostname, known);
                        return known;
                    } catch (Exception ignored) {}
                }

                // 1. Try DoH Cloudflare with Raw IP bootstrap
                try {
                    List<InetAddress> res = dohCloudflare.lookup(hostname);
                    List<InetAddress> safe = filterPoisonedIps(res);
                    if (!safe.isEmpty()) {
                        dnsMemoryCache.put(hostname, safe);
                        return safe;
                    }
                } catch (Exception ignored) {}

                // 2. Try System DNS with anti-poison filter
                try {
                    List<InetAddress> sysRes = Dns.SYSTEM.lookup(hostname);
                    List<InetAddress> safe = filterPoisonedIps(sysRes);
                    if (!safe.isEmpty()) {
                        dnsMemoryCache.put(hostname, safe);
                        return safe;
                    }
                } catch (Exception ignored) {}

                // Fallback to System lookup
                return Dns.SYSTEM.lookup(hostname);
            });
        } catch (Exception ignored) {
            builder.dns(Dns.SYSTEM);
        }

        return builder.build();
    }

    private static List<InetAddress> filterPoisonedIps(List<InetAddress> list) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<InetAddress> clean = new ArrayList<>();
        for (InetAddress addr : list) {
            String ip = addr.getHostAddress();
            if (ip == null) continue;
            // Filter Indonesian ISP poisoned DNS addresses (10.x, 127.x, 192.168.x, 118.98.x Mercusuar/Internet Positif)
            if (ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("118.98.") || ip.equals("0.0.0.0")) {
                continue;
            }
            clean.add(addr);
        }
        return clean;
    }
}
