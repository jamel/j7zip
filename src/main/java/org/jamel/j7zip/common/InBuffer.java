package org.jamel.j7zip.common;

public class InBuffer {
    int _bufferPos;
    int _bufferLimit;
    byte[] _bufferBase;
    java.io.InputStream _stream = null;
    long _processedSize;
    int _bufferSize;
    boolean _wasFinished;

    public InBuffer() {
    }

    public void Create(int bufferSize) {
        final int kMinBlockSize = 1;
        if (bufferSize < kMinBlockSize) {
            bufferSize = kMinBlockSize;
        }
        if (_bufferBase != null && _bufferSize == bufferSize) {
            return;
        }
        Free();
        _bufferSize = bufferSize;
        _bufferBase = new byte[bufferSize];
    }

    void Free() {
        _bufferBase = null;
    }

    public void SetStream(java.io.InputStream stream) {
        _stream = stream;
    }

    public void Init() {
        _processedSize = 0;
        _bufferPos = 0;
        _bufferLimit = 0;
        _wasFinished = false;
    }

    public void ReleaseStream() throws java.io.IOException {
        if (_stream != null) _stream.close();
        _stream = null;
    }

    public int read() throws java.io.IOException {
        if (_bufferPos >= _bufferLimit) {
            return readBlock2();
        }
        return _bufferBase[_bufferPos++] & 0xFF;
    }

    public boolean readBlock() throws java.io.IOException {
        if (_wasFinished) {
            return false;
        }
        _processedSize += _bufferPos;

        int numProcessedBytes = _stream.read(_bufferBase, 0, _bufferSize);
        if (numProcessedBytes == -1) numProcessedBytes = 0;

        _bufferPos = 0;
        _bufferLimit = numProcessedBytes;
        _wasFinished = (numProcessedBytes == 0);
        return (!_wasFinished);
    }

    private int readBlock2() throws java.io.IOException {
        if (!readBlock()) {
            return -1; // 0xFF;
        }
        return _bufferBase[_bufferPos++] & 0xFF;
    }

    public boolean wasFinished() {
        return _wasFinished;
    }
}
