package id.shisen.app.data.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fase 2.2 — tes perilaku jaringan (bukan tes baca source):
 * 1) Dua request GET identik yang berjalan bersamaan pada host metadata hanya
 *    mendarat SATU kali ke server, dan kedua pemanggil menerima body identik.
 * 2) GET di luar whitelist host (media/scraper) tidak didedup — server dapat
 *    request dua kali.
 * 3) Cache disk OkHttp: GET kedua identik dalam TTL tidak menyentuh server.
 */
public class NetworkTwinTest {

    private MockWebServer server;
    private File cacheDir;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        cacheDir = java.nio.file.Files.createTempDirectory("shisen-http-cache").toFile();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        HttpClientProvider.resetForTesting();
    }

    private OkHttpClient buildDedupClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(NetworkTwin.metadataDedupInterceptor(
                        Collections.singleton(server.getHostName())))
                .build();
    }

    @Test
    public void identicalInFlightGetsCollapseToOneServerHit() throws Exception {
        final CountDownLatch gate = new CountDownLatch(1);
        final AtomicInteger serverHits = new AtomicInteger();
        final StringBuilder body1 = new StringBuilder();
        final StringBuilder body2 = new StringBuilder();

        server.enqueue(new MockResponse().setResponseCode(200).setBody("meta payload v1"));

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(NetworkTwin.metadataDedupInterceptor(
                        Collections.singleton("localhost")))
                .build();

        Request req = new Request.Builder()
                .url(server.url("/v1/meta?album=1").toString())
                .build();

        // Thread 1: mulai request, thread 2 ikut thread 2 identik saat #1 masih in-flight
        Thread t1 = new Thread(() -> {
            try {
                Response r = client.newCall(req).execute();
                serverHits.incrementAndGet();
                gate.await(5, TimeUnit.SECONDS); // tahan agar t2ikut pasangan identik in-flight
                body1.append(r.body() != null ? r.body().string() : "");
            } catch (Exception ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try {
                gate.await(5, TimeUnit.SECONDS); // tunggu t1 menjalankan call dulu
                Response r = client.newCall(req).execute();
                serverHits.incrementAndGet();
                body2.append(r.body() != null ? r.body().string() : "");
            } catch (Exception ignored) {}
        });
        t1.start();
        // beri waktu t1 mengunci in-flight key, lalu t2 start
        Thread.sleep(400);
        t2.start();
        t1.join(10_000);
        t2.join(10_000);

        assertEquals("server harus menerima TEPAT 1 request GET", 1,
                server.getRequestCount());
        assertEquals("meta payload v1", body1.toString());
        assertEquals("follower harus menerima body identik", body1.toString(), body2.toString());
    }

    @Test
    public void mediaStyleGetsAreNeverDeduped() throws Exception {
        server.enqueue(new MockResponse().setBody("seg1"));
        server.enqueue(new MockResponse().setBody("seg2"));

        OkHttpClient client = buildDedupClient();
        Request req = new Request.Builder()
                .url(server.url("/stream.m3u8").toString())
                .addHeader("Range", "bytes=0-1023") // media selalu ranged
                .build();

        try (Response r1 = client.newCall(req).execute()) { r1.body().string(); }
        try (Response r2 = client.newCall(req).execute()) { r2.body().string(); }

        assertTrue("dua GET ranged harus tetap dua request server",
                server.getRequestCount() >= 2);
    }

    @Test
    public void diskCacheServesSecondIdenticalGetWithoutServerCall() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Cache-Control", "max-age=60")
                .setBody("cached meta"));

        // Hook test: client cache disk + dedup tanpa DNS resolver/stack Android.
        OkHttpClient client = HttpClientProvider.cacheEnabledClientForTesting(cacheDir);
        Request req = new Request.Builder()
                .url(server.url("/v1/meta").toString())
                .build();

        try (Response r1 = client.newCall(req).execute()) {
            assertEquals("cached meta", r1.body().string());
        }
        try (Response r2 = client.newCall(req).execute()) {
            assertEquals("cached meta", r2.body().string());
        }

        assertEquals("GET kedua dalam TTL harus dilayani cache, bukan server", 1,
                server.getRequestCount());
    }
}
