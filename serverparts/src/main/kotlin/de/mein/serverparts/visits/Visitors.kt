package de.mein.serverparts.visits

import com.sun.net.httpserver.HttpExchange
import de.mein.execute.SqliteExecutor
import de.mein.sql.RWLock
import de.mein.sql.SQLQueries
import de.mein.sql.conn.SQLConnector
import de.mein.sql.transform.SqlResultTransformer
import java.io.File
import java.time.LocalDateTime

class Visitors private constructor(private val dao: VisitsDao) {

    fun count(ex: HttpExchange) {
        if (ex.requestMethod == "GET") {
            val now = LocalDateTime.now()
            // day = 20190324
            val day = "${now.year}${if (now.monthValue > 9) "${now.monthValue}" else "0${now.monthValue}"}${if (now.dayOfMonth > 9) "${now.dayOfMonth}" else "0${now.dayOfMonth}"}".toInt()
            val src = ex.remoteAddress.hostName.toString()
            val target = ex.requestURI.toString()
            dao.increase(day, src, target)
        }
    }

    companion object {
        fun fromDbFile(file: File): Visitors {
            val sqlQueries = SQLQueries(SQLConnector.createSqliteConnection(file), true, RWLock(), SqlResultTransformer.sqliteResultSetTransformer())
            val dao = VisitsDao(sqlQueries)

            val sqliteExecutor = SqliteExecutor(sqlQueries.sqlConnection)
            if (!sqliteExecutor.checkTablesExist("visits")) {
                dao.createTable()
            }
            return Visitors(dao)
        }
    }

}