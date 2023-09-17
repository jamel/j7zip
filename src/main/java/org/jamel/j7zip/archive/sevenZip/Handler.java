package org.jamel.j7zip.archive.sevenZip;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jamel.j7zip.common.ObjectVector;
import org.jamel.j7zip.archive.IArchiveExtractCallback;
import org.jamel.j7zip.archive.IInArchive;
import org.jamel.j7zip.archive.SevenZipEntry;
import org.jamel.j7zip.IInStream;
import org.jamel.j7zip.Result;

public class Handler implements IInArchive {

    private final ArchiveDatabaseEx database;
    private Supplier<IInStream> streamSupplier;
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


    public Result open( Supplier<IInStream> stream )
    {
        streamSupplier = stream;
        try ( var srcStream = streamSupplier.get() )
        {
            return open( srcStream, kMaxCheckStartPosition );
        }
        catch ( IOException IO ) { IO.printStackTrace(); }
        return Result.ERROR_FAIL;
    }


    public Result extract( Supplier<IArchiveExtractCallback> extractCallbackSpec )
    {
        var exec = Executors.newFixedThreadPool( 4 );

        for ( ExtractFolderInfo efi : extractFolderInfo() )
        {
            if ( efi.UnPackSize > 0 ) exec.execute
            (
                () -> extract( efi, extractCallbackSpec.get() )
            );
        }

        try { exec.shutdown();  exec.awaitTermination( 30, TimeUnit.SECONDS ); }
        catch ( InterruptedException IE ) { IE.printStackTrace(); }

        return Result.OK;
    }


    private ObjectVector<ExtractFolderInfo> extractFolderInfo()
    {
        var extractFolderInfoVector = new ObjectVector<ExtractFolderInfo>();

        for ( int ref2Index = 0; ref2Index < database.files.size(); ref2Index++ )
        {
            int folderIndex = database.FileIndexToFolderIndexMap.get( ref2Index );

            if ( folderIndex == InArchive.kNumNoIndex )
            {
                extractFolderInfoVector.add( new ExtractFolderInfo( ref2Index, folderIndex ) );
                continue;
            }

            if ( extractFolderInfoVector.isEmpty() || folderIndex != extractFolderInfoVector.last().FolderIndex ) try
            {
                var efi = new ExtractFolderInfo( InArchive.kNumNoIndex, folderIndex );
                efi.UnPackSize = database.folders.get( folderIndex ).getUnPackSize();
                extractFolderInfoVector.add( efi );
            }
            catch ( IOException IO ) { IO.printStackTrace(); }

            var efi = extractFolderInfoVector.last();
            int targetIndex = ref2Index - database.FolderStartFileIndex.get( folderIndex );

            for ( int index = efi.ExtractStatuses.size(); index <= targetIndex; index++ )
            {
                efi.ExtractStatuses.add( index == targetIndex );
            }
        }

        // Sort the folders in descending order of size
        Collections.sort( extractFolderInfoVector, (a,b) -> -1 * Long.compare( a.UnPackSize, b.UnPackSize ) );

        return extractFolderInfoVector;
    }


    private void extract( ExtractFolderInfo efi, IArchiveExtractCallback extractCallbackSpec )
    {
        try ( var srcStream = streamSupplier.get() )
        {
            var folderOutStream = new FolderOutStream();
            var startIndex = database.FolderStartFileIndex.get( efi.FolderIndex );
            folderOutStream.init( this, database, 0, startIndex, efi.ExtractStatuses, extractCallbackSpec );

            var packStreamIndex = database.FolderStartPackStreamIndex.get( efi.FolderIndex );
            var folderStartPackPos = database.getFolderStreamPos( efi.FolderIndex, 0 );
            var folderInfo = database.folders.get( efi.FolderIndex );

            var result = new Decoder().decode
            (
                srcStream, folderStartPackPos, database.packSizes,
                packStreamIndex, folderInfo, folderOutStream
            );

            switch ( result )
            {
                case ERROR_NOT_IMPLEMENTED : folderOutStream.flushCorrupted( OperationResult.UNSUPPORTED_METHOD ); break;
                case FALSE : folderOutStream.flushCorrupted( OperationResult.DATA_ERROR ); break;
                case OK : if ( folderOutStream.wasWritingFinished() != Result.OK )
                {
                    folderOutStream.flushCorrupted( OperationResult.DATA_ERROR );
                }
            }
        }
        catch ( Exception EX )
        {
            EX.printStackTrace();
        }
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
