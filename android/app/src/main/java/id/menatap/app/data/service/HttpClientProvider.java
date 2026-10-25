package id.menatap.app.data.service;

import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import id.menatap.app.util.DesyncSSLSocketFactory;
import id.menatap.app.util.DnsFallbackResolver;
import id.menatap.app.util.DynamicBlockedHosts;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpClientProvider {
    private static volatile OkHttpClient client;
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

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

        // 1. Dynamic DPI-Blocked Host Learner Interceptor
        builder.addInterceptor(chain -> {
            Request request = chain.request();
            try {
                return chain.proceed(request);
            } catch (IOException e) {
                String host = request.url().host();
                if (e instanceof java.net.SocketTimeoutException
                        || e instanceof java.net.ConnectException
                        || e instanceof javax.net.ssl.SSLHandshakeException) {
                    DynamicBlockedHosts.markBlocked(host);
                }
                throw e;
            }
        });

        // 2. Anti-403 & Header Normalization Interceptor
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
                } else if (host.contains("dramahot") || host.contains("zokoanime")) {
                    if (original.header("Referer") == null || original.header("Referer").isEmpty()) {
                        rb.header("Referer", "https://zokoanime.video/");
                    }
                    if (original.header("Origin") == null || original.header("Origin").isEmpty()) {
                        rb.header("Origin", "https://zokoanime.video");
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

        // 3. Multi-tier DNS Fallback Chain (Cache -> System anti-poison -> DoH Cloudflare -> DoH Google -> Known CDN IPs)
        builder.dns(new DnsFallbackResolver());

        // 4. DPI-Bypass TLS ClientHello Desync Socket / SSLSocketFactory
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            X509TrustManager trustManager = null;
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    trustManager = (X509TrustManager) tm;
                    break;
                }
            }
            if (trustManager != null) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{ trustManager }, new SecureRandom());
                SSLSocketFactory defaultFactory = sslContext.getSocketFactory();
                DesyncSSLSocketFactory desyncFactory = new DesyncSSLSocketFactory(defaultFactory);
                builder.sslSocketFactory(desyncFactory, trustManager);
            }
        } catch (Exception ignored) {}

        return builder.build();
    }
}
