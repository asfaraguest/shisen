package id.shisen.app.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DynamicBlockedHostsTest {

    @Before
    @After
    public void cleanup() {
        DynamicBlockedHosts.reset();
    }

    @Test
    public void testKnownBlockedHostsAreRecognized() {
        assertTrue(DynamicBlockedHosts.isBlocked("dramahot.top"));
        assertTrue(DynamicBlockedHosts.isBlocked("hls.dramahot.top"));
        assertTrue(DynamicBlockedHosts.isBlocked("drama1.cfd"));
        assertTrue(DynamicBlockedHosts.isBlocked("zokoanime.video"));
        assertTrue(DynamicBlockedHosts.isBlocked("stream121.space"));
        assertTrue(DynamicBlockedHosts.isBlocked("megacloud.tv"));
        assertTrue(DynamicBlockedHosts.isBlocked("rapid-cloud.co"));
    }

    @Test
    public void testWhitelistedHostsNeverBlocked() {
        assertFalse(DynamicBlockedHosts.isBlocked("clients5.google.com"));
        assertFalse(DynamicBlockedHosts.isBlocked("translate.google.com"));
        assertFalse(DynamicBlockedHosts.isBlocked("graphql.anilist.co"));
        assertFalse(DynamicBlockedHosts.isBlocked("hianime.to"));
        assertFalse(DynamicBlockedHosts.isBlocked("hianimes.se"));
        assertFalse(DynamicBlockedHosts.isBlocked("animetosho.net"));

        // Attempting to dynamically mark a whitelisted host as blocked must be a no-op
        DynamicBlockedHosts.markBlocked("clients5.google.com");
        assertFalse(DynamicBlockedHosts.isBlocked("clients5.google.com"));

        DynamicBlockedHosts.markBlocked("hianime.to");
        assertFalse(DynamicBlockedHosts.isBlocked("hianime.to"));
    }

    @Test
    public void testDynamicBlockedHostLearning() {
        String testHost = "custom-blocked-cdn.xyz";
        assertFalse(DynamicBlockedHosts.isBlocked(testHost));

        DynamicBlockedHosts.markBlocked(testHost);
        assertTrue(DynamicBlockedHosts.isBlocked(testHost));

        DynamicBlockedHosts.unmark(testHost);
        assertFalse(DynamicBlockedHosts.isBlocked(testHost));
    }
}
