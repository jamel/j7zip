package org.jamel.j7zip.archive.common;

public class STCoderInfo extends CoderInfo {
    boolean IsMain;

    public STCoderInfo(int numInStreams, int numOutStreams, boolean isMain) {
        super(numInStreams, numOutStreams);
        this.IsMain = isMain;
    }
}
