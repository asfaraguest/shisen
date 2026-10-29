package id.shisen.app.data.service;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class NetworkExecutorTest {

    @Test
    public void testNetworkExecutorRunsOnManagedThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadNameRef = new AtomicReference<>();

        HttpClientProvider.execute(() -> {
            threadNameRef.set(Thread.currentThread().getName());
            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(threadNameRef.get());
        assertTrue("Thread harus berasal dari pool shisen-net: " + threadNameRef.get(),
                threadNameRef.get().startsWith("shisen-net-"));
    }

    @Test
    public void testHiAnimeErrorCallbackAlwaysInvokedOnBadHost() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean errorCalled = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        HiAnimeClient.getEpisodes("invalid-slug-without-numeric-id", new HiAnimeClient.Callback<java.util.List<id.shisen.app.data.model.Episode>>() {
            @Override
            public void onSuccess(java.util.List<id.shisen.app.data.model.Episode> result) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                errorCalled.set(true);
                errorRef.set(t);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue("Callback onError harus dipanggil saat terjadi kegagalan slug", errorCalled.get());
        assertNotNull(errorRef.get());
    }
}
