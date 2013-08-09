package org.jamel.j7zip.archive.common;

import org.jamel.j7zip.common.IntVector;
import org.jamel.j7zip.common.ObjectVector;

public class BindInfo {

    public ObjectVector<CoderStreamsInfo> coders = new ObjectVector<>();
    public ObjectVector<BindPair> bindPairs = new ObjectVector<>();
    public IntVector inStreams = new IntVector();
    public IntVector outStreams = new IntVector();


    public void clear() {
        coders.clear();
        bindPairs.clear();
        inStreams.clear();
        outStreams.clear();
    }

    public int findBinderForInStream(int inStream) {
        for (int i = 0; i < bindPairs.size(); i++) {
            if (bindPairs.get(i).inIndex == inStream) {
                return i;
            }
        }
        return -1;
    }

    public int findBinderForOutStream(int outStream) {
        for (int i = 0; i < bindPairs.size(); i++) {
            if (bindPairs.get(i).outIndex == outStream) {
                return i;
            }
        }
        return -1;
    }

    public int getCoderInStreamIndex(int coderIndex) {
        int streamIndex = 0;
        for (int i = 0; i < coderIndex; i++) {
            streamIndex += coders.get(i).getInStreamsCount();
        }
        return streamIndex;
    }

    public int GetCoderOutStreamIndex(int coderIndex) {
        int streamIndex = 0;
        for (int i = 0; i < coderIndex; i++) {
            streamIndex += coders.get(i).getOutStreamsCount();
        }
        return streamIndex;
    }

    public int findInStream(int streamIndex) {
        for (int coderIndex = 0; coderIndex < coders.size(); coderIndex++) {
            int curSize = coders.get(coderIndex).getInStreamsCount();
            if (streamIndex < curSize) {
                return coderIndex;
            }
            streamIndex -= curSize;
        }
        throw new UnknownError("1");
    }

    public int findOutStream(int streamIndex) {
        for (int coderIndex = 0; coderIndex < coders.size(); coderIndex++) {
            int curSize = coders.get(coderIndex).getOutStreamsCount();
            if (streamIndex < curSize) {
                return coderIndex;
            }
            streamIndex -= curSize;
        }
        throw new UnknownError("1");
    }

}

