package de.mein.auth;

import de.mein.auth.service.MeinAuthService;
import de.mein.auth.service.MeinService;

/**
 * Created by xor on 6/26/16.
 */
public interface MeinAuthAdmin {

    void start(MeinAuthService meinAuthService);

    /**
     * when there is an event that the user has to be noticed of or has to take care of
     * @param meinService
     * @param notification
     */
    void onNotificationFromService(MeinService meinService, MeinNotification notification);

    void onChanged();

    void shutDown();
}
