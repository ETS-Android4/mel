package de.mein.drive.index.watchdog;

import de.mein.DeferredRunnable;
import de.mein.Lok;
import de.mein.auth.service.power.PowerManager;
import de.mein.drive.data.PathCollection;
import de.mein.drive.index.IndexListener;
import de.mein.auth.file.AFile;
import de.mein.drive.service.MeinDriveService;
import de.mein.drive.sql.FsFile;
import de.mein.auth.tools.WatchDogTimer;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xor on 7/11/16.
 */
@SuppressWarnings("Duplicates")
public abstract class IndexWatchdogListener extends DeferredRunnable implements IndexListener, Runnable, WatchDogTimer.WatchDogTimerFinished, PowerManager.PowerManagerListener {

    private static WatchDogRunner watchDogRunner = meinDriveService1 -> {
        WatchService watchService1 = null;
        IndexWatchdogListener watchdogListener;
        try {
            watchService1 = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            Lok.debug("WatchDog.windows");
            watchdogListener = new IndexWatchDogListenerWindows(meinDriveService1, watchService1);
        } else {
            Lok.debug("WatchDog.unix");
            watchdogListener = new IndexWatchdogListenerUnix2(meinDriveService1, watchService1);
        }
        watchdogListener.meinDriveService = meinDriveService1;
        watchdogListener.meinDriveService.execute(watchdogListener);
        return watchdogListener;
    };
    protected String name;
    protected WatchDogTimer watchDogTimer = new WatchDogTimer(this, 20, 100, 150);
    protected MeinDriveService meinDriveService;
    protected PathCollection pathCollection = new PathCollection();
    protected Map<String, Integer> ignoredMap = new ConcurrentHashMap<>();
    protected Semaphore ignoredSemaphore = new Semaphore(1, true);
    protected String transferDirectoryPath;
    protected StageIndexer stageIndexer;
    private ReentrantLock surpressLock = new ReentrantLock();
    private boolean hasSupressedEvents = false;

    public IndexWatchdogListener(MeinDriveService meinDriveService) {
        this.meinDriveService = meinDriveService;
        this.meinDriveService.getMeinAuthService().getPowerManager().addPowerListener(this);
    }

    public static void setWatchDogRunner(WatchDogRunner watchDogRunner) {
        IndexWatchdogListener.watchDogRunner = watchDogRunner;
    }

    public static IndexWatchdogListener runInstance(MeinDriveService meinDriveService) {
        return IndexWatchdogListener.watchDogRunner.runInstance(meinDriveService);
    }

    @Override
    public void onTimerStopped() {
        Lok.debug("IndexWatchdogListener.onTimerStopped");
        //meinDriveService.addJob(new FsSyncJob(pathCollection));
        stageIndexer.examinePaths(this,pathCollection);
        pathCollection = new PathCollection();
    }

    public StageIndexer getStageIndexer() {
        return stageIndexer;
    }

    public void setStageIndexer(StageIndexer stageIndexer) {
        this.stageIndexer = stageIndexer;
    }

    @Override
    public void foundFile(FsFile fsFile) {

    }

    public abstract void watchDirectory(AFile dir);

    @Override
    public void done(Long stageSetId) {
        Lok.debug("IndexWatchdogListener.done");
    }

    public void ignore(String path, int amount) throws InterruptedException {
        Lok.debug("IndexWatchdogListener[" + meinDriveService.getDriveSettings().getDriveDetails().getRole()
                + "].ignore(" + path + ")");
        ignoredSemaphore.acquire();
        if (ignoredMap.containsKey(path))
            amount += ignoredMap.get(path);
        ignoredMap.put(path, amount);
        ignoredSemaphore.release();
    }

    public IndexWatchdogListener setTransferDirectoryPath(String transferDirectoryPath) {
        this.transferDirectoryPath = transferDirectoryPath;
        return this;
    }

    public void stopIgnore(String path) throws InterruptedException {
        ignoredSemaphore.acquire();
        Lok.debug("IndexWatchdogListener[" + meinDriveService.getDriveSettings().getDriveDetails().getRole()
                + "].stopignore(" + path + ")");
        ignoredMap.remove(path);
        ignoredSemaphore.release();
    }

    @Override
    public void onShutDown() {
        watchDogTimer.cancel();
        // dereference cause JavaDoc says so
        watchDogTimer = null;
    }

    protected void surpressEvent() {
        surpressLock.lock();
        hasSupressedEvents = true;
        surpressLock.unlock();
    }

    @Override
    public void onHeavyWorkAllowed() {
        surpressLock.lock();
        try {
            if (hasSupressedEvents) {
                hasSupressedEvents = false;
                watchDogTimer.start();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            surpressLock.unlock();
        }
    }

    public interface WatchDogRunner {
        IndexWatchdogListener runInstance(MeinDriveService meinDriveService);
    }
}
