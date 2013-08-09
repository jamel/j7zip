package org.jamel.j7zip.archive.sevenZip;

import org.jamel.j7zip.common.ByteBuffer;
import org.jamel.j7zip.common.ObjectVector;

class StreamSwitch {
    InArchive _archive;
    boolean _needRemove;

    public StreamSwitch() {
        _needRemove = false;
    }

    public void close() {
        remove();
    }

    void remove() {
        if (_needRemove) {
            _archive.deleteByteStream();
            _needRemove = false;
        }
    }

    void set(InArchive archive, ByteBuffer byteBuffer) {
        remove();
        _archive = archive;
        _archive.addByteStream(byteBuffer.data(), byteBuffer.GetCapacity());
        _needRemove = true;
    }

    void set(InArchive archive, ObjectVector<ByteBuffer> dataVector) throws java.io.IOException {
        remove();
        int external = archive.readByte();
        if (external != 0) {
            int dataIndex = archive.readNum();
            set(archive, dataVector.get(dataIndex));
        }
    }
}
