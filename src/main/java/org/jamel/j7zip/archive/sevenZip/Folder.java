package org.jamel.j7zip.archive.sevenZip;

import java.io.IOException;

import org.jamel.j7zip.common.IntVector;
import org.jamel.j7zip.common.LongVector;
import org.jamel.j7zip.common.ObjectVector;
import org.jamel.j7zip.archive.common.BindPair;

class Folder {
    public ObjectVector<CoderInfo> coders = new ObjectVector<>();
    ObjectVector<BindPair> bindPairs = new ObjectVector<>();
    IntVector packStreams = new IntVector();
    LongVector unPackSizes = new LongVector();
    int unPackCRC;
    boolean unPackCRCDefined;


    long getUnPackSize() throws IOException {
        if (unPackSizes.isEmpty()) return 0;

        for (int i = unPackSizes.size() - 1; i >= 0; i--) {
            if (findBindPairForOutStream(i) < 0) {
                return unPackSizes.get(i);
            }
        }

        throw new IOException("1"); // throw 1  // TBD
    }

    int findBindPairForInStream(int inStreamIndex) {
        for (int i = 0; i < bindPairs.size(); i++) {
            if (bindPairs.get(i).inIndex == inStreamIndex) {
                return i;
            }
        }
        return -1;
    }

    int findBindPairForOutStream(int outStreamIndex) {
        for (int i = 0; i < bindPairs.size(); i++) {
            if (bindPairs.get(i).outIndex == outStreamIndex) {
                return i;
            }
        }
        return -1;
    }

    int findPackStreamArrayIndex(int inStreamIndex) {
        for (int i = 0; i < packStreams.size(); i++) {
            if (packStreams.get(i) == inStreamIndex) {
                return i;
            }
        }
        return -1;
    }

    int getNumOutStreams() {
        int result = 0;
        for (CoderInfo Coder : coders) result += Coder.getOutStreamsCount();
        return result;
    }
}
