package de.mein.android.file;

import android.content.Context;

import java.io.File;

import de.mein.auth.file.AFile;

public class AndroidFileConfiguration extends AFile.Configuration {
    private Context context;

    public AndroidFileConfiguration(Context context) {
        this.context = context;
    }

    @Override
    public AFile instance(String path) {
        return new DFile(path);
    }

    @Override
    public String separator() {
        return "%2F";
    }

    @Override
    public AFile instance(File file) {
        return new DFile(file);
    }


    @Override
    public AFile instance(AFile parent, String name) {
        if (parent instanceof DFile)
            return new DFile((DFile) parent, name);
        else
            System.err.println(getClass().getSimpleName() + "instance(AFile parent, String name), got a '" + parent.getClass().getSimpleName() + "' as parent.");
        return null;
    }

    @Override
    public AFile instance(AFile originalFile) {
        return new DFile((DFile) originalFile);

    }

    public Context getContext() {
        return context;
    }
}