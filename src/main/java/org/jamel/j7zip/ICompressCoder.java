package org.jamel.j7zip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ICompressCoder {

    void code(InputStream inStream, OutputStream outStream, long outSize) throws IOException;
}
