package org.jamel.j7zip.archive.common;

public class BindPair {

    public final int inIndex;
    public final int outIndex;


    public BindPair(int inIndex, int outIndex) {
        this.inIndex = inIndex;
        this.outIndex = outIndex;
    }

    public int getInIndex() {
        return inIndex;
    }

    public int getOutIndex() {
        return outIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BindPair bindPair = (BindPair) o;
        return inIndex == bindPair.inIndex && outIndex == bindPair.outIndex;
    }

    @Override
    public int hashCode() {
        int result = inIndex;
        result = 31 * result + outIndex;
        return result;
    }
}
