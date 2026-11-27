package com.example.bmicalculator

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BmiDbTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "test-${UUID.randomUUID()}.db"

    @After
    fun removeTestDatabase() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun sameDayKeepsOnlyTheLatestMeasurement() {
        BmiDb(context, databaseName = databaseName).use { db ->
            assertTrue(db.history().isEmpty())
            db.save(21.10, timestamp = localTime(day = 21, hour = 9))
            db.save(22.45, timestamp = localTime(day = 21, hour = 18))
            db.save(23.70, timestamp = localTime(day = 21, hour = 12))

            assertEquals(listOf(BmiDay("2026-11-21", 22.45)), db.history())
        }
    }

    @Test
    fun recordsSurviveReopeningAndAreSortedByLocalDate() {
        BmiDb(context, databaseName = databaseName).use { db ->
            db.save(22.80, timestamp = localTime(day = 25, hour = 14))
            db.save(22.90, timestamp = localTime(day = 22, hour = 0, minute = 1))
            db.save(23.15, timestamp = localTime(day = 21, hour = 23, minute = 59))
        }

        BmiDb(context, databaseName = databaseName).use { reopened ->
            assertEquals(
                listOf(BmiDay("2026-11-21", 23.15), BmiDay("2026-11-22", 22.90), BmiDay("2026-11-25", 22.80)),
                reopened.history()
            )
        }
    }

    private fun localTime(day: Int, hour: Int, minute: Int = 0): Long =
        GregorianCalendar().apply {
            clear()
            set(2026, Calendar.NOVEMBER, day, hour, minute)
        }.timeInMillis
}
