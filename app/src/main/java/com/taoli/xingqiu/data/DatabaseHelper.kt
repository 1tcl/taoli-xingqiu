package com.taoli.xingqiu.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.taoli.xingqiu.model.Record

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "taoli_xingqiu.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_RECORDS = "records"
        private const val COL_ID = "id"
        private const val COL_AMOUNT = "amount"
        private const val COL_CATEGORY = "category"
        private const val COL_NOTE = "note"
        private const val COL_TIME = "time"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_RECORDS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_AMOUNT REAL NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_NOTE TEXT DEFAULT '',
                $COL_TIME INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
        onCreate(db)
    }

    // Insert a record
    fun insertRecord(record: Record): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_AMOUNT, record.amount)
            put(COL_CATEGORY, record.category)
            put(COL_NOTE, record.note)
            put(COL_TIME, record.time)
        }
        return db.insert(TABLE_RECORDS, null, values)
    }

    // Delete a record by ID
    fun deleteRecord(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_RECORDS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // Get all records, ordered by time descending
    fun getAllRecords(): List<Record> {
        val records = mutableListOf<Record>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_RECORDS, null, null, null,
            null, null, "$COL_TIME DESC"
        )
        while (cursor.moveToNext()) {
            records.add(cursorToRecord(cursor))
        }
        cursor.close()
        return records
    }

    // Get records in a time range
    fun getRecordsByTimeRange(startTime: Long, endTime: Long): List<Record> {
        val records = mutableListOf<Record>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_RECORDS, null,
            "$COL_TIME >= ? AND $COL_TIME < ?",
            arrayOf(startTime.toString(), endTime.toString()),
            null, null, "$COL_TIME DESC"
        )
        while (cursor.moveToNext()) {
            records.add(cursorToRecord(cursor))
        }
        cursor.close()
        return records
    }

    // Get records by year and month
    fun getRecordsByMonth(year: Int, month: Int): List<Record> {
        val cal = java.util.Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        cal.add(java.util.Calendar.MONTH, 1)
        val endTime = cal.timeInMillis
        return getRecordsByTimeRange(startTime, endTime)
    }

    // Get records for today
    fun getTodayRecords(): List<Record> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val endTime = cal.timeInMillis
        return getRecordsByTimeRange(startTime, endTime)
    }

    // Get records for this year
    fun getYearRecords(): List<Record> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        cal.add(java.util.Calendar.YEAR, 1)
        val endTime = cal.timeInMillis
        return getRecordsByTimeRange(startTime, endTime)
    }

    // Get category sum for a list of records
    fun getCategorySums(records: List<Record>): Map<String, Double> {
        val sums = mutableMapOf<String, Double>()
        for (record in records) {
            sums[record.category] = (sums[record.category] ?: 0.0) + record.amount
        }
        return sums
    }

    private fun cursorToRecord(cursor: Cursor): Record {
        return Record(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
            note = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE)) ?: "",
            time = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME))
        )
    }
}
