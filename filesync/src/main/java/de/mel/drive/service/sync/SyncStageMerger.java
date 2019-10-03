package de.mel.drive.service.sync;

import de.mel.auth.file.AFile;
import de.mel.drive.sql.Stage;
import de.mel.sql.SqlQueriesException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xor on 5/6/17.
 */
public abstract class SyncStageMerger {
    protected final Long lStageSetId;
    protected final Long rStageSetId;


    /**
     * is called with two Stages which reference the same logical File or Directory
     *
     * @param left
     * @param right
     * @throws SqlQueriesException
     */
    public abstract void stuffFound(Stage left, Stage right, AFile lFile, AFile rFile) throws SqlQueriesException;

    public SyncStageMerger(Long lStageSetId, Long rStageSetId) {
        this.lStageSetId = lStageSetId;
        this.rStageSetId = rStageSetId;
    }
}