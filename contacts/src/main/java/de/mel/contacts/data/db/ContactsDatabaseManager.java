package de.mel.contacts.data.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import de.mel.auth.data.access.FileRelatedManager;
import de.mel.contacts.data.ContactStrings;
import de.mel.contacts.data.ContactsSettings;
import de.mel.contacts.data.db.dao.ContactsDao;
import de.mel.core.serialize.exceptions.JsonDeserializationException;
import de.mel.core.serialize.exceptions.JsonSerializationException;
import de.mel.execute.SqliteExecutor;
import de.mel.sql.ISQLQueries;
import de.mel.sql.RWLock;
import de.mel.sql.SQLQueries;
import de.mel.sql.SQLStatement;
import de.mel.sql.SqlQueriesException;
import de.mel.sql.conn.SQLConnector;
import de.mel.sql.transform.SqlResultTransformer;
import de.mel.contacts.data.db.dao.PhoneBookDao;
import de.mel.contacts.service.ContactsService;

/**
 * Created by xor on 9/23/17.
 */

public class ContactsDatabaseManager extends FileRelatedManager {
    private final ISQLQueries sqlQueries;
    private final ContactsService contactsService;
    private final ContactsSettings settings;
    private final ContactsDao contactsDao;
    private final PhoneBookDao phoneBookDao;

    public ContactsDatabaseManager(ContactsService contactsService, File workingDirectory, ContactsSettings settingsCfg) throws SQLException, ClassNotFoundException, IOException, SqlQueriesException, JsonDeserializationException, JsonSerializationException, IllegalAccessException {
        super(workingDirectory);

        this.contactsService = contactsService;
//        this.dbConnection = sqlConnection; //sqlqueriesCreator.createConnection(this);//
        //SQLConnector.createSqliteConnection(new File(createWorkingPath() + DriveStrings.DB_FILENAME));
        //this.dbConnection = createSqliteConnection();
        /**
         * todo improve sqlite pragma suff
         *  org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig();
         config.enforceForeignKeys(true);
         config.setSynchronous(SynchronousMode.OFF);

         String url = "jdbc:sqlite:C:/temp/foo.db";


         java.sql.Driver driver = (java.sql.Driver) Class.forName("org.sqlite.JDBC").newInstance();
         java.sql.Connection conn = driver.connect(url, config.toProperties());
         */
        sqlQueries = sqlqueriesCreator.createConnection(this, contactsService.getUuid());
        SQLStatement st = sqlQueries.getSQLConnection().prepareStatement("PRAGMA synchronous=OFF");
        st.execute();
        st = sqlQueries.getSQLConnection().prepareStatement("PRAGMA foreign_keys=ON");
        st.execute();
        SqliteExecutor sqliteExecutor = new SqliteExecutor(sqlQueries.getSQLConnection());
        if (!sqliteExecutor.checkTablesExist("contacts", "phone", "email", "phonebook")) {
            //find sql file in workingdir
            sqliteExecutor.executeStream(contactsSqlInputStreamInjector.createSqlFileInputStream());
            hadToInitialize = true;
        }
        contactsDao = new ContactsDao(sqlQueries);
        phoneBookDao = new PhoneBookDao(contactsDao, sqlQueries);
        File settingsFile = new File(workingDirectory.getAbsolutePath() + File.separator + "contacts.settings.json");
        settings = ContactsSettings.load(settingsFile, settingsCfg);
    }

    public PhoneBookDao getPhoneBookDao() {
        return phoneBookDao;
    }

    public ContactsSettings getSettings() {
        return settings;
    }

    public ContactsDao getContactsDao() {
        return contactsDao;
    }

    /**
     * @return Master {@link PhoneBook} that does not provide any {@link Contact}s or null if there is no Master {@link PhoneBook}
     * @throws SqlQueriesException
     */
    public PhoneBook getFlatMasterPhoneBook() throws SqlQueriesException {
        if (settings.getMasterPhoneBookId() != null)
            return phoneBookDao.loadFlatPhoneBook(settings.getMasterPhoneBookId());
        return null;
    }

    public void maintenance() throws SqlQueriesException {
        // for now delete everything but the master phone book
        PhoneBook master = getFlatMasterPhoneBook();
        if (master != null)
            sqlQueries.delete(master, master.getId().k() + "<>?", ISQLQueries.args(master.getId().v()));
    }

    public interface SQLConnectionCreator {
        ISQLQueries createConnection(ContactsDatabaseManager contactsDatabaseManager, String uuid) throws SQLException, ClassNotFoundException;
    }

    private static SQLConnectionCreator sqlqueriesCreator = (contactsDatabaseManager, uuid) -> new SQLQueries(SQLConnector.createSqliteConnection(new File(contactsDatabaseManager.createWorkingPath() + ContactStrings.DB_FILENAME)), true, new RWLock(), SqlResultTransformer.sqliteResultSetTransformer());

    public static void setSqlqueriesCreator(SQLConnectionCreator sqlqueriesCreator) {
        ContactsDatabaseManager.sqlqueriesCreator = sqlqueriesCreator;
    }

    public interface ContactsSqlInputStreamInjector {
        InputStream createSqlFileInputStream();
    }

    private static ContactsSqlInputStreamInjector contactsSqlInputStreamInjector = () -> ContactsDatabaseManager.class.getClassLoader().getResourceAsStream("de/mel/contacts/contacts.sql");

    public static void setContactsSqlInputStreamInjector(ContactsSqlInputStreamInjector contactsSqlInputStreamInjector) {
        ContactsDatabaseManager.contactsSqlInputStreamInjector = contactsSqlInputStreamInjector;
    }
}
