package org.jamel.j7zip.archive.sevenZip;

import org.jamel.j7zip.common.ObjectVector;

class CoderInfo {

    private int inStreamsCount;
    private int outStreamsCount;

    private ObjectVector<AltCoderInfo> altCoders = new ObjectVector<>();


    public CoderInfo() {
    }

    boolean isSimpleCoder() {
        return (inStreamsCount == 1) && (outStreamsCount == 1);
    }

    int getInStreamsCount() {
        return inStreamsCount;
    }

    void setInStreamsCount(int inStreamsCount) {
        this.inStreamsCount = inStreamsCount;
    }

    int getOutStreamsCount() {
        return outStreamsCount;
    }

    void setOutStreamsCount(int outStreamsCount) {
        this.outStreamsCount = outStreamsCount;
    }

    public ObjectVector<AltCoderInfo> getAltCoders() {
        return altCoders;
    }
}
