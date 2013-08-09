package org.jamel.j7zip.archive;

import java.io.IOException;

import org.jamel.j7zip.IInStream;
import org.jamel.j7zip.Result;

public interface IInArchive {
    public static enum AskMode {EXTRACT, TEST, SKIP}

    public static enum OperationResult {OK, UNSUPPORTED_METHOD, DATA_ERROR, CRC_ERROR}

    // Static-SFX (for Linux) can be big.
    public final long kMaxCheckStartPosition = 1 << 22;

    SevenZipEntry getEntry(int index);

    int size();

    void close() throws IOException;

    Result extract(int[] indices, IArchiveExtractCallback extractCallbackSpec) throws IOException;

    Result open(IInStream stream) throws IOException;

    Result open(IInStream stream, long maxCheckStartPosition) throws IOException;


}

