package de.miniserver.blog

import de.mein.sql.*

class VisitsDao(sqlQueries: SQLQueries) : Dao(sqlQueries) {
    companion object {
        const val DAY_ID = "dayid"
        const val COUNT = "visits"
        const val SRC = "src"
        const val TARGET = "target"
    }

    class VisitEntry : SQLTableObject() {
        override fun getTableName(): String = "visits"
        val day = Pair(Int::class.java, DAY_ID)
        val count = Pair(Int::class.java, COUNT)
        val src = Pair(String::class.java, SRC)
        val target = Pair(String::class.java, TARGET)
        init {
            init()
        }
        override fun init() {
            populateInsert(day, count, src, target)
            populateAll()
        }
    }

    val dummy = VisitEntry()

    @Synchronized
    fun increase(day: Int, src: String, target: String) {
        try {
            val visit = VisitEntry()
            visit.day.v(day)
            visit.src.v(src)
            visit.target.v(target)
            visit.count.v(1)
            sqlQueries.insert(visit)
        } catch (e: SqlQueriesException) {
            val stmt = "update ${dummy.tableName} set ${dummy.count.k()}=${dummy.count.k()}+1 where ${dummy.day.k()}=? and ${dummy.src.k()}=? and ${dummy.target.k()}=?"
            sqlQueries.execute(stmt, ISQLQueries.args(day, src, target))
        }

        // the commented code below doesn't work but shoul. UPSERT is supported by sqlite 3.24.0 and higher and works perfectly well in SqliteBrowser
        // but it fails here despite current version is 3.28.0.

        //insert into visits (dayid,src,visits) values(123,"aaa",13) on CONFLICT(dayid,src) do update set visits=visits+excluded.visits;
//        val stmt = "insert into ${dummy.tableName} (${dummy.day.k()},${dummy.src.k()},${dummy.target.k()},${dummy.count.k()}) values(?,?,?,?) " +
//                "on conflict(${dummy.day.k()},${dummy.src.k()},${dummy.target.k()}) do update set ${dummy.count.k()}=${dummy.count.k()}+1"
//        sqlQueries.execute(stmt, ISQLQueries.args(day, src, target, 1))
    }

}