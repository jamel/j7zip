package org.jamel.j7zip.archive.common;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jamel.j7zip.ICompressCoder;
import org.jamel.j7zip.ICompressFilter;
import org.jamel.j7zip.ICompressSetOutStream;


public class FilterCoder extends OutputStream implements ICompressCoder, ICompressSetOutStream {

    public ICompressFilter filter = null;

    OutputStream _outStream = null;
    int _bufferPos;

    boolean _outSizeIsDefined;
    long _outSize;
    long _nowPos64;

    public void code(InputStream inStream, OutputStream outStream, long outSize) throws IOException {
        throw new IOException("Not implemented");
    }

    public void write(int b) {
        throw new UnknownError("FilterCoder write");
    }

    public void write(byte b[], int off, int size) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (size < 0) ||
                ((off + size) > b.length) || ((off + size) < 0))
        {
            throw new IndexOutOfBoundsException();
        } else if (size == 0) {
            return;
        }

        if (off != 0) throw new IOException("FilterCoder - off <> 0");

        int cur_off = 0;
        while (size > 0) {
            int sizeMax = kBufferSize - _bufferPos;
            int sizeTemp = size;
            if (sizeTemp > sizeMax) {
                sizeTemp = sizeMax;
            }
            System.arraycopy(b, cur_off, _buffer, _bufferPos, sizeTemp);
            size -= sizeTemp;
            cur_off = cur_off + sizeTemp;
            int endPos = _bufferPos + sizeTemp;
            _bufferPos = filter.filter(_buffer, endPos);
            if (_bufferPos == 0) {
                _bufferPos = endPos;
                break;
            }
            if (_bufferPos > endPos) {
                if (size != 0) {
                    throw new IOException("FilterCoder - write() : size  <> 0"); // return Result.ERROR_FAIL;
                }
                break;
            }

            WriteWithLimit(_outStream, _bufferPos);

            int i = 0;
            while (_bufferPos < endPos) {
                _buffer[i++] = _buffer[_bufferPos++];
            }
            _bufferPos = i;
        }

        // return Result.OK;
    }

    void WriteWithLimit(OutputStream outStream, int size) throws IOException {
        if (_outSizeIsDefined) {
            long remSize = _outSize - _nowPos64;
            if (size > remSize) {
                size = (int) remSize;
            }
        }

        outStream.write(_buffer, 0, size);

        _nowPos64 += size;
    }

    byte[] _buffer;

    static final int kBufferSize = 1 << 17;

    public FilterCoder() {
        _buffer = new byte[kBufferSize];
    }


    public void setOutStream(OutputStream outStream) {
        _bufferPos = 0;
        _outStream = outStream;
        _nowPos64 = 0;
        _outSizeIsDefined = false;
        filter.init();
    }

    public void flush() throws IOException {
        if (_bufferPos != 0) {
            int endPos = filter.filter(_buffer, _bufferPos);
            if (endPos > _bufferPos) {
                for (; _bufferPos < endPos; _bufferPos++) {
                    _buffer[_bufferPos] = 0;
                }
                if (filter.filter(_buffer, endPos) != endPos) {
                    throw new IOException("FilterCoder - flush() : ERROR_FAIL");
                }
            }
            _outStream.write(_buffer, 0, _bufferPos);
            _bufferPos = 0;
        }
        _outStream.flush();
    }

    public void close() throws IOException {
        if (_outStream != null) _outStream.close();
        _outStream = null;
    }

}
