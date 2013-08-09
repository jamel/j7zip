package org.jamel.j7zip.common;

public class SequentialOutStreamImp2 extends java.io.OutputStream {

    private final byte[] buffer;
    private final int size;
    private int pos;


    public SequentialOutStreamImp2(byte[] buffer, int size) {
        this.buffer = buffer;
        this.size = size;
    }

    public void write(int b) throws java.io.IOException {
        throw new java.io.IOException("SequentialOutStreamImp2 - write() not implemented");
    }

    public void write(byte[] data, int off, int size) throws java.io.IOException {
        for (int i = 0; i < size; i++) {
            if (pos < this.size) {
                buffer[pos++] = data[off + i];
            } else {
                throw new java.io.IOException("SequentialOutStreamImp2 - can't write");
            }
        }
    }
}

