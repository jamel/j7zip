
package org.jamel.j7zip.compression.Copy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jamel.j7zip.ICompressCoder;

public class Decoder implements ICompressCoder {

    static final int kBufferSize = 1 << 17;

    public void code(InputStream inStream, OutputStream outStream, long outSize) throws IOException {
        byte[] _buffer = new byte[kBufferSize];
        long totalSize = 0;

        while (true) {
            int realProcessedSize;
            int size = kBufferSize;

            if (outSize != -1) {
                if (size > (outSize - totalSize)) {
                    size = (int) (outSize - totalSize);
                }
            }

            realProcessedSize = inStream.read(_buffer, 0, size);
            if (realProcessedSize == -1) break;

            outStream.write(_buffer, 0, realProcessedSize);
            totalSize += realProcessedSize;
        }
    }
}
