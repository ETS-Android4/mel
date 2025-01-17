package de.mel.filesync;

import de.mel.auth.file.AbstractFile;
import de.mel.auth.service.MelAuthService;
import de.mel.filesync.service.MelFileSyncClientService;
import de.mel.filesync.service.MelFileSyncServerService;

/**
 * Created by xor on 11/13/16.
 */
public abstract class FileSyncSyncListener {
    public DTestStructure testStructure = new DTestStructure();
    private int count = 0;

    public int getCount() {
        return count;
    }

    public abstract void onSyncFailed();

    public abstract void onTransfersDone();


    public static class DTestStructure {
        public MelAuthService maServer, maClient;
        public MelFileSyncClientService clientDriveService;
        public MelFileSyncServerService serverDriveService;
        public AbstractFile testdir1;
        public AbstractFile testdir2;

        public DTestStructure setClientDriveService(MelFileSyncClientService clientDriveService) {
            this.clientDriveService = clientDriveService;
            return this;
        }

        public DTestStructure setMaClient(MelAuthService maClient) {
            this.maClient = maClient;
            return this;
        }

        public DTestStructure setMaServer(MelAuthService maServer) {
            this.maServer = maServer;
            return this;
        }

        public DTestStructure setServerDriveService(MelFileSyncServerService serverDriveService) {
            this.serverDriveService = serverDriveService;
            return this;
        }

        public DTestStructure setTestdir1(AbstractFile testdir1) {
            this.testdir1 = testdir1;
            return this;
        }

        public DTestStructure setTestdir2(AbstractFile testdir2) {
            this.testdir2 = testdir2;
            return this;
        }
    }

    public void onSyncDone() {
        onSyncDoneImpl();
        count++;
    }

    public abstract void onSyncDoneImpl();
}

