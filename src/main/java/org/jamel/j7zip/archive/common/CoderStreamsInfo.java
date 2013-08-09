package org.jamel.j7zip.archive.common;

public class CoderStreamsInfo {

    private final int inStreamsCount;
    private final int outStreamsCount;


    public CoderStreamsInfo(int inStreamsCount, int outStreamsCount) {
        this.inStreamsCount = inStreamsCount;
        this.outStreamsCount = outStreamsCount;
    }

    public int getInStreamsCount() {
        return inStreamsCount;
    }

    public int getOutStreamsCount() {
        return outStreamsCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoderStreamsInfo that = (CoderStreamsInfo) o;
        return inStreamsCount == that.inStreamsCount && outStreamsCount == that.outStreamsCount;
    }

    @Override
    public int hashCode() {
        int result = inStreamsCount;
        result = 31 * result + outStreamsCount;
        return result;
    }
}
