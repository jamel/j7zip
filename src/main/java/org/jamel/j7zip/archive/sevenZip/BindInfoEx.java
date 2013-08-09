package org.jamel.j7zip.archive.sevenZip;

import org.jamel.j7zip.common.ObjectVector;
import org.jamel.j7zip.archive.common.BindInfo;


class BindInfoEx extends BindInfo {

    ObjectVector<MethodID> CoderMethodIDs = new ObjectVector<>();

    public void clear() {
        super.clear();
        CoderMethodIDs.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BindInfoEx that = (BindInfoEx) o;
        if (coders.size() != that.coders.size()) {
            return false;
        }
        for (int i = 0; i < coders.size(); i++) {
            if (!coders.get(i).equals(that.coders.get(i))) {
                return false;
            }
        }
        if (bindPairs.size() != that.bindPairs.size()) {
            return false;
        }
        for (int i = 0; i < bindPairs.size(); i++) {
            if (!bindPairs.get(i).equals(that.bindPairs.get(i))) {
                return false;
            }
        }
        for (int i = 0; i < CoderMethodIDs.size(); i++) {
            if (CoderMethodIDs.get(i) != that.CoderMethodIDs.get(i)) {
                return false;
            }
        }
        if (inStreams.size() != that.inStreams.size()) {
            return false;
        }
        if (outStreams.size() != that.outStreams.size()) {
            return false;
        }
        return true;
    }

}
