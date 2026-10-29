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
        assertFalse(AppUpdateManager.GITHUB_RELEASES_URL.contains("asfara/shisen"));
    }
}
