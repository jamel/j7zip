package org.jamel.j7zip.archive.sevenZip;

import java.io.IOException;

import org.jamel.j7zip.common.BoolVector;
import org.jamel.j7zip.common.ByteBuffer;
import org.jamel.j7zip.common.CRC;
import org.jamel.j7zip.common.IntVector;
import org.jamel.j7zip.common.LongVector;
import org.jamel.j7zip.common.ObjectVector;
import org.jamel.j7zip.archive.common.BindPair;
import org.jamel.j7zip.common.SequentialOutStreamImp2;
import org.jamel.j7zip.common.StreamUtils;
import org.jamel.j7zip.IInStream;
import org.jamel.j7zip.Result;

class InArchive extends Header {

    static public final int kNumMax = 0x7FFFFFFF;
    static public final int kNumNoIndex = 0xFFFFFFFF;

    private final ObjectVector<InByte2> inBytes;

    private IInStream stream;
    private InByte2 lastInByte;

    private long arhiveBeginStreamPosition;
    private long position;


    public InArchive() {
        inBytes = new ObjectVector<>();
    }

    public void addByteStream(byte[] buffer, int size) {
        lastInByte = new InByte2(buffer, size);
        inBytes.add(lastInByte);
    }

    void deleteByteStream() {
        inBytes.popLast();
        if (!inBytes.isEmpty()) {
            lastInByte = inBytes.last();
        }
    }

    static boolean testSignatureCandidate(byte[] testBytes, int off) {
        for (int i = 0; i < kSignatureSize; i++) {
            if (testBytes[i + off] != kSignature[i]) {
                return false;
            }
        }
        return true;
    }

    int readDirect(IInStream stream, byte[] data, int off, int size) throws IOException {
        int realProcessedSize = StreamUtils.ReadStream(stream, data, off, size);
        if (realProcessedSize != -1) position += realProcessedSize;
        return realProcessedSize;
    }

    int readDirect(byte[] data, int size) throws IOException {
        return readDirect(stream, data, 0, size);
    }

    int safeReadDirectUInt32() throws IOException {
        int val = 0;
        byte[] b = new byte[4];

        int realProcessedSize = readDirect(b, 4);
        if (realProcessedSize != 4) {
            throw new IOException("Unexpected End Of Archive");
        }

        for (int i = 0; i < 4; i++) {
            val |= ((b[i] & 0xff) << (8 * i));
        }
        return val;
    }

    int readUInt32() throws IOException {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int b = readByte();
            value |= ((b) << (8 * i));
        }
        return value;
    }

    long readUInt64() throws IOException {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            int b = readByte();
            value |= (((long) b) << (8 * i));
        }
        return value;
    }

    Result readBytes(byte data[], int size) {
        if (!lastInByte.readBytes(data, size)) {
            return Result.ERROR_FAIL;
        }
        return Result.OK;
    }

    int readByte() throws IOException {
        return lastInByte.readByte();
    }

    long safeReadDirectUInt64() throws IOException {
        long val = 0;
        byte[] b = new byte[8];

        int realProcessedSize = readDirect(b, 8);
        if (realProcessedSize != 8) {
            throw new IOException(
                    "Unexpected End Of Archive"); // throw CInArchiveException(CInArchiveException::kUnexpectedEndOfArchive);
        }

        for (int i = 0; i < 8; i++) {
            val |= ((long) (b[i] & 0xFF) << (8 * i));
        }
        return val;
    }

    char readWideCharLE() throws IOException {
        int b1 = lastInByte.readByte();
        int b2 = lastInByte.readByte();
        return (char) (((char) (b2) << 8) + b1);
    }

    long readNumber() throws IOException {
        int firstByte = readByte();

        int mask = 0x80;
        long value = 0;
        for (int i = 0; i < 8; i++) {
            if ((firstByte & mask) == 0) {
                long highPart = firstByte & (mask - 1);
                value += (highPart << (i * 8));
                return value;
            }
            int b = readByte();
            if (b < 0) {
                throw new IOException("readNumber - Can't read stream");
            }

            value |= (((long) b) << (8 * i));
            mask >>= 1;
        }
        return value;
    }

    int readNum() throws IOException {
        long value64 = readNumber();
        if (value64 > InArchive.kNumMax) {
            throw new IOException("readNum - value > CNum.kNumMax");
        }

        return (int) value64;
    }

    long readID() throws IOException {
        return readNumber();
    }

    Result findAndReadSignature(IInStream stream, long searchHeaderSizeLimit) throws IOException {
        position = arhiveBeginStreamPosition;

        stream.seekFromBegin(arhiveBeginStreamPosition);

        byte[] signature = new byte[kSignatureSize];

        int processedSize = readDirect(stream, signature, 0, kSignatureSize);
        if (processedSize != kSignatureSize) {
            return Result.FALSE;
        }

        if (testSignatureCandidate(signature, 0)) {
            return Result.OK;
        }

        // SFX support
        ByteBuffer byteBuffer = new ByteBuffer();
        final int kBufferSize = (1 << 16);
        byteBuffer.SetCapacity(kBufferSize);
        byte[] buffer = byteBuffer.data();
        int numPrevBytes = kSignatureSize - 1;

        System.arraycopy(signature, 1, buffer, 0, numPrevBytes);

        long curTestPos = arhiveBeginStreamPosition + 1;
        while (true) {
            if (searchHeaderSizeLimit != -1) {
                if (curTestPos - arhiveBeginStreamPosition > searchHeaderSizeLimit) {
                    break;
                }
            }
            int numReadBytes = kBufferSize - numPrevBytes;

            processedSize = readDirect(stream, buffer, numPrevBytes, numReadBytes);
            if (processedSize == -1) return Result.FALSE;

            int numBytesInBuffer = numPrevBytes + processedSize;
            if (numBytesInBuffer < kSignatureSize) {
                break;
            }
            int numTests = numBytesInBuffer - kSignatureSize + 1;
            for (int pos = 0; pos < numTests; pos++, curTestPos++) {
                if (testSignatureCandidate(buffer, pos)) {
                    arhiveBeginStreamPosition = curTestPos;
                    position = curTestPos + kSignatureSize;
                    stream.seekFromBegin(position);
                    return Result.OK;
                }
            }
            numPrevBytes = numBytesInBuffer - numTests;
            System.arraycopy(buffer, numTests, buffer, 0, numPrevBytes);
        }

        return Result.FALSE;
    }

    void skeepData(long size) throws IOException {
        for (long i = 0; i < size; i++) {
            readByte();
        }
    }

    void skeepData() throws IOException {
        long size = readNumber();
        skeepData(size);
    }


    void readArchiveProperties() throws IOException {
        while (true) {
            long type = readID();
            if (type == NID.kEnd) break;
            skeepData();
        }
    }

    Result getNextFolderItem(Folder folder) throws IOException {
        int numCoders = readNum();

        folder.coders.clear();
        folder.coders.reserve(numCoders);
        int numInStreams = 0;
        int numOutStreams = 0;
        int i;
        for (i = 0; i < numCoders; i++) {
            folder.coders.add(new CoderInfo());
            CoderInfo coder = folder.coders.last();

            while (true) {
                coder.getAltCoders().add(new AltCoderInfo());
                AltCoderInfo altCoder = coder.getAltCoders().last();
                int mainByte = readByte();
                altCoder.MethodID.IDSize = (byte) (mainByte & 0xF);

                Result ret = readBytes(altCoder.MethodID.ID, altCoder.MethodID.IDSize);
                if (ret != Result.OK) return ret;

                if ((mainByte & 0x10) != 0) {
                    coder.setInStreamsCount(readNum());
                    coder.setOutStreamsCount(readNum());
                } else {
                    coder.setInStreamsCount(1);
                    coder.setOutStreamsCount(1);
                }
                if ((mainByte & 0x20) != 0) {
                    int propertiesSize = readNum();
                    altCoder.Properties.SetCapacity(propertiesSize);
                    readBytes(altCoder.Properties.data(), propertiesSize);
                }

                if ((mainByte & 0x80) == 0) break;
            }
            numInStreams += coder.getInStreamsCount();
            numOutStreams += coder.getOutStreamsCount();
        }

        int numBindPairs = numOutStreams - 1;
        folder.bindPairs.clear();
        folder.bindPairs.reserve(numBindPairs);
        for (i = 0; i < numBindPairs; i++) {
            folder.bindPairs.add(new BindPair(readNum(), readNum()));
        }

        int numPackedStreams = numInStreams - numBindPairs;
        folder.packStreams.reserve(numPackedStreams);
        if (numPackedStreams == 1) {
            for (int j = 0; j < numInStreams; j++) {
                if (folder.findBindPairForInStream(j) < 0) {
                    folder.packStreams.add(j);
                    break;
                }
            }
        } else {
            for (i = 0; i < numPackedStreams; i++) {
                int packStreamInfo = readNum();
                folder.packStreams.add(packStreamInfo);
            }
        }

        return Result.OK;
    }

    Result waitAttribute(long attribute) throws IOException {
        while (true) {
            long type = readID();
            if (type == attribute) return Result.OK;
            if (type == NID.kEnd) return Result.FALSE;
            skeepData();
        }
    }

    Result open(IInStream stream, long searchHeaderSizeLimit) throws IOException {
        close();

        arhiveBeginStreamPosition = stream.seekFromCurrent(0);
        position = arhiveBeginStreamPosition;

        Result ret = findAndReadSignature(stream, searchHeaderSizeLimit);
        if (ret != Result.OK) return ret;

        this.stream = stream;

        return Result.OK;
    }

    void close() throws IOException {
        if (stream != null) stream.close();
        stream = null;
    }

    Result readStreamsInfo(
            ObjectVector<ByteBuffer> dataVector,
            long[] dataOffset,
            LongVector packSizes,
            BoolVector packCRCsDefined,
            IntVector packCRCs,
            ObjectVector<Folder> folders,
            IntVector numUnPackStreamsInFolders,
            LongVector unPackSizes,
            BoolVector digestsDefined,
            IntVector digests) throws IOException
    {

        while (true) {
            long type = readID();
            switch ((int) type) {
                case NID.kEnd:
                    return Result.OK;
                case NID.kPackInfo: {
                    Result result = readPackInfo(dataOffset, packSizes,
                            packCRCsDefined, packCRCs);
                    if (result != Result.OK) return result;
                    break;
                }
                case NID.kUnPackInfo: {
                    Result result = ReadUnPackInfo(dataVector, folders);
                    if (result != Result.OK) return result;
                    break;
                }
                case NID.kSubStreamsInfo: {
                    readSubStreamsInfo(folders, numUnPackStreamsInFolders, unPackSizes, digestsDefined, digests);
                    break;
                }
            }
        }
    }

    void readFileNames(ObjectVector<FileItem> files) throws IOException {
        StringBuilder name = new StringBuilder(64);
        for (FileItem file : files) {
            while (true) {
                char c = readWideCharLE();
                if (c == '\0') break;
                name.append(c);
            }
            file.name = name.toString();
            name.setLength(0); // reuse buffer
        }
    }

    void readBoolVector(int numItems, BoolVector v) throws IOException {
        v.clear();
        v.reserve(numItems);
        int b = 0;
        int mask = 0;
        for (int i = 0; i < numItems; i++) {
            if (mask == 0) {
                b = readByte();
                mask = 0x80;
            }
            v.add((b & mask) != 0);
            mask >>= 1;
        }
    }

    void readBoolVector2(int numItems, BoolVector v) throws IOException { // CBoolVector
        int allAreDefined = readByte();
        if (allAreDefined == 0) {
            readBoolVector(numItems, v);
        } else {
            v.clear();
            v.reserve(numItems);
            for (int i = 0; i < numItems; i++) {
                v.add(true);
            }
        }
    }

    void readHashDigests(int numItems, BoolVector digestsDefined, IntVector digests) throws IOException {
        readBoolVector2(numItems, digestsDefined);
        digests.clear();
        digests.reserve(numItems);

        for (int i = 0; i < numItems; i++) {
            int crc = 0;
            if (digestsDefined.get(i)) {
                crc = readUInt32();
            }
            digests.add(crc);
        }
    }

    Result readPackInfo(long[] dataOffset, LongVector packSizes, BoolVector packCRCsDefined, IntVector packCRCs)
            throws IOException
    {
        dataOffset[0] = readNumber();
        int numPackStreams = readNum();

        Result ret = waitAttribute(NID.kSize);
        if (ret != Result.OK) return ret;

        packSizes.clear();
        packSizes.Reserve(numPackStreams);
        for (int i = 0; i < numPackStreams; i++) // CNum i
        {
            long size = readNumber();
            packSizes.add(size);
        }

        while (true) {
            long type = readID();
            if (type == NID.kEnd) break;
            if (type == NID.kCRC) {
                readHashDigests(numPackStreams, packCRCsDefined, packCRCs);
                continue;
            }
            skeepData();
        }

        if (packCRCsDefined.isEmpty()) {
            packCRCsDefined.reserve(numPackStreams);
            packCRCsDefined.clear();
            packCRCs.reserve(numPackStreams);
            packCRCs.clear();
            for (int i = 0; i < numPackStreams; i++) {
                packCRCsDefined.add(false);
                packCRCs.add(0);
            }
        }
        return Result.OK;
    }

    Result ReadUnPackInfo(ObjectVector<ByteBuffer> dataVector, ObjectVector<Folder> folders) throws IOException {
        Result ret = waitAttribute(NID.kFolder);
        if (ret != Result.OK) return ret;

        int numFolders = readNum();

        {
            StreamSwitch streamSwitch = new StreamSwitch();
            streamSwitch.set(this, dataVector);

            folders.clear();
            folders.reserve(numFolders);
            for (int i = 0; i < numFolders; i++) {
                folders.add(new Folder());
                ret = getNextFolderItem(folders.last());
                if (ret != Result.OK) {
                    streamSwitch.close();
                    return ret;
                }
            }
            streamSwitch.close();
        }

        ret = waitAttribute(NID.kCodersUnPackSize);
        if (ret != Result.OK) return ret;

        for (int i = 0; i < numFolders; i++) {
            Folder folder = folders.get(i);
            int numOutStreams = folder.getNumOutStreams();
            folder.unPackSizes.Reserve(numOutStreams);
            for (int j = 0; j < numOutStreams; j++) {
                long unPackSize = readNumber();
                folder.unPackSizes.add(unPackSize);
            }
        }

        while (true) {
            long type = readID();
            if (type == NID.kEnd) {
                return Result.OK;
            }
            if (type == NID.kCRC) {
                BoolVector crcsDefined = new BoolVector();
                IntVector crcs = new IntVector();
                readHashDigests(numFolders, crcsDefined, crcs);

                for (int i = 0; i < numFolders; i++) {
                    Folder folder = folders.get(i);
                    folder.unPackCRCDefined = crcsDefined.get(i);
                    folder.unPackCRC = crcs.get(i);
                }
                continue;
            }
            skeepData();
        }
    }

    void readSubStreamsInfo(ObjectVector<Folder> folders, IntVector numUnPackStreamsInFolders, LongVector unPackSizes,
            BoolVector digestsDefined, IntVector digests) throws IOException
    {
        numUnPackStreamsInFolders.clear();
        numUnPackStreamsInFolders.reserve(folders.size());
        long type;
        while (true) {
            type = readID();
            if (type == NID.kNumUnPackStream) {
                for (Folder folder : folders) {
                    numUnPackStreamsInFolders.add(readNum());
                }
                continue;
            }
            if (type == NID.kCRC || type == NID.kSize || type == NID.kEnd) break;
            skeepData();
        }

        if (numUnPackStreamsInFolders.isEmpty()) {
            for (Folder folder : folders) {
                numUnPackStreamsInFolders.add(1);
            }
        }

        for (int i = 0; i < numUnPackStreamsInFolders.size(); i++) {
            // v3.13 incorrectly worked with empty folders
            // v4.07: we check that folder is empty
            int numSubstreams = numUnPackStreamsInFolders.get(i);
            if (numSubstreams == 0) {
                continue;
            }
            long sum = 0;
            for (int j = 1; j < numSubstreams; j++) {
                long size;
                if (type == NID.kSize) {
                    size = readNumber();
                    unPackSizes.add(size);
                    sum += size;
                }
            }
            unPackSizes.add(folders.get(i).getUnPackSize() - sum);
        }
        if (type == NID.kSize) {
            type = readID();
        }

        int numDigests = 0;
        int numDigestsTotal = 0;
        for (int i = 0; i < folders.size(); i++) {
            int numSubstreams = numUnPackStreamsInFolders.get(i);
            if (numSubstreams != 1 || !folders.get(i).unPackCRCDefined) {
                numDigests += numSubstreams;
            }
            numDigestsTotal += numSubstreams;
        }

        while (true) {
            if (type == NID.kCRC) {
                BoolVector digestsDefined2 = new BoolVector();
                IntVector digests2 = new IntVector();
                readHashDigests(numDigests, digestsDefined2, digests2);

                int digestIndex = 0;
                for (int i = 0; i < folders.size(); i++) {
                    int numSubstreams = numUnPackStreamsInFolders.get(i);
                    Folder folder = folders.get(i);
                    if (numSubstreams == 1 && folder.unPackCRCDefined) {
                        digestsDefined.add(true);
                        digests.add(folder.unPackCRC);
                    } else {
                        for (int j = 0; j < numSubstreams; j++, digestIndex++) {
                            digestsDefined.add(digestsDefined2.get(digestIndex));
                            digests.add(digests2.get(digestIndex));
                        }
                    }
                }
            } else if (type == NID.kEnd) {
                if (digestsDefined.isEmpty()) {
                    digestsDefined.clear();
                    digests.clear();
                    for (int i = 0; i < numDigestsTotal; i++) {
                        digestsDefined.add(false);
                        digests.add(0);
                    }
                }
                return;
            } else {
                skeepData();
            }
            type = readID();
        }
    }

    static final long SECS_BETWEEN_EPOCHS = 11644473600L;
    static final long SECS_TO_100NS = 10000000L; /* 10^7 */

    static long fileTimeToLong(int dwHighDateTime, int dwLowDateTime) {
        // The FILETIME structure is a 64-bit value representing the number of 100-nanosecond intervals since January 1
        long tm = dwHighDateTime;
        tm <<= 32;
        tm |= (dwLowDateTime & 0xFFFFFFFFL);
        return (tm - (SECS_BETWEEN_EPOCHS * SECS_TO_100NS)) / (10000L); /* now convert to milliseconds */
    }

    void readTime(ObjectVector<ByteBuffer> dataVector, ObjectVector<FileItem> files, long type) throws IOException {
        BoolVector boolVector = new BoolVector();
        readBoolVector2(files.size(), boolVector);

        StreamSwitch streamSwitch = new StreamSwitch();
        streamSwitch.set(this, dataVector);

        for (int i = 0; i < files.size(); i++) {
            FileItem file = files.get(i);
            int low = 0;
            int high = 0;
            boolean defined = boolVector.get(i);
            if (defined) {
                low = readUInt32();
                high = readUInt32();
            }
            switch ((int) type) {
                case NID.kCreationTime:
                    if (defined) {
                        file.CreationTime = fileTimeToLong(high, low);
                    }
                    break;
                case NID.kLastWriteTime:
                    if (defined) {
                        file.LastWriteTime = fileTimeToLong(high, low);
                    }
                    break;
                case NID.kLastAccessTime:
                    if (defined) {
                        file.LastAccessTime = fileTimeToLong(high, low);
                    }
                    break;
            }
        }
        streamSwitch.close();
    }

    Result readAndDecodePackedStreams(long baseOffset, long[] dataOffset, ObjectVector<ByteBuffer> dataVector)
            throws IOException
    {
        LongVector packSizes = new LongVector();
        BoolVector packCRCsDefined = new BoolVector();
        IntVector packCRCs = new IntVector();

        ObjectVector<Folder> folders = new ObjectVector<>();

        IntVector numUnPackStreamsInFolders = new IntVector();
        LongVector unPackSizes = new LongVector();
        BoolVector digestsDefined = new BoolVector();
        IntVector digests = new IntVector();

        readStreamsInfo(null, dataOffset, packSizes, packCRCsDefined, packCRCs, folders, numUnPackStreamsInFolders,
                unPackSizes, digestsDefined, digests);

        int packIndex = 0;
        Decoder decoder = new Decoder();

        long dataStartPos = baseOffset + dataOffset[0];
        for (Folder folder : folders) {
            dataVector.add(new ByteBuffer());
            ByteBuffer data = dataVector.last();
            long unPackSize = folder.getUnPackSize();
            if (unPackSize > InArchive.kNumMax || unPackSize > 0xFFFFFFFFL) {
                return Result.ERROR_FAIL;
            }
            data.SetCapacity((int) unPackSize);

            SequentialOutStreamImp2 outStreamSpec = new SequentialOutStreamImp2(data.data(), (int) unPackSize);
            Result result = decoder.decode(stream, dataStartPos, packSizes, packIndex, folder, outStreamSpec);
            if (result != Result.OK) return result;

            if (folder.unPackCRCDefined && !CRC.VerifyDigest(folder.unPackCRC, data.data(), (int) unPackSize)) {
                throw new IOException("Incorrect Header");
            }

            for (int j = 0; j < folder.packStreams.size(); j++) {
                dataStartPos += packSizes.get(packIndex++);
            }
        }
        return Result.OK;
    }

    Result readDatabase(ArchiveDatabaseEx database) throws IOException {
        database.clear();
        database.ArchiveInfo.StartPosition = arhiveBeginStreamPosition;

        byte[] btmp = new byte[2];
        int realProcessedSize = readDirect(btmp, 2);
        if (realProcessedSize != 2) {
            throw new IOException("Unexpected End Of Archive");
        }

        database.ArchiveInfo.ArchiveVersion_Major = btmp[0];
        database.ArchiveInfo.ArchiveVersion_Minor = btmp[1];

        if (database.ArchiveInfo.ArchiveVersion_Major != kMajorVersion) {
            throw new IOException("Unsupported Version");
        }

        CRC crc = new CRC();
        int crcFromArchive = safeReadDirectUInt32();
        long nextHeaderOffset = safeReadDirectUInt64();
        long nextHeaderSize = safeReadDirectUInt64();
        int nextHeaderCRC = safeReadDirectUInt32();

        crc.UpdateUInt64(nextHeaderOffset);
        crc.UpdateUInt64(nextHeaderSize);
        crc.UpdateUInt32(nextHeaderCRC);

        database.ArchiveInfo.StartPositionAfterHeader = position;

        if (crc.getDigest() != crcFromArchive) {
            throw new IOException("Incorrect Header");
        }

        if (nextHeaderSize == 0) return Result.OK;
        if (nextHeaderSize >= 0xFFFFFFFFL) return Result.ERROR_FAIL;

        position = stream.seekFromCurrent(nextHeaderOffset);

        ByteBuffer buffer2 = new ByteBuffer();
        buffer2.SetCapacity((int) nextHeaderSize);

        realProcessedSize = readDirect(buffer2.data(), (int) nextHeaderSize);
        if (realProcessedSize != (int) nextHeaderSize) {
            throw new IOException(
                    "Unexpected End Of Archive");
        }

        if (!CRC.VerifyDigest(nextHeaderCRC, buffer2.data(), (int) nextHeaderSize)) {
            throw new IOException("Incorrect Header");
        }

        StreamSwitch streamSwitch = new StreamSwitch();
        streamSwitch.set(this, buffer2);

        ObjectVector<ByteBuffer> dataVector = new ObjectVector<>();

        for (; ; ) {
            long type = readID();
            if (type == NID.kHeader) {
                break;
            }
            if (type != NID.kEncodedHeader) {
                throw new IOException("Incorrect Header");
            }

            long[] ltmp = new long[1];
            ltmp[0] = database.ArchiveInfo.DataStartPosition2;
            Result result = readAndDecodePackedStreams(database.ArchiveInfo.StartPositionAfterHeader, ltmp, dataVector);
            if (result != Result.OK) return result;

            database.ArchiveInfo.DataStartPosition2 = ltmp[0];

            if (dataVector.size() == 0) {
                return Result.OK;
            }
            if (dataVector.size() > 1) {
                throw new IOException(
                        "Incorrect Header"); // CInArchiveException(CInArchiveException::kIncorrectHeader);
            }
            streamSwitch.remove();
            streamSwitch.set(this, dataVector.get(0)); // dataVector.first()
        }

        streamSwitch.close();
        return readHeader(database);
    }

    Result readHeader(ArchiveDatabaseEx database) throws IOException {
        long type = readID();

        if (type == NID.kArchiveProperties) {
            readArchiveProperties();
            type = readID();
        }

        ObjectVector<ByteBuffer> dataVector = new ObjectVector<>();

        if (type == NID.kAdditionalStreamsInfo) {
            long[] ltmp = new long[1];
            ltmp[0] = database.ArchiveInfo.DataStartPosition2;
            Result result = readAndDecodePackedStreams(database.ArchiveInfo.StartPositionAfterHeader, ltmp, dataVector);
            if (result != Result.OK) return result;

            database.ArchiveInfo.DataStartPosition2 = ltmp[0];

            database.ArchiveInfo.DataStartPosition2 += database.ArchiveInfo.StartPositionAfterHeader;
            type = readID();
        }

        LongVector unPackSizes = new LongVector();
        BoolVector digestsDefined = new BoolVector();
        IntVector digests = new IntVector();

        if (type == NID.kMainStreamsInfo) {
            long[] ltmp = new long[1];
            ltmp[0] = database.ArchiveInfo.DataStartPosition;
            Result result = readStreamsInfo(dataVector,
                    ltmp,
                    database.packSizes,
                    database.packCRCsDefined,
                    database.packCRCs,
                    database.folders,
                    database.numUnPackStreamsVector,
                    unPackSizes,
                    digestsDefined,
                    digests);
            if (result != Result.OK) return result;
            database.ArchiveInfo.DataStartPosition = ltmp[0];
            database.ArchiveInfo.DataStartPosition += database.ArchiveInfo.StartPositionAfterHeader;
            type = readID();
        } else {
            for (int i = 0; i < database.folders.size(); i++) {
                database.numUnPackStreamsVector.add(1);
                Folder folder = database.folders.get(i);
                unPackSizes.add(folder.getUnPackSize());
                digestsDefined.add(folder.unPackCRCDefined);
                digests.add(folder.unPackCRC);
            }
        }

        database.files.clear();

        if (type == NID.kEnd) {
            return Result.OK;
        }
        if (type != NID.kFilesInfo) {
            throw new IOException("Incorrect Header");
        }

        int numFiles = readNum();
        database.files.reserve(numFiles);
        for (int i = 0; i < numFiles; i++) {
            database.files.add(new FileItem());
        }

        database.ArchiveInfo.FileInfoPopIDs.add(NID.kSize);
        if (!database.packSizes.isEmpty()) {
            database.ArchiveInfo.FileInfoPopIDs.add(NID.kPackInfo);
        }
        if (numFiles > 0 && !digests.isEmpty()) {
            database.ArchiveInfo.FileInfoPopIDs.add(NID.kCRC);
        }

        BoolVector emptyStreamVector = new BoolVector();
        emptyStreamVector.reserve(numFiles);
        for (int i = 0; i < numFiles; i++) {
            emptyStreamVector.add(false);
        }
        BoolVector emptyFileVector = new BoolVector();
        BoolVector antiFileVector = new BoolVector();
        int numEmptyStreams = 0;

        while (true) {
            type = readID();
            if (type == NID.kEnd) {
                break;
            }
            long size = readNumber();

            database.ArchiveInfo.FileInfoPopIDs.add(type);
            switch ((int) type) {
                case NID.kName: {
                    StreamSwitch streamSwitch = new StreamSwitch();
                    streamSwitch.set(this, dataVector);
                    readFileNames(database.files);
                    streamSwitch.close();
                    break;
                }
                case NID.kWinAttributes: {
                    BoolVector boolVector = new BoolVector();
                    readBoolVector2(database.files.size(), boolVector);

                    StreamSwitch streamSwitch = new StreamSwitch();
                    streamSwitch.set(this, dataVector);

                    for (int i = 0; i < numFiles; i++) {
                        FileItem file = database.files.get(i);
                        file.AreAttributesDefined = boolVector.get(i);
                        if (file.AreAttributesDefined) {
                            file.Attributes = readUInt32();
                        }
                    }
                    streamSwitch.close();
                    break;
                }
                case NID.kStartPos: {
                    BoolVector boolVector = new BoolVector();
                    readBoolVector2(database.files.size(), boolVector);

                    StreamSwitch streamSwitch = new StreamSwitch();
                    streamSwitch.set(this, dataVector);

                    for (int i = 0; i < numFiles; i++) {
                        FileItem file = database.files.get(i);
                        file.IsStartPosDefined = boolVector.get(i);
                        if (file.IsStartPosDefined) {
                            file.StartPos = readUInt64();
                        }
                    }
                    streamSwitch.close();
                    break;
                }
                case NID.kEmptyStream: {
                    readBoolVector(numFiles, emptyStreamVector);

                    for (int i = 0; i < emptyStreamVector.size(); i++) {
                        if (emptyStreamVector.get(i)) {
                            numEmptyStreams++;
                        }
                    }
                    emptyFileVector.reserve(numEmptyStreams);
                    antiFileVector.reserve(numEmptyStreams);
                    for (int i = 0; i < numEmptyStreams; i++) {
                        emptyFileVector.add(false);
                        antiFileVector.add(false);
                    }
                    break;
                }
                case NID.kEmptyFile: {
                    readBoolVector(numEmptyStreams, emptyFileVector);
                    break;
                }
                case NID.kAnti: {
                    readBoolVector(numEmptyStreams, antiFileVector);
                    break;
                }
                case NID.kCreationTime:
                case NID.kLastWriteTime:
                case NID.kLastAccessTime: {
                    readTime(dataVector, database.files, type);
                    break;
                }
                default: {
                    database.ArchiveInfo.FileInfoPopIDs.DeleteBack();
                    skeepData(size);
                }
            }
        }

        int emptyFileIndex = 0;
        int sizeIndex = 0;
        for (int i = 0; i < numFiles; i++) {
            FileItem file = database.files.get(i);
            file.HasStream = !emptyStreamVector.get(i);
            if (file.HasStream) {
                file.IsDirectory = false;
                file.IsAnti = false;
                file.UnPackSize = unPackSizes.get(sizeIndex);
                file.FileCRC = digests.get(sizeIndex);
                file.IsFileCRCDefined = digestsDefined.get(sizeIndex);
                sizeIndex++;
            } else {
                file.IsDirectory = !emptyFileVector.get(emptyFileIndex);
                file.IsAnti = antiFileVector.get(emptyFileIndex);
                emptyFileIndex++;
                file.UnPackSize = 0;
                file.IsFileCRCDefined = false;
            }
        }

        return Result.OK;
    }
}
