package org.jamel.j7zip.common;

import org.jamel.j7zip.IInStream;

public class LockedInStream {
    IInStream _stream;

    public LockedInStream() {
    }

    public void Init(IInStream stream) {
        _stream = stream;
    }

    public synchronized int read(long startPos, byte[] data, int size) throws java.io.IOException {
        _stream.seekFromBegin(startPos);
        return _stream.read(data, 0, size);
    }

    public synchronized int read(long startPos, byte[] data, int off, int size) throws java.io.IOException {
        _stream.seekFromBegin(startPos);
        return _stream.read(data, off, size);
    }
}
