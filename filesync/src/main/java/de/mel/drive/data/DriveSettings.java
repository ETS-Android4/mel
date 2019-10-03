package de.mel.drive.data;

import de.mel.Lok;
import de.mel.auth.data.JsonSettings;
import de.mel.core.serialize.exceptions.JsonDeserializationException;
import de.mel.core.serialize.exceptions.JsonSerializationException;
import de.mel.drive.data.fs.RootDirectory;
import de.mel.auth.file.AFile;

import java.io.File;
import java.io.IOException;

/**
 * Created by xor on 30.08.2016.
 */
@SuppressWarnings("Duplicates")
public class DriveSettings extends JsonSettings {
    public static final float DEFAULT_WASTEBIN_RATIO = 0.1f;
    public static final int DEFAULT_WASTEBIN_MAXDAYS = 30;
    public static final int CACHE_LIST_SIZE = 2000;
    protected RootDirectory rootDirectory;
    protected String role = ":(";
    protected Long lastSyncedVersion = 0l;
    protected DriveClientSettingsDetails clientSettings;
    protected DriveServerSettingsDetails serverSettings;
    protected String transferDirectoryPath;
    protected Long maxWastebinSize;
    protected Long maxAge = 30L;
    protected AFile transferDirectory;
    protected Boolean useSymLinks = true;

    protected boolean fastBoot = true;

    public static RootDirectory buildRootDirectory(AFile rootFile) throws IllegalAccessException, JsonSerializationException, JsonDeserializationException, IOException {
        String path = rootFile.getCanonicalPath();
        RootDirectory rootDirectory = new RootDirectory().setPath(path);
        rootDirectory.setOriginalFile(rootFile);
        rootDirectory.backup();
        return rootDirectory;
    }

    public DriveSettings setUseSymLinks(Boolean useSymLinks) {
        this.useSymLinks = useSymLinks;
        return this;
    }

    public Boolean getUseSymLinks() {
        return useSymLinks;
    }

    public boolean isServer() {
        return role.equals(DriveStrings.ROLE_SERVER);
    }

    public DriveDetails getDriveDetails() {
        return new DriveDetails().setLastSyncVersion(lastSyncedVersion).setRole(role).setUsesSymLinks(useSymLinks);
    }


    public AFile getTransferDirectoryFile() {
        return transferDirectory;
    }

    public Long getMaxWastebinSize() {
        return maxWastebinSize;
    }

    public DriveSettings setMaxWastebinSize(Long maxWastebinSize) {
        this.maxWastebinSize = maxWastebinSize;
        return this;
    }

    public Long getMaxAge() {
        return maxAge;
    }

    public DriveSettings setMaxAge(Long maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    @Override
    protected void init() {
        if (serverSettings != null)
            serverSettings.init();
    }

    public DriveSettings setFastBoot(boolean fastBoot) {
        this.fastBoot = fastBoot;
        return this;
    }

    public boolean getFastBoot() {
        return fastBoot;
    }

    public interface DevRootDirInjector {
        File getRootDir(File jsonFile);
    }

    public static DevRootDirInjector devRootDirInjector;

    public DriveSettings() {

    }

    public DriveClientSettingsDetails getClientSettings() {
        return clientSettings;
    }

    public DriveServerSettingsDetails getServerSettings() {
        return serverSettings;
    }

    public DriveSettings setRole(String role) {
        this.role = role;
        if (role.equals(DriveStrings.ROLE_CLIENT) && clientSettings == null)
            clientSettings = new DriveClientSettingsDetails();
        else if (role.equals(DriveStrings.ROLE_SERVER) && serverSettings == null)
            serverSettings = new DriveServerSettingsDetails();
        return this;
    }

    public DriveSettings setRootDirectory(RootDirectory rootDirectory) {
        this.rootDirectory = rootDirectory;
        return this;
    }

    public DriveSettings setTransferDirectory(AFile transferDirectory) {
        this.transferDirectory = transferDirectory;
        this.transferDirectoryPath = transferDirectory.getAbsolutePath();
        return this;
    }

    public AFile getTransferDirectory() {
        return transferDirectory;
    }

    public static DriveSettings load(File jsonFile) throws IOException, JsonDeserializationException, JsonSerializationException, IllegalAccessException {
        DriveSettings driveSettings = (DriveSettings) JsonSettings.load(jsonFile);
        if (driveSettings != null) {
            driveSettings.setJsonFile(jsonFile);
            driveSettings.getRootDirectory().backup();
            driveSettings.setTransferDirectory(AFile.instance(driveSettings.transferDirectoryPath));
        }
        return driveSettings;
    }

    public DriveSettings setLastSyncedVersion(Long lastSyncedVersion) {
        this.lastSyncedVersion = lastSyncedVersion;
        return this;
    }

    public Long getLastSyncedVersion() {
        return lastSyncedVersion;
    }

    public RootDirectory getRootDirectory() {
        return rootDirectory;
    }

    public String getRole() {
        return role;
    }
}