package org.jamel.j7zip.archive.sevenZip;

import org.jamel.j7zip.common.IntVector;
import org.jamel.j7zip.common.LongVector;

public class ArchiveDatabaseEx extends ArchiveDatabase {
    final InArchiveInfo ArchiveInfo = new InArchiveInfo();
    final LongVector PackStreamStartPositions = new LongVector();
    final IntVector FolderStartPackStreamIndex = new IntVector();

    final IntVector FolderStartFileIndex = new IntVector();
    final IntVector FileIndexToFolderIndexMap = new IntVector();

    void clear() {
        super.clear();
        ArchiveInfo.Clear();
        PackStreamStartPositions.clear();
        FolderStartPackStreamIndex.clear();
        FolderStartFileIndex.clear();
        FileIndexToFolderIndexMap.clear();
    }

    void fillFolderStartPackStream() {
        FolderStartPackStreamIndex.clear();
        FolderStartPackStreamIndex.reserve(folders.size());

        int startPos = 0;
        for (Folder folder : folders) {
            FolderStartPackStreamIndex.add(startPos);
            startPos += folder.packStreams.size();
        }
    }

    void fillStartPos() {
        PackStreamStartPositions.clear();
        PackStreamStartPositions.Reserve(packSizes.size());
        long startPos = 0;
        for (int i = 0; i < packSizes.size(); i++) {
            PackStreamStartPositions.add(startPos);
            startPos += packSizes.get(i);
        }
    }

    public void fill() throws java.io.IOException {
        fillFolderStartPackStream();
        fillStartPos();
        fillFolderStartFileIndex();
    }

    public long getFolderFullPackSize(int folderIndex) {
        int packStreamIndex = FolderStartPackStreamIndex.get(folderIndex);
        Folder folder = folders.get(folderIndex);
        long size = 0;
        for (int i = 0; i < folder.packStreams.size(); i++) {
            size += packSizes.get(packStreamIndex + i);
        }
        return size;
    }


    void fillFolderStartFileIndex() throws java.io.IOException {
        FolderStartFileIndex.clear();
        FolderStartFileIndex.reserve(folders.size());
        FileIndexToFolderIndexMap.clear();
        FileIndexToFolderIndexMap.reserve(files.size());

        int folderIndex = 0;
        int indexInFolder = 0;
        for (int i = 0; i < files.size(); i++) {
            FileItem file = files.get(i);
            boolean emptyStream = !file.HasStream;
            if (emptyStream && indexInFolder == 0) {
                FileIndexToFolderIndexMap.add(InArchive.kNumNoIndex);
                continue;
            }
            if (indexInFolder == 0) {
                // v3.13 incorrectly worked with empty folders
                // v4.07: Loop for skipping empty folders
                for (; ; ) {
                    if (folderIndex >= folders.size()) {
                        throw new java.io.IOException(
                                "Incorrect Header"); // CInArchiveException(CInArchiveException::kIncorrectHeader);
                    }
                    FolderStartFileIndex.add(i); // check it
                    if (numUnPackStreamsVector.get(folderIndex) != 0) {
                        break;
                    }
                    folderIndex++;
                }
            }
            FileIndexToFolderIndexMap.add(folderIndex);
            if (emptyStream) {
                continue;
            }
            indexInFolder++;
            if (indexInFolder >= numUnPackStreamsVector.get(folderIndex)) {
                folderIndex++;
                indexInFolder = 0;
            }
        }
    }

    public long getFolderStreamPos(int folderIndex, int indexInFolder) {
        return ArchiveInfo.DataStartPosition +
                PackStreamStartPositions.get(FolderStartPackStreamIndex.get(folderIndex) +
                        indexInFolder);
    }
}
