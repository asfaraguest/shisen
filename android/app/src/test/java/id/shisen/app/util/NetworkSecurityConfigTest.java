package id.shisen.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Tes perilaku konfigurasi jaringan (resource XML):
 * 1) base-config TIDAK boleh mempercayai <certificates src="user"/> (CA user = risiko MITM),
 *    di luar blok <debug-overrides>.
 * 2) cleartextTrafficPermitted="false" wajib ada di base-config.
 * Ini menegakkan kebijakan, bukan mencocokan string].
 */
public class NetworkSecurityConfigTest {

    private File configFile;

    @Before
    public void locateConfig() throws Exception {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        // dari modul android/app turun menemukan res/xml
        File candidate = new File(dir, "src/main/res/xml/network_security_config.xml");
        while (!candidate.exists() && dir.getParentFile() != null) {
            dir = dir.getParentFile();
            candidate = new File(dir, "android/app/src/main/res/xml/network_security_config.xml");
        }
        if (!candidate.exists()) {
            // fallback: cari dari current dir langsung
            candidate = new File(dir, "app/src/main/res/xml/network_security_config.xml");
        }
        assertTrue("network_security_config.xml tidak ditemukan dari " + System.getProperty("user.dir"), candidate.exists());
        configFile = candidate;
    }

    private Document parse(File f) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringComments(true);
        return dbf.newDocumentBuilder().parse(new FileInputStream(f));
    }

    private static boolean trustAnchorContainsUser(Element container) {
        NodeList anchors = container.getElementsByTagName("trust-anchors");
        for (int i = 0; i < anchors.getLength(); i++) {
            Element ta = (Element) anchors.item(i);
            NodeList certs = ta.getElementsByTagName("certificates");
            for (int j = 0; j < certs.getLength(); j++) {
                Element c = (Element) certs.item(j);
                if ("user".equals(c.getAttribute("src"))) return true;
            }
        }
        return false;
    }

    @Test
    public void baseConfigRequiresNoUserTrustAnchorAndNoCleartext() throws Exception {
        Document doc = parse(configFile);
        NodeList bases = doc.getDocumentElement()
                .getElementsByTagName("base-config");
        assertEquals("harus ada tepat satu base-config", 1, bases.getLength());

        Element base = (Element) bases.item(0);
        assertFalse("base-config wajib cleartextTrafficPermitted=\"false\"",
                base.hasAttribute("cleartextTrafficPermitted") && base.getAttribute("cleartextTrafficPermitted").equals("true"));
        assertNotNull(base.getAttribute("cleartextTrafficPermitted"));
        assertEquals("cleartextTrafficPermitted base-config wajib false", "false", base.getAttribute("cleartextTrafficPermitted"));
        assertFalse("base-config TIDAK boleh mempercayai CA user", trustAnchorContainsUser(base));
    }

    @Test
    public void userTrustAnchorOnlyAllowedInsideDebugOverrides() throws Exception {
        Document doc = parse(configFile);
        NodeList overrides = doc.getDocumentElement().getElementsByTagName("debug-overrides");
        if (overrides.getLength() == 0) {
            // tanpa debug-overrides pun aman — pastikan juga tidak ada user anchor di seluruh dokumen
            assertFalse(trustAnchorContainsUser(doc.getDocumentElement()));
            return;
        }
        // debug-overrides boleh punya src="user"
        boolean overrideHasUser = trustAnchorContainsUser((Element) overrides.item(0));
        boolean outsideHasUser = false;
        // cek semua <domain-config> lama/kedua bila ada (selain base-config & debug-overrides)
        NodeList all = doc.getDocumentElement().getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element e = (Element) all.item(i);
            String tag = e.getTagName();
            if (tag.equals("domain-config")) {
                outsideHasUser |= trustAnchorContainsUser(e);
            }
        }
        assertFalse("domain-config (selain debug-overrides) tidak boleh mempercayai CA user", outsideHasUser);
    }

    @Test
    public void xmlParsesAndHasRootElement() throws Exception {
        Document doc = parse(configFile);
        assertEquals("network-security-config", doc.getDocumentElement().getTagName());
    }

    @Test
    public void cleartextDomainsLimitedToLocalhostForOllamaByok() throws Exception {
        Document doc = parse(configFile);
        NodeList configs = doc.getDocumentElement().getElementsByTagName("domain-config");
        for (int i = 0; i < configs.getLength(); i++) {
            Element dc = (Element) configs.item(i);
            if ("true".equals(dc.getAttribute("cleartextTrafficPermitted"))) {
                NodeList domains = dc.getElementsByTagName("domain");
                assertEquals("domain-config cleartext hanya boleh localhost", 2, domains.getLength());
                assertEquals("localhost", domains.item(0).getTextContent());
                assertEquals("127.0.0.1", domains.item(1).getTextContent());
            }
        }
    }
}
