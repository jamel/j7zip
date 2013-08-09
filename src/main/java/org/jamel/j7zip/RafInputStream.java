package org.jamel.j7zip;

import java.io.IOException;
import java.io.RandomAccessFile;

public class RafInputStream extends IInStream {

    private final RandomAccessFile file;


    public RafInputStream(String filename, String mode) throws IOException {
        file = new RandomAccessFile(filename, mode);
    }

    @Override
    public long seekFromBegin(long offset) throws IOException {
        file.seek(offset);
        return file.getFilePointer();
    }

    @Override
    public long seekFromCurrent(long offset) throws IOException {
        file.seek(offset + file.getFilePointer());
        return file.getFilePointer();
    }

    public int read() throws IOException {
        return file.read();
    }

    public int read(byte[] data, int off, int size) throws IOException {
        return file.read(data, off, size);
    }

    public int read(byte[] data, int size) throws IOException {
        return file.read(data, 0, size);
    }

    public void write(byte[] b) throws IOException {
        file.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        file.write(b, off, len);
    }

    public void write(int b) throws IOException {
        file.write(b);
    }

    public void close() throws IOException {
        file.close();
    }
}
