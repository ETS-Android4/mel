package de.mel.drive.index;

import de.mel.DeferredRunnable;
import de.mel.auth.file.AFile;
import de.mel.drive.data.fs.RootDirectory;
import de.mel.drive.service.MelDriveService;
import de.mel.drive.service.sync.SyncHandler;
import de.mel.drive.sql.DriveDatabaseManager;
import de.mel.drive.sql.FsDirectory;
import de.mel.drive.index.watchdog.IndexWatchdogListener;
import de.mel.sql.SqlQueriesException;

import org.jdeferred.impl.DeferredObject;

import java.io.IOException;

/**
 * Created by xor on 10.07.2016.
 */
public class Indexer {
    private final MelDriveService melDriveService;
    private IndexerRunnable indexerRunnable;

    public IndexerRunnable getIndexerRunnable() {
        return indexerRunnable;
    }

    public Indexer(DriveDatabaseManager databaseManager, IndexWatchdogListener indexWatchdogListener, IndexListener... listeners) throws SqlQueriesException {
        melDriveService = databaseManager.getMelDriveService();
        indexerRunnable = new IndexerRunnable(databaseManager, indexWatchdogListener, listeners);
    }

    public void setSyncHandler(SyncHandler syncHandler) {
        indexerRunnable.setSyncHandler(syncHandler);
    }

    public void ignorePath(String path, int amount) {
        //todo escalate?
        try {
            indexerRunnable.getIndexWatchdogListener().ignore(path, amount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopIgnore(String path) throws InterruptedException {
        indexerRunnable.getIndexWatchdogListener().stopIgnore(path);
    }



    public void watchDirectory(AFile dir) throws IOException {
        indexerRunnable.getIndexWatchdogListener().watchDirectory(dir);
    }

    public RootDirectory getRootDirectory() {
        return indexerRunnable.getRootDirectory();
    }


    public void shutDown() {
        indexerRunnable.shutDown();
    }

    public DeferredObject<DeferredRunnable, Exception, Void> start() {
        melDriveService.execute(indexerRunnable);
        return indexerRunnable.getStartedDeferred();
    }

    public DeferredObject<DeferredRunnable, Exception, Void> getIndexerStartedDeferred() {
        return indexerRunnable.getStartedDeferred();
    }

    public void suspend() {
        indexerRunnable.stop();
    }

    public void resume() {
        // put  a new promise in place
//        if (indexerRunnable.getStartedDeferred().isResolved())
//            indexerRunnable.setStartedPromise(new DeferredObject<>());
//        indexerRunnable.getStartedDeferred().done(result -> N.r(melDriveService::onIndexerDone));
        melDriveService.execute(indexerRunnable);
    }

    public void setConflictHelper(InitialIndexConflictHelper conflictHelper) {
        indexerRunnable.setInitialIndexConflictHelper(conflictHelper);
    }
}