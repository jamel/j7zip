package org.jamel.j7zip;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.jamel.j7zip.archive.IArchiveExtractCallback;
import org.jamel.j7zip.archive.IInArchive;
import org.jamel.j7zip.archive.SevenZipEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ArchiveExtractCallback implements IArchiveExtractCallback {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveExtractCallback.class);

    private int numErrors;
    private final File outDir;


    public ArchiveExtractCallback(File outDir) {
        this.outDir = outDir;
    }

    public ArchiveExtractCallback() {
        outDir = null;
    }

    public int getNumErrors() {
        return numErrors;
    }

    @Override
    public IInArchive.AskMode getAskMode() {
        return IInArchive.AskMode.EXTRACT;
    }

    public void setOperationResult(IInArchive.OperationResult operationResult) {
        if (operationResult != IInArchive.OperationResult.OK) {
            numErrors++;

            switch (operationResult) {
                case UNSUPPORTED_METHOD:
                    logger.error("Unsupported Method");
                    break;
                case CRC_ERROR:
                    logger.error("CRC Failed");
                    break;
                case DATA_ERROR:
                    logger.error("Data Error");
                    break;
                default:
                    logger.error("Unknown Error");
                    break;
            }
        }
    }

    public OutputStream getStream(SevenZipEntry item) throws IOException {
        File file = new File(outDir, item.getName());

        if (item.isDirectory()) {
            if (file.exists() && file.isDirectory()) return null;
            if (file.mkdirs()) {
                return null;
            } else {
                throw new IOException("can't create directory " + file);
            }
        }

        long pos = item.getPosition();
        if (pos == -1 && file.exists()) file.delete();

        RafOutputStream outputStream = new RafOutputStream(file, "rw");
        if (pos != -1) outputStream.seek(pos);

        return outputStream;
    }
}
