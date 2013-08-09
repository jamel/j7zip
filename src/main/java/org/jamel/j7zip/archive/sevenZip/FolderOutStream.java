package org.jamel.j7zip.archive.sevenZip;

import java.io.IOException;
import java.io.OutputStream;

import org.jamel.j7zip.common.BoolVector;
import org.jamel.j7zip.archive.common.OutStreamWithCRC;
import org.jamel.j7zip.archive.IArchiveExtractCallback;
import org.jamel.j7zip.archive.IInArchive;
import org.jamel.j7zip.archive.SevenZipEntry;
import org.jamel.j7zip.Result;

class FolderOutStream extends OutputStream {
    OutStreamWithCRC _outStreamWithHashSpec;
    OutputStream _outStreamWithHash;

    IInArchive archive;
    ArchiveDatabaseEx _archiveDatabase;
    BoolVector _extractStatuses;
    int _startIndex;
    int _ref2Offset;
    IArchiveExtractCallback _extractCallback;
    int _currentIndex;
    boolean _fileIsOpen;

    long _filePos;

    public FolderOutStream() {
        _outStreamWithHashSpec = new OutStreamWithCRC();
        _outStreamWithHash = _outStreamWithHashSpec;
    }

    public void init(
            IInArchive archive, ArchiveDatabaseEx archiveDatabase,
            int ref2Offset,
            int startIndex,
            BoolVector extractStatuses,
            IArchiveExtractCallback extractCallback) throws IOException
    {
        this.archive = archive;
        _archiveDatabase = archiveDatabase;
        _ref2Offset = ref2Offset;
        _startIndex = startIndex;

        _extractStatuses = extractStatuses;
        _extractCallback = extractCallback;

        _currentIndex = 0;
        _fileIsOpen = false;
        writeEmptyFiles();
    }

    void openFile() throws IOException {
        IInArchive.AskMode askMode;
        if (_extractStatuses.get(_currentIndex)) {
            askMode = _extractCallback.getAskMode();
        } else {
            askMode = IInArchive.AskMode.SKIP;
        }

        int index = _startIndex + _currentIndex;
        SevenZipEntry item = archive.getEntry(_ref2Offset + index);
        OutputStream realOutStream = _extractCallback.getStream(item);
        _outStreamWithHashSpec.SetStream(realOutStream);
        _outStreamWithHashSpec.Init();
        if (askMode == IInArchive.AskMode.EXTRACT && (realOutStream == null)) {
            FileItem fileInfo = _archiveDatabase.files.get(index);
            if (!fileInfo.IsAnti && !fileInfo.IsDirectory) {
                askMode = IInArchive.AskMode.SKIP;
            }
        }

        System.out.println(askMode + " " + item.getName());
    }

    void writeEmptyFiles() throws IOException {
        for (; _currentIndex < _extractStatuses.size(); _currentIndex++) {
            int index = _startIndex + _currentIndex;
            FileItem fileInfo = _archiveDatabase.files.get(index);
            if (!fileInfo.IsAnti && !fileInfo.IsDirectory && fileInfo.UnPackSize != 0) return;

            openFile();
            _extractCallback.setOperationResult(IInArchive.OperationResult.OK);
            _outStreamWithHashSpec.releaseStream();
        }
    }

    public void write(int b) throws IOException {
        throw new IOException("FolderOutStream - write() not implemented");
    }

    public void write(byte[] data, int off, int size) throws IOException {
        int realProcessedSize = 0;
        while (_currentIndex < _extractStatuses.size()) {
            if (_fileIsOpen) {
                int index = _startIndex + _currentIndex;
                FileItem fileInfo = _archiveDatabase.files.get(index);
                long fileSize = fileInfo.UnPackSize;

                long numBytesToWrite2 = (int) (fileSize - _filePos);
                int tmp = size - realProcessedSize;
                if (tmp < numBytesToWrite2) numBytesToWrite2 = tmp;

                int numBytesToWrite = (int) numBytesToWrite2;

                int processedSizeLocal;
                // int res = _outStreamWithHash.Write((const Byte *)data + realProcessedSize,numBytesToWrite, &processedSizeLocal));
                // if (res != Result.OK) throw new IOException("_outStreamWithHash.Write : " + res); // return res;
                processedSizeLocal = numBytesToWrite;
                _outStreamWithHash.write(data, realProcessedSize + off, numBytesToWrite);

                _filePos += processedSizeLocal;
                realProcessedSize += processedSizeLocal;

                if (_filePos == fileSize) {
                    boolean digestsAreEqual =
                            !fileInfo.IsFileCRCDefined || (fileInfo.FileCRC == _outStreamWithHashSpec.GetCRC());

                    IInArchive.OperationResult operationResult = digestsAreEqual
                            ? IInArchive.OperationResult.OK
                            : IInArchive.OperationResult.CRC_ERROR;

                    _extractCallback.setOperationResult(operationResult);
                    _outStreamWithHashSpec.releaseStream();
                    _fileIsOpen = false;
                    _currentIndex++;
                }
                if (realProcessedSize == size) {
                    writeEmptyFiles();
                    return;
                }
            } else {
                openFile();
                _fileIsOpen = true;
                _filePos = 0;
            }
        }
    }

    public void flushCorrupted(IInArchive.OperationResult resultEOperationResult) throws IOException {
        while (_currentIndex < _extractStatuses.size()) {
            if (_fileIsOpen) {
                _extractCallback.setOperationResult(resultEOperationResult);
                _outStreamWithHashSpec.releaseStream();
                _fileIsOpen = false;
                _currentIndex++;
            } else {
                openFile();
                _fileIsOpen = true;
            }
        }
    }

    public Result wasWritingFinished() {
        int val = _extractStatuses.size();
        if (_currentIndex == val) {
            return Result.OK;
        }
        return Result.ERROR_FAIL;
    }

}
