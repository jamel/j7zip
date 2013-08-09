package org.jamel.j7zip.common;

public class LongVector {
    private static final int CAPACITY_INCR = 10;

    private long[] data = new long[CAPACITY_INCR];
    int elt = 0;

    public LongVector() {
    }

    public int size() {
        return elt;
    }

    private void ensureCapacity(int minCapacity) {
        int oldCapacity = data.length;
        if (minCapacity > oldCapacity) {
            long[] oldData = data;
            int newCapacity = oldCapacity + CAPACITY_INCR;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            data = new long[newCapacity];
            System.arraycopy(oldData, 0, data, 0, elt);
        }
    }

    public long get(int index) {
        if (index >= elt) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        return data[index];
    }

    public void Reserve(int s) {
        ensureCapacity(s);
    }

    public void add(long b) {
        ensureCapacity(elt + 1);
        data[elt++] = b;
    }

    public void clear() {
        elt = 0;
    }

    public boolean isEmpty() {
        return elt == 0;
    }

    public long Back() {
        if (elt < 1) {
            throw new ArrayIndexOutOfBoundsException(0);
        }

        return data[elt - 1];
    }

    public void DeleteBack() {
        remove(elt - 1);
    }

    public long remove(int index) {
        if (index >= elt) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        long oldValue = data[index];

        int numMoved = elt - index - 1;
        if (numMoved > 0) {
            System.arraycopy(data, index + 1, data, index, numMoved);
        }

        return oldValue;
    }

}
