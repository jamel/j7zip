package org.jamel.j7zip.archive.sevenZip;

public class FileItem {

    public long CreationTime;
    public long LastWriteTime;
    public long LastAccessTime;

    public long UnPackSize;
    public long StartPos;
    public int Attributes;
    public int FileCRC;

    public boolean IsDirectory;
    public boolean IsAnti;
    public boolean IsFileCRCDefined;
    public boolean AreAttributesDefined;
    public boolean HasStream;
    public boolean IsStartPosDefined;
    public String name;

    public FileItem() {
        HasStream = true;
        IsDirectory = false;
        IsAnti = false;
        IsFileCRCDefined = false;
        AreAttributesDefined = false;
        CreationTime = 0;
        LastWriteTime = 0;
        LastAccessTime = 0;
        IsStartPosDefined = false;
    }
}
