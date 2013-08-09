package org.jamel.j7zip.compression.LZMA;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jamel.j7zip.compression.LZ.OutWindow;
import org.jamel.j7zip.compression.RangeCoder.BitTreeDecoder;
import org.jamel.j7zip.ICompressCoder;
import org.jamel.j7zip.ICompressSetInStream;
import org.jamel.j7zip.ICompressSetOutStreamSize;

import static org.jamel.j7zip.compression.RangeCoder.Decoder.InitBitModels;


public class Decoder extends InputStream implements ICompressCoder, ICompressSetInStream, ICompressSetOutStreamSize {
    class LenDecoder {
        short[] m_Choice = new short[2];
        BitTreeDecoder[] m_LowCoder = new BitTreeDecoder[Base.kNumPosStatesMax];
        BitTreeDecoder[] m_MidCoder = new BitTreeDecoder[Base.kNumPosStatesMax];
        BitTreeDecoder m_HighCoder = new BitTreeDecoder(Base.kNumHighLenBits);
        int m_NumPosStates = 0;

        public void Create(int numPosStates) {
            for (; m_NumPosStates < numPosStates; m_NumPosStates++) {
                m_LowCoder[m_NumPosStates] = new BitTreeDecoder(Base.kNumLowLenBits);
                m_MidCoder[m_NumPosStates] = new BitTreeDecoder(Base.kNumMidLenBits);
            }
        }

        public void Init() {
            InitBitModels(m_Choice);
            for (int posState = 0; posState < m_NumPosStates; posState++) {
                m_LowCoder[posState].Init();
                m_MidCoder[posState].Init();
            }
            m_HighCoder.Init();
        }

        public int Decode(org.jamel.j7zip.compression.RangeCoder.Decoder rangeDecoder,
                int posState) throws IOException
        {
            if (rangeDecoder.DecodeBit(m_Choice, 0) == 0) {
                return m_LowCoder[posState].Decode(rangeDecoder);
            }
            int symbol = Base.kNumLowLenSymbols;
            if (rangeDecoder.DecodeBit(m_Choice, 1) == 0) {
                symbol += m_MidCoder[posState].Decode(rangeDecoder);
            } else {
                symbol += Base.kNumMidLenSymbols + m_HighCoder.Decode(rangeDecoder);
            }
            return symbol;
        }
    }

    class LiteralDecoder {
        class Decoder2 {
            short[] m_Decoders = new short[0x300];

            public void Init() {
                InitBitModels(m_Decoders);
            }

            public byte DecodeNormal(org.jamel.j7zip.compression.RangeCoder.Decoder rangeDecoder) throws
                    IOException
            {
                int symbol = 1;
                do {
                    symbol = (symbol << 1) | rangeDecoder.DecodeBit(m_Decoders, symbol);
                }
                while (symbol < 0x100);
                return (byte) symbol;
            }

            public byte DecodeWithMatchByte(org.jamel.j7zip.compression.RangeCoder.Decoder rangeDecoder,
                    byte matchByte) throws IOException
            {
                int symbol = 1;
                do {
                    int matchBit = (matchByte >> 7) & 1;
                    matchByte <<= 1;
                    int bit = rangeDecoder.DecodeBit(m_Decoders, ((1 + matchBit) << 8) + symbol);
                    symbol = (symbol << 1) | bit;
                    if (matchBit != bit) {
                        while (symbol < 0x100) {
                            symbol = (symbol << 1) | rangeDecoder.DecodeBit(m_Decoders, symbol);
                        }
                        break;
                    }
                }
                while (symbol < 0x100);
                return (byte) symbol;
            }
        }

        Decoder2[] m_Coders;
        int m_NumPrevBits;
        int m_NumPosBits;
        int m_PosMask;

        public void create(int numPosBits, int numPrevBits) {
            if (m_Coders != null && m_NumPrevBits == numPrevBits && m_NumPosBits == numPosBits) {
                return;
            }
            m_NumPosBits = numPosBits;
            m_PosMask = (1 << numPosBits) - 1;
            m_NumPrevBits = numPrevBits;
            int numStates = 1 << (m_NumPrevBits + m_NumPosBits);
            m_Coders = new Decoder2[numStates];
            for (int i = 0; i < numStates; i++) {
                m_Coders[i] = new Decoder2();
            }
        }

        public void init() {
            int numStates = 1 << (m_NumPrevBits + m_NumPosBits);
            for (int i = 0; i < numStates; i++) {
                m_Coders[i].Init();
            }
        }

        Decoder2 GetDecoder(int pos, byte prevByte) {
            return m_Coders[((pos & m_PosMask) << m_NumPrevBits) + ((prevByte & 0xFF) >>> (8 - m_NumPrevBits))];
        }
    }

    OutWindow m_OutWindow = new OutWindow();
    org.jamel.j7zip.compression.RangeCoder.Decoder m_RangeDecoder =
            new org.jamel.j7zip.compression.RangeCoder.Decoder();

    short[] m_IsMatchDecoders = new short[Base.kNumStates << Base.kNumPosStatesBitsMax];
    short[] m_IsRepDecoders = new short[Base.kNumStates];
    short[] m_IsRepG0Decoders = new short[Base.kNumStates];
    short[] m_IsRepG1Decoders = new short[Base.kNumStates];
    short[] m_IsRepG2Decoders = new short[Base.kNumStates];
    short[] m_IsRep0LongDecoders = new short[Base.kNumStates << Base.kNumPosStatesBitsMax];

    BitTreeDecoder[] m_PosSlotDecoder = new BitTreeDecoder[Base.kNumLenToPosStates];
    short[] m_PosDecoders = new short[Base.kNumFullDistances - Base.kEndPosModelIndex];

    BitTreeDecoder m_PosAlignDecoder = new BitTreeDecoder(Base.kNumAlignBits);

    LenDecoder m_LenDecoder = new LenDecoder();
    LenDecoder m_RepLenDecoder = new LenDecoder();

    LiteralDecoder m_LiteralDecoder = new LiteralDecoder();

    int m_DictionarySize = -1;
    int m_DictionarySizeCheck = -1;

    int m_posStateMask;

    public Decoder() {
        for (int i = 0; i < Base.kNumLenToPosStates; i++) {
            m_PosSlotDecoder[i] = new BitTreeDecoder(Base.kNumPosSlotBits);
        }
    }

    void setDictionarySize(int dictionarySize) {
        if (m_DictionarySize != dictionarySize) {
            m_DictionarySize = dictionarySize;
            m_DictionarySizeCheck = Math.max(m_DictionarySize, 1);
            m_OutWindow.Create(Math.max(m_DictionarySizeCheck, (1 << 12)));
            m_RangeDecoder.Create(1 << 20);
        }
    }

    void setLcLpPb(int lc, int lp, int pb) {
        m_LiteralDecoder.create(lp, lc);
        int numPosStates = 1 << pb;
        m_LenDecoder.Create(numPosStates);
        m_RepLenDecoder.Create(numPosStates);
        m_posStateMask = numPosStates - 1;
    }


    public void releaseInStream() throws IOException {
        m_RangeDecoder.releaseStream();
    }

    public void setInStream(InputStream inStream) {
        m_RangeDecoder.setStream(inStream);
    }

    long _outSize = 0;
    boolean _outSizeDefined = false;
    int _remainLen; // -1 means end of stream. // -2 means need init
    static final int kLenIdFinished = -1;
    static final int kLenIdNeedInit = -2;
    int _rep0;
    int _rep1;
    int _rep2;
    int _rep3;
    int _state;

    public void setOutStreamSize(long outSize /* const UInt64 *outSize*/) {
        _outSizeDefined = (outSize != ICompressSetOutStreamSize.INVALID_OUTSIZE);
        if (_outSizeDefined) {
            _outSize = outSize;
        }
        _remainLen = kLenIdNeedInit;
        m_OutWindow.Init();
    }

    public int read() throws IOException {
        throw new IOException("LZMA Decoder - read() not implemented");
    }

    public int read(byte[] data, int off, int size) throws IOException {
        if (off != 0) {
            throw new IOException("LZMA Decoder - read(byte [] data, int off != 0, int size)) not implemented");
        }

        long startPos = m_OutWindow.GetProcessedSize();
        m_OutWindow.SetMemStream(data);
        codeSpec(size);

        flush();

        int ret = (int) (m_OutWindow.GetProcessedSize() - startPos);
        return ret == 0 ? -1 : ret;
    }

    void init() {
        m_OutWindow.Init(false);

        InitBitModels(m_IsMatchDecoders);
        InitBitModels(m_IsRep0LongDecoders);
        InitBitModels(m_IsRepDecoders);
        InitBitModels(m_IsRepG0Decoders);
        InitBitModels(m_IsRepG1Decoders);
        InitBitModels(m_IsRepG2Decoders);
        InitBitModels(m_PosDecoders);

        _rep0 = _rep1 = _rep2 = _rep3 = 0;
        _state = 0;

        m_LiteralDecoder.init();
        int i;
        for (i = 0; i < Base.kNumLenToPosStates; i++) {
            m_PosSlotDecoder[i].Init();
        }
        m_LenDecoder.Init();
        m_RepLenDecoder.Init();
        m_PosAlignDecoder.Init();
    }

    public void flush() throws IOException {
        m_OutWindow.flush();
    }

    void releaseStreams() throws IOException {
        m_OutWindow.ReleaseStream();
        releaseInStream();
    }

    public void codeReal(InputStream inStream, OutputStream outStream, long outSize) throws IOException {
        setInStream(inStream);
        m_OutWindow.SetStream(outStream);
        setOutStreamSize(outSize);

        while (true) {
            int curSize = 1 << 18;
            codeSpec(curSize);

            if (_remainLen == kLenIdFinished) break;

            if (_outSizeDefined) {
                if (m_OutWindow.GetProcessedSize() >= _outSize) {
                    break;
                }
            }
        }
        flush();
    }

    public void code(InputStream inStream, OutputStream outStream, long outSize) throws IOException {
        try {
            codeReal(inStream, outStream, outSize);
        } catch (IOException e) {
            e.printStackTrace();
            this.flush();
            this.releaseStreams();
            throw e;
        } finally {
            this.flush();
            this.releaseStreams();
        }
    }

    void codeSpec(int curSize) throws IOException {
        if (_outSizeDefined) {
            long rem = _outSize - m_OutWindow.GetProcessedSize();
            if (curSize > rem) {
                curSize = (int) rem;
            }
        }

        if (_remainLen == kLenIdFinished) return;
        if (_remainLen == kLenIdNeedInit) {
            m_RangeDecoder.Init();
            init();
            _remainLen = 0;
        }

        if (curSize == 0) return;

        int rep0 = _rep0;
        int rep1 = _rep1;
        int rep2 = _rep2;
        int rep3 = _rep3;
        int state = _state;
        byte prevByte;

        while (_remainLen > 0 && curSize > 0) {
            prevByte = m_OutWindow.GetByte(rep0);
            m_OutWindow.PutByte(prevByte);
            _remainLen--;
            curSize--;
        }
        long nowPos64 = m_OutWindow.GetProcessedSize();
        if (nowPos64 == 0) {
            prevByte = 0;
        } else {
            prevByte = m_OutWindow.GetByte(0);
        }

        while (curSize > 0) {
            if (m_RangeDecoder.bufferedStream.wasFinished()) return;

            int posState = (int) nowPos64 & m_posStateMask;
            if (m_RangeDecoder.DecodeBit(m_IsMatchDecoders, (state << Base.kNumPosStatesBitsMax) + posState) == 0) {
                LiteralDecoder.Decoder2 decoder2 = m_LiteralDecoder.GetDecoder((int) nowPos64, prevByte);
                if (!Base.StateIsCharState(state)) {
                    prevByte = decoder2.DecodeWithMatchByte(m_RangeDecoder, m_OutWindow.GetByte(rep0));
                } else {
                    prevByte = decoder2.DecodeNormal(m_RangeDecoder);
                }
                m_OutWindow.PutByte(prevByte);
                state = Base.StateUpdateChar(state);
                curSize--;
                nowPos64++;
            } else {
                int len;
                if (m_RangeDecoder.DecodeBit(m_IsRepDecoders, state) == 1) {
                    len = 0;
                    if (m_RangeDecoder.DecodeBit(m_IsRepG0Decoders, state) == 0) {
                        if (m_RangeDecoder
                                .DecodeBit(m_IsRep0LongDecoders, (state << Base.kNumPosStatesBitsMax) + posState) == 0)
                        {
                            state = Base.StateUpdateShortRep(state);
                            len = 1;
                        }
                    } else {
                        int distance;
                        if (m_RangeDecoder.DecodeBit(m_IsRepG1Decoders, state) == 0) {
                            distance = rep1;
                        } else {
                            if (m_RangeDecoder.DecodeBit(m_IsRepG2Decoders, state) == 0) {
                                distance = rep2;
                            } else {
                                distance = rep3;
                                rep3 = rep2;
                            }
                            rep2 = rep1;
                        }
                        rep1 = rep0;
                        rep0 = distance;
                    }
                    if (len == 0) {
                        len = m_RepLenDecoder.Decode(m_RangeDecoder, posState) + Base.kMatchMinLen;
                        state = Base.StateUpdateRep(state);
                    }
                } else {
                    rep3 = rep2;
                    rep2 = rep1;
                    rep1 = rep0;
                    len = Base.kMatchMinLen + m_LenDecoder.Decode(m_RangeDecoder, posState);
                    state = Base.StateUpdateMatch(state);
                    int posSlot = m_PosSlotDecoder[Base.GetLenToPosState(len)].Decode(m_RangeDecoder);
                    if (posSlot >= Base.kStartPosModelIndex) {
                        int numDirectBits = (posSlot >> 1) - 1;
                        rep0 = ((2 | (posSlot & 1)) << numDirectBits);
                        if (posSlot < Base.kEndPosModelIndex) {
                            rep0 += BitTreeDecoder.ReverseDecode(m_PosDecoders,
                                    rep0 - posSlot - 1, m_RangeDecoder, numDirectBits);
                        } else {
                            rep0 += (m_RangeDecoder.DecodeDirectBits(
                                    numDirectBits - Base.kNumAlignBits) << Base.kNumAlignBits);
                            rep0 += m_PosAlignDecoder.ReverseDecode(m_RangeDecoder);
                            if (rep0 < 0) {
                                if (rep0 == -1) {
                                    break;
                                }
                                throw new IOException("Read error");
                            }
                        }
                    } else {
                        rep0 = posSlot;
                    }
                }
                if (rep0 >= nowPos64 || rep0 >= m_DictionarySizeCheck) {
                    _remainLen = kLenIdFinished;
                    throw new IOException("Read error");
                }

                int locLen = len;
                if (len > curSize) {
                    locLen = curSize;
                }
                m_OutWindow.CopyBlock(rep0, locLen);
                prevByte = m_OutWindow.GetByte(0);
                curSize -= locLen;
                nowPos64 += locLen;
                len -= locLen;
                if (len != 0) {
                    _remainLen = len;
                    break;
                }
            }
        }

        if (m_RangeDecoder.bufferedStream.wasFinished()) {
            throw new IOException("Read error");
        }

        _rep0 = rep0;
        _rep1 = rep1;
        _rep2 = rep2;
        _rep3 = rep3;
        _state = state;
    }

    public boolean setDecoderProperties2(byte[] properties) {
        if (properties.length < 5) {
            return false;
        }
        int val = properties[0] & 0xFF;
        int lc = val % 9;
        int remainder = val / 9;
        int lp = remainder % 5;
        int pb = remainder / 5;
        int dictionarySize = 0;
        for (int i = 0; i < 4; i++) {
            dictionarySize += ((int) (properties[1 + i]) & 0xFF) << (i * 8);
        }

        if (lc > Base.kNumLitContextBitsMax || lp > 4 || pb > Base.kNumPosStatesBitsMax) {
            return false;
        } else {
            setLcLpPb(lc, lp, pb);
        }

        if (dictionarySize < 0) {
            return false;
        } else {
            setDictionarySize(dictionarySize);
        }

        return true;
    }
}
