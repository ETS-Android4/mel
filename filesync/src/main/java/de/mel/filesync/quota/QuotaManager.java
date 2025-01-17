package de.mel.filesync.quota;

import de.mel.auth.tools.N;
import de.mel.filesync.data.FileSyncSettings;
import de.mel.filesync.service.MelFileSyncService;
import de.mel.filesync.service.Wastebin;
import de.mel.filesync.sql.DbTransferDetails;
import de.mel.filesync.sql.FsFile;
import de.mel.filesync.sql.Stage;
import de.mel.filesync.sql.Waste;
import de.mel.sql.ISQLQueries;
import de.mel.sql.SqlQueriesException;

/**
 * Created by xor on 10.11.2017.
 */

public class QuotaManager {
    private final ISQLQueries sqlQueries;
    private final MelFileSyncService melFileSyncService;
    private final FileSyncSettings fileSyncSettings;
    private final Wastebin wastebin;

    public QuotaManager(MelFileSyncService melFileSyncService) {
        this.melFileSyncService = melFileSyncService;
        this.fileSyncSettings = melFileSyncService.getFileSyncSettings();
        this.wastebin = melFileSyncService.getWastebin();
        this.sqlQueries = melFileSyncService.getFileSyncDatabaseManager().getFsDao().getSqlQueries();
    }

    public void freeSpaceForStageSet(Long stageSetId) throws SqlQueriesException, OutOfSpaceException {
        Stage nStage = new Stage();
        DbTransferDetails nTransfer = new DbTransferDetails();
        FsFile nFsEntry = new FsFile();
        Waste nWaste = new Waste();
        /**
         * sum up everything we have to transfer
         * - (literally MINUS) all would be canceled transfers
         * - all progress already made by required transfers
         * - stuff that is required and in Wastebin
         */
        String query =
                "select (sum(n*" + nStage.getSizePair().k() + ")-\n" +
                        "coalesce((\n" +
                        "	select sum(t." + nTransfer.getState().k() + " * (t." + nTransfer.getTransferred().k() + ")) as deletablebytes from " + nTransfer.getTableName() + " t left join --transfers that cancel\n" +
                        "	(\n" +
                        "		select * from (\n" +
                        "			select " + nStage.getContentHashPair().k() + ", sum(1)  + (select (sum(not " + nStage.getDeletedPair().k() + ") - sum(" + nStage.getDeletedPair().k() + ")) as n from " + nStage.getTableName() + " s where s." + nStage.getContentHashPair().k() + "=ff." + nFsEntry.getContentHash().k() + ") as exis from " + nFsEntry.getTableName() + " ff where " + nFsEntry.getSynced().k() + "=? group by " + nStage.getContentHashPair().k() + "\n" +
                        "		)\n" +
                        "		where exis=?\n" +
                        "	) ex on ex." + nStage.getContentHashPair().k() + " = t." + nTransfer.getHash().k() + " where ex." + nStage.getContentHashPair().k() + " not null group by ex." + nStage.getContentHashPair().k() + "\n" +
                        "),0)-\n" +
                        "coalesce((\n" +
                        "	select sum(t." + nTransfer.getState().k() + " * (t." + nTransfer.getTransferred().k() + ")) as deletablebytes from " + nTransfer.getTableName() + " t left join --transfers which would remain and already made progress\n" +
                        "	(\n" +
                        "		select * from (\n" +
                        "			select " + nStage.getContentHashPair().k() + ", sum(1)  + (select (sum(not " + nStage.getDeletedPair().k() + ") - sum(" + nStage.getDeletedPair().k() + ")) as n from " + nStage.getTableName() + " s where s." + nStage.getContentHashPair().k() + "=ff." + nFsEntry.getContentHash().k() + ") as exis from " + nFsEntry.getTableName() + " ff where " + nFsEntry.getSynced().k() + "=? group by " + nStage.getContentHashPair().k() + "\n" +
                        "		)\n" +
                        "		where exis=?\n" +
                        "	) ex on ex." + nStage.getContentHashPair().k() + " = t." + nTransfer.getHash().k() + " where ex." + nStage.getContentHashPair().k() + " is null group by ex." + nStage.getContentHashPair().k() + "\n" +
                        "),0)\n" +
                        ") as requiredspace from (\n" +
                        "	select (sum(not " + nStage.getDeletedPair().k() + ") - sum(" + nStage.getDeletedPair().k() + ") + (select count(*) from " + nFsEntry.getTableName() + " f where f." + nStage.getContentHashPair().k() + "=s." + nStage.getContentHashPair().k() + " and f." + nFsEntry.getSynced().k() + "=?) - (select count(*) from " + nWaste.getTableName() + " w where w." + nTransfer.getHash().k() + " = s." + nStage.getContentHashPair().k() + " and " + nWaste.getInplace().k() + "=?)) as n, " + nStage.getContentHashPair().k() + ", " + nStage.getSizePair().k() + " from " + nStage.getTableName() + " s " +
                        "   where s." + nStage.getStageSetPair().k() + "=?" +
                        "   group by " + nStage.getContentHashPair().k() + "\n" +
                        ")";
        Long requiredSpace = sqlQueries.queryValue(query, Long.class, ISQLQueries.args(
                0,//synced
                0,//exis
                0,//synced
                0,//exis
                0,//synced
                1,//inplace
                stageSetId));
        final Long availableSpace = fileSyncSettings.getRootDirectory().getOriginalFile().getFreeSpace();
        //try to clean up wastebin if that happens
        if (requiredSpace == null) {
            System.err.println("QuotaManager.freeSpaceForStageSet.FIX:THIS!!!");
            System.err.println("QuotaManager.freeSpaceForStageSet.FIX:THIS!!!");
            System.err.println("QuotaManager.freeSpaceForStageSet.FIX:THIS!!!");
            System.err.println("QuotaManager.freeSpaceForStageSet.FIX:THIS!!!");
            System.err.println("QuotaManager.freeSpaceForStageSet.FIX:THIS!!!");
            System.err.println("QuotaManager.freeSpaceForStageSet.FIX:THIS!!!");
            System.err.println("QuotaManager.freeSpaceForStageSet.FIX:THIS!!!");
            requiredSpace = 0L;
        }
        if (requiredSpace > availableSpace) {
            freeSpace(requiredSpace - availableSpace, stageSetId);
        }
    }

    /**
     * @param requiredSpace amount of bytes which shall be free
     * @param stageSetId    Files which are part of this StageSet won't be deleted
     */
    private void freeSpace(long requiredSpace, long stageSetId) throws SqlQueriesException, OutOfSpaceException {
        Stage nStage = new Stage();
        WasteDummy wasteDummy = new WasteDummy();
        Waste nWaste = new Waste();
        Long[] freed = new Long[]{0L};
        String query = "select w." + nWaste.getId().k() + ", w." + nWaste.getSize().k() + " from " + nWaste.getTableName() + " w left join (select ss." + nStage.getStageSetPair().k() + ", ss." + nStage.getIdPair().k() + ", ss." + nStage.getContentHashPair().k() + " from " + nStage.getTableName() + " ss " +
                "where ss." + nStage.getStageSetPair().k() + "=? ) s on w." + nWaste.getHash().k() + "=s." + nStage.getContentHashPair().k() + " where s." + nStage.getIdPair().k() + " is null " +
                "order by w." + nWaste.getDeleted().k();
        N.readSqlResource(sqlQueries.loadQueryResource(query, wasteDummy.getAllAttributes(), WasteDummy.class, ISQLQueries.args(stageSetId)), (sqlResource, wasteDum) -> {
            //delete until the required space is freed
            freed[0] += wasteDum.getSize().v();
            wastebin.rm(wasteDum.getId().v());
            if (freed[0] > requiredSpace)
                sqlResource.close();
        });
        wastebin.deleteFlagged();
        if (requiredSpace > freed[0]) {
            throw new OutOfSpaceException();
        }
    }

    private void estimateOccupiedSpace() {
        Waste nWaste = new Waste();
        FsFile nFsFile = new FsFile();
        String query = "select \n" +
                "	sum(\n" +
                "		case when sedeleted then \n" +
                "			-ssize --substract stuff that is deleted \n" +
                "		else (\n" +
                "			case when ssize is null then --stage might have another size than fs \n" +
                "				fsize \n" +
                "			else \n" +
                "				ssize \n" +
                "			end \n" +
                "		)\n" +
                "		end \n" +
                "	)\n" +
                "	+\n" +
                "	(--sum up everthing in the wastebin (might be null if nothing in waste table)\n" +
                "	case when(select count(*) from waste w where w.inplace=1)=0 then  \n" +
                "		0 \n" +
                "	else \n" +
                "		(select sum(w.size) from waste w where w.inplace=1)\n" +
                "	end \n" +
                "	)\n" +
                "	+\n" +
                "	(\n" +
                "		select sum(transferred) from transfer\n" +
                "	)\n" +
                "	as summ \n" +
                "from \n" +
                "(\n" +
                "	select f.size as fsize, f.id, sedeleted, ssize from fsentry f left join \n" +
                "	(--join fsentries with the latest stage entries which source from \"fs\"\n" +
                "		select s.size as ssize, s.fsid, deleted as sedeleted from stage s, stageset ss on s.stageset=ss.id where ss.source=\"fs\" order by ss.created \n" +
                "	) \n" +
                "	s on f.id=s.fsid \n" +
                "where f.dir=0 group by f.id);";
    }
}
