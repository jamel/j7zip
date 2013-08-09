package org.jamel.j7zip;

import java.io.IOException;
import java.io.OutputStream;

import org.jamel.j7zip.archive.IArchiveExtractCallback;
import org.jamel.j7zip.archive.IInArchive;
import org.jamel.j7zip.archive.SevenZipEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ArchiveTestCallback implements IArchiveExtractCallback {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveTestCallback.class);

    private int numErrors;


    public int getNumErrors() {
        return numErrors;
    }

    @Override
    public IInArchive.AskMode getAskMode() {
        return IInArchive.AskMode.TEST;
    }

    public OutputStream getStream(SevenZipEntry item) throws IOException {
        String filePath = item.getName();
        logger.info("Testing {}", filePath);
        return null;
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
}
