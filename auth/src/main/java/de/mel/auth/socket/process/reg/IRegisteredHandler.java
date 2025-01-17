package de.mel.auth.socket.process.reg;

import de.mel.auth.data.db.Certificate;
import de.mel.auth.service.MelAuthService;
import de.mel.sql.SqlQueriesException;

/**
 * Created by xor on 4/25/16.
 */
public  interface IRegisteredHandler {
    void onCertificateRegistered(MelAuthService melAuthService, Certificate registered) throws SqlQueriesException;
}
