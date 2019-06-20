package de.mein.drive.data;

import de.mein.auth.data.ServicePayload;

/**
 * Created by xor on 10/20/16.
 */
public class DriveDetails extends ServicePayload {
    private String role;
    private long lastSyncVersion;
    private String serviceUuid;
    private boolean containsSymLinks = false;

    public DriveDetails setLastSyncVersion(long lastSyncVersion) {
        this.lastSyncVersion = lastSyncVersion;
        return this;
    }

    public DriveDetails setRole(String role) {
        this.role = role;
        return this;
    }

    public DriveDetails setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
        return this;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public long getLastSyncVersion() {
        return lastSyncVersion;
    }

    public String getRole() {
        return role;
    }

    public void containsSymLinks(boolean containsSymLinks) {
        this.containsSymLinks = containsSymLinks;
    }

    public boolean containsSymLinks() {
        return containsSymLinks;
    }
}
