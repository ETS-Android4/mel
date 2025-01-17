package de.mel.filesync.index;

import de.mel.DeferredRunnable;
import de.mel.auth.file.AbstractFile;
import de.mel.filesync.data.fs.RootDirectory;
import de.mel.filesync.service.MelFileSyncService;
import de.mel.filesync.service.sync.SyncHandler;
import de.mel.filesync.sql.FileSyncDatabaseManager;
import de.mel.filesync.index.watchdog.IndexWatchdogListener;
import de.mel.sql.SqlQueriesException;

import org.jdeferred.impl.DeferredObject;

import java.io.IOException;

/**
 * Created by xor on 10.07.2016.
 */
public class Indexer {
    private final MelFileSyncService melFileSyncService;
    private IndexerRunnable indexerRunnable;

    public IndexerRunnable getIndexerRunnable() {
        return indexerRunnable;
    }

    public Indexer(FileSyncDatabaseManager databaseManager, IndexWatchdogListener indexWatchdogListener, IndexListener... listeners) throws SqlQueriesException {
        melFileSyncService = databaseManager.getMelFileSyncService();
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



    public void watchDirectory(AbstractFile dir) throws IOException {
        indexerRunnable.getIndexWatchdogListener().watchDirectory(dir);
    }

    public RootDirectory getRootDirectory() {
        return indexerRunnable.getRootDirectory();
    }


    public void shutDown() {
        indexerRunnable.shutDown();
    }

    public DeferredObject<DeferredRunnable, Exception, Void> start() {
        melFileSyncService.execute(indexerRunnable);
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
        melFileSyncService.execute(indexerRunnable);
    }

    public void setConflictHelper(InitialIndexConflictHelper conflictHelper) {
        indexerRunnable.setInitialIndexConflictHelper(conflictHelper);
    }
}
