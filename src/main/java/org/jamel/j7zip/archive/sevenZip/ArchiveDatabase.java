package org.jamel.j7zip.archive.sevenZip;

import org.jamel.j7zip.common.BoolVector;
import org.jamel.j7zip.common.IntVector;
import org.jamel.j7zip.common.LongVector;
import org.jamel.j7zip.common.ObjectVector;

class ArchiveDatabase {
    public final LongVector packSizes = new LongVector();
    public final BoolVector packCRCsDefined = new BoolVector();
    public final IntVector packCRCs = new IntVector();
    public final ObjectVector<Folder> folders = new ObjectVector<>();
    public final IntVector numUnPackStreamsVector = new IntVector();
    public final ObjectVector<FileItem> files = new ObjectVector<>();

    void clear() {
        packSizes.clear();
        packCRCsDefined.clear();
        packCRCs.clear();
        folders.clear();
        numUnPackStreamsVector.clear();
        files.clear();
    }
}
