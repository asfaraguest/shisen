package id.shisen.app.data.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fase 2.2 — Dedup request GET in-flight yang identik untuk host metadata
 * (AniList/GitHub/Jikan dsb). Caller pertama menjalankan request; pemanggil
 * identik yang datang bersamaan menunggu hasil pertama dan menerima salinan
 * respons yang sama. Body dipisahkan per-pemanggil (Buffer baru masing-masing)
 * sehingga tidak pernah dibagikan.
 *
 * Hanya GET tanpa Range pada host whitelist — dikeluarkan media streaming,
 * jadi tidak pernah menyangga video/audio besar.
 */
public final class NetworkTwin {

    static final long DEFAULT_WAIT_MS = 20_000L;

    /** Host metadata yang layak dipulangkan; media streaming & scraper HTML tidak didedup. */
    public static Set<String> defaultHosts() {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "api.github.com",
                "graphql.anilist.co",
                "api.anilist.co",
                "anilist.co",
                "api.jikan.moe",
                "kitsu.io"
        )));
    }

    /** Entri request pertama yang sedang berjalan di map in-flight. */
    private static final class Candidate {
        volatile Response firstResponse;
        volatile byte[] bytes;
        final CountDownLatch ready = new CountDownLatch(1);

        Candidate(Response firstResponse, byte[] bytes) {
            this.firstResponse = firstResponse;
            this.bytes = bytes;
        }
    }

    /** Interceptor dedup dengan whitelist host default. */
    public static Interceptor metadataDedupInterceptor() {
        return metadataDedupInterceptor(defaultHosts());
    }

    /** Versi whitelist yang dapat disesuaikan (dipakai unit test). */
    public static Interceptor metadataDedupInterceptor(Set<String> hosts) {
        final Set<String> allowed = hosts != null ? hosts : defaultHosts();
        final ConcurrentHashMap<String, Candidate> inflight = new ConcurrentHashMap<>();

        return chain -> {
            Request req = chain.request();
            boolean eligible = "GET".equals(req.method())
                    && req.header("Range") == null
                    && allowed.contains(req.url().host());
            if (!eligible) {
                return chain.proceed(req);
            }

            String key = req.method() + " " + req.url();
            Candidate placeholder = new Candidate(null, null);
            Candidate existing = inflight.putIfAbsent(key, placeholder);
            if (existing != null && existing != placeholder) {
                // Follower: tunggu hasil caller pertama, timeout → jalankan sendiri
                try {
                    if (existing.ready.await(DEFAULT_WAIT_MS, TimeUnit.MILLISECONDS)
                            && existing.bytes != null && existing.firstResponse != null) {
                        return duplicate(existing.firstResponse, existing.bytes);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return chain.proceed(req);
            }

            // Caller pertama
            try {
                Response resp = chain.proceed(req);
                if (isLargeBody(resp)) {
                    // Respons besar: tidak disangga, jalankan normal
                    return resp;
                }
                byte[] bytes = resp.body() != null ? resp.body().bytes() : new byte[0];
                placeholder.firstResponse = resp;
                placeholder.bytes = bytes;
                return duplicate(resp, bytes);
            } finally {
                inflight.remove(key, placeholder);
                placeholder.ready.countDown();
            }
        };
    }

    /** Respons metadata besar/unknown-length tidak ikut didedup. */
    private static boolean isLargeBody(Response resp) {
        try {
            long len = resp.body() != null ? resp.body().contentLength() : -1;
            return len < 0 || len > 512_000;
        } catch (Exception e) {
            return true;
        }
    }

    /** Respons identik dengan body BARU — setiap Buffer milik sendiri. */
    private static Response duplicate(Response original, byte[] bytes) {
        MediaType type = original.body() != null ? original.body().contentType() : null;
        ResponseBody newBody = ResponseBody.create(bytes != null ? bytes : new byte[0], type);
        return original.newBuilder().body(newBody).build();
    }

    private NetworkTwin() {}
}
