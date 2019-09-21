package de.mein;

import de.mein.auth.data.access.DatabaseManager;
import de.mein.core.serialize.deserialize.binary.BinaryDeserializer;
import de.mein.core.serialize.serialize.fieldserializer.binary.BinaryFieldSerializer;
import de.mein.execute.SqliteExecutor;
import de.mein.execute.SqliteExecutorInjection;
import de.mein.sql.Pair;
import de.mein.sql.PairTypeConverter;

/**
 * Collection of all places where some customized implementations might be useful.
 * e.g. where the standard way of doing things in Java do not work on Android
 */

public class MeinInjector {
    private MeinInjector() {
    }

    public static void setMeinAuthSqlInputStreamInjector(DatabaseManager.SqlInputStreamInjector sqlInputStreamInjector) {
        DatabaseManager.setSqlInputStreamInjector(sqlInputStreamInjector);
    }

    public static void setSQLConnectionCreator(DatabaseManager.SQLConnectionCreator connectionCreator) {
        DatabaseManager.setSqlConnectionCreator(connectionCreator);
    }

    public static void setBase64Encoder(BinaryFieldSerializer.Base64Encoder encoder) {
        BinaryFieldSerializer.setBase64Encoder(encoder);
    }

    public static void setBase64Decoder(BinaryDeserializer.Base64Decoder decoder) {
        BinaryDeserializer.setBase64Decoder(decoder);
    }

    public static void setExecutorImpl(SqliteExecutorInjection injectedImpl) {
        SqliteExecutor.setExecutorImpl(injectedImpl);
    }

}
