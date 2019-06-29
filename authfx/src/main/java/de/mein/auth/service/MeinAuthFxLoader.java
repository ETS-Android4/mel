package de.mein.auth.service;

import de.mein.Lok;
import de.mein.auth.MeinAuthAdmin;
import de.mein.auth.MeinNotification;

/**
 * Created by xor on 5/30/17.
 */
public class MeinAuthFxLoader implements MeinAuthAdmin {
    private MeinAuthAdminFX meinAuthAdminFX;
    private MeinAuthService meinAuthService;

    @Override
    public void start(MeinAuthService meinAuthService) {
        meinAuthAdminFX = MeinAuthAdminFX.load(meinAuthService);
        this.meinAuthService = meinAuthService;
    }

    public MeinAuthAdminFX getMeinAuthAdminFX() {
        return meinAuthAdminFX;
    }

    @Override
    public void onNotificationFromService(IMeinService meinService, MeinNotification notification) {
        meinAuthAdminFX.onNotificationFromService(meinService, notification);
    }

    @Override
    public void onChanged() {
        meinAuthAdminFX.onChanged();
    }

    @Override
    public void shutDown() {
        meinAuthAdminFX.shutDown();
    }

    @Override
    public void onProgress(MeinNotification notification, int max, int current, boolean indeterminate) {
        meinAuthAdminFX.onProgress(notification, max, current, indeterminate);
    }

    @Override
    public void onCancel(MeinNotification notification) {
        meinAuthAdminFX.onCancel(notification);
    }

    @Override
    public void onFinish(MeinNotification notification) {

    }
}
