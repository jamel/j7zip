package org.jamel.j7zip;

import java.io.InputStream;
import java.io.OutputStream;

import org.jamel.j7zip.common.ObjectVector;

public interface ICompressCoder2 {

    public Result code(
            ObjectVector<InputStream> inStreams,
            int numInStreams,
            ObjectVector<OutputStream> outStreams,
            int numOutStreams) throws java.io.IOException;

    public void close() throws java.io.IOException;
}
