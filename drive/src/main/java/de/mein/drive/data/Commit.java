package de.mein.drive.data;

import de.mein.Lok;
import de.mein.auth.data.cached.CachedIterable;
import de.mein.auth.service.Bootloader;
import de.mein.drive.sql.Stage;

import java.io.File;

/**
 * Created by xor on 1/14/17.
 */
public class Commit extends CachedIterable<Stage> {
    private Long basedOnVersion;

    public Commit(){
        Lok.debug("Commit.Commit");
        this.level = Bootloader.BootLevel.LONG;
    }

    public Commit(File cacheDir, int partSize) {
        super(cacheDir,partSize);
        this.level = Bootloader.BootLevel.LONG;
    }

    public Commit setBasedOnVersion(Long basedOnVersion) {
        this.basedOnVersion = basedOnVersion;
        return this;
    }

    public Long getBasedOnVersion() {
        return basedOnVersion;
    }

}
