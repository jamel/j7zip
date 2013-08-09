package org.jamel.j7zip.archive.sevenZip;

import java.io.IOException;

import org.jamel.j7zip.common.ObjectVector;
import org.jamel.j7zip.archive.IArchiveExtractCallback;
import org.jamel.j7zip.archive.IInArchive;
import org.jamel.j7zip.archive.SevenZipEntry;
import org.jamel.j7zip.IInStream;
import org.jamel.j7zip.Result;

public class Handler implements IInArchive {

    private final ArchiveDatabaseEx database;
    private IInStream inStream;


    public Handler() {
        database = new ArchiveDatabaseEx();
    }

    public Result open(IInStream stream) throws IOException {
        return open(stream, kMaxCheckStartPosition);
    }

    public Result open(IInStream stream, long maxCheckStartPosition) throws IOException {
        close();

        InArchive archive = new InArchive();
        Result ret = archive.open(stream, maxCheckStartPosition);
        if (ret != Result.OK) return ret;

        ret = archive.readDatabase(database); // getTextPassword
        if (ret != Result.OK) return ret;

        database.fill();
        inStream = stream;
        return Result.OK;
    }

    public Result extract(int[] indices, IArchiveExtractCallback extractCallbackSpec) throws IOException {
        int numItems = (indices == null ? database.files.size() : indices.length);
        if (numItems == 0) {
            return Result.OK;
        }

        ObjectVector<ExtractFolderInfo> extractFolderInfoVector = new ObjectVector<>();
        for (int ii = 0; ii < numItems; ii++) {
            int ref2Index = (indices == null ? ii : indices[ii]);

            ArchiveDatabaseEx database = this.database;

            int folderIndex = database.FileIndexToFolderIndexMap.get(ref2Index);
            if (folderIndex == InArchive.kNumNoIndex) {
                extractFolderInfoVector.add(new ExtractFolderInfo(ref2Index, folderIndex));
                continue;
            }

            if (extractFolderInfoVector.isEmpty() || folderIndex != extractFolderInfoVector.last().FolderIndex) {
                extractFolderInfoVector.add(new ExtractFolderInfo(InArchive.kNumNoIndex, folderIndex));
                Folder folderInfo = database.folders.get(folderIndex);
                extractFolderInfoVector.last().UnPackSize = folderInfo.getUnPackSize();
            }

            ExtractFolderInfo efi = extractFolderInfoVector.last();
            int startIndex = database.FolderStartFileIndex.get(folderIndex);
            for (int index = efi.ExtractStatuses.size(); index <= ref2Index - startIndex; index++) {
                efi.ExtractStatuses.add(index == ref2Index - startIndex);
            }
        }

        Decoder decoder = new Decoder();
        long currentImportantTotalUnPacked = 0;
        long totalFolderUnPacked;

        for (int i = 0; i < extractFolderInfoVector.size(); i++, currentImportantTotalUnPacked += totalFolderUnPacked) {
            ExtractFolderInfo efi = extractFolderInfoVector.get(i);
            totalFolderUnPacked = efi.UnPackSize;

            FolderOutStream folderOutStream = new FolderOutStream();
            ArchiveDatabaseEx database = this.database;

            int startIndex;
            if (efi.FileIndex != InArchive.kNumNoIndex) {
                startIndex = efi.FileIndex;
            } else {
                startIndex = database.FolderStartFileIndex.get(efi.FolderIndex);
            }

            folderOutStream.init(this, database, 0, startIndex, efi.ExtractStatuses, extractCallbackSpec);
            if (efi.FileIndex != InArchive.kNumNoIndex) {
                continue;
            }

            int folderIndex = efi.FolderIndex;
            Folder folderInfo = database.folders.get(folderIndex);

            int packStreamIndex = database.FolderStartPackStreamIndex.get(folderIndex); // CNum
            long folderStartPackPos = database.getFolderStreamPos(folderIndex, 0);

            try {
                Result result = decoder.decode(inStream, folderStartPackPos, database.packSizes, packStreamIndex,
                        folderInfo, folderOutStream);

                if (result == Result.FALSE) {
                    folderOutStream.flushCorrupted(OperationResult.DATA_ERROR);
                    continue;
                }

                if (result == Result.ERROR_NOT_IMPLEMENTED) {
                    folderOutStream.flushCorrupted(OperationResult.UNSUPPORTED_METHOD);
                    continue;
                }

                if (result != Result.OK) {
                    return result;
                }

                if (folderOutStream.wasWritingFinished() != Result.OK) {
                    folderOutStream.flushCorrupted(OperationResult.DATA_ERROR);
                }
            } catch (Exception e) {
                System.out.println("IOException : " + e);
                e.printStackTrace();
                folderOutStream.flushCorrupted(OperationResult.UNSUPPORTED_METHOD);
            }
        }

        return Result.OK;
    }

    public void close() throws IOException {
        if (inStream != null) inStream.close();
        inStream = null;
        database.clear();
    }

    public int size() {
        return database.files.size();
    }

    long getPackSize(int index2) {
        long packSize = 0;
        int folderIndex = database.FileIndexToFolderIndexMap.get(index2);
        if (folderIndex != InArchive.kNumNoIndex) {
            if (database.FolderStartFileIndex.get(folderIndex) == index2) {
                packSize = database.getFolderFullPackSize(folderIndex);
            }
        }
        return packSize;
    }

    public SevenZipEntry getEntry(int index) {
        FileItem item = database.files.get(index);
        long crc = (item.IsFileCRCDefined ? item.FileCRC & 0xFFFFFFFFL : -1);
        long position = (item.IsStartPosDefined ? item.StartPos : -1);

        return new SevenZipEntry(item.name, getPackSize(index), item.UnPackSize, crc, item.LastWriteTime,
                position, item.IsDirectory, item.Attributes);
    }

}
