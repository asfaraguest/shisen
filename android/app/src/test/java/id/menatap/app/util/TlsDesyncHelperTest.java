package id.menatap.app.util;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class TlsDesyncHelperTest {

    /**
     * Synthesizes a valid TLS 1.2 ClientHello record with SNI extension for testing.
     */
    private byte[] createSyntheticClientHello(String serverName) throws Exception {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();

        // Handshake header: Type 1 (ClientHello), Length placeholder (3 bytes)
        payload.write(0x01);
        payload.write(new byte[]{ 0x00, 0x00, 0x00 }); // placeholder for length
        payload.write(new byte[]{ 0x03, 0x03 }); // ClientVersion TLS 1.2

        // 32-byte Random
        byte[] random = new byte[32];
        Arrays.fill(random, (byte) 0xAA);
        payload.write(random);

        // Session ID length: 0
        payload.write(0x00);

        // Cipher suites length: 2 (1 suite)
        payload.write(new byte[]{ 0x00, 0x02, (byte) 0xC0, 0x2F });

        // Compression methods: 1 byte (null compression)
        payload.write(new byte[]{ 0x01, 0x00 });

        // Extensions
        ByteArrayOutputStream extPayload = new ByteArrayOutputStream();

        // Extension: server_name (0x0000)
        byte[] snBytes = serverName.getBytes(StandardCharsets.UTF_8);
        int snListLen = snBytes.length + 3; // 1 byte type + 2 bytes len + name
        int extLen = snListLen + 2;

        extPayload.write(new byte[]{ 0x00, 0x00 }); // type: server_name
        extPayload.write(new byte[]{ (byte) ((extLen >> 8) & 0xFF), (byte) (extLen & 0xFF) });
        extPayload.write(new byte[]{ (byte) ((snListLen >> 8) & 0xFF), (byte) (snListLen & 0xFF) });
        extPayload.write(0x00); // host_name type
        extPayload.write(new byte[]{ (byte) ((snBytes.length >> 8) & 0xFF), (byte) (snBytes.length & 0xFF) });
        extPayload.write(snBytes);

        byte[] allExt = extPayload.toByteArray();
        payload.write(new byte[]{ (byte) ((allExt.length >> 8) & 0xFF), (byte) (allExt.length & 0xFF) });
        payload.write(allExt);

        byte[] rawPayload = payload.toByteArray();
        // Fill 3-byte handshake length
        int hsLen = rawPayload.length - 4;
        rawPayload[1] = (byte) ((hsLen >> 16) & 0xFF);
        rawPayload[2] = (byte) ((hsLen >> 8) & 0xFF);
        rawPayload[3] = (byte) (hsLen & 0xFF);

        // TLS Record Header: 0x16 (Handshake), Version 0x03, 0x01, 2-byte Record Length
        ByteArrayOutputStream record = new ByteArrayOutputStream();
        record.write(0x16);
        record.write(new byte[]{ 0x03, 0x01 });
        record.write(new byte[]{ (byte) ((rawPayload.length >> 8) & 0xFF), (byte) (rawPayload.length & 0xFF) });
        record.write(rawPayload);

        return record.toByteArray();
    }

    @Test
    public void testClientHelloSplitReassemblesToExactOriginalPayload() throws Exception {
        byte[] clientHello = createSyntheticClientHello("hls.dramahot.top");
        assertTrue("ClientHello should be a valid record", clientHello.length > 5);
        assertEquals(0x16, clientHello[0] & 0xFF);

        byte[] originalPayload = Arrays.copyOfRange(clientHello, 5, clientHello.length);

        byte[][] split = TlsDesyncHelper.splitClientHello(clientHello);
        assertEquals(2, split.length);

        byte[] rec1 = split[0];
        byte[] rec2 = split[1];

        // Record 1 verification
        assertEquals(0x16, rec1[0] & 0xFF);
        assertEquals(clientHello[1], rec1[1]);
        assertEquals(clientHello[2], rec1[2]);
        assertEquals(0x00, rec1[3] & 0xFF);
        assertEquals(0x01, rec1[4] & 0xFF); // Length must be 1
        assertEquals(6, rec1.length); // 5 bytes header + 1 byte data
        assertEquals(originalPayload[0], rec1[5]);

        // Record 2 verification
        assertEquals(0x16, rec2[0] & 0xFF);
        assertEquals(clientHello[1], rec2[1]);
        assertEquals(clientHello[2], rec2[2]);
        int rec2Len = ((rec2[3] & 0xFF) << 8) | (rec2[4] & 0xFF);
        assertEquals(originalPayload.length - 1, rec2Len);
        assertEquals(5 + rec2Len, rec2.length);

        // Mathematical proof: Reassembly of split records is 100% identical to original
        byte[] reassembled = TlsDesyncHelper.reassembleHandshakePayload(split);
        assertArrayEquals("Reassembled payload must match original handshake payload byte-for-byte",
                originalPayload, reassembled);
    }

    @Test
    public void testTcpSegmentSplitConcatenationIsIdentical() {
        byte[] data = "GET /playlist.m3u8 HTTP/1.1\r\nHost: hls.dramahot.top\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        int splitOffset = 14;

        byte[][] segments = TlsDesyncHelper.splitTcpSegments(data, splitOffset);
        assertEquals(2, segments.length);

        ByteArrayOutputStream combined = new ByteArrayOutputStream();
        combined.write(segments[0], 0, segments[0].length);
        combined.write(segments[1], 0, segments[1].length);

        assertArrayEquals(data, combined.toByteArray());
    }

    @Test
    public void testFindSniOffsetLocatesServerName() throws Exception {
        String host = "zokoanime.video";
        byte[] clientHello = createSyntheticClientHello(host);

        int sniOffset = TlsDesyncHelper.findSniOffset(clientHello);
        assertTrue("SNI offset should be found in synthetic ClientHello", sniOffset > 0);

        byte[] extractedNameBytes = Arrays.copyOfRange(clientHello, sniOffset, sniOffset + host.length());
        String extractedName = new String(extractedNameBytes, StandardCharsets.UTF_8);
        assertEquals(host, extractedName);
    }
}
