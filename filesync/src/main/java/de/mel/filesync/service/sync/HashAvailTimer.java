package de.mel.filesync.service.sync;

import de.mel.auth.tools.N;
import de.mel.auth.tools.WatchDogTimer;
import de.mel.filesync.data.AvailableHashes;

import java.util.concurrent.locks.ReentrantLock;

public class HashAvailTimer extends WatchDogTimer {

    private AvailableHashes hashesAvailable = new AvailableHashes();
    private ReentrantLock stateLock = new ReentrantLock(true);

    /**
     * @param watchDogTimerFinished
     */
    public HashAvailTimer(WatchDogTimerFinished watchDogTimerFinished) {
        super(HashAvailTimer.class.getSimpleName(), watchDogTimerFinished, 10, 100, 1000);
        setWatchDogTimerFinished(() -> {
            stateLock.lock();
            watchDogTimerFinished.onTimerStopped();
            hashesAvailable.clear();
            stateLock.unlock();
        });
    }

    public synchronized void addHash(String hash) {
        stateLock.lock();
        N.r(this::start);
        hashesAvailable.addHash(hash);
        stateLock.unlock();
    }

    public AvailableHashes getHashesAvailableCopy() {
        AvailableHashes copy = new AvailableHashes();
        copy.getHashes().addAll(hashesAvailable.getHashes());
        return copy;
    }
}
