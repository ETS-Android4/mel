package de.mel.auth.data;

import de.mel.Lok;
import de.mel.core.serialize.SerializableEntity;

/**
 * asks for more cached parts
 */
public class CachedRequest extends AbstractCachedMessage<CachedRequest> {


    private int partNumber;


    public CachedRequest setPartNumber(int partNumber) {
        this.partNumber = partNumber;
        return this;
    }

    public int getPartNumber() {
        return partNumber;
    }
}
