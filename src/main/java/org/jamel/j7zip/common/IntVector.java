package org.jamel.j7zip.common;

public class IntVector {
    private static final int CAPACITY_INCR = 10;

    private int[] data = new int[CAPACITY_INCR];
    int elt = 0;

    public IntVector() {
    }

    public int size() {
        return elt;
    }

    private void ensureCapacity(int minCapacity) {
        int oldCapacity = data.length;
        if (minCapacity > oldCapacity) {
            int[] oldData = data;
            int newCapacity = oldCapacity + CAPACITY_INCR;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            data = new int[newCapacity];
            System.arraycopy(oldData, 0, data, 0, elt);
        }
    }

    public int get(int index) {
        if (index >= elt) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        return data[index];
    }

    public void reserve(int s) {
        ensureCapacity(s);
    }

    public void add(int b) {
        ensureCapacity(elt + 1);
        data[elt++] = b;
    }

    public void clear() {
        elt = 0;
    }

    public boolean isEmpty() {
        return elt == 0;
    }
}
