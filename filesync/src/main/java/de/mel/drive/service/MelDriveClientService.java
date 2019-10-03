package de.mel.drive.service;

import de.mel.DeferredRunnable;
import de.mel.Lok;
import de.mel.auth.MelNotification;
import de.mel.auth.jobs.Job;
import de.mel.auth.jobs.ServiceRequestHandlerJob;
import de.mel.auth.service.MelAuthService;
import de.mel.auth.socket.process.val.Request;
import de.mel.auth.tools.N;
import de.mel.auth.tools.lock.P;
import de.mel.auth.tools.lock.Warden;
import de.mel.drive.data.AvailableHashes;
import de.mel.drive.data.DriveSettings;
import de.mel.drive.data.DriveStrings;
import de.mel.drive.data.conflict.Conflict;
import de.mel.drive.data.conflict.ConflictSolver;
import de.mel.drive.index.IndexListener;
import de.mel.drive.index.InitialIndexConflictHelper;
import de.mel.drive.jobs.CommitJob;
import de.mel.drive.jobs.SyncClientJob;
import de.mel.drive.service.sync.ClientSyncHandler;
import de.mel.drive.sql.StageSet;
import de.mel.drive.sql.TransferState;
import de.mel.sql.SqlQueriesException;

import org.jdeferred.impl.DeferredObject;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by xor on 10/21/16.
 */
public class MelDriveClientService extends MelDriveService<ClientSyncHandler> {
    private MelNotification latestConflictNotification;

    public MelDriveClientService(MelAuthService melAuthService, File workingDirectory, Long serviceTypeId, String uuid, DriveSettings driveSettings) {
        super(melAuthService, workingDirectory, serviceTypeId, uuid, driveSettings);
        try {
            conflictHelper = new InitialIndexConflictHelper(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected ExecutorService createExecutorService(ThreadFactory threadFactory) {
        return Executors.newCachedThreadPool(threadFactory);
    }


    @Override
    protected void onSyncReceived(Request request) {
        try {
            addJob(new SyncClientJob());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onTransfersDone() {
        super.onTransfersDone();
        // check if we stored not-available transfers. if so ask the server if they are available now
        syncHandler.askForAvailableTransfers();
    }

    @Override
    protected boolean workWorkWork(Job unknownJob) {
        Lok.debug(melAuthService.getName() + ".MelDriveClientService.workWorkWork :)");
        if (unknownJob instanceof ServiceRequestHandlerJob) {
            ServiceRequestHandlerJob job = (ServiceRequestHandlerJob) unknownJob;
            if (job.isRequest()) {
                Request request = job.getRequest();

            } else if (job.isMessage()) {
                if (job.getIntent().equals(DriveStrings.INTENT_PROPAGATE_NEW_VERSION)) {
//                    DriveDetails driveDetails = (DriveDetails) job.getPayLoad();
//                    driveDetails.getLastSyncVersion();
                    try {
                        addJob(new SyncClientJob());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (job.getIntent().equals(DriveStrings.INTENT_HASH_AVAILABLE)) {
                    AvailableHashes availableHashes = (AvailableHashes) job.getPayLoad();
                    syncHandler.updateHashes(availableHashes.getHashes());
                    availableHashes.clear();
                    //addJob(new UpdateHashesJob(availableHashes.getHashes()));
                }
            }
        } else if (unknownJob instanceof Job.CertificateSpottedJob) {
            Job.CertificateSpottedJob spottedJob = (Job.CertificateSpottedJob) unknownJob;
            //check if connected certificate is the server. if so: sync()
            if (driveSettings.getClientSettings().getServerCertId().equals(spottedJob.getPartnerCertificate().getId().v())) {
                try {
                    // reset the remaining transfers so we can start again
                    P.confine(driveDatabaseManager.getTransferDao())
                            .run(() -> driveDatabaseManager.getTransferDao().flagStateForRemainingTransfers(driveSettings.getClientSettings().getServerCertId(), driveSettings.getClientSettings().getServerServiceUuid(), TransferState.NOT_STARTED))
                            .end();
                    addJob(new SyncClientJob());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        } else if (unknownJob instanceof CommitJob) {
            syncHandler.execCommitJob((CommitJob) unknownJob);
            return true;
        } else if (unknownJob instanceof Job.ConnectionAuthenticatedJob) {
            Job.ConnectionAuthenticatedJob authenticatedJob = (Job.ConnectionAuthenticatedJob) unknownJob;
            if (authenticatedJob.getPartnerCertificate().getId().v().equals(driveSettings.getClientSettings().getServerCertId())) {
                N.r(() -> addJob(new CommitJob(true)));
            }
            return true;
        } else if (unknownJob instanceof SyncClientJob) {
            syncJobCount.decrementAndGet();
            N.r(() -> syncHandler.syncFromServer());
        }
        return false;
    }

    private AtomicInteger syncJobCount = new AtomicInteger(0);

    @Override
    public void addJob(Job job) {
        if (job instanceof SyncClientJob) {
            int count = syncJobCount.incrementAndGet();
            if (count > 1) {
                Lok.debug("MelDriveClientService.addJob: SyncJob already in progress. skipping.");
                syncJobCount.decrementAndGet();
                return;
            }
        }
        super.addJob(job);
    }

    @Override
    public void onIndexerDone() {

    }


    @Override
    protected ClientSyncHandler initSyncHandler() {
        return new ClientSyncHandler(melAuthService, this);
    }

    public void syncThisClient() {
        addJob(new SyncClientJob());
    }

    @Override
    public DeferredObject<DeferredRunnable, Exception, Void> startIndexer() throws SqlQueriesException {
        super.startIndexer();
        stageIndexer.setStagingDoneListener(stageSetId -> {
            Lok.debug("committed stageset " + stageSetId);
            addJob(new CommitJob());
        });
        return startIndexerDonePromise;
    }

    @Override
    protected IndexListener createIndexListener() {
        return new IndexListener() {


            @Override
            public void done(Long stageSetId, Warden warden) {
                //addJob(new CommitJob(true));
            }
        };
    }

    @Override
    public void onServiceRegistered() {
    }


    public synchronized void onConflicts() {
        Lok.debug("MelDriveClientService.onConflicts.oj9h034800");
        if (latestConflictNotification != null) {
            latestConflictNotification.cancel();
        }
        latestConflictNotification = new MelNotification(uuid, DriveStrings.Notifications.INTENTION_CONFLICT_DETECTED
                , "Conflict detected!"
                , "Click to solve!").setUserCancelable(false);
        melAuthService.onNotificationFromService(this, latestConflictNotification);
    }

    /**
     * merged {@link StageSet}s might be handled by the {@link de.mel.drive.service.sync.SyncHandler} at some point.
     * eg. it still might show some GUI handling {@link Conflict}s.
     *
     * @param lStageSetId
     * @param rStageSetId
     * @param mergedStageSet
     */
    public void onStageSetsMerged(Long lStageSetId, Long rStageSetId, StageSet mergedStageSet) {
        syncHandler.onStageSetsMerged(lStageSetId, rStageSetId, mergedStageSet);
    }

    public Map<String, ConflictSolver> getConflictSolverMap() {
        return syncHandler.getConflictSolverMap();
    }

    @Override
    public void start() {
        super.start();
        addJob(new CommitJob(true));
    }

    @Override
    public void onBootLevel2Finished() {
        Lok.debug("MelDriveClientService.onServiceRegistered");
        N.r(() -> {
            Long serverId = driveSettings.getClientSettings().getServerCertId();
            if (serverId != null) {
                melAuthService.connect(serverId).done(result1 -> addJob(new CommitJob(true)));
            }
        });
        N.r(() -> startedPromise.resolve(this));
    }

    public void onInsufficientSpaceAvailable(Long stageSetId) {
        MelNotification melNotification = new MelNotification(getUuid(), DriveStrings.Notifications.INTENTION_OUT_OF_SPACE, "Out of (Disk) Space", "could not apply changes(" + stageSetId + ") from server");
        melAuthService.onNotificationFromService(this, melNotification);
    }
}