package com.alienhe.art.vproxy.dex.writer;

import java.io.Closeable;
import java.util.Arrays;

/**
 * @author alienhe
 */
class DexOutputStream implements Closeable {

    private int pos;

    private int count;

    private byte[] buf = new byte[32];

    private byte[] tmp = new byte[8];

    int getPosition() {
        return pos;
    }

    void setPosition(final int pos) {
        this.pos = pos;
        count = Math.max(count, pos);
    }

    int getSize() {
        return count;
    }

    @Override
    public void close() {

    }

    void write(final byte[] data) {
        write(data, 0, data.length);
    }

    void write(final byte[] data, final int start, final int size) {
        ensureCapacity(pos + size);
        System.arraycopy(data, start, buf, pos, size);
        setPosition(pos + size);
    }

    void writeInt(final int value) {
        tmp[0] = (byte) (value & 0xFF);
        tmp[1] = (byte) ((value >> 8) & 0xFF);
        tmp[2] = (byte) ((value >> 16) & 0xFF);
        tmp[3] = (byte) ((value >> 24) & 0xFF);
        write(tmp, 0, 4);
    }

    void writeShort(final int value) {
        tmp[0] = (byte) (value & 0xFF);
        tmp[1] = (byte) ((value >> 8) & 0xFF);
        write(tmp, 0, 2);
    }

    void writeByte(final int value) {
        tmp[0] = (byte) (value & 0xFF);
        write(tmp, 0, 1);
    }

    void writeUleb128(int uleb) {
        int bi = 0;
        do {
            int b = (uleb & 0x7F);
            uleb >>= 7;
            if (uleb != 0) {
                b |= 0x80;
            }
            tmp[bi++] = (byte) b;
        } while (uleb != 0);

        write(tmp, 0, bi);
    }

    private void ensureCapacity(final int capacity) {
        if (capacity > buf.length) {
            int newSize = 2 * buf.length;
            while (newSize < capacity) {
                newSize *= 2;
            }
            byte[] old = buf;
            buf = new byte[newSize];
            System.arraycopy(old, 0, buf, 0, old.length);
        }
    }

    byte[] toByteArray() {
        return Arrays.copyOf(buf, count);
    }
}
