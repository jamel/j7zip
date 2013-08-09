package org.jamel.j7zip.archive.sevenZip;

import java.io.IOException;

class InByte2 {
    private final byte[] buffer;
    private final int size;
    private int pos;


    InByte2(byte[] buffer, int size) {
        this.buffer = buffer;
        this.size = size;
    }

    public int readByte() throws IOException {
        if (pos >= size) {
            throw new IOException("CInByte2 - Can't read stream");
        }
        return (buffer[pos++] & 0xFF);
    }

    int readBytes2(byte[] data, int size) {
        int processedSize;
        for (processedSize = 0; processedSize < size && pos < this.size; processedSize++) {
            data[processedSize] = buffer[pos++];
        }
        return processedSize;
    }

    boolean readBytes(byte[] data, int size) {
        int processedSize = readBytes2(data, size);
        return (processedSize == size);
    }
}
