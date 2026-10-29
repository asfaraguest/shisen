package id.shisen.app.util;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DnsFallbackResolverTest {

    @Test
    public void testCleanSystemDnsReturnsImmediately() throws Exception {
        InetAddress cleanIp = InetAddress.getByName("104.21.5.12");

        DnsFallbackResolver.TierResolver sys = host -> Collections.singletonList(cleanIp);
        DnsFallbackResolver.TierResolver cf = host -> { throw new AssertionError("CF should not be called"); };
        DnsFallbackResolver.TierResolver gg = host -> { throw new AssertionError("GG should not be called"); };

        DnsFallbackResolver resolver = new DnsFallbackResolver(sys, cf, gg, null);
        List<InetAddress> result = resolver.lookup("clean-host.com");

        assertEquals(1, result.size());
        assertEquals("104.21.5.12", result.get(0).getHostAddress());
    }

    @Test
    public void testPoisonedSystemDnsFallsBackToCloudflare() throws Exception {
        // 127.0.0.1 and 118.98.x.x are known Indonesian ISP sinkhole / Internet Positif IPs
        InetAddress poisonedIp = InetAddress.getByName("127.0.0.1");
        InetAddress cfIp = InetAddress.getByName("172.67.140.22");

        DnsFallbackResolver.TierResolver sys = host -> Collections.singletonList(poisonedIp);
        DnsFallbackResolver.TierResolver cf = host -> Collections.singletonList(cfIp);
        DnsFallbackResolver.TierResolver gg = host -> { throw new AssertionError("GG should not be called"); };

        DnsFallbackResolver resolver = new DnsFallbackResolver(sys, cf, gg, null);
        List<InetAddress> result = resolver.lookup("blocked-anime-host.com");

        assertEquals(1, result.size());
        assertEquals("172.67.140.22", result.get(0).getHostAddress());
    }

    @Test
    public void testCloudflareFailureFallsBackToGoogle() throws Exception {
        InetAddress ggIp = InetAddress.getByName("142.250.190.46");

        DnsFallbackResolver.TierResolver sys = host -> { throw new Exception("System DNS timeout"); };
        DnsFallbackResolver.TierResolver cf = host -> { throw new Exception("Cloudflare DoH timeout"); };
        DnsFallbackResolver.TierResolver gg = host -> Collections.singletonList(ggIp);

        DnsFallbackResolver resolver = new DnsFallbackResolver(sys, cf, gg, null);
        List<InetAddress> result = resolver.lookup("stream-cdn.com");

        assertEquals(1, result.size());
        assertEquals("142.250.190.46", result.get(0).getHostAddress());
    }

    @Test
    public void testAllDohFailureFallsBackToHardcodedCdnIp() throws Exception {
        Map<String, String> known = new HashMap<>();
        known.put("hls.dramahot.top", "93.123.109.210");

        DnsFallbackResolver.TierResolver sys = host -> { throw new Exception("Sys fail"); };
        DnsFallbackResolver.TierResolver cf = host -> { throw new Exception("CF fail"); };
        DnsFallbackResolver.TierResolver gg = host -> { throw new Exception("GG fail"); };

        DnsFallbackResolver resolver = new DnsFallbackResolver(sys, cf, gg, known);
        List<InetAddress> result = resolver.lookup("hls.dramahot.top");

        assertEquals(1, result.size());
        assertEquals("93.123.109.210", result.get(0).getHostAddress());
    }

    @Test
    public void testMemoryCacheAvoidsRedundantResolution() throws Exception {
        final int[] sysCallCount = {0};
        InetAddress ip = InetAddress.getByName("104.26.12.31");

        DnsFallbackResolver.TierResolver sys = host -> {
            sysCallCount[0]++;
            return Collections.singletonList(ip);
        };

        DnsFallbackResolver resolver = new DnsFallbackResolver(sys, null, null, null);

        // First call populates cache
        resolver.lookup("cached-domain.com");
        assertEquals(1, sysCallCount[0]);

        // Second call hits cache directly
        resolver.lookup("cached-domain.com");
        assertEquals("Subsequent lookup must hit cache without invoking resolver", 1, sysCallCount[0]);
    }

    @Test(expected = UnknownHostException.class)
    public void testExhaustedTiersThrowsUnknownHostException() throws Exception {
        DnsFallbackResolver.TierResolver fail = host -> { throw new Exception("Down"); };
        DnsFallbackResolver resolver = new DnsFallbackResolver(fail, fail, fail, Collections.emptyMap());
        resolver.lookup("completely-unresolvable.xyz");
    }

    @Test
    public void testFilterPoisonedIpsRemovesSinkholesAndLocalAddresses() throws Exception {
        java.util.List<InetAddress> ips = new java.util.ArrayList<>();
        ips.add(InetAddress.getByName("0.0.0.0"));
        ips.add(InetAddress.getByName("127.0.0.1"));
        ips.add(InetAddress.getByName("10.0.0.1"));
        ips.add(InetAddress.getByName("192.168.1.1"));
        ips.add(InetAddress.getByName("118.98.44.10")); // Mercusuar ISP sinkhole
        ips.add(InetAddress.getByName("::1"));
        ips.add(InetAddress.getByName("104.21.5.12")); // Valid public IP

        List<InetAddress> clean = DnsFallbackResolver.filterPoisonedIps(ips);
        assertEquals(1, clean.size());
        assertEquals("104.21.5.12", clean.get(0).getHostAddress());
    }

    @Test
    public void testLookupHostnameNormalizedToLowercaseAndTrimmed() throws Exception {
        InetAddress ip = InetAddress.getByName("104.21.5.12");
        DnsFallbackResolver.TierResolver sys = host -> {
            assertEquals("mixed-case-domain.com", host);
            return Collections.singletonList(ip);
        };
        DnsFallbackResolver resolver = new DnsFallbackResolver(sys, null, null, null);
        List<InetAddress> res = resolver.lookup("   MIXED-CASE-DOMAIN.COM   ");
        assertEquals(1, res.size());
        assertEquals("104.21.5.12", res.get(0).getHostAddress());
    }
}
