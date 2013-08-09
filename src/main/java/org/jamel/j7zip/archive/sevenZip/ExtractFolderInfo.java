package org.jamel.j7zip.archive.sevenZip;

import org.jamel.j7zip.common.BoolVector;

class ExtractFolderInfo {
    public int FileIndex;
    public int FolderIndex;
    public BoolVector ExtractStatuses = new BoolVector();
    public long UnPackSize;

    public ExtractFolderInfo(int fileIndex, int folderIndex) {
        FileIndex = fileIndex;
        FolderIndex = folderIndex;
        UnPackSize = 0;

        if (fileIndex != InArchive.kNumNoIndex) {
            ExtractStatuses.reserve(1);
            ExtractStatuses.add(true);
        }
    }
}

