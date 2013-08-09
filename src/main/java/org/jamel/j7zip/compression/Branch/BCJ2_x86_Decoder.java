
package org.jamel.j7zip.compression.Branch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jamel.j7zip.common.ObjectVector;
import org.jamel.j7zip.common.InBuffer;
import org.jamel.j7zip.compression.LZ.OutWindow;
import org.jamel.j7zip.compression.RangeCoder.BitDecoder;
import org.jamel.j7zip.compression.RangeCoder.Decoder;
import org.jamel.j7zip.ICompressCoder2;
import org.jamel.j7zip.Result;

public class BCJ2_x86_Decoder implements ICompressCoder2 {

    public static final int kNumMoveBits = 5;

    InBuffer _mainInStream = new InBuffer();
    InBuffer _callStream = new InBuffer();
    InBuffer _jumpStream = new InBuffer();

    BitDecoder _statusE8Decoder[] = new BitDecoder[256];
    BitDecoder _statusE9Decoder = new BitDecoder(kNumMoveBits);
    BitDecoder _statusJccDecoder = new BitDecoder(kNumMoveBits);

    OutWindow _outStream = new OutWindow();
    Decoder _rangeDecoder = new Decoder();


    Result codeReal(
            ObjectVector<java.io.InputStream> inStreams,
            int numInStreams,
            ObjectVector<java.io.OutputStream> outStreams,
            int numOutStreams) throws java.io.IOException
    {

        if (numInStreams != 4 || numOutStreams != 1) {
            return Result.ERROR_INVALID_ARGS;
        }

        _mainInStream.Create(1 << 16);
        _callStream.Create(1 << 20);
        _jumpStream.Create(1 << 16);
        _rangeDecoder.Create(1 << 20);
        _outStream.Create(1 << 16);

        _mainInStream.SetStream(inStreams.get(0));
        _callStream.SetStream(inStreams.get(1));
        _jumpStream.SetStream(inStreams.get(2));
        _rangeDecoder.setStream(inStreams.get(3));
        _outStream.SetStream(outStreams.get(0));

        _mainInStream.Init();
        _callStream.Init();
        _jumpStream.Init();
        _rangeDecoder.Init();
        _outStream.Init();

        for (int i = 0; i < 256; i++) {
            _statusE8Decoder[i] = new BitDecoder(kNumMoveBits);
            _statusE8Decoder[i].Init();
        }
        _statusE9Decoder.Init();
        _statusJccDecoder.Init();

        int prevByte = 0;
        int processedBytes = 0;
        for (; ; ) {
            if (processedBytes > (1 << 20)) {
                processedBytes = 0;
            }

            processedBytes++;
            int b = _mainInStream.read();
            if (b == -1) {
                flush();
                return Result.OK;
            }
            _outStream.WriteByte(b);
            if ((b != 0xE8) && (b != 0xE9) && (!((prevByte == 0x0F) && ((b & 0xF0) == 0x80)))) {
                prevByte = b;
                continue;
            }

            boolean status;
            if (b == 0xE8) {
                status = (_statusE8Decoder[prevByte].decode(_rangeDecoder) == 1);
            } else if (b == 0xE9) {
                status = (_statusE9Decoder.decode(_rangeDecoder) == 1);
            } else {
                status = (_statusJccDecoder.decode(_rangeDecoder) == 1);
            }

            if (status) {
                int src;
                if (b == 0xE8) {
                    int b0 = _callStream.read();
                    src = b0 << 24;

                    b0 = _callStream.read();
                    src |= b0 << 16;

                    b0 = _callStream.read();
                    src |= b0 << 8;

                    b0 = _callStream.read();
                    if (b0 == -1) return Result.FALSE;
                    src |= b0;

                } else {
                    int b0 = _jumpStream.read();
                    src = b0 << 24;

                    b0 = _jumpStream.read();
                    src |= b0 << 16;

                    b0 = _jumpStream.read();
                    src |= b0 << 8;

                    b0 = _jumpStream.read();
                    if (b0 == -1) return Result.FALSE;
                    src |= b0;

                }
                int dest = src - ((int) _outStream.GetProcessedSize() + 4);
                _outStream.WriteByte(dest);
                _outStream.WriteByte((dest >> 8));
                _outStream.WriteByte((dest >> 16));
                _outStream.WriteByte((dest >> 24));
                prevByte = dest >> 24 & 0xFF;
                processedBytes += 4;
            } else {
                prevByte = b;
            }
        }
    }

    public void flush() throws java.io.IOException {
        _outStream.flush();
    }

    public Result code(
            ObjectVector<InputStream> inStreams,
            int numInStreams,
            ObjectVector<OutputStream> outStreams,
            int numOutStreams) throws IOException
    {
        try {
            return codeReal(inStreams, numInStreams, outStreams, numOutStreams);
        } finally {
            releaseStreams();
        }
    }

    void releaseStreams() throws java.io.IOException {
        _mainInStream.ReleaseStream();
        _callStream.ReleaseStream();
        _jumpStream.ReleaseStream();
        _rangeDecoder.releaseStream();
        _outStream.ReleaseStream();
    }

    public void close() throws java.io.IOException {
        releaseStreams();
    }

}
