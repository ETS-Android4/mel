package de.mein.auth.data.db.dao;

import de.mein.auth.data.db.Certificate;
import de.mein.sql.Dao;
import de.mein.sql.ISQLQueries;
import de.mein.sql.Pair;
import de.mein.sql.SQLTableObject;
import de.mein.sql.SqlQueriesException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xor on 4/11/16.
 */
public class CertificateDao extends Dao.ConnectionLockingDao {
    public CertificateDao(ISQLQueries ISQLQueries) {
        super(ISQLQueries);
    }

    public CertificateDao(ISQLQueries ISQLQueries, boolean lock) {
        super(ISQLQueries, lock);
    }

    public List<Certificate> getTrustedCertificates() throws SqlQueriesException {
        Certificate dummy = new Certificate();
        List<Certificate> re = sqlQueries.load(dummy.getAllAttributes(), dummy, dummy.getTrusted().k() + "=?", ISQLQueries.whereArgs(true));
        return re;
    }

    public Certificate insertCertificate(Certificate certificate) throws SqlQueriesException {
        return certificate.setId(sqlQueries.insert(certificate));
    }

    public boolean existsUUID(String uuid) throws SqlQueriesException {
        Certificate dummy = new Certificate();
        String where = dummy.getUuid().k() + "=?";
        List<Object> args = new ArrayList<>();
        args.add(uuid);
        List<SQLTableObject> result = sqlQueries.load(dummy.getAllAttributes(), dummy, where, args);
        return result.size() > 0;
    }

    public void updateCertificate(Certificate certificate) throws SqlQueriesException {
        List<Object> whereArgs = new ArrayList<>();
        whereArgs.add(certificate.getId().v());
        sqlQueries.update(certificate, certificate.getId().k() + "=?", whereArgs);
    }

    public Certificate getTrustedCertificateByUuid(String uuid) throws SqlQueriesException {
        Certificate dummy = new Certificate();
        String where = dummy.getUuid().k() + "=? and " + dummy.getTrusted().k() + "=?";
        List<SQLTableObject> result = sqlQueries.load(dummy.getAllAttributes(), dummy, where, ISQLQueries.whereArgs(uuid, true));
        //todo debug
        if (result.size() == 0) {
            System.err.println("CertificateDao.getTrustedCertificateByUuid for uuid " + uuid);
        }
        return (Certificate) result.get(0);
    }

    public Certificate getTrustedCertificateById(Long id) throws SqlQueriesException {
        Certificate dummy = new Certificate();
        String where = dummy.getId().k() + "=? and " + dummy.getTrusted().k() + "=?";
        List<Certificate> result = sqlQueries.load(dummy.getAllAttributes(), dummy, where, ISQLQueries.whereArgs(id, true));
        if (result.size() == 1)
            return result.get(0);
        return null;
    }

    public Certificate getCertificateById(Long id) throws SqlQueriesException {
        Certificate dummy = new Certificate();
        String where = dummy.getId().k() + "=?";
        List<Certificate> result = sqlQueries.load(dummy.getAllAttributes(), dummy, where, ISQLQueries.whereArgs(id));
        if (result.size() == 1)
            return result.get(0);
        return null;
    }

    public void delete(Long id) throws SqlQueriesException {
        Certificate dummy = new Certificate().setId(id);
        List<Object> args = new ArrayList<>();
        args.add(id);
        sqlQueries.delete(dummy, dummy.getId().k() + "=?", args);
    }

    public List<Certificate> getCertificatesByGreeting(String greeting) throws SqlQueriesException {
        Certificate dummy = new Certificate();
        String where = dummy.getGreeting().k() + "=?";
        List<Object> args = new ArrayList<>();
        args.add(greeting);
        List<SQLTableObject> re = sqlQueries.load(dummy.getAllAttributes(), dummy, where, args);
        List<Certificate> result = new ArrayList<>();
        for (SQLTableObject cert : re) {
            result.add((Certificate) cert);
        }
        return result;
    }

    public Certificate getCertificateByBytes(byte[] certBytes) throws SqlQueriesException {
        Certificate dummy = new Certificate();
        String where = dummy.getCertificate().k() + "=?";
        List<Object> args = new ArrayList<>();
        args.add(certBytes);
        // todo for some unknown reason this returns nothing on android
        List<SQLTableObject> result = sqlQueries.load(dummy.getAllAttributes(), dummy, where, args);
        if (result.size() == 1)
            return (Certificate) result.get(0);
        return null;
    }

    public void trustCertificate(Long certId, boolean trusted) throws SqlQueriesException {
        Certificate cert = new Certificate();
        String sql = "update " + cert.getTableName() + " set " + cert.getTrusted().k() + "=? where "
                + cert.getId().k() + "=?";
        List<Object> args = new ArrayList<>();
        args.add(trusted);
        args.add(certId);
        sqlQueries.execute(sql, args);
        System.out.println("CertificateDao.trustCertificate");
    }

    public Certificate getTrustedCertificateByHash(String hash) throws SqlQueriesException {
        Certificate dummy = new Certificate();
        String where = dummy.getHash().k() + "=? and " + dummy.getTrusted().k() + "=?";
        List<SQLTableObject> result = sqlQueries.load(dummy.getAllAttributes(), dummy, where, ISQLQueries.whereArgs(hash, true));
        if (result.size() == 1)
            return (Certificate) result.get(0);
        return null;
    }


    public List<Certificate> getAllCertificateDetails() throws SqlQueriesException {
        Certificate certificate = new Certificate();
        List<Certificate> certificates = sqlQueries.load(ISQLQueries.columns(certificate.getId(), certificate.getPort(), certificate.getName(), certificate.getTrusted(), certificate.getHash()), certificate, null, null);
        return certificates;
    }
}
