package de.mel.filesync.data;

import de.mel.core.serialize.SerializableEntity;

/**
 * Created by xor on 10/26/16.
 */
public class FileSyncClientSettingsDetails implements SerializableEntity {
    private Long serverCertId;
    private String serverServiceUuid;
    /**
     * if set to false the initialization needs to be done when booting (pair with the server service)
     */
    private Boolean initFinished = false;

    public FileSyncClientSettingsDetails setServerCertId(Long serverCertId) {
        this.serverCertId = serverCertId;
        return this;
    }

    public FileSyncClientSettingsDetails setServerServiceUuid(String serverServiceUuid) {
        this.serverServiceUuid = serverServiceUuid;
        return this;
    }


    public FileSyncClientSettingsDetails setInitFinished(Boolean initFinished) {
        this.initFinished = initFinished;
        return this;
    }

    public Boolean getInitFinished() {
        return initFinished;
    }

    public Long getServerCertId() {
        return serverCertId;
    }

    public String getServerServiceUuid() {
        return serverServiceUuid;
    }
}
