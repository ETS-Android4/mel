package de.mel.auth.data.cached;

import de.mel.auth.data.AbstractCachedMessage;
import de.mel.core.serialize.JsonIgnore;
import de.mel.core.serialize.SerializableEntity;
import de.mel.core.serialize.deserialize.entity.SerializableEntityDeserializer;
import de.mel.core.serialize.exceptions.JsonDeserializationException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class CachedPart extends AbstractCachedMessage {
    private int partNumber;
    private boolean serialized = false;
    private int size = 0;

    public CachedPart(){

    }

    public int size() {
        return size;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public CachedPart( Long cacheId, int partNumber){
        this.cacheId = cacheId;
        this.partNumber = partNumber;
    }

    public CachedPart setSerialized() {
        this.serialized = true;
        return this;
    }

    public boolean isSerialized() {
        return serialized;
    }

    public static CachedPart read(File file) throws IOException, JsonDeserializationException {
        byte[] bytes = new byte[(int) file.length()];
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        in.read(bytes);
        in.close();
        String json = new String(bytes);
        //deserialize
        CachedPart part = (CachedPart) SerializableEntityDeserializer.deserialize(json);
        part.setSerialized();
        return part;
    }
}
