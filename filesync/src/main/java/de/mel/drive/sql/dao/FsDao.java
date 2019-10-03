
package de.mel.drive.sql.dao;

import de.mel.Lok;
import de.mel.auth.file.AFile;
import de.mel.auth.tools.N;
import de.mel.auth.tools.RWSemaphore;
import de.mel.drive.data.DriveSettings;
import de.mel.drive.data.fs.RootDirectory;
import de.mel.drive.sql.*;
import de.mel.sql.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class FsDao extends Dao {

    private RWSemaphore rwSemaphore = new RWSemaphore();
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private AtomicInteger rcount = new AtomicInteger(0);
    private AtomicInteger urcount = new AtomicInteger(0);
    private AtomicInteger wcount = new AtomicInteger(0);
    private AtomicInteger uwcount = new AtomicInteger(0);

    public FsEntry getBottomFsEntry(Stack<AFile> fileStack) throws SqlQueriesException {
        if (fileStack.size() == 0) { //&& fileStack[0].length() == 0) {
            return getRootDirectory();
        }
        FsEntry bottomFsEntry = getRootDirectory();
        FsEntry lastFsEntry = null;
        do {
            Long parentId = (lastFsEntry != null) ? lastFsEntry.getId().v() : driveSettings.getRootDirectory().getId();
            lastFsEntry = getGenericSubByName(parentId, fileStack.peek().getName());
            if (lastFsEntry != null) {
                bottomFsEntry = lastFsEntry;
                fileStack.pop();
            }
        } while (lastFsEntry != null && !fileStack.empty());
        return bottomFsEntry;
    }

    private int printLock(String method, AtomicInteger count) {
        int n = count.incrementAndGet();
        Lok.debug("FsDao." + method + "(" + n + ").on " + Thread.currentThread().getName());
        return n;
    }

    private final DriveDatabaseManager driveDatabaseManager;
    private DriveSettings driveSettings;

    public FsDao(DriveDatabaseManager driveDatabaseManager, ISQLQueries ISQLQueries) {
        super(ISQLQueries);
        this.driveDatabaseManager = driveDatabaseManager;
    }


    public void update(FsEntry fsEntry) throws SqlQueriesException {
        List<Object> whereArgs = new ArrayList<>();
        whereArgs.add(fsEntry.getId().v());
        sqlQueries.update(fsEntry, fsEntry.getId().k() + "=?", whereArgs);
    }

    public FsFile getFsFileByName(FsFile fsFile) throws SqlQueriesException {
        Long id = fsFile.getParentId().v();
        String where = "";
        List<Object> whereArguments = new ArrayList<>();
        if (id == null) {
            where = fsFile.getParentId().k() + " is null";
        } else {
            where = fsFile.getParentId().k() + "=?";
            whereArguments.add(id);
        }
        where += " and " + fsFile.getIsDirectory().k() + "=?"
                + " and " + fsFile.getName().k() + "=?";
        whereArguments.add(0);
        whereArguments.add(fsFile.getName().v());
        List<FsFile> tableObjects = sqlQueries.load(fsFile.getAllAttributes(), fsFile, where, whereArguments);
        if (tableObjects.size() == 0) {
            return null;
        } else {
            return tableObjects.get(0);
        }
    }

    public FsFile getFsFileByName(Long dirId, String name) throws SqlQueriesException {
        FsFile fsFile = new FsFile();
        fsFile.getParentId().v(dirId);
        fsFile.getName().v(name);
        return getFsFileByName(fsFile);
    }

    public List<FsFile> getFilesByHash(String hash) throws SqlQueriesException {
        FsFile dummy = new FsFile();
        String where = dummy.getContentHash().k() + "=?";
        List<FsFile> fsFiles = sqlQueries.load(dummy.getAllAttributes(), dummy, where, ISQLQueries.args(hash));
        return fsFiles;
    }

    public List<FsFile> getSyncedFilesByHash(String hash) throws SqlQueriesException {
        FsFile dummy = new FsFile();
        String where = dummy.getContentHash().k() + "=? and " + dummy.getSynced().k() + "=?";
        List<FsFile> fsFiles = sqlQueries.load(dummy.getAllAttributes(), dummy, where, ISQLQueries.args(hash, true));
        return fsFiles;
    }

    public List<FsFile> getFilesByFsDirectory(Long id) throws SqlQueriesException {
        FsFile file = new FsFile();

        String where = "";
        List<Object> whereArguments = new ArrayList<>();
        if (id == null) {
            where = file.getParentId().k() + " is null";
        } else {
            where = file.getParentId().k() + "=?";
            whereArguments.add(id);
        }
        where += " and " + file.getIsDirectory().k() + "=?";
        whereArguments.add(0);
        List<FsFile> result = sqlQueries.load(file.getAllAttributes(), file, where, whereArguments);
        return result;
    }

    public void markFileMissed(FsFile f) throws SqlQueriesException {
        f.getVersion().v((Long) null);
        update(f);
    }


    public FsFile getFile(Long id) throws SqlQueriesException {
        FsFile fsFile = new FsFile();
        String where = fsFile.getId().k() + "=?";
        List<Object> whereArguments = new ArrayList<>();
        whereArguments.add(id);
        List<FsFile> tableObjects = sqlQueries.load(fsFile.getAllAttributes(), fsFile, where, whereArguments);
        if (tableObjects.size() == 0) {
            return null;
        } else {
            return tableObjects.get(0);
        }
    }


    public FsEntry insert(FsEntry fsEntry) throws SqlQueriesException {
        Long id;
        if (fsEntry.getId().notNull())
            id = sqlQueries.insertWithAttributes(fsEntry, fsEntry.getAllAttributes());
        else
            id = sqlQueries.insert(fsEntry);

        fsEntry.getId().v(id);
        return fsEntry;
    }

    public FsFile insertLeFile(FsFile fsFile) throws SqlQueriesException {
        Long id = new Long(sqlQueries.insert(fsFile));
        fsFile.getId().v(id);
        return fsFile;
        //new MetaDao(sqlQueries, lock).updateSyncId(fsFile);
    }

    // directory stuff
    public FsDirectory insertLeDirectory(FsDirectory fsDirectory) throws SqlQueriesException {
        Long id = sqlQueries.insert(fsDirectory);
        fsDirectory.getId().v(id);
        return fsDirectory;
    }

    public List<FsDirectory> getSubDirectoriesByParentId(Long id) throws SqlQueriesException {
        FsDirectory dir = new FsDirectory();

        String where = "";
        List<Object> whereArgs = new ArrayList<>();
        if (id == null) {
            where = dir.getParentId().k() + " is null";
        } else {
            where = dir.getParentId().k() + "=?";
            whereArgs.add(id);
        }
        where += " and " + dir.getIsDirectory().k() + "=?";
        whereArgs.add(1);

        List<FsDirectory> result = sqlQueries.load(dir.getAllAttributes(), dir, where, whereArgs);
        return result;
    }


    public FsDirectory getSubDirectory(FsDirectory parent, FsDirectory directory) throws SqlQueriesException {
        String where = "";
        List<Object> whereArgs = new ArrayList<>();
        if (parent.getParentId().v() == null) {
            where = directory.getParentId().k() + " is null";
        } else {
            where = directory.getParentId().k() + "=?";
            whereArgs.add(parent.getId().v());
        }
        where += " and " + directory.getIsDirectory().k() + "=? and " + directory.getName().k() + "=?";
        whereArgs.add(1);
        whereArgs.add(directory.getName().v());
        List<FsDirectory> result = sqlQueries.load(directory.getAllAttributes(), directory, where, whereArgs);
        if (result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    public FsDirectory getSubDirectoryByName(Long parentId, String name) throws SqlQueriesException {
        FsDirectory directory = new FsDirectory();
        String where = "";
        List<Object> whereArgs = new ArrayList<>();
        where = directory.getParentId().k() + "=?";
        whereArgs.add(parentId);
        where += " and " + directory.getIsDirectory().k() + "=? and " + directory.getName().k() + "=?";
        whereArgs.add(1);
        whereArgs.add(name);
        List<FsDirectory> result = sqlQueries.load(directory.getAllAttributes(), directory, where, whereArgs);
        if (result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    public List<FsDirectory> getSubDirectoriesVersion(Long id, Long version) throws SqlQueriesException {
        FsDirectory dir = new FsDirectory();
        String where = dir.getParentId().k() + "=? and " + dir.getVersion().k() + ">?";
        List<Object> whereArgs = ISQLQueries.args(id, version);
        List<SQLTableObject> result = sqlQueries.load(dir.getAllAttributes(), dir, where, whereArgs);
        List<FsDirectory> dirs = result.stream().map(s -> (FsDirectory) s).collect(Collectors.toList());
        return dirs;
    }

    public List<FsEntry> getDirectoryContent(Long id) throws SqlQueriesException {
        GenericFSEntry fsEntry = new GenericFSEntry();
        String where = fsEntry.getParentId().k() + "=?";
        ArrayList<Object> whereArgs = new ArrayList<>();
        whereArgs.add(id);
        List<FsEntry> result = sqlQueries.load(fsEntry.getAllAttributes(), fsEntry, where, whereArgs);
        return result;
    }


    public FsDirectory getDirectoryById(Long id) throws SqlQueriesException {
        FsDirectory directory = new FsDirectory();
        List<Object> whereArgs = new ArrayList<>();
        String where = directory.getId().k() + "=?";
        if (id != null) {
            where = directory.getId().k() + "=?";
            whereArgs.add(id);
        } else {
            where = directory.getId().k() + " is null";
        }
        List<FsDirectory> result = sqlQueries.load(directory.getAllAttributes(), directory, where, whereArgs);
        if (result.size() == 1)
            return result.get(0);
        return null;
    }

    public Long getLatestVersion() throws SqlQueriesException {
        FsDirectory directory = new FsDirectory();
        String sql = "select max(" + directory.getVersion().k() + ") from " + directory.getTableName();
        Long v = sqlQueries.queryValue(sql, Long.class, null);
        if (v == null)
            return 0L;
        else
            return Long.valueOf(v.toString());
    }

    public void setDriveSettings(DriveSettings driveSettings) {
        this.driveSettings = driveSettings;
    }

    public List<GenericFSEntry> getDelta(long version) throws SqlQueriesException {
        GenericFSEntry fsEntry = new GenericFSEntry();
        String where = fsEntry.getVersion().k() + ">?";
        List<Object> args = new ArrayList<>();
        args.add(version);
        List<GenericFSEntry> result = sqlQueries.load(fsEntry.getAllAttributes(), fsEntry, where, args, null);
        return result;
    }

    public ISQLResource<GenericFSEntry> getDeltaResource(long version) throws SqlQueriesException {
        GenericFSEntry fsEntry = new GenericFSEntry();
        String where = fsEntry.getVersion().k() + ">?";
        List<Object> args = new ArrayList<>();
        args.add(version);
        ISQLResource<GenericFSEntry> result = sqlQueries.loadResource(fsEntry.getAllAttributes(), GenericFSEntry.class, where, args);
        return result;
    }

    public GenericFSEntry getGenericByINode(Long inode) throws SqlQueriesException {
        GenericFSEntry fsEntry = new GenericFSEntry();
        List<Object> args = new ArrayList<>();
        args.add(inode);
        List<GenericFSEntry> res = sqlQueries.load(fsEntry.getAllAttributes(), fsEntry, fsEntry.getiNode().k() + "=?", args);
        if (res.size() == 1)
            return res.get(0);
        else if (res.size() > 1)
            System.err.println("FsDao.getGenericFilesByINode.MORE:THAN:ONE");
        return null;

    }

    public FsDirectory getFsDirectoryByPath(AFile f) throws SqlQueriesException {
        try {
            RootDirectory rootDirectory = driveDatabaseManager.getDriveSettings().getRootDirectory();
            String rootPath = rootDirectory.getPath();
            //todo Exception here
            if (!f.getAbsolutePath().startsWith(rootPath))
                return null;
            if (f.getAbsolutePath().length() == rootPath.length())
                return getRootDirectory();
            FsDirectory parent = this.getRootDirectory();
            Stack<AFile> fileStack = new Stack<>();
            AFile ff = f;
            while (ff.getAbsolutePath().length() > rootPath.length()) {
                fileStack.push(ff);
                ff = ff.getParentFile();
            }
            while (!fileStack.empty()) {
                String name = fileStack.pop().getName();
                if (parent == null)
                    return null;
                parent = this.getSubDirectoryByName(parent.getId().v(), name);
            }
            return parent;
        } catch (StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return null;
    }

    public FsDirectory getRootDirectory() throws SqlQueriesException {
        FsDirectory dummy = new FsDirectory();
        String where = dummy.getParentId().k() + " is null";
        List<FsDirectory> roots = sqlQueries.load(dummy.getAllAttributes(), dummy, where, null, null);
        assert roots.size() == 1;
        return roots.get(0);
    }

    public GenericFSEntry getGenericChildByName(long parentId, String name) throws SqlQueriesException {
        GenericFSEntry dummy = new GenericFSEntry();
        dummy.getParentId().v(parentId);
        dummy.getName().v(name);
        return getGenericFileByName(dummy);
    }

    public GenericFSEntry getGenericFileByName(GenericFSEntry genericFSEntry) throws SqlQueriesException {
        List<Object> args = new ArrayList<>();
        if (genericFSEntry.getParentId().v() != null)
            args.add(genericFSEntry.getParentId().v());
        args.add(genericFSEntry.getName().v());
        String where = genericFSEntry.getParentId().k() + " is null and " + genericFSEntry.getName().k() + "=?";
        if (genericFSEntry.getParentId().v() != null)
            where = genericFSEntry.getParentId().k() + "=? and " + genericFSEntry.getName().k() + "=?";
        List<GenericFSEntry> res = sqlQueries.load(genericFSEntry.getAllAttributes(), genericFSEntry, where, args);
        if (res.size() == 1)
            return res.get(0);
        return null;
    }

    public boolean hasId(Long id) throws SqlQueriesException {
        GenericFSEntry dummy = new GenericFSEntry();
        String where = dummy.getId().k() + "=?";
        List<Object> args = new ArrayList<>();
        args.add(id);
        return sqlQueries.load(dummy.getInsertAttributes(), dummy, where, args).size() > 0;
    }

    public void deleteById(Long fsId) throws SqlQueriesException {
        List<Object> whereArgs = new ArrayList<>();
        whereArgs.add(fsId);
        FsEntry fsEntry = new GenericFSEntry();
        List<FsEntry> dirContent = this.getDirectoryContent(fsId);
        for (FsEntry content : dirContent)
            this.delete(content);
        sqlQueries.delete(fsEntry, fsEntry.getId().k() + "=?", whereArgs);
    }

    public void delete(FsEntry fsEntry) throws SqlQueriesException {
        deleteById(fsEntry.getId().v());
    }

    public void insertOrUpdate(FsEntry fsEntry) throws SqlQueriesException {
        if (fsEntry.getId().v() != null && hasId(fsEntry.getId().v())) {
            update(fsEntry);
        } else {
            insert(fsEntry);
        }
    }

    public List<GenericFSEntry> getContentByFsDirectory(Long fsId) throws SqlQueriesException {
        GenericFSEntry genericFSEntry = new GenericFSEntry();
        String where = "";
        List<Object> whereArgs = new ArrayList<>();
        if (fsId == null) {
            where = genericFSEntry.getParentId().k() + " is null";
        } else {
            where = genericFSEntry.getParentId().k() + "=?";
            whereArgs.add(fsId);
        }
        //where += " and " + genericFSEntry.getIsDirectory().k() + "=?";
        //whereArgs.add(1);

        List<GenericFSEntry> result = sqlQueries.load(genericFSEntry.getAllAttributes(), genericFSEntry, where, whereArgs);
        return result;
    }

    public AFile getFileByFsFile(RootDirectory rootDirectory, FsEntry fsEntry) throws SqlQueriesException {
        if (fsEntry.getParentId().v() == null)
            return AFile.instance(rootDirectory.getPath());
        Stack<FsDirectory> stack = new Stack<>();
        FsDirectory dbDir = this.getDirectoryById(fsEntry.getParentId().v());
        while (dbDir != null && dbDir.getParentId().v() != null) {
            stack.add(dbDir);
            dbDir = this.getDirectoryById(dbDir.getParentId().v());
        }
        String path = rootDirectory.getPath() + File.separator;
        while (!stack.empty()) {
            dbDir = stack.pop();
            path += dbDir.getName().v() + File.separator;
        }
        path += fsEntry.getName().v();
        return AFile.instance(path);

    }

    public FsDirectory getFsDirectoryById(Long fsId) throws SqlQueriesException {
        FsDirectory dummy = new FsDirectory();
        String where = dummy.getId().k() + "=?";
        List<FsDirectory> directories = sqlQueries.load(dummy.getAllAttributes(), dummy, where, ISQLQueries.args(fsId));
        if (directories.size() == 1)
            return directories.get(0);
        return null;
    }

    public GenericFSEntry getGenericById(Long fsId) throws SqlQueriesException {
        GenericFSEntry dummy = new GenericFSEntry();
        String where = dummy.getId().k() + "=?";
        List<GenericFSEntry> directories = sqlQueries.load(dummy.getAllAttributes(), dummy, where, ISQLQueries.args(fsId));
        if (directories.size() == 1)
            return directories.get(0);
        return null;
    }

    public List<FsFile> getNonSyncedFilesByHash(String hash) throws SqlQueriesException {
        FsFile dummy = new FsFile();
        String where = dummy.getContentHash().k() + "=? and " + dummy.getSynced().k() + "=?";
        List<FsFile> fsFiles = sqlQueries.load(dummy.getAllAttributes(), dummy, where, ISQLQueries.args(hash, false));
        //return new HashSet<>(fsFiles);
        return fsFiles;
    }

    public List<FsFile> getNonSyncedFilesByFsDirectory(Long fsId) throws SqlQueriesException {
        FsFile dummy = new FsFile();
        String where = dummy.getParentId().k() + "=? and " + dummy.getSynced().k() + "=? and " + dummy.getIsDirectory().k() + "=?";
        List<FsFile> fsFiles = sqlQueries.load(dummy.getAllAttributes(), dummy, where, ISQLQueries.args(fsId, false, false));
        return fsFiles;
    }

    /**
     * searches for all hashes that the {@link de.mel.drive.transfer.TManager} is looking for and are already in the share
     *
     * @return
     * @throws SqlQueriesException
     */
    public List<String> searchTransfer() throws SqlQueriesException {
        FsFile f = new FsFile();
        DbTransferDetails t = new DbTransferDetails();
        String query = "select f." + f.getContentHash().k() + " from " + t.getTableName() + " t left join " + f.getTableName()
                + " f on f." + f.getContentHash().k() + "=t." + t.getHash().k() + " where f." + f.getSynced().k() + "=?";
        return sqlQueries.loadColumn(f.getContentHash(), String.class, query, ISQLQueries.args(true));
    }

    public void setSynced(Long id, boolean synced) throws SqlQueriesException {
        assert id != null;
        FsFile dummy = new FsFile();
        String statement = "update " + dummy.getTableName() + " set " + dummy.getSynced().k() + "=? where " + dummy.getId().k() + "=?";
        sqlQueries.execute(statement, ISQLQueries.args(synced, id));
    }

    public ISQLResource<FsFile> getNonSyncedFilesResource() throws SqlQueriesException {
        FsFile fsFile = new FsFile();
        String where = fsFile.getSynced().k() + "=?";
        return sqlQueries.loadResource(fsFile.getAllAttributes(), FsFile.class, where, ISQLQueries.args(false));
    }

    public GenericFSEntry getGenericSubByName(Long parentId, String name) throws SqlQueriesException {
        GenericFSEntry genericFSEntry = new GenericFSEntry();
        String where = genericFSEntry.getParentId().k() + " =? and " + genericFSEntry.getName().k() + "=?";
        List<GenericFSEntry> gens = sqlQueries.load(genericFSEntry.getAllAttributes(), genericFSEntry, where, ISQLQueries.args(parentId, name));
        if (gens.size() == 1)
            return gens.get(0);
        return null;
    }

    public FsFile getFsFileByFile(File file) throws SqlQueriesException {
        RootDirectory rootDirectory = driveDatabaseManager.getDriveSettings().getRootDirectory();
        String rootPath = rootDirectory.getPath();
        //todo throw Exception if f is not in rootDirectory
        if (file.getAbsolutePath().length() < rootPath.length())
            return null;
        File ff = new File(file.getAbsolutePath());
        Stack<File> fileStack = new Stack<>();
        while (ff.getAbsolutePath().length() > rootPath.length()) {
            fileStack.push(ff);
            ff = ff.getParentFile();
        }
        FsEntry lastEntry = this.getRootDirectory();
        while (!fileStack.empty()) {
            if (lastEntry == null) {
                Lok.debug("FsDao.getFsFileByFile.did not find");
                return null;
            }
            String name = fileStack.pop().getName();
            lastEntry = this.getGenericSubByName(lastEntry.getId().v(), name);
        }
        return (FsFile) lastEntry.copyInstance();
    }

    public boolean desiresHash(String hash) throws SqlQueriesException {
        FsFile fsFile = new FsFile();
        String query = "select count(*)>0 from " + fsFile.getTableName() + " where " + fsFile.getSynced().k() + "=? and " + fsFile.getContentHash().k() + "=?";
        Integer result = sqlQueries.queryValue(query, Integer.class, ISQLQueries.args(false, hash));
        return SqliteConverter.intToBoolean(result);
    }

    public Long countDirectories() {
        FsEntry dummy = new FsDirectory();
        return N.result(() -> sqlQueries.queryValue("select count(*) from " + dummy.getTableName()
                        + " where " + dummy.getIsDirectory().k() + "=?", Long.class, ISQLQueries.args(true))
                , 0L);
    }

    public ISQLResource<GenericFSEntry> all() throws SqlQueriesException {
        FsEntry f = new FsFile();
        return sqlQueries.loadResource(f.getAllAttributes(), GenericFSEntry.class, null, null);
    }

    private final FsFile dummy = new FsFile();

    public void updateName(long id, @NotNull String name) throws SqlQueriesException {
        String stmt = "update " + dummy.getTableName() + " set " + dummy.getName().k() + "=? where " + dummy.getId().k() + "=?";
        sqlQueries.execute(stmt, ISQLQueries.args(name,id));
    }
}