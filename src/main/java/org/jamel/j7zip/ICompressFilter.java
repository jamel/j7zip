package org.jamel.j7zip;

public interface ICompressFilter {

    void init();

    /**
     * if (outSize <= size): filter have converted outSize bytes
     * if (outSize > size): filter have not converted anything.
     * and it needs at least outSize bytes to convert one block
     * (it's for crypto block algorithms).
     *
     * @return outSize (UInt32)
     */
    int filter(byte[] data, int size);
}
