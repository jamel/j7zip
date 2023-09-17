package org.jamel.j7zip.archive.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jamel.j7zip.common.LongVector;
import org.jamel.j7zip.common.ObjectVector;
import org.jamel.j7zip.ICompressCoder;
import org.jamel.j7zip.ICompressCoder2;
import org.jamel.j7zip.ICompressSetInStream;
import org.jamel.j7zip.ICompressSetOutStream;
import org.jamel.j7zip.ICompressSetOutStreamSize;
import org.jamel.j7zip.Result;

public class CoderMixer2ST implements ICompressCoder2, CoderMixer2 {

    private BindInfo bindInfo = new BindInfo();
    private ObjectVector<STCoderInfo> coders = new ObjectVector<>();


    public void setBindInfo(BindInfo bindInfo) {
        this.bindInfo = bindInfo;
    }

    public void addCoderCommon(boolean isMain) {
        CoderStreamsInfo csi = bindInfo.coders.get(coders.size());
        coders.add(new STCoderInfo(csi.getInStreamsCount(), csi.getOutStreamsCount(), isMain));
    }

    public void addCoder2(ICompressCoder2 coder, boolean isMain) {
        addCoderCommon(isMain);
        coders.last().Coder2 = coder;
    }

    public void addCoder(ICompressCoder coder, boolean isMain) {
        addCoderCommon(isMain);
        coders.last().Coder = coder;
    }

    public void setCoderInfo(int coderIndex, LongVector inSizes, LongVector outSizes) {
        coders.get(coderIndex).setCoderInfo(inSizes, outSizes);
    }

    public InputStream getInStream(ObjectVector<InputStream> inStreams, int streamIndex) throws IOException {
        for (int i = 0; i < bindInfo.inStreams.size(); i++) {
            if (bindInfo.inStreams.get(i) == streamIndex) {
                return inStreams.get(i);
            }
        }

        int binderIndex = bindInfo.findBinderForInStream(streamIndex);
        if (binderIndex < 0) {
            throw new IOException("Can't find binder for input stream");
        }

        int coderIndex = bindInfo.findOutStream(bindInfo.bindPairs.get(binderIndex).outIndex);
        CoderInfo coder = coders.get(coderIndex);
        if (!(coder.Coder instanceof InputStream) || !(coder.Coder instanceof ICompressSetInStream)
                || coder.NumInStreams > 1)
        {
            throw new IOException("Coder is not implemented");
        }

        ICompressSetInStream setInStream = (ICompressSetInStream) coder.Coder;
        int startIndex = bindInfo.getCoderInStreamIndex(coderIndex);

        for (int i = 0; i < coder.NumInStreams; i++) {
            setInStream.setInStream(getInStream(inStreams, startIndex + i));
        }

        return (InputStream) coder.Coder;
    }

    public OutputStream getOutStream(ObjectVector<OutputStream> outStreams, int streamIndex) throws IOException {
        for (int i = 0; i < bindInfo.outStreams.size(); i++) {
            if (bindInfo.outStreams.get(i) == streamIndex) {
                return outStreams.get(i);
            }
        }
        int binderIndex = bindInfo.findBinderForOutStream(streamIndex);
        if (binderIndex < 0) {
            throw new IOException("Can't find binder for out stream");
        }

        int coderIndex = bindInfo.findInStream(bindInfo.bindPairs.get(binderIndex).inIndex);

        CoderInfo coder = coders.get(coderIndex);
        if (!(coder.Coder instanceof OutputStream) || !(coder.Coder instanceof ICompressSetOutStream)
                || coder.NumOutStreams > 1)
        {
            throw new IOException("Coder is not implemented");
        }

        ICompressSetOutStream setOutStream = (ICompressSetOutStream) coder.Coder;
        int startIndex = bindInfo.GetCoderOutStreamIndex(coderIndex);

        for (int i = 0; i < coder.NumOutStreams; i++) {
            setOutStream.setOutStream(getOutStream(outStreams, startIndex + i));
        }

        return (OutputStream) coder.Coder;
    }

    public Result code(
            ObjectVector<InputStream> inStreams,
            int numInStreams,
            ObjectVector<OutputStream> outStreams,
            int numOutStreams) throws IOException
    {
        if (numInStreams != bindInfo.inStreams.size() || numOutStreams != bindInfo.outStreams.size()) {
            return Result.ERROR_INVALID_ARGS;
        }

        // Find main coder
        int _mainCoderIndex = -1;
        for (int i = 0; i < coders.size(); i++) {
            if (coders.get(i).IsMain) {
                _mainCoderIndex = i;
                break;
            }
        }
        if (_mainCoderIndex < 0) {
            for (int i = 0; i < coders.size(); i++) {
                if (coders.get(i).NumInStreams > 1) {
                    if (_mainCoderIndex >= 0) {
                        return Result.ERROR_NOT_IMPLEMENTED;
                    }
                    _mainCoderIndex = i;
                }
            }
        }
        if (_mainCoderIndex < 0) {
            _mainCoderIndex = 0;
        }

        CoderInfo mainCoder = coders.get(_mainCoderIndex);

        ObjectVector<InputStream> seqInStreams = new ObjectVector<>();
        ObjectVector<OutputStream> seqOutStreams = new ObjectVector<>();
        int startInIndex = bindInfo.getCoderInStreamIndex(_mainCoderIndex);
        int startOutIndex = bindInfo.GetCoderOutStreamIndex(_mainCoderIndex);
        for (int i = 0; i < mainCoder.NumInStreams; i++) {
            seqInStreams.add(getInStream(inStreams, startInIndex + i));
        }
        for (int i = 0; i < mainCoder.NumOutStreams; i++) {
            seqOutStreams.add(getOutStream(outStreams, startOutIndex + i));
        }
        ObjectVector<InputStream> seqInStreamsSpec = new ObjectVector<>();
        ObjectVector<OutputStream> seqOutStreamsSpec = new ObjectVector<>();
        for (int i = 0; i < mainCoder.NumInStreams; i++) {
            seqInStreamsSpec.add(seqInStreams.get(i));
        }
        for (int i = 0; i < mainCoder.NumOutStreams; i++) {
            seqOutStreamsSpec.add(seqOutStreams.get(i));
        }

        for (int i = 0; i < coders.size(); i++) {
            if (i == _mainCoderIndex) {
                continue;
            }

            CoderInfo coder = coders.get(i);
            if (coder.Coder instanceof ICompressSetOutStreamSize) {
                ICompressSetOutStreamSize setOutStreamSize = (ICompressSetOutStreamSize) coder.Coder;
                setOutStreamSize.setOutStreamSize(coder.OutSizePointers.get(0));
            }
        }
        if (mainCoder.Coder != null) {
            mainCoder.Coder.code(
                    seqInStreamsSpec.get(0),
                    seqOutStreamsSpec.get(0),
                    mainCoder.OutSizePointers.get(0));
        } else {
            Result res = mainCoder.Coder2.code(
                    seqInStreamsSpec,
                    mainCoder.NumInStreams,
                    seqOutStreamsSpec,
                    mainCoder.NumOutStreams);
            if (res != Result.OK) return res;
        }

        OutputStream stream = seqOutStreams.first();
        stream.flush();

        return Result.OK;
    }

    public void close() {
    }
}

