package org.jamel.j7zip;

import java.io.IOException;
import java.io.InputStream;

public abstract class IInStream extends InputStream {

    public abstract long seekFromBegin(long offset) throws IOException;

    public abstract long seekFromCurrent(long offset) throws IOException;
}

