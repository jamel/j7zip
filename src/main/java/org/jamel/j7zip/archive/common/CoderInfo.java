package org.jamel.j7zip.archive.common;

import org.jamel.j7zip.common.LongVector;
import org.jamel.j7zip.ICompressCoder;
import org.jamel.j7zip.ICompressCoder2;

public class CoderInfo {
    ICompressCoder Coder;
    ICompressCoder2 Coder2;
    int NumInStreams;
    int NumOutStreams;

    LongVector InSizes = new LongVector();
    LongVector OutSizes = new LongVector();
    LongVector InSizePointers = new LongVector();
    LongVector OutSizePointers = new LongVector();


    public CoderInfo(int numInStreams, int numOutStreams) {
        NumInStreams = numInStreams;
        NumOutStreams = numOutStreams;
        InSizes.Reserve(NumInStreams);
        InSizePointers.Reserve(NumInStreams);
        OutSizePointers.Reserve(NumOutStreams);
        OutSizePointers.Reserve(NumOutStreams);
    }

    static public void setSizes(LongVector srcSizes, LongVector sizes, LongVector sizePointers, int numItems) {
        sizes.clear();
        sizePointers.clear();
        for (int i = 0; i < numItems; i++) {
            if (srcSizes == null || srcSizes.get(i) == -1) {
                sizes.add(0L);
                sizePointers.add(-1L);
            } else {
                sizes.add(srcSizes.get(i));
                sizePointers.add(sizes.Back());
            }
        }
    }

    public void setCoderInfo(LongVector inSizes, LongVector outSizes) {
        setSizes(inSizes, InSizes, InSizePointers, NumInStreams);
        setSizes(outSizes, OutSizes, OutSizePointers, NumOutStreams);
    }
}
