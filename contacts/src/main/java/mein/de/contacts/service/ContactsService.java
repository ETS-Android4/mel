package mein.de.contacts.service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import de.mein.auth.MeinNotification;
import de.mein.auth.data.IPayload;
import de.mein.auth.data.db.Certificate;
import de.mein.auth.service.IMeinService;
import de.mein.auth.service.MeinAuthService;
import de.mein.auth.service.MeinService;
import de.mein.auth.socket.process.transfer.MeinIsolatedProcess;
import de.mein.auth.socket.process.val.Request;
import de.mein.core.serialize.exceptions.JsonDeserializationException;
import de.mein.core.serialize.exceptions.JsonSerializationException;
import de.mein.sql.SqlQueriesException;
import mein.de.contacts.data.ContactsSettings;
import mein.de.contacts.data.db.ContactsDatabaseManager;

/**
 * Created by xor on 9/21/17.
 */

public abstract class ContactsService extends MeinService {


    protected final ContactsDatabaseManager contactsDatabaseManager;

    public ContactsService(MeinAuthService meinAuthService, File serviceInstanceWorkingDirectory, Long serviceTypeId, String uuid, ContactsSettings settingsCfg) throws JsonDeserializationException, JsonSerializationException, IOException, SQLException, SqlQueriesException, IllegalAccessException, ClassNotFoundException {
        super(meinAuthService, serviceInstanceWorkingDirectory, serviceTypeId, uuid);
        contactsDatabaseManager = new ContactsDatabaseManager(this, serviceInstanceWorkingDirectory, settingsCfg);

    }

    @Override
    public void handleRequest(Request request) throws Exception {
        System.out.println("ContactsService.handleRequest");
    }

    @Override
    public void handleMessage(IPayload payload, Certificate partnerCertificate, String intent) {
        System.out.println("ContactsService.handleMessage");
    }

    @Override
    public void connectionAuthenticated(Certificate partnerCertificate) {
        System.out.println("ContactsService.connectionAuthenticated");
    }

    @Override
    public void handleCertificateSpotted(Certificate partnerCertificate) {
        System.out.println("ContactsService.handleCertificateSpotted");
    }

    @Override
    public void onMeinAuthIsUp() {
        System.out.println("ContactsService.onMeinAuthIsUp");
    }

    @Override
    public MeinNotification createSendingNotification() {
        return null;
    }

    @Override
    public void onCommunicationsDisabled() {

    }

    @Override
    public void onCommunicationsEnabled() {

    }

    @Override
    public void onIsolatedConnectionEstablished(MeinIsolatedProcess isolatedProcess) {

    }
}
