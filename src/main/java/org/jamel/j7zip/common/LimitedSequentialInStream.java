package org.jamel.j7zip.common;

import java.io.InputStream;

public class LimitedSequentialInStream extends InputStream {

    private static final int EOF = -1;

    InputStream _stream;
    long _size;
    long _pos;
    boolean _wasFinished;

    public LimitedSequentialInStream() {
    }

    public void setStream(java.io.InputStream stream) {
        _stream = stream;
    }

    public void init(long streamSize) {
        _size = streamSize;
        _pos = 0;
        _wasFinished = false;
    }

    public int read() throws java.io.IOException {
        int ret = _stream.read();
        if (ret == EOF) _wasFinished = true;
        return ret;
    }

    public int read(byte[] data, int off, int size) throws java.io.IOException {
        long sizeToRead2 = (_size - _pos);
        if (size < sizeToRead2) sizeToRead2 = size;

        int sizeToRead = (int) sizeToRead2;

        if (sizeToRead > 0) {
            int realProcessedSize = _stream.read(data, off, sizeToRead);
            if (realProcessedSize == EOF) {
                _wasFinished = true;
                return EOF;
            }
            _pos += realProcessedSize;
            return realProcessedSize;
        }

        return EOF;
    }
}

