package de.mel.auth.data;

import de.mel.Lok;
import de.mel.core.serialize.SerializableEntity;
import de.mel.core.serialize.deserialize.entity.SerializableEntityDeserializer;
import de.mel.core.serialize.exceptions.JsonDeserializationException;
import de.mel.core.serialize.exceptions.JsonSerializationException;
import de.mel.core.serialize.serialize.fieldserializer.entity.SerializableEntitySerializer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by xor on 30.08.2016.
 */
public abstract class JsonSettings implements SerializableEntity {
    private File file;

    public static JsonSettings load(File file) throws IOException, JsonDeserializationException, JsonSerializationException, IllegalAccessException {
        if (file.exists()) {
            byte[] bytes = new byte[(int) file.length()];
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            in.read(bytes);
            in.close();
            String json = new String(bytes);
            JsonSettings jsonSettings = (JsonSettings) SerializableEntityDeserializer.deserialize(json);
            jsonSettings.setJsonFile(file);
            jsonSettings.init();
            return jsonSettings;
        }
        return null;
    }

    /**
     * create file objects from paths here if necessary
     */
    protected abstract void init();

    public void save() throws JsonSerializationException, IllegalAccessException, IOException {
        String json = SerializableEntitySerializer.serialize(this);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        out.write(json.getBytes());
        out.close();
        // Files.write(Paths.get(file.getAbsolutePath()), json.getBytes());
    }

    public JsonSettings setJsonFile(File file) {
        this.file = file;
        return this;
    }

}
