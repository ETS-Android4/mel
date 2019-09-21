package de.mein.dump

import de.mein.Lok
import de.mein.auth.data.access.CertificateManager
import de.mein.auth.service.MeinAuthService
import de.mein.auth.socket.process.`val`.Request
import de.mein.auth.tools.N
import de.mein.auth.tools.lock.T
import de.mein.auth.tools.lock.Transaction
import de.mein.drive.data.Commit
import de.mein.drive.service.sync.ServerSyncHandler
import de.mein.drive.sql.DbTransferDetails
import de.mein.drive.sql.FsEntry
import de.mein.drive.sql.GenericFSEntry
import de.mein.drive.sql.Stage
import de.mein.sql.ISQLResource
import java.time.LocalDateTime
import java.time.ZoneOffset

class TargetSyncHandler(meinAuthService: MeinAuthService, targetService: TargetService) : ServerSyncHandler(meinAuthService, targetService) {
    override fun handleCommit(request: Request<*>?) {
        val commit = request!!.payload as Commit
        val transaction: Transaction<*> = T.lockingTransaction(fsDao)
        try {
            executeCommit(request, commit, transaction)
        } finally {
            transaction.end()
        }
        transferManager.research()
        Lok.debug("MeinDriveServerService.handleCommit")
    }

    private var booted = false

    override fun commitStage(stageSetId: Long, transaction: Transaction<*>, stageIdFsIdMap: MutableMap<Long, Long>?) {
        val stageSet = stageDao.getStageSetById(stageSetId)
        // just let the first fs commit through, it is the boot index stage set
        if (stageSet.fromFs() && !booted) {
            booted = true
            super.commitStage(stageSetId, transaction, stageIdFsIdMap)
            return
        }
        transaction.run {
            val localVersion = fsDao.latestVersion + 1
            // put new stuff in place
            N.readSqlResource(stageDao.getNotDeletedStagesByStageSet(stageSetId)) { sqlResource: ISQLResource<Stage>, stage: Stage ->
                val fsParentId = stage.fsParentId ?: stageIdFsIdMap?.get(stage.parentId)
                val fsEntry = GenericFSEntry()
                fsEntry.isDirectory.v(stage.isDirectory)
                fsEntry.name.v(stage.name)
                fsEntry.contentHash.v(stage.contentHash)
                fsEntry.parentId.v(fsParentId)
                fsEntry.created.v(stage.created)
                fsEntry.version.v(localVersion)
                fsEntry.size.v(stage.size)
                fsEntry.synced.v(stage.isDirectory)

                if (fsParentId == null) {
                    Lok.error("this is either the root dir or an error: ${stage.name}")
                } else {
                    val existing = fsDao.getGenericChildByName(fsParentId, stage.name)
                    if (existing == null) {
                        fsDao.insert(fsEntry)
                    } else {
                        resolveConflict(stage, existing, fsEntry, stageIdFsIdMap)
                    }
                    stageIdFsIdMap?.set(stage.id, fsEntry.id.v())
                    createDirs(driveDatabaseManager.driveSettings.rootDirectory, fsEntry)
                    // transfer if file
                    if (!stage.isDirectory) {
                        // this file porobably has to be transferred
                        val details = DbTransferDetails()
                        details.available.v(stage.synced)
                        details.certId.v(stageSet.originCertId)
                        details.serviceUuid.v(stageSet.originServiceUuid)
                        details.hash.v(stage.contentHash)
                        details.deleted.v(false)
                        details.size.v(stage.size)
                        setupTransferAvailable(details, stageSet, stage)
                        N.r { transferManager.createTransfer(details) }
                    }
                }
            }
            driveDatabaseManager.updateVersion()
            stageDao.deleteStageSet(stageSetId)
            transferManager.stop()
            transferManager.removeUnnecessaryTransfers()
            transferManager.start()
            transferManager.research()
        }
    }

    /**
     * find an appropriate file name for the new file.
     * new name be like: FILE_NAME[.CREATED][.FS_ID][.RANDOM_UUID].FILE_EXT
     */
    private fun resolveConflict(stage: Stage, existing: GenericFSEntry, fsEntry: FsEntry, stageIdFsIdMap: MutableMap<Long, Long>?) {
        val baseName = existing.name.v()

        if (baseName == stage.name && stage.isDirectory && existing.isDirectory.v())
            return

        // insert with tmp name. this gives us an fs id
        val tmpName = "$baseName.${CertificateManager.randomUUID()}"
        fsEntry.name.v(tmpName)
        fsDao.insert(fsEntry)

        val extension = N.result {
            val lastDotIndex = baseName.lastIndexOf('.')
            if (lastDotIndex < 1)
                ""
            else
                baseName.drop(lastDotIndex)
        }
        val name = baseName.dropLast(extension.length)
        val ldt = LocalDateTime.ofEpochSecond(stage.created/1000, 0, ZoneOffset.UTC)
        val created = "${ldt.year}.${ldt.monthValue}.${ldt.dayOfMonth}.${ldt.hour}.${ldt.minute}.${ldt.second}"
        var newName = "${name}_$created$extension"
        if (fsDao.getGenericChildByName(existing.parentId.v(), newName) != null)
            newName = "${name}_${created}_${fsEntry.id.v()}$extension"
        // enough is enough! just try random UUIDs until one is not taken.
        while (fsDao.getGenericChildByName(existing.parentId.v(), newName) != null)
            newName = "${name}_${created}_${fsEntry.id.v()}_${CertificateManager.randomUUID()}$extension"
        fsEntry.name.v(newName)
        fsDao.updateName(fsEntry.id.v(), newName)
    }
}