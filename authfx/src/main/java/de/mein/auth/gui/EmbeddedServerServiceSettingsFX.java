package de.mein.auth.gui;

import de.mein.auth.data.db.Certificate;
import de.mein.auth.data.db.ServiceJoinServiceType;

public abstract class EmbeddedServerServiceSettingsFX extends AuthSettingsFX {

    private RemoteServiceChooserFX remoteServiceChooserFX;

    public boolean isServerSelected() {
        return remoteServiceChooserFX.isServerSelected();
    }

    public ServiceJoinServiceType getSelectedService() {
        return remoteServiceChooserFX.getSelectedService();
    }

    public Certificate getSelectedCertificate() {
        return remoteServiceChooserFX.getSelectedCertificate();
    }

    public abstract void onServiceSpotted(RemoteServiceChooserFX.FoundServices foundServices, Long certId, ServiceJoinServiceType service);

    public void setRemoteServiceChooserFX(RemoteServiceChooserFX remoteServiceChooserFX) {
        this.remoteServiceChooserFX = remoteServiceChooserFX;
    }
}