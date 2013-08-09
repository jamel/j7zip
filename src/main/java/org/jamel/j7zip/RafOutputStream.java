package org.jamel.j7zip;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class RafOutputStream extends OutputStream {

    private final RandomAccessFile file;


    public RafOutputStream(File file, String mode) throws IOException {
        this.file = new RandomAccessFile(file, mode);
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

    public void seek(long pos) throws IOException {
        file.seek(pos);
    }
}
