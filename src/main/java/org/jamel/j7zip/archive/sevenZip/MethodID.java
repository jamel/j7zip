package org.jamel.j7zip.archive.sevenZip;

class MethodID {

    static public final MethodID k_LZMA = new MethodID(0x3, 0x1, 0x1);
    static public final MethodID k_BCJ_X86 = new MethodID(0x3, 0x3, 0x1, 0x3);
    static public final MethodID k_BCJ = new MethodID(0x3, 0x3, 0x1, 0x3);
    static public final MethodID k_BCJ2 = new MethodID(0x3, 0x3, 0x1, 0x1B);
    static public final MethodID k_Deflate64 = new MethodID(0x4, 0x1, 0x9);
    static public final MethodID k_Copy = new MethodID(0x0);

    static final int kMethodIDSize = 15;
    byte[] ID;
    byte IDSize;

    public MethodID() {
        ID = new byte[kMethodIDSize];
        IDSize = 0;
    }

    public MethodID(int a) {
        int size = 1;
        ID = new byte[size];
        IDSize = (byte) size;
        ID[0] = (byte) a;
    }

    public MethodID(int a, int b, int c) {
        int size = 3;
        ID = new byte[size];
        IDSize = (byte) size;
        ID[0] = (byte) a;
        ID[1] = (byte) b;
        ID[2] = (byte) c;
    }

    public MethodID(int a, int b, int c, int d) {
        int size = 4;
        ID = new byte[size];
        IDSize = (byte) size;
        ID[0] = (byte) a;
        ID[1] = (byte) b;
        ID[2] = (byte) c;
        ID[3] = (byte) d;
    }

    public boolean equals(MethodID anObject) {
        if (IDSize != anObject.IDSize) return false;

        for (int i = 0; i < IDSize; i++) {
            if (ID[i] != anObject.ID[i]) return false;
        }

        return true;
    }
}
