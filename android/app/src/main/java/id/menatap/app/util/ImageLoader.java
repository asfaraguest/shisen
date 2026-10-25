package id.menatap.app.util;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {
    private static LruCache<String, Bitmap> memoryCache;
    private static File diskCacheDir;
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void init(Context context) {
        if (memoryCache == null) {
            int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            int cacheSize = maxMemory / 8; // 1/8 of available RAM
            memoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }
            };
        }
        if (diskCacheDir == null && context != null) {
            diskCacheDir = new File(context.getCacheDir(), "menatap_img_cache");
            if (!diskCacheDir.exists()) diskCacheDir.mkdirs();
        }
    }

    public static void clearMemoryCache() {
        if (memoryCache != null) {
            memoryCache.evictAll();
        }
    }

    public static void trimMemory(int level) {
        if (memoryCache == null) return;
        try {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
                memoryCache.evictAll();
            } else if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                memoryCache.trimToSize(memoryCache.size() / 2);
            }
        } catch (Throwable ignored) {}
    }

    public static void load(String url, ImageView imageView) {
        load(url, imageView, 600, 900);
    }

    public static void load(String url, ImageView imageView, int reqWidth, int reqHeight) {
        if (imageView == null) return;
        if (url == null || url.trim().isEmpty()) {
            imageView.setImageDrawable(null);
            return;
        }

        imageView.setTag(url);

        // 1. Check memory cache
        if (memoryCache != null) {
            Bitmap memBmp = memoryCache.get(url);
            if (memBmp != null && !memBmp.isRecycled()) {
                imageView.setImageBitmap(memBmp);
                return;
            }
        }

        final int targetW = reqWidth > 0 ? reqWidth : 600;
        final int targetH = reqHeight > 0 ? reqHeight : 900;

        // 2. Load from disk or network asynchronously
        executor.execute(() -> {
            Bitmap bmp = loadFromDisk(url, targetW, targetH);
            if (bmp == null) {
                boolean downloaded = downloadToDisk(url);
                if (downloaded) {
                    bmp = loadFromDisk(url, targetW, targetH);
                }
            }

            if (bmp != null) {
                if (memoryCache != null) {
                    memoryCache.put(url, bmp);
                }
                final Bitmap finalBmp = bmp;
                mainHandler.post(() -> {
                    if (url.equals(imageView.getTag())) {
                        imageView.setImageBitmap(finalBmp);
                    }
                });
            }
        });
    }

    private static Bitmap loadFromDisk(String url, int reqWidth, int reqHeight) {
        if (diskCacheDir == null) return null;
        try {
            File f = new File(diskCacheDir, md5(url));
            if (f.exists()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(f.getAbsolutePath(), opts);

                opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
                opts.inJustDecodeBounds = false;
                opts.inPreferredConfig = Bitmap.Config.RGB_565; // High performance, 50% RAM savings
                return BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean downloadToDisk(String urlStr) {
        if (diskCacheDir == null) return false;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", id.menatap.app.data.service.HttpClientProvider.USER_AGENT);
            conn.connect();

            if (conn.getResponseCode() == 200) {
                File f = new File(diskCacheDir, md5(urlStr));
                try (InputStream is = conn.getInputStream();
                     FileOutputStream fos = new FileOutputStream(f)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    return true;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Throwable ignored) {}
            }
        }
        return false;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private static String md5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }
}
