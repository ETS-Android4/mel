package de.mein;

import org.jdeferred.impl.DeferredObject;

/**
 * A Runnable that comes with a Deferred object, that is resolved when the Thread has started
 * Created by xor on 04.09.2016.
 */
public abstract class DeferredRunnable implements MeinRunnable {
    protected DeferredObject<DeferredRunnable, Exception, Void> startedPromise = new DeferredObject<>();
    protected Thread thread;
    private boolean interrupted = false;

    /**
     * you must not override this
     */
    public void shutDown() {
        String line = "shutting down: " + getClass().getSimpleName();
        if (thread != null)
            line += "/" + thread.getName();
        Lok.debug(line);
        if (thread != null) {
            thread.interrupt();
            interrupted = true;
        } else {
            Lok.error(getClass().getSimpleName() + ".shutDown: Thread was null :'(  " + getRunnableName());
        }
        onShutDown();
    }

    /**
     * Is called after shutDown() was called. The current Thread is already interrupted.
     * You may want to shut down other components as well. They should be interrupted but might block somewhere.
     * Unblock them here.
     */
    public abstract void onShutDown();

    @Override
    public void run() {
        thread = Thread.currentThread();
        thread.setName(getRunnableName());
        runImpl();
        Lok.debug(getClass().getSimpleName()+".run.done on "+thread.getName());
    }

    /**
     * This is where the stuff you would usually do in run() belongs.
     */
    public abstract void runImpl();

    /**
     * call this from(!) the running Thread to see whether or not the Thread should stop.
     * @return
     */
    protected boolean isInterrupted() {
        return interrupted;
    }

    public DeferredObject<DeferredRunnable, Exception, Void> getStartedDeferred() {
        return startedPromise;
    }

    public DeferredRunnable setStartedPromise(DeferredObject<DeferredRunnable, Exception, Void> startedPromise) {
        this.startedPromise = startedPromise;
        return this;
    }
}
