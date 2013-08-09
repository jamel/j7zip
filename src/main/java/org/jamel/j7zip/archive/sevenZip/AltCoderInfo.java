package org.jamel.j7zip.archive.sevenZip;

import org.jamel.j7zip.common.ByteBuffer;

class AltCoderInfo {
    public final MethodID MethodID;
    public final ByteBuffer Properties;

    public AltCoderInfo() {
        MethodID = new MethodID();
        Properties = new ByteBuffer();
    }
}
