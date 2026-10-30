package id.shisen.app.data.service;

import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import id.shisen.app.util.DesyncSSLSocketFactory;
import id.shisen.app.util.DnsFallbackResolver;
import id.shisen.app.util.DynamicBlockedHosts;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpClientProvider {
    private static volatile OkHttpClient client;
    private static volatile okhttp3.Cache diskCache;
    private static volatile ExecutorService networkExecutor;
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public static ExecutorService getExecutor() {
        if (networkExecutor == null) {
            synchronized (HttpClientProvider.class) {
                if (networkExecutor == null) {
                    networkExecutor = new ThreadPoolExecutor(
                            4,
                            16,
                            60L,
                            TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(256),
                            new ThreadFactory() {
                                private final AtomicInteger count = new AtomicInteger(1);
                                @Override
                                public Thread newThread(Runnable r) {
                                    Thread t = new Thread(r, "shisen-net-" + count.getAndIncrement());
                                    t.setDaemon(true);
                                    return t;
                                }
                            },
                            new ThreadPoolExecutor.CallerRunsPolicy()
                    );
                }
            }
        }
        return networkExecutor;
    }

    public static void execute(Runnable task) {
        getExecutor().execute(task);
    }

    /** Fase 2.2: panggil sekali dari Application untuk mengaktifkan cache disk HTTP 50MB. */
    public static void configureCache(java.io.File cacheDir) {
        if (cacheDir == null) return;
        if (diskCache == null) {
            synchronized (HttpClientProvider.class) {
                if (diskCache == null) {
                    diskCache = new okhttp3.Cache(cacheDir, 50L * 1024L * 1024L);
                }
            }
        }
    }

    /** Hook unit test: buang client & cache agar dibangun ulang dari nol. */
    static void resetForTesting() {
        synchronized (HttpClientProvider.class) {
            client = null;
            diskCache = null;
        }
    }

    /** Hook unit test: client cache disk + dedup, TANPA DNS resolver/stack Android. */
    public static OkHttpClient cacheEnabledClientForTesting(java.io.File dir) {
        return new okhttp3.OkHttpClient.Builder()
                .addInterceptor(NetworkTwin.metadataDedupInterceptor())
                .cache(new okhttp3.Cache(dir, 50L * 1024L * 1024L))
                .build();
    }

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
                String path = original.url().encodedPath().toLowerCase(java.util.Locale.US);
                String fullUrl = original.url().toString().toLowerCase(java.util.Locale.US);

                // Anti-403 CDN Referer/Origin Injection
                boolean isDirect = "none".equalsIgnoreCase(original.header("Sec-Fetch-Site"));

                boolean isMegaCdn = NetworkConfig.isMegaCdnHost(host, path, fullUrl);

                if (!isDirect) {
                    if (host.contains("anichan")) {
                        if (original.header("Referer") == null || original.header("Referer").isEmpty()) {
                            rb.header("Referer", "https://anichan.to/");
                        }
                    } else if (host.contains("otakudesu")) {
                        if (original.header("Referer") == null || original.header("Referer").isEmpty()) {
                            rb.header("Referer", "https://ww2.otakudesu.biz/");
                        }
                    } else if (host.contains("animetosho")) {
                        if (original.header("Referer") == null || original.header("Referer").isEmpty()) {
                            rb.header("Referer", "https://animetosho.net/");
                        }
                    } else if (host.contains("opensubtitles")) {
                        if (original.header("Referer") == null || original.header("Referer").isEmpty()) {
                            rb.header("Referer", "https://opensubtitles.org/");
                        }
                    } else if (host.contains("dramahot") || host.contains("zokoanime")) {
                        if (original.header("Referer") == null || original.header("Referer").isEmpty()) {
                            rb.header("Referer", "https://zokoanime.video/");
                        }
                        if (original.header("Origin") == null || original.header("Origin").isEmpty()) {
                            rb.header("Origin", "https://zokoanime.video");
                        }
                    } else if (host.contains("animehot") || host.contains("anitv") || host.contains("hianimes.se")) {
                        if (original.header("Referer") == null || original.header("Referer").isEmpty()) {
                            rb.header("Referer", "https://hianimes.se/");
                        }
                        if (original.header("Origin") == null || original.header("Origin").isEmpty()) {
                            rb.header("Origin", "https://hianimes.se");
                        }
                    } else if (isMegaCdn) {
                        String determinedReferer = original.header("Referer");
                        if (determinedReferer == null || determinedReferer.isEmpty()) {
                            if (host.contains("hianime")) {
                                determinedReferer = "https://hianime.to/";
                            } else if (host.contains("zoro") || host.contains("aniwave")) {
                                determinedReferer = "https://zoro.to/";
                            } else if (host.contains("zoko")) {
                                determinedReferer = "https://zokoanime.video/";
                            } else {
                                determinedReferer = "https://megaplay.buzz/";
                            }
                            rb.header("Referer", determinedReferer);
                        }
                        if (original.header("Origin") == null || original.header("Origin").isEmpty()) {
                            String origin = determinedReferer.endsWith("/")
                                    ? determinedReferer.substring(0, determinedReferer.length() - 1)
                                    : determinedReferer;
                            rb.header("Origin", origin);
                        }
                    } else if (host.contains("hianime")) {
                        if (original.header("Referer") == null) {
                            rb.header("Referer", "https://hianime.at/");
                        }
                    }
                }

                if (original.header("Accept") == null) {
                    rb.header("Accept", "*/*");
                }
                if (original.header("Accept-Language") == null) {
                    rb.header("Accept-Language", "en-US,en;q=0.9,id;q=0.8");
                }
                if (original.header("Sec-Fetch-Dest") == null) {
                    rb.header("Sec-Fetch-Dest", "empty");
                }
                if (original.header("Sec-Fetch-Mode") == null) {
                    rb.header("Sec-Fetch-Mode", "cors");
                }
                if (original.header("Sec-Fetch-Site") == null) {
                    rb.header("Sec-Fetch-Site", "cross-site");
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

        // 5. Fase 2.2: Dedup request GET metadata identik in-flight
        builder.addInterceptor(NetworkTwin.metadataDedupInterceptor());

        // 6. Fase 2.2: Cache disk OkHttp untuk GET metadata (berlaku bila server kirim header cacheable)
        if (diskCache != null) {
            builder.cache(diskCache);
        }

        // 7. Logger performa DEBUG-ONLY: hitung total byte jaringan (tanpa menyimpan URL berparameter sensitif)
        if (id.shisen.app.BuildConfig.DEBUG) {
            builder.addInterceptor(chain -> {
                okhttp3.Response r = chain.proceed(chain.request());
                id.shisen.app.util.PerfLogger.addNetworkBytes(0, r.body() != null ? r.body().contentLength() : 0);
                return r;
            });
        }

        return builder.build();
    }
}
