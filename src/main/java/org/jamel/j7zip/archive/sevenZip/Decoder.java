package org.jamel.j7zip.archive.sevenZip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jamel.j7zip.common.ByteBuffer;
import org.jamel.j7zip.common.LimitedSequentialInStream;
import org.jamel.j7zip.common.LockedInStream;
import org.jamel.j7zip.common.LockedSequentialInStreamImp;
import org.jamel.j7zip.common.LongVector;
import org.jamel.j7zip.common.ObjectVector;
import org.jamel.j7zip.archive.common.BindPair;
import org.jamel.j7zip.archive.common.CoderMixer2;
import org.jamel.j7zip.archive.common.CoderMixer2ST;
import org.jamel.j7zip.archive.common.CoderStreamsInfo;
import org.jamel.j7zip.archive.common.FilterCoder;
import org.jamel.j7zip.compression.Branch.BCJ2_x86_Decoder;
import org.jamel.j7zip.compression.Branch.BCJ_x86_Decoder;
import org.jamel.j7zip.ICompressCoder;
import org.jamel.j7zip.ICompressCoder2;
import org.jamel.j7zip.ICompressFilter;
import org.jamel.j7zip.IInStream;
import org.jamel.j7zip.Result;

class Decoder {

    boolean _bindInfoExPrevIsDefined;
    BindInfoEx _bindInfoExPrev;

    CoderMixer2ST _mixerCoderSTSpec;
    CoderMixer2 _mixerCoderCommon;

    ICompressCoder2 _mixerCoder;
    ObjectVector<Object> _decoders;

    public Decoder() {
        _bindInfoExPrevIsDefined = false;
        _bindInfoExPrev = new BindInfoEx();

        _mixerCoder = null;
        _decoders = new ObjectVector<>();
    }

    static void ConvertFolderItemInfoToBindInfo(Folder folder, BindInfoEx bindInfo) {
        bindInfo.clear();

        for (int i = 0; i < folder.bindPairs.size(); i++) {
            BindPair bindPair = new BindPair(
                    folder.bindPairs.get(i).inIndex,
                    folder.bindPairs.get(i).outIndex);
            bindInfo.bindPairs.add(bindPair);
        }
        int outStreamIndex = 0;
        for (int i = 0; i < folder.coders.size(); i++) {
            CoderInfo coderInfo = folder.coders.get(i);
            bindInfo.coders.add(new CoderStreamsInfo(
                    coderInfo.getInStreamsCount(),
                    coderInfo.getOutStreamsCount()));
            AltCoderInfo altCoderInfo = coderInfo.getAltCoders().first();
            bindInfo.CoderMethodIDs.add(altCoderInfo.MethodID);
            for (int j = 0; j < coderInfo.getOutStreamsCount(); j++, outStreamIndex++) {
                if (folder.findBindPairForOutStream(outStreamIndex) < 0) {
                    bindInfo.outStreams.add(outStreamIndex);
                }
            }
        }
        for (int i = 0; i < folder.packStreams.size(); i++) {
            bindInfo.inStreams.add(folder.packStreams.get(i));
        }
    }

    Result decode(IInStream inStream, long startPos, LongVector packSizes, int packSizesOffset,
            Folder folderInfo, OutputStream outStream) throws IOException
    {
        ObjectVector<InputStream> inStreams = new ObjectVector<>();

        LockedInStream lockedInStream = new LockedInStream();
        lockedInStream.Init(inStream);

        for (int j = 0; j < folderInfo.packStreams.size(); j++) {
            LockedSequentialInStreamImp lockedStreamImpSpec = new LockedSequentialInStreamImp();
            lockedStreamImpSpec.Init(lockedInStream, startPos);
            startPos += packSizes.get(j + packSizesOffset);

            LimitedSequentialInStream streamSpec = new LimitedSequentialInStream();
            streamSpec.setStream(lockedStreamImpSpec);
            streamSpec.init(packSizes.get(j + packSizesOffset));
            inStreams.add(streamSpec);
        }

        int numCoders = folderInfo.coders.size();

        BindInfoEx bindInfo = new BindInfoEx();
        ConvertFolderItemInfoToBindInfo(folderInfo, bindInfo);
        boolean createNewCoders;
        createNewCoders = (!_bindInfoExPrevIsDefined || !bindInfo.equals(_bindInfoExPrev));

        if (createNewCoders) {
            int i;
            _decoders.clear();

            if (_mixerCoder != null) _mixerCoder.close();

            _mixerCoderSTSpec = new CoderMixer2ST();
            _mixerCoder = _mixerCoderSTSpec;
            _mixerCoderCommon = _mixerCoderSTSpec;
            _mixerCoderCommon.setBindInfo(bindInfo);

            for (i = 0; i < numCoders; i++) {
                CoderInfo coderInfo = folderInfo.coders.get(i);
                AltCoderInfo altCoderInfo = coderInfo.getAltCoders().first();

                if (coderInfo.isSimpleCoder()) {
                    ICompressCoder decoder = null;
                    ICompressFilter filter = null;

                    if (altCoderInfo.MethodID.equals(MethodID.k_LZMA)) {
                        decoder = new org.jamel.j7zip.compression.LZMA.Decoder();
                    }

                    if (altCoderInfo.MethodID.equals(MethodID.k_BCJ_X86)) {
                        filter = new BCJ_x86_Decoder();
                    }

                    if (altCoderInfo.MethodID.equals(MethodID.k_Copy)) {
                        decoder = new org.jamel.j7zip.compression.Copy.Decoder();
                    }

                    if (filter != null) {
                        FilterCoder coderSpec = new FilterCoder();
                        decoder = coderSpec;
                        coderSpec.filter = filter;
                    }

                    if (decoder == null) {
                        return Result.ERROR_NOT_IMPLEMENTED;
                    }

                    _decoders.add(decoder);
                    _mixerCoderSTSpec.addCoder(decoder, false);
                } else {
                    ICompressCoder2 decoder = null;
                    if (altCoderInfo.MethodID.equals(MethodID.k_BCJ2)) {
                        decoder = new BCJ2_x86_Decoder();
                    }

                    if (decoder == null) {
                        return Result.ERROR_NOT_IMPLEMENTED;
                    }

                    _decoders.add(decoder);
                    _mixerCoderSTSpec.addCoder2(decoder, false);
                }
            }
            _bindInfoExPrev = bindInfo;
            _bindInfoExPrevIsDefined = true;
        }

        int packStreamIndex = 0, unPackStreamIndex = 0;
        int coderIndex = 0;

        for (int i = 0; i < numCoders; i++) {
            CoderInfo coderInfo = folderInfo.coders.get(i);
            AltCoderInfo altCoderInfo = coderInfo.getAltCoders().first();

            ByteBuffer properties = altCoderInfo.Properties;
            int size = properties.GetCapacity();
            if (size == -1) {
                return Result.ERROR_NOT_IMPLEMENTED;
            }
            if (size > 0) {
                Object decoder = _decoders.get(coderIndex);
                if (decoder instanceof org.jamel.j7zip.compression.LZMA.Decoder) {
                    boolean ret = ((org.jamel.j7zip.compression.LZMA.Decoder) decoder)
                            .setDecoderProperties2(properties.data());
                    if (!ret) return Result.ERROR_FAIL;
                }
            }

            coderIndex++;

            int numInStreams = coderInfo.getInStreamsCount();
            int numOutStreams = coderInfo.getOutStreamsCount();
            LongVector packSizesPointers = new LongVector();
            LongVector unPackSizesPointers = new LongVector();
            packSizesPointers.Reserve(numInStreams);
            unPackSizesPointers.Reserve(numOutStreams);
            int j;
            for (j = 0; j < numOutStreams; j++, unPackStreamIndex++) {
                unPackSizesPointers.add(folderInfo.unPackSizes.get(unPackStreamIndex));
            }

            for (j = 0; j < numInStreams; j++, packStreamIndex++) {
                int bindPairIndex = folderInfo.findBindPairForInStream(packStreamIndex);
                if (bindPairIndex >= 0) {
                    packSizesPointers.add(
                            folderInfo.unPackSizes.get(folderInfo.bindPairs.get(bindPairIndex).outIndex));
                } else {
                    int index = folderInfo.findPackStreamArrayIndex(packStreamIndex);
                    if (index < 0) {
                        return Result.ERROR_FAIL;
                    }
                    packSizesPointers.add(packSizes.get(index));
                }
            }

            _mixerCoderCommon.setCoderInfo(i, packSizesPointers, unPackSizesPointers);
        }

        if (numCoders == 0) return Result.ERROR_INVALID_ARGS;

        ObjectVector<java.io.InputStream> inStreamPointers = new ObjectVector<>();
        inStreamPointers.reserve(inStreams.size());
        for (InputStream stream : inStreams) {
            inStreamPointers.add(stream);
        }

        ObjectVector<java.io.OutputStream> outStreamPointer = new ObjectVector<>();
        outStreamPointer.add(outStream);
        return _mixerCoder.code(inStreamPointers, inStreams.size(), outStreamPointer, 1);
    }
}
