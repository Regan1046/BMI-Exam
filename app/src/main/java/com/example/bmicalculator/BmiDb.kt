package com.example.bmicalculator

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BmiDay(val date: String, val bmi: Double)

class BmiDb(context: Context, databaseName: String = "bmi.db") :
    SQLiteOpenHelper(context.applicationContext, databaseName, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE bmi_history (
                day TEXT PRIMARY KEY NOT NULL,
                bmi REAL NOT NULL,
                measured_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("No migration from $oldVersion to $newVersion")
    }

    fun save(bmi: Double, timestamp: Long = System.currentTimeMillis()) {
        require(bmi.isFinite() && bmi > 0)
        // 日期按本地时区生成，ISO 格式可直接按字符串排序。
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(timestamp))
        val values = ContentValues().apply {
            put("day", day)
            put("bmi", bmi)
            put("measured_at", timestamp)
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 日期主键保证一天一条。比较时间戳可防止迟到的写入覆盖较新的结果。
            val updated = db.update("bmi_history", values, "day = ? AND measured_at <= ?", arrayOf(day, timestamp.toString()))
            // 没有更新时尝试插入。若当天已有较新记录，主键冲突会保留原记录。
            if (updated == 0) db.insertWithOnConflict("bmi_history", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun history(): List<BmiDay> {
        // 折线需要从旧到新绘制。use 在读取完成后关闭 Cursor。
        return readableDatabase.query(
            "bmi_history", arrayOf("day", "bmi"), null, null, null, null, "day ASC"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(BmiDay(cursor.getString(0), cursor.getDouble(1)))
            }
        }
    }
}
