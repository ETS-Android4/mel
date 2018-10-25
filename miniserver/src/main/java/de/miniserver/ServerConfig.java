package de.miniserver;

import de.mein.konsole.KResult;

import java.util.HashMap;
import java.util.Map;

public class ServerConfig extends KResult {
    private String certPath, workingDirectory;
    private Map<String, String> files = new HashMap<>();

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public ServerConfig addEntry(String binaryFile, String versionFile) {
        files.put(binaryFile, versionFile);
        return this;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }
}