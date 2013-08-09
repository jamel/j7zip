package org.jamel.j7zip.archive;

import java.io.IOException;
import java.io.OutputStream;

public interface IArchiveExtractCallback {

    // getStream OUT: OK - OK, FALSE - skip this file
    OutputStream getStream(SevenZipEntry item) throws IOException;

    void setOperationResult(IInArchive.OperationResult resultEOperationResult);

    int getNumErrors();

    IInArchive.AskMode getAskMode();
}
