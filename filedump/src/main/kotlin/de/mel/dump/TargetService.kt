package de.mel.dump

import de.mel.auth.service.MelAuthService
import de.mel.filesync.data.FileSyncSettings
import de.mel.filesync.service.MelFileSyncServerService
import de.mel.filesync.service.sync.ServerSyncHandler
import java.io.File

class TargetService(melAuthService: MelAuthService, serviceInstanceWorkingDirectory: File, serviceTypeId: Long, uuid: String, fileSyncSettings: FileSyncSettings) : MelFileSyncServerService(melAuthService, serviceInstanceWorkingDirectory, serviceTypeId, uuid, fileSyncSettings) {

    override fun initSyncHandler(): ServerSyncHandler = TargetSyncHandler(melAuthService, this)


}