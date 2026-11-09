package id.shisen.app.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class AppUpdateManagerTest {

    @Test
    public void testStableReleaseIsNewerThanBetaOfSameVersion() {
        // 0.1.0 (stabil) harus dianggap lebih baru dari 0.1.0-beta.5
        assertTrue(AppUpdateManager.isNewerVersion("0.1.0", "0.1.0-beta.5"));
        assertTrue(AppUpdateManager.isNewerVersion("v0.1.0", "v0.1.0-beta.16"));

        // Sebaliknya, 0.1.0-beta.5 TIDAK lebih baru dari 0.1.0 stabil
        assertFalse(AppUpdateManager.isNewerVersion("0.1.0-beta.5", "0.1.0"));
        assertFalse(AppUpdateManager.isNewerVersion("v0.1.0-beta.16", "v0.1.0"));
    }

    @Test
    public void testHigherBetaIsNewerThanLowerBeta() {
        assertTrue(AppUpdateManager.isNewerVersion("0.1.0-beta.6", "0.1.0-beta.5"));
        assertTrue(AppUpdateManager.isNewerVersion("0.1.0-beta.16", "0.1.0-beta.9"));
        assertFalse(AppUpdateManager.isNewerVersion("0.1.0-beta.5", "0.1.0-beta.6"));
    }

    @Test
    public void testHigherMajorMinorPatchIsNewerRegardlessOfBeta() {
        // Minor/patch naik lebih tinggi
        assertTrue(AppUpdateManager.isNewerVersion("0.1.1-beta.1", "0.1.0"));
        assertTrue(AppUpdateManager.isNewerVersion("0.2.0-beta.1", "0.1.9"));
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "0.9.9"));
        assertFalse(AppUpdateManager.isNewerVersion("0.1.0", "0.1.1"));
    }

    @Test
    public void testIdenticalVersionsReturnFalse() {
        assertFalse(AppUpdateManager.isNewerVersion("0.1.0", "0.1.0"));
        assertFalse(AppUpdateManager.isNewerVersion("v0.1.0-beta.5", "0.1.0-beta.5"));
    }

    @Test
    public void testSingleRepoOwnerAndConsistentConstants() {
        assertEquals("asfaraguest", AppUpdateManager.GITHUB_OWNER);
        assertEquals("shisen", AppUpdateManager.GITHUB_REPO);
        assertTrue(AppUpdateManager.GITHUB_RELEASES_URL.contains("asfaraguest/shisen"));
        assertTrue(AppUpdateManager.GITHUB_API_LATEST_RELEASE.contains("asfaraguest/shisen"));
        assertTrue(AppUpdateManager.GITHUB_API_RELEASES.contains("asfaraguest/shisen"));
        assertFalse(AppUpdateManager.GITHUB_RELEASES_URL.contains("asfara/shisen"));
    }

    @Test
    public void testFindNewerReleaseFromReleasesArray() {
        String json = "[\n" +
                "  {\n" +
                "    \"tag_name\": \"v1.1.0-beta.30\",\n" +
                "    \"body\": \"Perbaikan TV Coocaa SSL\",\n" +
                "    \"assets\": [\n" +
                "      {\n" +
                "        \"name\": \"shisen-release-v1.1.0-beta.30.apk\",\n" +
                "        \"browser_download_url\": \"https://github.com/asfaraguest/shisen/releases/download/v1.1.0-beta.30/shisen-release-v1.1.0-beta.30.apk\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": \"shisen-tv-release-v1.1.0-beta.30.apk\",\n" +
                "        \"browser_download_url\": \"https://github.com/asfaraguest/shisen/releases/download/v1.1.0-beta.30/shisen-tv-release-v1.1.0-beta.30.apk\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"tag_name\": \"v1.1.0-beta.29\"\n" +
                "  }\n" +
                "]";

        // TV flavor
        AppUpdateManager.UpdateCandidate candidateTv = AppUpdateManager.findNewerRelease(json, "1.1.0-beta.29", true);
        assertNotNull(candidateTv);
        assertEquals("1.1.0-beta.30", candidateTv.version);
        assertTrue(candidateTv.downloadUrl.contains("shisen-tv-release-v1.1.0-beta.30.apk"));

        // Mobile flavor
        AppUpdateManager.UpdateCandidate candidateMobile = AppUpdateManager.findNewerRelease(json, "1.1.0-beta.29", false);
        assertNotNull(candidateMobile);
        assertEquals("1.1.0-beta.30", candidateMobile.version);
        assertTrue(candidateMobile.downloadUrl.contains("shisen-release-v1.1.0-beta.30.apk"));

        // Same version -> null
        AppUpdateManager.UpdateCandidate candidateSame = AppUpdateManager.findNewerRelease(json, "1.1.0-beta.30", true);
        assertNull(candidateSame);
    }

    @Test
    public void testFindNewerReleaseFallbackDeterministicUrl() {
        String jsonWithoutAssets = "[\n" +
                "  {\n" +
                "    \"tag_name\": \"v1.1.0-beta.30\",\n" +
                "    \"assets\": []\n" +
                "  }\n" +
                "]";

        AppUpdateManager.UpdateCandidate candidate = AppUpdateManager.findNewerRelease(jsonWithoutAssets, "1.1.0-beta.29", true);
        assertNotNull(candidate);
        assertEquals("https://github.com/asfaraguest/shisen/releases/download/v1.1.0-beta.30/shisen-tv-release-v1.1.0-beta.30.apk", candidate.downloadUrl);
    }
}
