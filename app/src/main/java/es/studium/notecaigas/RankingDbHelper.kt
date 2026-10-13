package es.studium.notecaigas

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RankingItem(
    val id: Int,
    val name: String,
    val timeSeconds: Long,
    val date: String
)

class RankingDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "notecaigas.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_RANKING = "ranking"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_TIME = "time_seconds"
        private const val COLUMN_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_RANKING (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_NAME TEXT, " +
                "$COLUMN_TIME INTEGER, " +
                "$COLUMN_DATE TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RANKING")
        onCreate(db)
    }

    fun addScore(name: String, timeSeconds: Long): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_TIME, timeSeconds)
            put(COLUMN_DATE, SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
        }
        return db.insert(TABLE_RANKING, null, values)
    }

    fun getTopScores(limit: Int = 10): List<RankingItem> {
        val list = mutableListOf<RankingItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RANKING,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_TIME, COLUMN_DATE),
            null, null, null, null,
            "$COLUMN_TIME ASC", // Menor tiempo es mejor (más rápido)
            limit.toString()
        )
        cursor.use {
            val idIdx = it.getColumnIndex(COLUMN_ID)
            val nameIdx = it.getColumnIndex(COLUMN_NAME)
            val timeIdx = it.getColumnIndex(COLUMN_TIME)
            val dateIdx = it.getColumnIndex(COLUMN_DATE)
            while (it.moveToNext()) {
                val id = if (idIdx != -1) it.getInt(idIdx) else 0
                val name = if (nameIdx != -1) it.getString(nameIdx) else "Anónimo"
                val time = if (timeIdx != -1) it.getLong(timeIdx) else 0L
                val date = if (dateIdx != -1) it.getString(dateIdx) else ""
                list.add(RankingItem(id, name, time, date))
            }
        }
        return list
    }
}
