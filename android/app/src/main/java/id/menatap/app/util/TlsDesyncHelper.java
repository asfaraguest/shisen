package id.menatap.app.util;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * TlsDesyncHelper implements RFC-compliant TLS ClientHello multi-record splitting
 * and TCP segment segmentation to bypass deep packet inspection (DPI) and SNI filtering.
 *
 * Parity reference: backend/api/desync.py (Tatap OG).
 */
public final class TlsDesyncHelper {

    private TlsDesyncHelper() {}

    /**
     * Splits a TLS Handshake record (ClientHello) into two valid RFC-compliant TLS records.
     * Record 1: 5-byte TLS header + first byte of handshake payload (length = 1).
     * Record 2: 5-byte TLS header + remaining bytes of handshake payload.
     *
     * Middlebox DPI inspecting only the first TLS record fails to locate the SNI hostname,
     * allowing the connection to succeed without triggering blocking.
     */
    public static byte[][] splitClientHello(byte[] record) {
        if (record == null || record.length <= 6) {
            return new byte[][]{ record != null ? record.clone() : new byte[0] };
        }

        // Validate TLS Handshake ContentType (0x16)
        if ((record[0] & 0xFF) != 0x16) {
            return new byte[][]{ record.clone() };
        }

        // 3-byte record prefix: ContentType (0x16) + ProtocolVersion (e.g. 0x03, 0x01 / 0x03, 0x03)
        byte[] prefix = Arrays.copyOfRange(record, 0, 3);
        byte[] payload = Arrays.copyOfRange(record, 5, record.length);

        if (payload.length <= 1) {
            return new byte[][]{ record.clone() };
        }

        // Record 1: 1 byte of handshake data
        byte[] rec1 = new byte[6];
        System.arraycopy(prefix, 0, rec1, 0, 3);
        rec1[3] = 0x00;
        rec1[4] = 0x01; // length = 1
        rec1[5] = payload[0];

        // Record 2: Remaining handshake data
        int rec2DataLen = payload.length - 1;
        byte[] rec2 = new byte[5 + rec2DataLen];
        System.arraycopy(prefix, 0, rec2, 0, 3);
        rec2[3] = (byte) ((rec2DataLen >> 8) & 0xFF);
        rec2[4] = (byte) (rec2DataLen & 0xFF);
        System.arraycopy(payload, 1, rec2, 5, rec2DataLen);

        return new byte[][]{ rec1, rec2 };
    }

    /**
     * Splits TLS ClientHello at the SNI boundary if found, or at splitOffset.
     */
    public static byte[][] splitClientHelloAtOffset(byte[] record, int splitOffset) {
        if (record == null || record.length <= 6) {
            return new byte[][]{ record != null ? record.clone() : new byte[0] };
        }
        if ((record[0] & 0xFF) != 0x16) {
            return new byte[][]{ record.clone() };
        }

        byte[] prefix = Arrays.copyOfRange(record, 0, 3);
        byte[] payload = Arrays.copyOfRange(record, 5, record.length);

        int split = Math.max(1, Math.min(splitOffset, payload.length - 1));

        // Record 1
        byte[] rec1 = new byte[5 + split];
        System.arraycopy(prefix, 0, rec1, 0, 3);
        rec1[3] = (byte) ((split >> 8) & 0xFF);
        rec1[4] = (byte) (split & 0xFF);
        System.arraycopy(payload, 0, rec1, 5, split);

        // Record 2
        int remLen = payload.length - split;
        byte[] rec2 = new byte[5 + remLen];
        System.arraycopy(prefix, 0, rec2, 0, 3);
        rec2[3] = (byte) ((remLen >> 8) & 0xFF);
        rec2[4] = (byte) (remLen & 0xFF);
        System.arraycopy(payload, split, rec2, 5, remLen);

        return new byte[][]{ rec1, rec2 };
    }

    /**
     * Reassembles the handshake payload from multiple TLS records.
     * Used in unit tests to mathematically prove that splitting preserves exact original payload.
     */
    public static byte[] reassembleHandshakePayload(byte[][] records) {
        if (records == null || records.length == 0) return new byte[0];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (byte[] rec : records) {
            if (rec != null && rec.length > 5) {
                baos.write(rec, 5, rec.length - 5);
            }
        }
        return baos.toByteArray();
    }

    /**
     * Splits raw TCP byte stream into two segments at the given split offset.
     */
    public static byte[][] splitTcpSegments(byte[] data, int splitOffset) {
        if (data == null || data.length <= 1) {
            return new byte[][]{ data != null ? data.clone() : new byte[0] };
        }
        int split = Math.max(1, Math.min(splitOffset, data.length - 1));
        byte[] seg1 = Arrays.copyOfRange(data, 0, split);
        byte[] seg2 = Arrays.copyOfRange(data, split, data.length);
        return new byte[][]{ seg1, seg2 };
    }

    /**
     * Scans a TLS ClientHello record to find the offset of the Server Name (SNI) extension.
     * Returns -1 if SNI is not found or malformed.
     */
    public static int findSniOffset(byte[] record) {
        if (record == null || record.length < 44 || (record[0] & 0xFF) != 0x16) {
            return -1;
        }
        try {
            int pos = 5; // Start of handshake
            if ((record[pos] & 0xFF) != 0x01) return -1; // HandshakeType: ClientHello (1)
            pos += 4; // Skip type (1) + length (3)
            pos += 2; // Skip ClientVersion (2)
            pos += 32; // Skip Random (32)

            if (pos >= record.length) return -1;
            int sessionIdLen = record[pos] & 0xFF;
            pos += 1 + sessionIdLen;

            if (pos + 2 >= record.length) return -1;
            int cipherSuitesLen = ((record[pos] & 0xFF) << 8) | (record[pos + 1] & 0xFF);
            pos += 2 + cipherSuitesLen;

            if (pos >= record.length) return -1;
            int compressionMethodsLen = record[pos] & 0xFF;
            pos += 1 + compressionMethodsLen;

            if (pos + 2 >= record.length) return -1;
            int extensionsLen = ((record[pos] & 0xFF) << 8) | (record[pos + 1] & 0xFF);
            pos += 2;

            int endExt = Math.min(pos + extensionsLen, record.length);
            while (pos + 4 <= endExt) {
                int extType = ((record[pos] & 0xFF) << 8) | (record[pos + 1] & 0xFF);
                int extLen = ((record[pos + 2] & 0xFF) << 8) | (record[pos + 3] & 0xFF);
                pos += 4;
                if (extType == 0) { // server_name (SNI)
                    // ServerNameList length (2 bytes) + ServerNameType (1 byte) + Name length (2 bytes)
                    if (pos + 5 <= endExt) {
                        return pos + 5; // Offset of actual hostname bytes
                    }
                    return pos;
                }
                pos += extLen;
            }
        } catch (Exception ignored) {}
        return -1;
    }
}
