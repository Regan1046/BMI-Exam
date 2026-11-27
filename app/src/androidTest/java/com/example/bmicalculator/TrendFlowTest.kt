package com.example.bmicalculator

import android.app.LocaleManager
import android.graphics.Bitmap
import android.os.Build
import android.os.LocaleList
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class TrendFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Before fun prepare() {
        clearData()
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("en")
        }
        instrumentation.waitForIdleSync()
    }

    @After fun clearData() {
        context.deleteDatabase("bmi.db")
        context.getSharedPreferences("body_data", 0).edit().clear().commit()
        context.getSharedPreferences("settings", 0).edit().clear().commit()
    }

    @Test fun emptyHistoryCanBeOpenedFromTheForm() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.historyButton)).perform(scrollTo(), click())
            awaitView(R.id.historyMessage, withText(R.string.history_empty))
            pressBack()
        }
    }

    @Test fun savedProfileReopensAndTheLatestBmiAppearsOnTheTrend() {
        BmiDb(context).use { db ->
            // Together with today's result, these fixtures cover a 30-day period.
            db.save(24.3, daysAgo(29))
            db.save(23.5, daysAgo(19))
            db.save(23.6, daysAgo(9))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            enter(R.id.heightInput, "170")
            enter(R.id.ageInput, "25")
            onView(withId(R.id.femaleOption)).perform(scrollTo(), click())
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.heightInput)).check(matches(withText("170")))
            onView(withId(R.id.ageInput)).check(matches(withText("25")))
            onView(withId(R.id.femaleOption)).check(matches(isChecked()))
            enter(R.id.weightInput, "68")
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            awaitView(R.id.bmiValue, withText("23.53"))
            pressBack()
            enter(R.id.weightInput, "65")
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            awaitView(R.id.bmiValue, withText("22.49"))
            openActionBarOverflowOrOptionsMenu(context)
            onView(withText(R.string.history_title)).perform(click())
            awaitView(R.id.trendChart, isDisplayed())

            val rows = BmiDb(context).use { db -> db.history() }
            assertEquals(4, rows.size)
            assertEquals(22.49, rows.last().bmi, 0.01)
            captureIfRequested()
            pressBack()
            pressBack()
        }
    }

    private fun enter(id: Int, text: String) =
        onView(withId(id)).perform(scrollTo(), replaceText(text), closeSoftKeyboard())

    private fun daysAgo(days: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -days)
    }.timeInMillis

    private fun awaitView(id: Int, matcher: org.hamcrest.Matcher<android.view.View>) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                onView(withId(id)).check(matches(matcher))
                return
            } catch (_: NoMatchingViewException) {
                SystemClock.sleep(50)
            } catch (_: AssertionError) {
                SystemClock.sleep(50)
            }
        }
        onView(withId(id)).check(matches(matcher))
    }

    private fun captureIfRequested() {
        if (InstrumentationRegistry.getArguments().getString("captureTrend") != "true") return
        instrumentation.waitForIdleSync()
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(context.getExternalFilesDir(null), "Trend.png").outputStream().use {
                assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            screenshot.recycle()
        }
    }
}
