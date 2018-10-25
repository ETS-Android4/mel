package de.mein.auth.data.cached;

import de.mein.Lok;
import de.mein.core.serialize.SerializableEntity;
import de.mein.core.serialize.exceptions.JsonSerializationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

public class CachedIterator<T extends SerializableEntity> extends CachedData implements Iterator<T> {

    private CachedIterable iterable;
    private int pos = 0;
    private Iterator<SerializableEntity> partIterator;

    public CachedIterator(CachedIterable iterable) {
        this.iterable = iterable;
        partCount = 1;
    }

    @Override
    public boolean hasNext() {
        return pos < iterable.getSize();
    }

    @Override
    public T next() {
        try {
            pos++;
            if (partIterator == null || !partIterator.hasNext()) {
                //read
                File file = iterable.createCachedPartFile(partCount);
                partCount++;
                // todo debug .. remove try block if done. but not whats inside
                try {
                    CachedListPart part = (CachedListPart) CachedPart.read(file);
                    partIterator = part.getElements().iterator();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Lok.debug(" ");
                }
            }
            return (T) partIterator.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void toDisk() throws IllegalAccessException, JsonSerializationException, IOException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        //nothing to do here
        Lok.warn("this method has no purpose on an iterator. you probably wanted to call it on another object.");
    }
}