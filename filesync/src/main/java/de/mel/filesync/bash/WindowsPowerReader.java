package de.mel.filesync.bash;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by xor on 18.07.2017.
 */
public class WindowsPowerReader extends BufferedReader {
    private String prependLine;

    public WindowsPowerReader(Reader in, int sz) {
        super(in, sz);
    }

    public WindowsPowerReader(Reader in) {
        super(in);
    }

    /*
    * PowerShell adds a few lines at the beginning and one at the end.
    * this is where we get rid of it.
    */
    @SuppressWarnings("Duplicates")
    @Override
    public Stream<String> lines() {
        Iterator<String> iter = new Iterator<String>() {
            String next = null;
            String after = null;
            private boolean reachedStartLine = false;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    try {
                        if (after != null)
                            return true;
                        after = readLine();
                        if (after == null)
                            return false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                } else {
                    try {
                        if (!reachedStartLine) {
                            String d = readLine();
                            d = readLine();
                            d = readLine();
                            d = readLine();
                            reachedStartLine = true;
                        }
                        if (prependLine != null) {
                            next = prependLine;
                            prependLine = null;
                            return true;
                        } else {
                            next = readLine();
                            after = readLine();
                        }
                        if (next == null || after == null)
                            return false;
                        return (next != null);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }

            @Override
            public String next() {
                if (next != null || hasNext()) {
                    String line = next;
                    next = after;
                    after = null;
                    return line;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                iter, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    public void prependLine(String prependLine) {
        this.prependLine = prependLine;
    }
}
