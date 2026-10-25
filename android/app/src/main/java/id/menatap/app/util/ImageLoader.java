package id.menatap.app.util;

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
        if (diskCacheDir == null) {
            diskCacheDir = new File(context.getCacheDir(), "menatap_img_cache");
            if (!diskCacheDir.exists()) diskCacheDir.mkdirs();
        }
    }

    public static void load(String url, ImageView imageView) {
        if (imageView == null) return;
        if (url == null || url.trim().isEmpty()) {
            imageView.setImageDrawable(null);
            return;
        }

        imageView.setTag(url);

        // 1. Check memory cache
        if (memoryCache != null) {
            Bitmap memBmp = memoryCache.get(url);
            if (memBmp != null) {
                imageView.setImageBitmap(memBmp);
                return;
            }
        }

        // 2. Load from disk or network asynchronously
        executor.execute(() -> {
            Bitmap bmp = loadFromDisk(url);
            if (bmp == null) {
                bmp = downloadFromNetwork(url);
                if (bmp != null) {
                    saveToDisk(url, bmp);
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

    private static Bitmap loadFromDisk(String url) {
        if (diskCacheDir == null) return null;
        try {
            File f = new File(diskCacheDir, md5(url));
            if (f.exists()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.RGB_565; // High performance, 50% RAM savings
                return BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void saveToDisk(String url, Bitmap bmp) {
        if (diskCacheDir == null || bmp == null) return;
        try {
            File f = new File(diskCacheDir, md5(url));
            try (FileOutputStream fos = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            }
        } catch (Exception ignored) {}
    }

    private static Bitmap downloadFromNetwork(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", id.menatap.app.data.service.HttpClientProvider.USER_AGENT);
            conn.connect();

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inPreferredConfig = Bitmap.Config.RGB_565;
                    return BitmapFactory.decodeStream(is, null, opts);
                }
            }
        } catch (Exception ignored) {}
        return null;
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
