package id.shisen.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Tes perilaku penyimpanan APK pembaruan:
 * 1) APK selalu ditulis di subfolder "updates" di bawah root cache (bukan root itu sendiri).
 * 2) file_paths.xml hanya membagikan subfolder updates/ — tidak ada lagi path="." yang
 *    membagikan seluruh cache/files.
 * 3) deleteOldApks menghapus APK lama dan mempertahankan berkas yang sedang dipasang.
 */
public class UpdateApkStoreTest {

    @Test
    public void apkFileAlwaysInsideUpdatesSubfolder() throws Exception {
        File base = Files.createTempDirectory("shisen-cache").toFile();
        File apk = UpdateApkStore.apkFile(base, "0.1.0-beta.5");

        assertEquals(new File(base, "updates").getAbsolutePath(), apk.getParentFile().getAbsolutePath());
        assertEquals("shisen-update-v0.1.0-beta.5.apk", apk.getName());
        assertTrue(apk.getParentFile().isDirectory());
        // root cache sendiri TIDAK dipakai sebagai lokasi APK
        assertFalse(new File(base, "shisen-update-v0.1.0-beta.5.apk").exists());
    }

    @Test
    public void filePathsXmlOnlySharesUpdatesSubfolder() throws Exception {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        File candidate = new File(dir, "src/main/res/xml/file_paths.xml");
        while (!candidate.exists() && dir.getParentFile() != null) {
            dir = dir.getParentFile();
            candidate = new File(dir, "android/app/src/main/res/xml/file_paths.xml");
        }
        assertTrue("file_paths.xml tidak ditemukan", candidate.exists());

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringComments(true);
        Document doc = dbf.newDocumentBuilder().parse(new FileInputStream(candidate));

        NodeList paths = doc.getDocumentElement().getElementsByTagName("*");
        assertTrue("harus ada minimal satu path-share", paths.getLength() > 0);
        for (int i = 0; i < paths.getLength(); i++) {
            Element e = (Element) paths.item(i);
            String path = e.getAttribute("path");
            assertFalse("path-share '\" + e.getTagName() + \"' tidak boleh path=\".\" (membagikan seluruh folder)",
                    path.equals(".") || path.isEmpty());
            assertTrue("path-share harus di dalam subfolder updates/",
                    path.startsWith("updates") || path.equals("updates/"));
        }
    }

    @Test
    public void deleteOldApksKeepsPendingInstallAndRemovesOthers() throws Exception {
        File base = Files.createTempDirectory("shisen-cache").toFile();
        File keep = UpdateApkStore.apkFile(base, "9.9.9");
        Files.write(keep.toPath(), "apk".getBytes(StandardCharsets.UTF_8));

        File old1 = UpdateApkStore.apkFile(base, "0.1.0");
        Files.write(old1.toPath(), "old".getBytes(StandardCharsets.UTF_8));
        File notApk = new File(new File(base, "updates"), "notes.txt");
        Files.write(notApk.toPath(), "data".getBytes(StandardCharsets.UTF_8));

        UpdateApkStore.deleteOldApks(base, keep);

        assertTrue("APK yang sedang dipasang harus dipertahankan", keep.exists());
        assertFalse("APK lama harus dihapus", old1.exists());
        assertTrue("Berkas non-APK tidak perlu dihapus", notApk.exists());
    }
}
