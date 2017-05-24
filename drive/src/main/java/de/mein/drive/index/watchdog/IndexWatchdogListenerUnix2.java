package de.mein.drive.index.watchdog;

import de.mein.drive.data.PathCollection;
import de.mein.drive.service.MeinDriveService;
import de.mein.drive.sql.FsDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by xor on 7/11/16.
 */
@SuppressWarnings("Duplicates")
class IndexWatchdogListenerUnix2 extends IndexWatchdogListenerPC {
    // we
    private UnixReferenceFileHandler unixReferenceFileHandler;

    IndexWatchdogListenerUnix2(MeinDriveService meinDriveService, WatchService watchService) {
        super(meinDriveService, "IndexWatchdogListenerUnix", watchService);
        unixReferenceFileHandler = new UnixReferenceFileHandler(meinDriveService.getServiceInstanceWorkingDirectory(), meinDriveService.getDriveSettings().getRootDirectory().getOriginalFile(), new File(meinDriveService.getDriveSettings().getTransferDirectoryPath()));
    }

    @Override
    public void foundDirectory(FsDirectory fsDirectory) {
        try {
            Path path = Paths.get(fsDirectory.getOriginal().getAbsolutePath());
            WatchKey key = path.register(watchService, KINDS);
            System.out.println("IndexWatchdogListener.foundDirectory: " + path.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void runImpl() {
        try {
            unixReferenceFileHandler.onStart();
            /**
             * cause the WatchService sometimes confuses the WatchKeys when creating folders we have to go around that.
             * We will only process all "delete" and "modify" (cause they can be ongoing for some time) events directly.
             * when an Event pops up, we will start the Timer and once it is finished we ask the Bash to show us every new/modified
             * File or Directory.
             */
            while (true) {
                WatchKey watchKey = watchService.take();
                ignoredSemaphore.acquire();
                try {
                    Path keyPath = (Path) watchKey.watchable();
                    for (WatchEvent<?> event : watchKey.pollEvents()) {
                        Path eventPath = (Path) event.context();
                        String absolutePath = keyPath.toString() + File.separator + eventPath.toString();
                        if (!absolutePath.startsWith(transferDirectoryPath)) {
                            File file = new File(absolutePath);
                            System.out.println("IndexWatchdogListener[" + meinDriveService.getDriveSettings().getRole() + "].got event[" + event.kind() + "] for: " + absolutePath);
                            if (event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                                // start the timer but do not analyze. Sometimes we get the wrong WatchKey so we cannot trust it.
                                watchDogTimer.start();
                                System.out.println("ignored");
                            } else {
                                analyze(event, file);
                                System.out.println("analyzed");
                            }
                        }
                        watchKey.reset();
                    }
                } finally {
                    ignoredSemaphore.release();
                }
                // reset the key
                boolean valid = watchKey.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void watchDirectory(File dir) {
        try {
            Path path = Paths.get(dir.getAbsolutePath());
            WatchKey key = path.register(watchService, KINDS);
            System.out.println("IndexWatchdogListener.watchDirectory: " + path.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTimerStopped() {
        System.out.println("IndexWatchdogListener.onTimerStopped");
        PathCollection pathCollection = new PathCollection();
        try {
            /**
             * we cannot retrieve all newly created things, so we have to do it now.
             * and watching the directories as well
             */
            Stream<String> paths = unixReferenceFileHandler.stuffModifiedAfter();
            pathCollection.addAll(paths);
            for (String p : paths) {
                File f = new File(p);
                if (f.exists() && f.isDirectory()) {
                    watchDirectory(f);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //meinDriveService.addJob(new FsSyncJob(pathCollection));
        stageIndexer.examinePaths(pathCollection);
        pathCollection = new PathCollection();
    }

    @Override
    public void onShutDown() {
        super.onShutDown();
    }
}