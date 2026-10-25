package id.menatap.app.data.service;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;

public class HttpClientProvider {
    private static volatile OkHttpClient client;
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";

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
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(20, 60, TimeUnit.SECONDS))
                .followRedirects(true)
                .followSslRedirects(true);

        // DNS-over-HTTPS (DoH) via Cloudflare with system DNS fallback
        try {
            OkHttpClient bootstrapClient = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .build();

            DnsOverHttps dnsOverHttps = new DnsOverHttps.Builder()
                    .client(bootstrapClient)
                    .url(HttpUrl.get("https://cloudflare-dns.com/dns-query"))
                    .includeIPv6(false)
                    .build();

            builder.dns(hostname -> {
                try {
                    List<InetAddress> addresses = dnsOverHttps.lookup(hostname);
                    if (addresses != null && !addresses.isEmpty()) {
                        return addresses;
                    }
                } catch (Exception ignored) {}
                return Dns.SYSTEM.lookup(hostname);
            });
        } catch (Exception ignored) {
            builder.dns(Dns.SYSTEM);
        }

        return builder.build();
    }
}
