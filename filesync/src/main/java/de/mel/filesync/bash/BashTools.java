package de.mel.filesync.bash;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import de.mel.Lok;
import de.mel.auth.file.AbstractFile;

/**
 * Created by xor on 10/28/16.
 */
@SuppressWarnings("Duplicates")
public abstract class BashTools {

    public static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    private static BashToolsImpl instance;

    public static BashToolsImpl getInstance() {
        return instance;
    }

    public static void setInstance(BashToolsImpl instance) {
        BashTools.instance = instance;
    }

    public static void init() {
        if (instance == null)
            if (isWindows) {
                instance = new BashToolsWindows();
            } else {
                instance = new BashToolsUnix();
            }
    }

    public static void setBinPath(String binPath) {
        instance.setBinPath(binPath);
    }

    public static FsBashDetails getFsBashDetails(AbstractFile file) throws IOException, InterruptedException {
        return instance.getFsBashDetails(file);
    }

    public static void rmRf(AbstractFile directory) throws IOException {
        instance.rmRf(directory);
    }

    public static void rmRf(File directory) throws IOException {
        instance.rmRf(AbstractFile.instance(directory));
    }

    public static List<AbstractFile<?>> stuffModifiedAfter(AbstractFile referenceFile, AbstractFile directory, AbstractFile pruneDir) throws IOException, BashToolsException {
        return instance.stuffModifiedAfter(referenceFile, directory, pruneDir);
    }

    public static AutoKlausIterator<AbstractFile<?>> find(AbstractFile directory, AbstractFile pruneDir) throws IOException {
        return instance.find(directory, pruneDir);
    }

    public static AutoKlausIterator<AbstractFile<?>> stuffModifiedAfter(AbstractFile originalFile, AbstractFile pruneDir, long timeStamp) throws IOException, InterruptedException {
        return instance.stuffModifiedAfter(originalFile, pruneDir, timeStamp);
    }

    public static AutoKlausIterator<AbstractFile> inputStreamToFileIterator(InputStream inputStream) {
        BufferedIterator bufferedReader = new BufferedIterator.BufferedFileIterator(new InputStreamReader(inputStream));
        return bufferedReader;
    }

    public static AutoKlausIterator<String> inputStreamToIterator(InputStream inputStream) {
        BufferedIterator bufferedReader = new BufferedIterator.BufferedStringIterator(new InputStreamReader(inputStream));
        return bufferedReader;
    }

    public static void mkdir(AbstractFile dir) throws IOException {
        int i = 0;
        while (!dir.exists()) {
            dir.mkdirs();
            Lok.debug("BashTools.mkdir(" + i + ") for " + dir.getAbsolutePath());
        }
        //instance.mkdir(dir);
    }

    public static File[] lsD(String path) {
        File root = new File(path);
        if (root.exists()) {
            File[] dirs = root.listFiles((file, s) -> file.isDirectory());
            return dirs;
        }
        return new File[0];
    }


    public static boolean isSymLink(AbstractFile f) {
        return instance.isSymLink(f);
    }

    public static Map<String, FsBashDetails> getContentFsBashDetails(AbstractFile file) {
        return instance.getContentFsBashDetails(file);
    }

    public static void lnS(AbstractFile file, String target) {
        Lok.debug("creating symlink: '" + file.getAbsolutePath() + "' -> '" + target + "'");
        instance.lnS(file, target);
    }

    public static void setCreationDate(AbstractFile target, Long created) {
        if (created != null)
            instance.setCreationDate(target, created);
    }
}
