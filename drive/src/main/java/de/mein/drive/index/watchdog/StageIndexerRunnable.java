package de.mein.drive.index.watchdog;

import de.mein.drive.data.DriveStrings;
import de.mein.drive.data.PathCollection;
import de.mein.drive.index.AbstractIndexer;
import de.mein.drive.sql.DriveDatabaseManager;
import de.mein.drive.sql.StageSet;
import de.mein.drive.sql.dao.FsDao;

/**
 * Locks fsDao for reading
 * Created by xor on 11/24/16.
 */
public class StageIndexerRunnable extends AbstractIndexer {

    private final PathCollection pathCollection;
    private StageIndexer.StagingDoneListener stagingDoneListener;

    public StageIndexerRunnable(DriveDatabaseManager databaseManager, PathCollection pathCollection) {
        super(databaseManager);

        this.pathCollection = pathCollection;
    }


    public FsDao getFsDao() {
        return fsDao;
    }


    @Override
    public void runImpl() {
        try {
            fsDao.lockRead();
            initStage(DriveStrings.STAGESET_TYPE_FS, pathCollection.getPaths().stream());
            examineStage();
            stagingDoneListener.onStagingFsEventsDone(stageSetId);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fsDao.unlockRead();
        }
    }


    public void setStagingDoneListener(StageIndexer.StagingDoneListener stagingDoneListener) {
        assert stagingDoneListener != null;
        this.stagingDoneListener = stagingDoneListener;
    }
}
