package org.jamel.j7zip;

public interface ICompressSetInStream {

    public void setInStream(java.io.InputStream inStream);

    public void releaseInStream() throws java.io.IOException;
}

