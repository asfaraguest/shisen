package id.shisen.app.util;

import java.io.File;
import java.util.Arrays;

/**
 * Pusat penyimpanan APK pembaruan: hanya folder <cache-root>/updates/ yang dibagikan
 * lewat FileProvider (lihat res/xml/file_paths.xml), sehingga URI hanya bisa dibentuk
 * untuk berkas di folder itu — bukan seluruh isi cache/files.
 */
public final class UpdateApkStore {

    private UpdateApkStore() {}

    /** Subfolder khusus updates di dalam root cache (internal atau external). */
    public static File updatesDir(File cacheRoot) {
        if (cacheRoot == null) return null;
        File dir = new File(cacheRoot, "updates");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** Berkas APK target untuk versi tertentu, di subfolder updates. */
    public static File apkFile(File cacheRoot, String version) {
        File dir = updatesDir(cacheRoot);
        return dir == null ? null : new File(dir, "shisen-update-v" + version + ".apk");
    }

    /** Hapus SELURUH APK lama di folder updates kecuali berkas yang sedang dipasang. */
    public static void deleteOldApks(File cacheRoot, File keep) {
        File dir = updatesDir(cacheRoot);
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".apk") && (keep == null || !f.equals(keep))) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }
}
