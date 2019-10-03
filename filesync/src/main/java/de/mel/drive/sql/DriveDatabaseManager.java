package de.mel.drive.sql;

import de.mel.Lok;
import de.mel.auth.data.access.FileRelatedManager;
import de.mel.core.serialize.exceptions.JsonDeserializationException;
import de.mel.core.serialize.exceptions.JsonSerializationException;
import de.mel.drive.data.DriveSettings;
import de.mel.drive.data.DriveStrings;
import de.mel.drive.service.MelDriveService;
import de.mel.drive.sql.dao.*;
import de.mel.execute.SqliteExecutor;
import de.mel.sql.*;
import de.mel.sql.conn.SQLConnector;
import de.mel.sql.transform.SqlResultTransformer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * does everything file (database) related
 * Created by xor on 09.07.2016.
 */
public class DriveDatabaseManager extends FileRelatedManager {
    private final MelDriveService melDriveService;
    private final ISQLQueries sqlQueries;
    private final FileDistTaskDao fileDistTaskDao;
    private FsDao fsDao;
    private StageDao stageDao;
    private final DriveSettings driveSettings;
    private TransferDao transferDao;
    private WasteDao wasteDao;

    public void shutDown() {
        sqlQueries.onShutDown();
    }

    public void cleanUp() throws SqlQueriesException {
        Stage stage = new Stage();
        StageSet stageSet = new StageSet();
        String stmt1 = "delete from " + stage.getTableName();
        String stmt2 = "delete from " + stageSet.getTableName();
        stageDao.getSqlQueries().execute(stmt1, null);
        stageDao.getSqlQueries().execute(stmt2, null);
    }

    public FileDistTaskDao getFileDistTaskDao() {
        return fileDistTaskDao;
    }

    public interface SQLConnectionCreator {
        ISQLQueries createConnection(DriveDatabaseManager driveDatabaseManager, String uuid) throws SQLException, ClassNotFoundException;
    }

    public static SQLConnectionCreator getSqlqueriesCreator() {
        return sqlqueriesCreator;
    }

    private static SQLConnectionCreator sqlqueriesCreator = (driveDatabaseManager, uuid) -> {
        File f = new File(driveDatabaseManager.createWorkingPath() + DriveStrings.DB_FILENAME);
        return new SQLQueries(SQLConnector.createSqliteConnection(f), true, new RWLock(), SqlResultTransformer.sqliteResultSetTransformer());
    };

    public static void setSqlqueriesCreator(SQLConnectionCreator sqlqueriesCreator) {
        DriveDatabaseManager.sqlqueriesCreator = sqlqueriesCreator;
    }

    public static Connection createSqliteConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    public interface DriveSqlInputStreamInjector {
        InputStream createSqlFileInputStream();
    }

    private static DriveSqlInputStreamInjector driveSqlInputStreamInjector = () -> DriveDatabaseManager.class.getClassLoader().getResourceAsStream("de/mel/filesync/filesync.sql");

    public static void setDriveSqlInputStreamInjector(DriveSqlInputStreamInjector driveSqlInputStreamInjector) {
        DriveDatabaseManager.driveSqlInputStreamInjector = driveSqlInputStreamInjector;
    }

    public DriveDatabaseManager(MelDriveService melDriveService, File workingDirectory, DriveSettings driveSettings) throws SQLException, ClassNotFoundException, IOException, JsonDeserializationException, JsonSerializationException, IllegalAccessException, SqlQueriesException {
        super(workingDirectory);
        this.melDriveService = melDriveService;
        this.driveSettings = driveSettings;

        sqlQueries = sqlqueriesCreator.createConnection(this, melDriveService.getUuid());
        {
            Lok.error("synchronous PRAGMA is turned off!");
            SQLStatement st = sqlQueries.getSQLConnection().prepareStatement("PRAGMA synchronous=OFF");
            st.execute();
        }
        {
            SQLStatement st = sqlQueries.getSQLConnection().prepareStatement("PRAGMA foreign_keys=ON");
            st.execute();
        }
        sqlQueries.getSQLConnection().setAutoCommit(false);
//        sqlQueries.enableWAL();

        SqliteExecutor sqliteExecutor = new SqliteExecutor(sqlQueries.getSQLConnection());
        if (!sqliteExecutor.checkTablesExist("fsentry", "stage", "stageset", "transfer", "waste", "filedist")) {
            //find sql file in workingdir
            sqliteExecutor.executeStream(driveSqlInputStreamInjector.createSqlFileInputStream());
            hadToInitialize = true;
        }
        this.driveSettings.getRootDirectory().backup();


        fsDao = new FsDao(this, sqlQueries);
        stageDao = new StageDao(driveSettings, sqlQueries, fsDao);
        transferDao = new TransferDao(sqlQueries);
        wasteDao = new WasteDao(sqlQueries);
        fileDistTaskDao = new FileDistTaskDao(sqlQueries);


        fsDao.setDriveSettings(this.driveSettings);
        transferDao.resetStarted();
        Lok.debug("DriveDatabaseManager.initialised");
    }

    public MelDriveService getMelDriveService() {
        return melDriveService;
    }

    public FsDao getFsDao() {
        return fsDao;
    }

    public DriveSettings getDriveSettings() {
        return driveSettings;
    }


    public StageDao getStageDao() {
        return stageDao;
    }

    public void updateVersion() throws IllegalAccessException, IOException, JsonSerializationException, SqlQueriesException {
        long version = getLatestVersion();
        Lok.debug("updating settings, set version from " + driveSettings.getLastSyncedVersion() + " to " + version);
        driveSettings.setLastSyncedVersion(version);
        driveSettings.save();
    }

    public TransferDao getTransferDao() {
        return transferDao;
    }

    public long getLatestVersion() throws SqlQueriesException {
        long fs = fsDao.getLatestVersion();
        return fs;
    }

    public List<GenericFSEntry> getDelta(long version) throws SqlQueriesException {
        List<GenericFSEntry> delta = fsDao.getDelta(version);
        return delta;
    }

    public ISQLResource<GenericFSEntry> getDeltaResource(long version) throws SqlQueriesException {
        return fsDao.getDeltaResource(version);
    }

    public WasteDao getWasteDao() {
        return wasteDao;
    }
}