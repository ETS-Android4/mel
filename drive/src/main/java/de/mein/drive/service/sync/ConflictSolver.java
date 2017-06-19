package de.mein.drive.service.sync;

import de.mein.auth.tools.N;
import de.mein.auth.tools.Order;
import de.mein.drive.data.DriveStrings;
import de.mein.drive.data.fs.RootDirectory;
import de.mein.drive.sql.*;
import de.mein.drive.sql.dao.FsDao;
import de.mein.drive.sql.dao.StageDao;
import de.mein.sql.ISQLResource;
import de.mein.sql.SqlQueriesException;

import java.io.File;
import java.util.*;

/**
 * Created by xor on 5/30/17.
 */
public class ConflictSolver extends SyncStageMerger {
    private final String identifier;
    private final StageSet lStageSet, rStageSet;
    private final RootDirectory rootDirectory;
    private Map<String, Conflict> conflicts = new HashMap<>();
    private Order order;
    private Map<Long, Long> oldeNewIdMap;
    private StageDao stageDao;
    private StageSet mergeStageSet;
    private FsDao fsDao;
    protected Map<String, Conflict> deletedParents = new HashMap<>();

    /**
     * will try to merge first. if it fails the merged {@link StageSet} is removed,
     * conflicts are collected and must be resolved elsewhere (eg. ask the user for help)
     *
     * @param left
     * @param right
     * @throws SqlQueriesException
     */
    @Override
    public void stuffFound(Stage left, Stage right, File lFile, File rFile) throws SqlQueriesException {
        if (!(left == null && lFile == null || left != null && lFile != null)) {
            System.err.println("ConflictSolver.stuffFound.e1");
        }
        if (!(right == null && rFile == null || right != null && rFile != null)) {
            System.err.println("ConflictSolver.stuffFound.e2");
        }

        if (left != null && right != null) {
            String key = Conflict.createKey(left, right);
            if (!conflicts.containsKey(key)) {
                //todo oof for deletion
                Conflict conflict = createConflict(left, right);
                if ((left.getIsDirectory() && left.getDeleted()) || (right.getIsDirectory() && right.getDeleted())) {
                    // there might already exist a Conflict. in this case we have more detailed information now (on stage should be null)
                    // and therefor must replace ist but retain the dependencies
                    if (deletedParents.get(lFile.getAbsolutePath()) != null) {
                        Conflict oldeConflict = deletedParents.get(lFile.getAbsolutePath());
                        conflict.dependOn(oldeConflict.getDependsOn());
                    }
                    deletedParents.put(lFile.getAbsolutePath(), conflict);
                }
            }
            return;
        }
        if (left != null) {
            conflictSearch(left, lFile, false);
        }
        if (right != null) {
            conflictSearch(right, rFile, true);
        }
    }

    private void conflictSearch(Stage stage, File stageFile, final boolean stageIsFromRight) {
        File directory;
        if (stage.getIsDirectory())
            directory = stageFile;
        else
            directory = stageFile.getParentFile();

        Set<String> conflictFreePaths = new HashSet<>();
        while (!directory.getAbsolutePath().equals(rootDirectory.getPath())) {
            if (deletedParents.containsKey(directory.getAbsolutePath())) {
                Conflict conflict = deletedParents.get(directory.getAbsolutePath());
                // it is proven that this file is not in conflict
                if (conflict == null) {
                    deletedParents.put(directory.getAbsolutePath(), null);
                    for (String path : conflictFreePaths)
                        deletedParents.put(path, null);
                } else {
                    Conflict dependentConflict = (stageIsFromRight ? createConflict(null, stage) : createConflict(stage, null));
                    dependentConflict.dependOn(conflict);
                }
            } else {
                conflictFreePaths.add(directory.getAbsolutePath());
            }
            directory = directory.getParentFile();
        }
    }


    private Conflict createConflict(Stage left, Stage right) {
        String key = Conflict.createKey(left, right);
        Conflict conflict = new Conflict(stageDao, left, right);
        conflicts.put(key, conflict);
        return conflict;
    }

    public void beforeStart(StageSet remoteStageSet) throws SqlQueriesException {
        order = new Order();
        deletedParents = new HashMap<>();
        oldeNewIdMap = new HashMap<>();
        this.fsDao = fsDao;
        N.r(() -> {
            mergeStageSet = stageDao.createStageSet(DriveStrings.STAGESET_TYPE_FS, remoteStageSet.getOriginCertId().v(), remoteStageSet.getOriginServiceUuid().v());
        });
        // now lets find directories that have been deleted. so we can build nice dependencies between conflicts
        List<Stage> leftDirs = stageDao.getDeletedDirectories(lStageSetId);
        List<Stage> rightDirs = stageDao.getDeletedDirectories(rStageSetId);
        for (Stage stage : leftDirs) {
            Conflict conflict = new Conflict(stageDao, stage, null);
            File f = stageDao.getFileByStage(stage);
            deletedParents.put(f.getAbsolutePath(), conflict);
        }
        for (Stage stage : rightDirs) {
            Conflict conflict = new Conflict(stageDao, null, stage);
            File f = stageDao.getFileByStage(stage);
            deletedParents.put(f.getAbsolutePath(), conflict);
        }
    }

    private void mergeFsDirectoryWithSubStages(FsDirectory fsDirToMergeInto, Stage stageToMergeWith) throws SqlQueriesException {
        List<Stage> content = stageDao.getStageContent(stageToMergeWith.getId(), stageToMergeWith.getStageSet());
        for (Stage stage : content) {
            if (stage.getIsDirectory()) {
                if (stage.getDeleted()) {
                    fsDirToMergeInto.removeSubFsDirecoryByName(stage.getName());
                } else {
                    fsDirToMergeInto.addDummySubFsDirectory(stage.getName());
                }
            } else {
                if (stage.getDeleted()) {
                    fsDirToMergeInto.removeFsFileByName(stage.getName());
                } else {
                    fsDirToMergeInto.addDummyFsFile(stage.getName());
                }
            }
            stageDao.flagMerged(stage.getId(), true);
        }
    }

    /**
     * merges already merged {@link StageSet} with itself and predecessors (Fs->Left->Right) to find parent directories
     * which were not in the right StageSet but in the left. When a directory has changed its content
     * on the left side but not/or otherwise changed on the right
     * it is missed there and has a wrong content hash.
     *
     * @throws SqlQueriesException
     */
    public void directoryStuff() throws SqlQueriesException {
        final long oldeMergedSetId = mergeStageSet.getId().v();
        order = new Order();
        StageSet targetStageSet = stageDao.createStageSet(DriveStrings.STAGESET_TYPE_FS, mergeStageSet.getOriginCertId().v(), mergeStageSet.getOriginServiceUuid().v());
        ISQLResource<Stage> stageSet = stageDao.getStagesResource(oldeMergedSetId);
        Stage rightStage = stageSet.getNext();
        while (rightStage != null) {
            rightStage.setId(null);
            File parentFile = stageDao.getFileByStage(rightStage).getParentFile();
            // first, lets see if we have already created a parent!
            Stage alreadyStagedParent = stageDao.getStageByPath(targetStageSet.getId().v(), parentFile);
            Stage targetParentStage = new Stage()
                    .setName(parentFile.getName())
                    .setStageSet(targetStageSet.getId().v())
                    .setIsDirectory(true);
            if (alreadyStagedParent == null) {
                FsDirectory parentFsDirectory = fsDao.getFsDirectoryByPath(parentFile);
                Stage leftParentStage = stageDao.getStageByPath(lStageSet.getId().v(), parentFile);
                FsDirectory targetParentFsDirectory = new FsDirectory().setName(parentFile.getName());
                // check if right parent already exists. else...
                Stage mergedParentStage = stageDao.getStageByPath(oldeMergedSetId, parentFile);
                // add fs content
                if (parentFsDirectory != null) {
                    List<GenericFSEntry> content = fsDao.getContentByFsDirectory(parentFsDirectory.getId().v());
                    targetParentFsDirectory.addContent(content);
                    targetParentStage.setFsId(parentFsDirectory.getId().v());
                    targetParentStage.setFsParentId(parentFsDirectory.getParentId().v());
                }
                // merge with left content
                if (leftParentStage != null) {
                    mergeFsDirectoryWithSubStages(targetParentFsDirectory, leftParentStage);
                    targetParentStage.setFsId(leftParentStage.getFsId());
                    targetParentStage.setFsParentId(leftParentStage.getFsParentId());
                }
                // merge with right content
                if (mergedParentStage != null) {
                    mergeFsDirectoryWithSubStages(targetParentFsDirectory, mergedParentStage);
                    targetParentStage.setFsId(mergedParentStage.getFsId());
                    targetParentStage.setFsParentId(mergedParentStage.getFsParentId());
                }
                targetParentFsDirectory.calcContentHash();
                // stage if content hash has changed
                if (!targetParentFsDirectory.getContentHash().v().equals(leftParentStage.getContentHash())) {
                    targetParentStage.setOrder(order.ord());
                    stageDao.insert(targetParentStage);
                    rightStage.setParentId(targetParentStage.getId());
                }
            } else {
                // we already staged it!
                rightStage.setParentId(alreadyStagedParent.getId());
            }
            // copy the stage
            rightStage.setStageSet(targetStageSet.getId().v());
            rightStage.setOrder(order.ord());
            stageDao.insert(rightStage);
            rightStage = stageSet.getNext();
        }
        stageDao.deleteStageSet(oldeMergedSetId);
        mergeStageSet = targetStageSet;
    }

    public void cleanup() throws SqlQueriesException {
        //cleanup
        stageDao.deleteStageSet(rStageSetId);
        if (!stageDao.stageSetHasContent(mergeStageSet.getId().v()))
            stageDao.deleteStageSet(mergeStageSet.getId().v());
        mergeStageSet.setStatus(DriveStrings.STAGESET_STATUS_STAGED);
        stageDao.updateStageSet(mergeStageSet);
    }

    public StageSet getMergeStageSet() {
        return mergeStageSet;
    }

    public void solve(Stage left, Stage right) {
        Stage solvedStage = null;
        if (left != null && right != null) {
            String key = Conflict.createKey(left, right);
            if (conflicts.containsKey(key)) {
                Conflict conflict = conflicts.remove(key);
                if (conflict.isRight()) {
                    try {
                        solvedStage = right;
                        solvedStage.setFsParentId(left.getFsParentId());
                        solvedStage.setFsId(left.getFsId());
                        solvedStage.setVersion(left.getVersion());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (left != null) {
            //solvedStage = left;
        } else {
            solvedStage = right;
        }
        if (solvedStage != null) {
            try {
                solvedStage.setOrder(order.ord());
                solvedStage.setStageSet(mergeStageSet.getId().v());
                // adjust ids
                Long oldeId = solvedStage.getId();
                solvedStage.setMerged(false);
                solvedStage.setId(null);
                if (oldeNewIdMap.containsKey(solvedStage.getParentId())) {
                    solvedStage.setParentId(oldeNewIdMap.get(solvedStage.getParentId()));
                }
                stageDao.insert(solvedStage);
                oldeNewIdMap.put(solvedStage.getId(), oldeId);
                // map new id
            } catch (SqlQueriesException e) {
                e.printStackTrace();
            }
        }
    }


    public ConflictSolver(DriveDatabaseManager driveDatabaseManager, StageSet lStageSet, StageSet rStageSet) {
        super(lStageSet.getId().v(), rStageSet.getId().v());
        this.rootDirectory = driveDatabaseManager.getDriveSettings().getRootDirectory();
        this.lStageSet = lStageSet;
        this.rStageSet = rStageSet;
        stageDao = driveDatabaseManager.getStageDao();
        fsDao = driveDatabaseManager.getFsDao();
        identifier = createIdentifier(lStageSet.getId().v(), rStageSet.getId().v());
    }

    public static String createIdentifier(Long lStageSetId, Long rStageSetId) {
        return lStageSetId + ":" + rStageSetId;
    }

    public String getIdentifier() {
        return identifier;
    }


    public boolean isSolved() {
        return conflicts.values().stream().allMatch(Conflict::hasDecision);
    }

    public boolean hasConflicts() {
        return conflicts.size() > 0;
    }

    public Collection<Conflict> getConflicts() {
        return conflicts.values();
    }
}
