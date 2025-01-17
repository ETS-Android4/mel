package de.mel.filesync.service.sync;

import de.mel.Lok;
import de.mel.auth.data.cached.CachedInitializer;
import de.mel.auth.data.db.Certificate;
import de.mel.auth.file.AbstractFile;
import de.mel.auth.service.MelAuthService;
import de.mel.auth.socket.MelValidationProcess;
import de.mel.auth.socket.process.val.Request;
import de.mel.auth.tools.N;
import de.mel.auth.tools.Order;
import de.mel.auth.tools.WaitLock;
import de.mel.auth.tools.lock.P;
import de.mel.auth.tools.lock.Warden;
import de.mel.core.serialize.serialize.tools.OTimer;
import de.mel.filesync.FileSyncSyncListener;
import de.mel.filesync.data.*;
import de.mel.filesync.data.conflict.ConflictSolver;
import de.mel.filesync.jobs.CommitJob;
import de.mel.filesync.jobs.SyncClientJob;
import de.mel.filesync.quota.OutOfSpaceException;
import de.mel.filesync.service.MelFileSyncClientService;
import de.mel.filesync.sql.*;
import de.mel.filesync.sql.dao.FsDao;
import de.mel.filesync.sql.dao.StageDao;
import de.mel.filesync.sql.dao.TransferDao;
import de.mel.filesync.tasks.AvailHashEntry;
import de.mel.filesync.tasks.AvailableHashesContainer;
import de.mel.filesync.tasks.SyncRequest;
import de.mel.sql.ISQLResource;
import de.mel.sql.SqlQueriesException;

import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.io.IOException;
import java.util.*;

/**
 * Created by xor on 10/27/16.
 */
@SuppressWarnings("Duplicates")
public class ClientSyncHandler extends SyncHandler {

    private final FileSyncClientSettingsDetails clientSettings;
    private FileSyncSyncListener syncListener;
    private MelFileSyncClientService melDriveService;
    private Map<String, ConflictSolver> conflictSolverMap = new keks<>();
    private Map<Long, Set<ConflictSolver>> relatedSolvers = new HashMap<>();

    public void updateHashes(Set<String> hashes) {
        FsDao fsDao = fileSyncDatabaseManager.getFsDao();
        StageDao stageDao = fileSyncDatabaseManager.getStageDao();
        TransferDao transferDao = fileSyncDatabaseManager.getTransferDao();
        Warden warden = P.confine(fsDao);
        N.forEach(hashes, s -> {
            // if is stage from server or is transfer -> flag as available
            N.forEach(stageDao.getUpdateStageSetsFromServer(), stageSet -> {
                stageDao.devUpdateSyncedByHashSet(stageSet.getId().v(), hashes);
            });
            FileSyncClientSettingsDetails clientSettings = fileSyncSettings.getClientSettings();
            transferDao.updateAvailableByHashSet(clientSettings.getServerCertId(), clientSettings.getServerServiceUuid(), hashes);
        });
        warden.end();
        transferManager.research();
    }

    private boolean debugFlag = true;

    public void askForAvailableTransfers() {
        try {
            if (fileSyncSettings.getClientSettings().getServerCertId() == null)
                return;
            Lok.debug("asking for new available hashes....");
            TransferDao transferDao = fileSyncDatabaseManager.getTransferDao();
            ISQLResource<DbTransferDetails> transfers = transferDao.getNotStartedNotAvailableTransfers(fileSyncSettings.getClientSettings().getServerCertId());
            AvailableHashesContainer availableHashesTask = new AvailableHashesContainer(melDriveService.getCacheDirectory(), CachedInitializer.randomId(), FileSyncSettings.CACHE_LIST_SIZE);
            availableHashesTask.setIntent(FileSyncStrings.INTENT_ASK_HASHES_AVAILABLE);
            N.readSqlResource(transfers, (sqlResource, transfer) -> {
                availableHashesTask.add(new AvailHashEntry(transfer.getHash().v()));
            });
            // uncomment to see some communication happen. but be warned: will cause infinite loop!
//            N.r(() -> {
//                availableHashesTask.add(new AvailHashEntry("51037a4a37730f52c8732586d3aaa316"));
//                availableHashesTask.add(new AvailHashEntry("238810397cd86edae7957bca350098bc"));
//                availableHashesTask.add(new AvailHashEntry("fdcbc1aca23cfebaa128bac31df20969"));
//            });

            if (availableHashesTask.getSize() > 0 && debugFlag) {
                melAuthService.connect(fileSyncSettings.getClientSettings().getServerCertId())
                        .done(mvp -> {
                            N.r(() -> {
                                mvp.request(fileSyncSettings.getClientSettings().getServerServiceUuid(), availableHashesTask)
                                        .done(result -> N.r(() -> {
                                            debugFlag = false;
                                            Lok.debug("got available hashes.");
                                            AvailableHashesContainer availHashesContainer = (AvailableHashesContainer) result;
                                            availHashesContainer.toDisk();
                                            availHashesContainer.loadFirstCached();
                                            N.forEachIgnorantly(availHashesContainer, hashEntry -> {
                                                transferDao.flagNotStartedHashAvailable(hashEntry.getHash());
                                            });
                                            availHashesContainer.cleanUp();
                                            if (availHashesContainer.getSize() > 0)
                                                transferManager.research();
                                        }));
                                availableHashesTask.cleanUp();
                            });
                        }).fail(result -> {
                    availableHashesTask.cleanUp();
                    Lok.debug("fail");
                });
            } else {
                availableHashesTask.cleanUp();
            }

        } catch (SqlQueriesException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class keks<K, V> extends HashMap<K, V> {
        @Override
        public V put(K key, V value) {
            return super.put(key, value);
        }
    }

    public void setSyncListener(FileSyncSyncListener syncListener) {
        this.syncListener = syncListener;
    }


    public ClientSyncHandler(MelAuthService melAuthService, MelFileSyncClientService melDriveService) {
        super(melAuthService, melDriveService);
        this.melDriveService = melDriveService;
        this.clientSettings = melDriveService.getFileSyncSettings().getClientSettings();
    }


    /**
     * Sends the StageSet to the server and updates it with the FsIds provided by the server.
     * blocks until server has answered or the attempt failed.
     *
     * @param stageSetId
     * @throws SqlQueriesException
     */
    @SuppressWarnings("unchecked")
    private void syncToServerLocked(Long stageSetId) throws SqlQueriesException, InterruptedException {
        // stage is complete. first lock on FS
        StageDao stageDao = fileSyncDatabaseManager.getStageDao();
        Warden warden = P.confine(P.read(stageDao));
        try {
            if (stageDao.stageSetHasContent(stageSetId)) {
                //all other stages we can find at this point are complete/valid and wait at this point.
                //todo conflict checking goes here - has to block
                Promise<MelValidationProcess, Exception, Void> connected = melAuthService.connect(clientSettings.getServerCertId());
                connected.done(mvp -> warden.run(() -> {
                    // load to cached data structure
                    StageSet stageSet = stageDao.getStageSetById(stageSetId);
                    Commit commit = new Commit(melDriveService.getCacheDirectory(), CachedInitializer.randomId(), FileSyncSettings.CACHE_LIST_SIZE, melDriveService.getUuid());
                    N.readSqlResource(fileSyncDatabaseManager.getStageDao().getStagesByStageSetForCommitResource(stageSetId), (sqlResource, stage) -> commit.add(stage));
                    commit.setBasedOnVersion(stageSet.getBasedOnVersion().v());
                    commit.setIntent(FileSyncStrings.INTENT_COMMIT);
                    Request committed = mvp.request(clientSettings.getServerServiceUuid(), commit);
                    committed.done(res -> warden.run(() -> {
                        CommitAnswer answer = (CommitAnswer) res;
                        for (Long stageId : answer.getStageIdFsIdMap().keySet()) {
                            Long fsId = answer.getStageIdFsIdMap().get(stageId);
                            Stage stage = stageDao.getStageById(stageId);
                            stage.setFsId(fsId);
                            if (stage.getParentId() != null && stage.getFsParentId() == null) {
                                Long fsParentId = answer.getStageIdFsIdMap().get(stage.getParentId());
                                stage.setFsParentId(fsParentId);
                            }
                            stageDao.update(stage);
                        }
                        stageSet.setStatus(FileSyncStrings.STAGESET_STATUS_STAGED);
                        stageSet.setSource(FileSyncStrings.STAGESET_SOURCE_SERVER);
                        commitStage(stageSetId, warden);
                        researchTransfers();
                        transferManager.research();
                        warden.end();
                    })).fail(result -> warden.run(() -> {
                        if (result instanceof TooOldVersionException) {
                            Lok.debug("ClientSyncHandler.syncWithServer");
                            N.r(() -> {
                                // check if the new server version is already present
                                TooOldVersionException tooOldVersionException = (TooOldVersionException) result;
                                Lok.debug("ClientSyncHandler.syncWithServer.TooOldVersionException");
                                Long latestVersion = fileSyncDatabaseManager.getLatestVersion();
                                if (latestVersion < tooOldVersionException.getNewVersion()) {
                                    latestVersion = stageDao.getLatestStageSetVersion();
                                    if (latestVersion == null || latestVersion < tooOldVersionException.getNewVersion()) {
                                        melDriveService.addJob(new SyncClientJob(tooOldVersionException.getNewVersion()));
                                    }
                                }
                                Lok.debug("ClientSyncHandler.syncWithServer");
                            });
                        }
                        warden.end();
                    }));
                    warden.after(commit::cleanUp);
                })).fail(result -> warden.run(() -> {
                    // todo server did not commit. it probably had a local change. have to solve it here
                    Exception ex = result;
                    System.err.println("MelDriveClientService.startIndexer.could not connect :( due to: " + ex.getMessage());
                    warden.end();
                    melDriveService.onSyncFailed();
                }));
            } else {
                stageDao.deleteStageSet(stageSetId);
            }
        } finally {
        }
    }

    /**
     * Merges all staged StageSets (from file system) and checks the result for conflicts
     * with the stage from the server (if it exists).
     * If conflicts are resolved, server StageSet is committed.
     * if a single staged StageSet from file system remains it is send to the server
     * and a new CommitJob added to the MelDriveServices working queue.
     *
     * @param commitJob
     */
    public void execCommitJob(CommitJob commitJob) {
        Lok.debug("ClientSyncHandler.execCommitJob");
        Warden warden = P.confine(P.read(fsDao));
        try {
            // first wait until every staging stuff is finished.

            List<StageSet> stagedFromFs = stageDao.getStagedStageSetsFromFS();
            Lok.debug("ClientSyncHandler.execCommitJob");

            // first: merge everything which has been analysed by the indexer
            mergeStageSets(stagedFromFs);

            stagedFromFs = stageDao.getStagedStageSetsFromFS();
            if (stagedFromFs.size() > 1) {
                melDriveService.addJob(new CommitJob());
                return;
            } else if (stagedFromFs.size() == 0 && commitJob.getSyncAnyway()) {
                melDriveService.addJob(new SyncClientJob());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            warden.end();
        }
        warden = P.confine(fsDao);
        try {

            // conflict check

            // if no conflict occurred, commit if we all staged StageSets have been merged
            List<StageSet> updateSets = stageDao.getUpdateStageSetsFromServer();
            List<StageSet> stagedFromFs = stageDao.getStagedStageSetsFromFS();
            // List<StageSet> committedStageSets = stageDao.getCommittedStageSets();
            if (updateSets.size() > 1) {
                System.err.println("ClientSyncHandler.execCommitJob.something went seriously wrong");
                stageDao.deleteServerStageSets();
                melDriveService.addJob(new SyncClientJob());
                return;
            }
            if (updateSets.size() == 1 && stagedFromFs.size() == 1) {
                // method should create a new CommitJob with conflict solving details
                handleConflict(updateSets.get(0), stagedFromFs.get(0), warden);
                Lok.debug("setupTransfers() was here before");
                //transferManager.research();
                return;
            } else if (stagedFromFs.size() == 1) {
                //method should create a new CommitJob ? method blocks
                syncToServerLocked(stagedFromFs.get(0).getId().v());
                return;
            } else if (stagedFromFs.size() > 1) {
                // merge again
                melDriveService.addJob(new CommitJob());
                return;
            } else if (commitJob.getSyncAnyway() && stagedFromFs.size() == 0 && updateSets.size() == 0) {
                syncFromServer();
                return;
            } else if (updateSets.size() == 1) {
                StageSet stageSet = updateSets.get(0);
                if (stageSet.getSource().equalsValue(FileSyncStrings.STAGESET_SOURCE_SERVER)) {
                    Warden finalWarden = warden;
                    N.r(() -> commitStage(stageSet.getId().v(), finalWarden));
                    researchTransfers();
                    transferManager.research();
                }
            } else if (commitJob.getSyncAnyway() && stagedFromFs.size() == 0 && updateSets.size() == 0) {
                syncFromServer();
                return;
            }


            // lets assume that everything went fine!
            boolean hasCommitted = false;
            for (StageSet stageSet : updateSets) {
                if (stageDao.stageSetHasContent(stageSet.getId().v())) {
                    try {
                        commitStage(stageSet.getId().v(), warden);
                        researchTransfers();
                        hasCommitted = true;
                    } catch (OutOfSpaceException e) {
                        e.printStackTrace();
                        melDriveService.onInsufficientSpaceAvailable(stageSet.getId().v());
                    }
                } else
                    stageDao.deleteStageSet(stageSet.getId().v());
            }
            // new job in case we have solved conflicts in this run.
            // they must be committed to the server
            if (hasCommitted)
                melDriveService.addJob(new CommitJob());
            // we are done here
            melDriveService.onSyncDone();
        } catch (SqlQueriesException | InterruptedException e) {
            e.printStackTrace();
            melDriveService.onSyncFailed();
        } finally {
            warden.end();
        }

    }

    public void handleConflict(StageSet serverStageSet, StageSet stagedFromFs, Warden warden) throws SqlQueriesException {
        handleConflict(serverStageSet, stagedFromFs, warden, null);
    }


    /**
     * check whether or not there are any conflicts between stuff that happened on this computer and stuff
     * that happened on the server. this will block until all conflicts are resolved.
     *
     * @param serverStageSet
     * @param stagedFromFs
     */
    public void handleConflict(StageSet serverStageSet, StageSet stagedFromFs, Warden warden, String conflictHelperUuid) throws SqlQueriesException {
        String identifier = ConflictSolver.createIdentifier(serverStageSet.getId().v(), stagedFromFs.getId().v());
        ConflictSolver conflictSolver;
        // check if there is a solved ConflictSolver available. if so, use it. if not, make a new one.
        if (conflictSolverMap.containsKey(identifier)) {
            conflictSolver = conflictSolverMap.get(identifier);
            if (conflictSolver.isSolved()) {
                iterateStageSets(serverStageSet, stagedFromFs, null, conflictSolver);
                conflictSolver.setSolving(false);
            } else {
                System.err.println(getClass().getSimpleName() + ".handleConflict(): conflict " + identifier + " was not resolved");
            }
        } else {
            conflictSolver = new ConflictSolver(fileSyncDatabaseManager, serverStageSet, stagedFromFs);
            conflictSolver.beforeStart(serverStageSet);
            iterateStageSets(serverStageSet, stagedFromFs, conflictSolver, null);
        }
        // only remember the conflict solver if it actually has conflicts
        if (!conflictSolver.isSolving()) {
            if (conflictSolver.hasConflicts()) {
                System.err.println("conflicts!!!!1!");
                if (conflictHelperUuid != null)
                    conflictSolver.setConflictHelperUuid(conflictHelperUuid);
                putConflictSolver(conflictSolver);
                conflictSolver.setSolving(true);
                melDriveService.onConflicts();
            } else {
                // todo FsDir hash conflicts
                conflictSolver.directoryStuff();
                try {
                    this.commitStage(serverStageSet.getId().v(), warden);
                    this.deleteObsolete(conflictSolver);
                } catch (OutOfSpaceException e) {
                    e.printStackTrace();
                    melDriveService.onInsufficientSpaceAvailable(serverStageSet.getId().v());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                conflictSolver.cleanup();
                Long mergedId = conflictSolver.getMergeStageSet().getId().v();
                this.minimizeStage(mergedId);
                if (stageDao.stageSetHasContent(mergedId))
                    melDriveService.addJob(new CommitJob());
                else {
                    stageDao.deleteStageSet(mergedId);
                }
            }
        }
    }

    private void deleteObsolete(ConflictSolver conflictSolver) throws SqlQueriesException, IOException {
        N.readSqlResource(stageDao.getObsoleteFileStagesResource(conflictSolver.getObsoleteStageSet().getId().v()), (sqlResource, stage) -> {
            AbstractFile file = stageDao.getFileByStage(stage);
            if (file != null && file.exists()) {
                wastebin.deleteUnknown(file);
            }
        });
        N.readSqlResource(stageDao.getObsoleteDirStagesResource(conflictSolver.getObsoleteStageSet().getId().v()), (sqlResource, stage) -> {
            AbstractFile file = stageDao.getFileByStage(stage);
            if (file != null && file.exists()) {
                wastebin.deleteUnknown(file);
            }
        });
        stageDao.deleteStageSet(conflictSolver.getObsoleteStageSet().getId().v());
    }

    private void putConflictSolver(ConflictSolver conflictSolver) {
        // search for related ConflictSolvers before. they deprecate as we insert the new one.
        deleteRelated(conflictSolver.getServerStageSet().getId().v());
        deleteRelated(conflictSolver.getLocalStageSet().getId().v());
        addRelated(conflictSolver, conflictSolver.getLocalStageSet().getId().v());
        addRelated(conflictSolver, conflictSolver.getServerStageSet().getId().v());
        conflictSolverMap.put(conflictSolver.getIdentifier(), conflictSolver);
    }

    private void addRelated(ConflictSolver solver, Long stageSetId) {
        if (relatedSolvers.containsKey(stageSetId)) {
            relatedSolvers.get(stageSetId).add(solver);
        } else {
            Set<ConflictSolver> set = new HashSet<>();
            set.add(solver);
            relatedSolvers.put(stageSetId, set);
        }
    }

    private void deleteRelated(Long stageSetId) {
        if (relatedSolvers.containsKey(stageSetId)) {
            Set<ConflictSolver> solvers = relatedSolvers.remove(stageSetId);
            for (ConflictSolver solver : solvers) {
                ConflictSolver relatedSolver = conflictSolverMap.remove(solver.getIdentifier());
                N.r(relatedSolver::cleanup);
            }
        }
    }

    private void minimizeStage(Long stageSetId) throws SqlQueriesException {
        if (stageSetId == 9)
            Lok.warn("debug");
        stageDao.deleteIdenticalToFs(stageSetId);
    }

    /**
     * merges two {@link StageSet}s to a new one
     *
     * @param stageSets
     * @throws SqlQueriesException
     */
    private void mergeStageSets(List<StageSet> stageSets) throws SqlQueriesException {
        if (stageSets.size() > 2)
            System.err.println("ClientSyncHandler.mergeStageSets.TOO MANY!!!1");
        if (stageSets.size() <= 1)
            return;
        StageSet lStageSet = stageSets.get(0);
        StageSet rStageSet = stageSets.get(1);
        long basedOnVersion = lStageSet.getBasedOnVersion().v() >= rStageSet.getBasedOnVersion().v() ? lStageSet.getBasedOnVersion().v() : rStageSet.getBasedOnVersion().v();
        Lok.debug("ClientSyncHandler.mergeStageSets L: " + lStageSet.getId().v() + " R: " + rStageSet.getId().v());
        StageSet mStageSet = stageDao.createStageSet(FileSyncStrings.STAGESET_SOURCE_MERGED, lStageSet.getOriginCertId().v(), lStageSet.getOriginServiceUuid().v()
                , basedOnVersion
                , lStageSet.getVersion().v());
        final Long mStageSetId = mStageSet.getId().v();
        SyncStageMerger merger = new SyncStageMerger(lStageSet.getId().v(), rStageSet.getId().v()) {
            private Order order = new Order();
            private Map<Long, Long> idMapRight = new HashMap<>();
            private Map<Long, Long> idMapLeft = new HashMap<>();

            @Override
            public void stuffFound(Stage left, Stage right, AbstractFile lFile, AbstractFile rFile) throws SqlQueriesException {
                if (left != null) {
                    if (right != null) {
                        Stage stage = new Stage().setOrder(order.ord()).setStageSet(mStageSetId);
                        stage.mergeValuesFrom(right);
                        // do not forget to relate to the parent!
                        if (idMapRight.containsKey(right.getParentId()))
                            stage.setParentId(idMapRight.get(right.getParentId()));
                        stageDao.insert(stage);
                        idMapRight.put(right.getId(), stage.getId());
                        idMapLeft.put(left.getId(), stage.getId());
                        stageDao.flagMerged(right.getId(), true);
                    } else {
                        // only merge if file exists
//                        AFile fLeft = stageDao.getFileByStage(left);
//                        if (fLeft.exists() || left.getSynced()) {
                        Stage stage = new Stage().setOrder(order.ord()).setStageSet(mStageSetId);
                        stage.mergeValuesFrom(left);
                        if (idMapLeft.containsKey(left.getParentId()))
                            stage.setParentId(idMapLeft.get(left.getParentId()));
                        stageDao.insert(stage);
                        idMapLeft.put(left.getId(), stage.getId());
//                        }
                    }
                } else {
                    if (right != null) {
                        if (right.getParentId() == null) {
                            // stage is completely unconnected to whatever is on the left side
                            Stage stage = new Stage().setOrder(order.ord()).setStageSet(mStageSetId);
                            stage.mergeValuesFrom(right);
                            stageDao.insert(stage);
                            stageDao.flagMerged(right.getId(), true);
                        } else {
                            /**
                             * We're iterating top down through the StageSet.
                             * We probably will find the parent on the left side and therefore have
                             * to "attach" our new left Stage to it.
                             */
                            Stage stage = new Stage().setOrder(order.ord()).setStageSet(mStageSetId);
                            stage.mergeValuesFrom(right);
                            if (right.getParentId() == null) {
                                stageDao.insert(stage);
                            } else {
                                Stage rParent = stageDao.getStageById(right.getParentId());
                                AbstractFile rParentFile = stageDao.getFileByStage(rParent);
                                Stage lParent = stageDao.getStageByPath(mStageSetId, rParentFile);
                                if (lParent != null) {
                                    stage.setParentId(lParent.getId());
                                } else {
                                    System.err.println("ClientSyncHandler.stuffFound.8c8h38h9");
                                }
                                stageDao.insert(stage);
                            }
                            idMapRight.put(right.getId(), stage.getId());
                        }
                    }
                }
            }
        };
        iterateStageSets(lStageSet, rStageSet, merger, null);
        stageDao.deleteStageSet(rStageSet.getId().v());
        stageDao.deleteStageSet(lStageSet.getId().v());
        stageDao.updateStageSet(mStageSet.setStatus(FileSyncStrings.STAGESET_STATUS_STAGED).setSource(FileSyncStrings.STAGESET_SOURCE_FS));
        // tell the service which StageSets had been merged
        melDriveService.onStageSetsMerged(lStageSet.getId().v(), rStageSet.getId().v(), mStageSet);
    }

    /**
     * iterates over the left {@link StageSet} first and finds
     * relating entries in the right {@link StageSet} and flags everything it finds (in the right StageSet).
     * Afterwards it iterates over all non flagged {@link Stage}s of the right {@link StageSet}.
     * Iteration is in order of the Stages insertion.
     *
     * @param lStageSet
     * @param rStageSet
     * @param merger
     * @throws SqlQueriesException
     */
    @SuppressWarnings("Duplicates")
    public void iterateStageSets(StageSet lStageSet, StageSet rStageSet, SyncStageMerger merger, ConflictSolver conflictSolver) throws SqlQueriesException {
        OTimer timer1 = new OTimer("iter 1");
        OTimer timer2 = new OTimer("iter 2");
        OTimer timer3 = new OTimer("iter 3");
        N.sqlResource(stageDao.getStagesResource(lStageSet.getId().v()), lStages -> {
            Stage lStage = lStages.getNext();
            while (lStage != null) {
                timer1.start();
                AbstractFile lFile = stageDao.getFileByStage(lStage);
                timer1.stop();
                timer2.start();
                Stage rStage = stageDao.getStageByPath(rStageSet.getId().v(), lFile);
                timer2.stop();
                if (conflictSolver != null)
                    conflictSolver.solve(lStage, rStage);
                else {
                    timer3.start();
                    AbstractFile rFile = (rStage != null) ? AbstractFile.instance(lFile.getAbsolutePath()) : null;
                    timer3.stop();
                    merger.stuffFound(lStage, rStage, lFile, rFile);
                }
                if (rStage != null)
                    stageDao.flagMerged(rStage.getId(), true);
                lStage = lStages.getNext();
            }
        });
        timer1.print().reset();
        timer2.print().reset();
        timer3.print().reset();
        N.sqlResource(stageDao.getNotMergedStagesResource(rStageSet.getId().v()), rStages -> {
            Stage rStage = rStages.getNext();
            while (rStage != null) {
                if (conflictSolver != null)
                    conflictSolver.solve(null, rStage);
                else {
                    AbstractFile rFile = stageDao.getFileByStage(rStage);
                    merger.stuffFound(null, rStage, null, rFile);
                }
                rStage = rStages.getNext();
            }
        });
    }

    /**
     * pulls StageSet from the Server. locks.
     *
     * @throws SqlQueriesException
     */
    public void syncFromServer() throws SqlQueriesException, InterruptedException {
        Lok.debug();
        WaitLock waitLock = new WaitLock();
        runner.runTry(() -> {
            stageDao.deleteServerStageSets();
            waitLock.unlock();
        });
        Certificate serverCert = melAuthService.getCertificateManager().getTrustedCertificateById(clientSettings.getServerCertId());
        Promise<MelValidationProcess, Exception, Void> connected = melAuthService.connect(serverCert.getId().v());
        connected.done(mvp -> runner.runTry(() -> {
            long oldeSyncedVersion = fileSyncDatabaseManager.getFileSyncSettings().getLastSyncedVersion();
            StageSet stageSet = stageDao.createStageSet(FileSyncStrings.STAGESET_SOURCE_SERVER, clientSettings.getServerCertId(), clientSettings.getServerServiceUuid(), null, oldeSyncedVersion);
            SyncRequest sentSyncRequest = new SyncRequest()
                    .setOldVersion(oldeSyncedVersion);
            sentSyncRequest.setIntent(FileSyncStrings.INTENT_SYNC);
            Request<SyncAnswer> request = mvp.request(clientSettings.getServerServiceUuid(), sentSyncRequest);
            request.done(syncAnswer -> runner.runTry(() -> {
                try {
                    syncAnswer.setStageSet(stageSet);
                    //server might have got a new version in the meantime and sent us that
                    stageSet.setVersion(syncAnswer.getNewVersion());
                    stageSet.setBasedOnVersion(syncAnswer.getOldVersion());
                    stageDao.updateStageSet(stageSet);
                    Promise<Long, Void, Void> promise = this.sync2Stage(syncAnswer);
                    promise.done(nil -> runner.runTry(() -> {
                        try {
                            melDriveService.addJob(new CommitJob());
                            if (syncListener != null)
                                syncListener.onSyncDone();
                        } finally {
                            waitLock.unlock();
                        }
                    })).fail(result -> {
                                try {
                                    System.err.println("ClientSyncHandler.syncFromServer.j99f49459f54");
                                    N.r(() -> stageDao.deleteStageSet(stageSet.getId().v()));
                                } finally {
                                    waitLock.unlock();
                                }
                            }
                    );
                } finally {
                    waitLock.unlock();
                }
            })).fail(result -> waitLock.unlock());
        })).fail(result -> waitLock.unlock());

        Lok.debug("waiting to finish");
        waitLock.lock().lock();
        Lok.debug("done waiting");
    }

    private void insertWithParentId(Map<Long, Long> entryIdStageIdMap, GenericFSEntry genericFSEntry, Stage stage) throws SqlQueriesException {
        if (entryIdStageIdMap.containsKey(genericFSEntry.getParentId().v())) {
            stage.setParentId(entryIdStageIdMap.get(genericFSEntry.getParentId().v()));
        }
        stageDao.insert(stage);
        entryIdStageIdMap.put(genericFSEntry.getId().v(), stage.getId());
    }

    /**
     * delta goes in here
     *
     * @param syncAnswer contains delta
     * @return stageSetId in Promise
     * @throws SqlQueriesException
     */
    private Promise<Long, Void, Void> sync2Stage(SyncAnswer syncAnswer) throws SqlQueriesException, InterruptedException {
        DeferredObject<Void, Void, Void> communicationDone = new DeferredObject<>();
        DeferredObject<Long, Void, Void> finished = new DeferredObject<>();
        Map<Long, Long> entryIdStageIdMap = new HashMap<>();
        Order order = new Order();
//        syncAnswer.setCacheDir(melDriveService.getCacheDirectory());
        Iterator<GenericFSEntry> iterator = syncAnswer.iterator();
        if (!iterator.hasNext()) {
            syncAnswer.cleanUp();
            finished.reject(null);
            return finished;
        }
        StageSet stageSet = syncAnswer.getStageSet();
        syncAnswer.setStageSetId(stageSet.getId().v());
        // stage first
        while (iterator.hasNext()) {
            GenericFSEntry genericFSEntry = iterator.next();
            Stage stage = GenericFSEntry.generic2Stage(genericFSEntry, stageSet.getId().v());
            stage.setOrder(order.ord());
            insertWithParentId(entryIdStageIdMap, genericFSEntry, stage);
        }
        syncAnswer.cleanUp();
        // check if something was deleted
        List<Stage> stages = stageDao.getDirectoriesByStageSet(stageSet.getId().v());
        Map<Long, FsDirectory> fsDirIdsToRetrieve = new HashMap<>();
        for (Stage stage : stages) {
            FsDirectory fsDirectory = fsDao.getDirectoryById(stage.getFsId());
            // if dir is not new check if something has been deleted
            if (fsDirectory != null) {
                String oldeHash = fsDirectory.getContentHash().v();
                List<Stage> subStages = stageDao.getSubStagesByFsDirectoryId(fsDirectory.getId().v(), stageSet.getId().v());
                List<GenericFSEntry> subFsEntries = fsDao.getContentByFsDirectory(fsDirectory.getId().v());
                Map<String, GenericFSEntry> fsNameMap = new HashMap<>();
                for (GenericFSEntry gen : subFsEntries) {
                    fsNameMap.put(gen.getName().v(), gen);
                }
                Map<String, Stage> stageNameMap = new HashMap<>();
                for (Stage s : subStages) {
                    stageNameMap.put(s.getName(), s);
                }
                for (GenericFSEntry genSub : subFsEntries) {
                    Stage subStage = stageNameMap.get(genSub.getName().v());
                    if (subStage != null) {
                        stageNameMap.remove(genSub.getName().v());
                        if (!subStage.getDeleted()) {
                            if (subStage.getIsDirectory() && genSub.getIsDirectory().v()) {
                                fsDirectory.addSubDirectory((FsDirectory) genSub.ins());
                            } else if (!subStage.getIsDirectory() && !genSub.getIsDirectory().v()) {
                                fsDirectory.addFile((FsFile) genSub.ins());
                            }
                        }
                    } else {
                        if (genSub.getIsDirectory().v()) {
                            fsDirectory.addSubDirectory((FsDirectory) genSub.ins());
                        } else {

                            fsDirectory.addFile((FsFile) genSub.ins());
                        }
                    }
                }
                for (String key : stageNameMap.keySet()) {
                    Stage newStage = stageNameMap.get(key);
                    if (newStage.getIsDirectory() && !newStage.getDeleted()) {
                        FsDirectory newDir = new FsDirectory();
                        newDir.getName().v(newStage.getName());
                        fsDirectory.addSubDirectory(newDir);
                    } else if (!newStage.getDeleted()) {
                        FsFile newFile = new FsFile();
                        newFile.getName().v(newStage.getName());
                        fsDirectory.addFile(newFile);
                    }
                }
                fsDirectory.calcContentHash();
                String hash = fsDirectory.getContentHash().v();
                if (!hash.equals(stage.getContentHash())) {
                    Lok.debug("ClientSyncHandler.syncFromServer.SOMETHING.DELETED?");
                    fsDirIdsToRetrieve.put(fsDirectory.getId().v(), fsDirectory);
                }
            }
        }
        if (fsDirIdsToRetrieve.size() > 0) {
            Promise<List<FsDirectory>, Exception, Void> promise = melDriveService.requestDirectoriesByIds(fsDirIdsToRetrieve.keySet(), clientSettings.getServerCertId(), clientSettings.getServerServiceUuid());
            promise.done(directories -> runner.runTry(() -> {
                // stage everything not in the directories as deleted
                for (FsDirectory directory : directories) {
                    Set<Long> stillExistingIds = new HashSet<>();
                    for (FsFile fsFile : directory.getFiles())
                        stillExistingIds.add(fsFile.getId().v());
                    for (FsDirectory subDir : directory.getSubDirectories())
                        stillExistingIds.add(subDir.getId().v());
                    List<GenericFSEntry> fsDirs = fsDao.getContentByFsDirectory(directory.getId().v());
                    for (GenericFSEntry genSub : fsDirs) {
                        if (!stillExistingIds.contains(genSub.getId().v())) {
                            // genSub shall be deleted
                            Stage stage = GenericFSEntry.generic2Stage(genSub, stageSet.getId().v());
                            stage.setDeleted(true);
                            stage.setOrder(order.ord());
                            stage.setSynced(false);
                            insertWithParentId(entryIdStageIdMap, genSub, stage);
                            recursiveDeleteOnStage(entryIdStageIdMap, order, genSub, stage);
                        }
                    }
                }
                communicationDone.resolve(null);
            }));
        } else {
            communicationDone.resolve(null);
        }
        communicationDone.done(nul -> {
            stageSet.setStatus(FileSyncStrings.STAGESET_STATUS_STAGED);
            N.r(() -> stageDao.updateStageSet(stageSet));
            reorderStageSet(stageSet);
            finished.resolve(stageSet.getId().v());
        }).fail(nul -> {
            finished.reject(null);
        });
        return finished;
    }

    private void reorderStageSet(StageSet stageSet) {
        Lok.debug("reordering stageset: " + stageSet);
        try {
            // insert missing parent ids first
            stageDao.repairParentIdsByFsParentIds(stageSet.getId().v());
            // find stuff that is in the wrong order
            UnorderedStagePair unorderedStagePair = stageDao.getUnorderedStagePair(stageSet.getId().v());
            while (unorderedStagePair != null) {
                // swap if found
                Lok.debug("swapping (" + unorderedStagePair.getSmallId().v() + "," + unorderedStagePair.getSmallOrder().v()
                        + ") and (" + unorderedStagePair.getBigId().v() + "," + unorderedStagePair.getBigOrder().v() + ")");
                stageDao.updateOrder(unorderedStagePair.getSmallId().v(), unorderedStagePair.getBigOrder().v());
                stageDao.updateOrder(unorderedStagePair.getBigId().v(), unorderedStagePair.getSmallOrder().v());
                unorderedStagePair = stageDao.getUnorderedStagePair(stageSet.getId().v());
            }
        } catch (SqlQueriesException e) {
            e.printStackTrace();
        }
        Lok.debug("reordering done");
    }

    private void recursiveDeleteOnStage(Map<Long, Long> entryIdStageIdMap, Order order, GenericFSEntry generic, Stage stage) throws SqlQueriesException {
        List<GenericFSEntry> content = fsDao.getContentByFsDirectory(generic.getId().v());
        for (GenericFSEntry subGen : content) {
            Stage subStage = GenericFSEntry.generic2Stage(subGen, stage.getStageSet());
            subStage.setDeleted(true);
            subStage.setOrder(order.ord());
            subStage.setParentId(stage.getId());
            if (!subStage.getIsDirectory())
                subStage.setSynced(false);
            stageDao.insert(subStage);
            entryIdStageIdMap.put(subGen.getId().v(), subStage.getId());
            if (subGen.getIsDirectory().v()) {
                recursiveDeleteOnStage(entryIdStageIdMap, order, subGen, subStage);
            }
        }
    }

    public void onStageSetsMerged(Long lStageSetId, Long rStageSetId, StageSet mergedStageSet) {
        for (ConflictSolver solver : conflictSolverMap.values()) {
            solver.checkObsolete(lStageSetId, rStageSetId);
        }
    }

    public Map<String, ConflictSolver> getConflictSolverMap() {
        return conflictSolverMap;
    }
}
