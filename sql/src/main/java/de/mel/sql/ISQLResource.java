package de.mel.sql;

import java.sql.SQLException;

/**
 * Created by xor on 2/6/17.
 */
public interface ISQLResource<T extends SQLTableObject> extends AutoCloseable {
    T getNext() throws SqlQueriesException;

    @Override
    void close() throws SqlQueriesException;

    boolean isClosed() throws SqlQueriesException;
}
