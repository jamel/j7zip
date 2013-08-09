package org.jamel.j7zip.common;

import java.util.ArrayList;

public class ObjectVector<E> extends ArrayList<E> {

    public ObjectVector() {
        super();
    }

    public void reserve(int s) {
        ensureCapacity(s);
    }

    public E last() {
        return get(size() - 1);
    }

    public E first() {
        return get(0);
    }

    public E popLast() {
        return remove(size() - 1);
    }
}
