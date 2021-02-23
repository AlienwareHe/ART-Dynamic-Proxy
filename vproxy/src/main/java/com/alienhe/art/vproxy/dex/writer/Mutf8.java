package com.alienhe.art.vproxy.dex.writer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author alienhe
 */
final class Mutf8 {
    private Mutf8() {
    }

    static byte[] encode(String s) throws IOException {
        try (final OutputStream os = new OutputStream()) {
            try (DataOutputStream dos = new DataOutputStream(os)) {
                dos.writeUTF(s);
                return os.getBytes();
            }
        }
    }

    private static class OutputStream extends ByteArrayOutputStream {
        byte[] getBytes() {
            byte[] bytes = new byte[count - 2];
            System.arraycopy(buf, 2, bytes, 0, bytes.length);
            return bytes;
        }
    }
}