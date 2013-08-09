package org.jamel.j7zip.common;

public class ByteBuffer {
    int _capacity;
    byte[] _items;

    public ByteBuffer() {
        _capacity = 0;
        _items = null;
    }

    public byte[] data() {
        return _items;
    }

    public int GetCapacity() {
        return _capacity;
    }

    public void SetCapacity(int newCapacity) {
        if (newCapacity == _capacity) {
            return;
        }

        byte[] newBuffer;
        if (newCapacity > 0) {
            newBuffer = new byte[newCapacity];
            if (_capacity > 0) {
                int len = _capacity;
                if (newCapacity < len) len = newCapacity;

                System.arraycopy(_items, 0, newBuffer, 0, len);
            }
        } else {
            newBuffer = null;
        }

        _items = newBuffer;
        _capacity = newCapacity;
    }
}
