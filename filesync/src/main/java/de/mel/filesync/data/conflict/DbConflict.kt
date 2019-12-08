package de.mel.filesync.data.conflict

import de.mel.filesync.sql.Stage
import de.mel.sql.Pair
import de.mel.sql.SQLTableObject

class DbConflict() : SQLTableObject() {

    val localStageId = Pair(Long::class.java, "localstageid")
    val remoteStageId = Pair(Long::class.java, "remotestageid")

    override fun getTableName(): String = stage.tableName

    override fun init() {
        populateInsert()
        populateAll(localStageId, remoteStageId)
    }

    companion object {
        val stage = Stage()
    }

    init {
        init()
    }
}