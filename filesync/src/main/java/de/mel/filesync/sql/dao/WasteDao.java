package de.mel.filesync.sql.dao;

import de.mel.filesync.sql.FsFile;
import de.mel.filesync.sql.DbTransferDetails;
import de.mel.filesync.sql.Waste;
import de.mel.sql.Dao;
import de.mel.sql.ISQLQueries;
import de.mel.sql.ISQLResource;
import de.mel.sql.SqlQueriesException;

import java.util.List;

/**
 * Created by xor on 1/27/17.
 */
public class WasteDao extends Dao.LockingDao {
    public WasteDao(ISQLQueries ISQLQueries) {
        super(ISQLQueries);
    }

    public WasteDao(ISQLQueries ISQLQueries, boolean lock) {
        super(ISQLQueries, lock);
    }

    /**
     * @param file
     * @throws SqlQueriesException
     */
    public Waste fsToWaste(FsFile file) throws SqlQueriesException {
        Waste waste = Waste.fromFsFile(file);
        try {
            insert(waste);
        } catch (Exception e) {
            System.err.println("WasteDao.fsToWaste: " + e.getMessage());
        }
        return waste;
    }

    public Waste getWasteByInode(Long inode) throws SqlQueriesException {
        Waste dummy = new Waste();
        String where = dummy.getInode().k() + "=?";
        List<Waste> wastes = sqlQueries.load(dummy.getAllAttributes(), dummy, where, ISQLQueries.args(inode));
        if (wastes.size() > 0)
            return wastes.get(0);
        return null;
    }

    public void update(Waste waste) throws SqlQueriesException {
        sqlQueries.update(waste, waste.getId().k() + "=?", ISQLQueries.args(waste.getId().v()));
    }

    /**
     * @return all hashes that are present in waste and transfer
     */
    public List<String> searchTransfer() throws SqlQueriesException {
        DbTransferDetails transfer = new DbTransferDetails();
        Waste waste = new Waste();
        String where = "exists (select " + transfer.getHash().k() + " from " + transfer.getTableName() + " t where t." + transfer.getHash().k() + "=w." + waste.getHash().k() + ")";
        return sqlQueries.loadColumn(waste.getHash(), String.class, waste, "w", where, null, null);
    }

    public Waste insert(Waste waste) throws SqlQueriesException {
        Long id = sqlQueries.insert(waste);
        waste.getId().v(id);
        return waste;
    }

    public Waste getWasteByHash(String hash) throws SqlQueriesException {
        Waste waste = new Waste();
        String where = waste.getHash().k() + "=? and " + waste.getInplace().k() + "=? limit 1";
        List<Waste> wastes = sqlQueries.load(waste.getAllAttributes(), waste, where, ISQLQueries.args(hash, true));
        if (wastes.size() > 0)
            return wastes.get(0);
        return null;
    }

    public void delete(Long id) throws SqlQueriesException {
        Waste waste = new Waste();
        sqlQueries.delete(waste, waste.getId().k() + "=?", ISQLQueries.args(id));
    }

    public Waste getWasteById(Long id) throws SqlQueriesException {
        Waste waste = new Waste();
        return sqlQueries.loadFirstRow(waste.getAllAttributes(), waste, waste.getId().k() + "=?", ISQLQueries.args(id), Waste.class);
    }

    public ISQLResource<Waste> getAgeSortedResource() throws SqlQueriesException {
        Waste waste = new Waste();
        String query = "select * from " + waste.getTableName() + " order by " + waste.getDeleted().k();
        return sqlQueries.loadQueryResource(query, waste.getAllAttributes(), Waste.class, null);
    }

    public ISQLResource<Waste> getOlderThanResource(long days) throws SqlQueriesException {
        Waste waste = new Waste();
        //todo escape properly. currently says "index 1 is out of range"
        String where = waste.getDeleted().k() + "< date('now','-" + days + " days')";
        return sqlQueries.loadResource(waste.getAllAttributes(), Waste.class, where, null);
    }

    public void flagDeleted(Long id, boolean flag) throws SqlQueriesException {
        Waste waste = new Waste();
        String stmt = "update " + waste.getTableName() + " set " + waste.getFlagDelete().k() + "=? where " + waste.getId().k() + "=?";
        sqlQueries.execute(stmt, ISQLQueries.args(flag, id));
    }

    public void deleteFlagged() throws SqlQueriesException {
        Waste waste = new Waste();
        String stmt = "delete from " + waste.getTableName() + " where " + waste.getFlagDelete().k() + "=?";
        sqlQueries.execute(stmt, ISQLQueries.args(true));
    }

    public Long getSize() throws SqlQueriesException {
        Waste waste = new Waste();
        String query = "select coalesce(sum(" + waste.getSize().k() + "),0) from " + waste.getTableName() + " where " + waste.getInplace().k() + "=?";
        Long size = sqlQueries.queryValue(query, Long.class, ISQLQueries.args(true));
        return size == null ? 0L : size;
    }
}
