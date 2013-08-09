package org.jamel.j7zip.archive.common;

import org.jamel.j7zip.common.LongVector;

public interface CoderMixer2 {

    void setBindInfo(BindInfo bindInfo);

    void setCoderInfo(int coderIndex, LongVector inSizes, LongVector outSizes);
}
