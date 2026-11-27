package com.example.bmicalculator

import android.app.LocaleManager
import android.app.UiAutomation
import android.graphics.Bitmap
import android.os.Build
import android.os.LocaleList
import android.os.SystemClock
import android.widget.ScrollView
import android.widget.Toolbar
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EnhancementFlowTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before fun prepare() {
        val context = instrumentation.targetContext
        context.getSharedPreferences("body_data", 0).edit().clear().commit()
        context.getSharedPreferences("settings", 0).edit().clear().commit()
        if (Build.VERSION.SDK_INT >= 33) context.getSystemService(LocaleManager::class.java)
            .applicationLocales = LocaleList.forLanguageTags("en")
        instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_0)
        instrumentation.waitForIdleSync()
    }

    @After fun releaseRotation() {
        instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
    }

    private fun waitForScreen(chinese: Boolean? = null, landscape: Boolean? = null) {
        val deadline = SystemClock.uptimeMillis() + 10000
        var ready = false
        while (!ready && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
                ready = activity != null && activity.window.decorView.width > 0 &&
                    (chinese == null || (activity.resources.configuration.locales[0].language == "zh") == chinese) &&
                    (landscape == null || (activity.window.decorView.width > activity.window.decorView.height) == landscape)
            }
            if (!ready) SystemClock.sleep(50)
        }
        assertTrue("Expected language and orientation must settle", ready)
        instrumentation.waitForIdleSync()
        onIdle()
        SystemClock.sleep(400)
    }

    private fun language(chinese: Boolean) {
        openActionBarOverflowOrOptionsMenu(instrumentation.targetContext)
        onView(withText(if (chinese) "简体中文" else "English")).perform(click())
        waitForScreen(chinese = chinese)
    }

    private fun rotate(landscape: Boolean) {
        instrumentation.uiAutomation.setRotation(if (landscape) UiAutomation.ROTATION_FREEZE_90 else UiAutomation.ROTATION_FREEZE_0)
        waitForScreen(landscape = landscape)
    }

    private fun enterProfile() {
        onView(withId(R.id.heightInput)).perform(scrollTo(), replaceText("150"), closeSoftKeyboard())
        onView(withId(R.id.weightInput)).perform(scrollTo(), replaceText("70"), closeSoftKeyboard())
        onView(withId(R.id.ageInput)).perform(scrollTo(), replaceText("12"), closeSoftKeyboard())
        onView(withId(R.id.femaleOption)).perform(scrollTo(), click())
    }

    @Test fun languageAndRotationPreserveUnsubmittedDraftAndReport() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForScreen(chinese = false, landscape = false)
            enterProfile()
            language(true)
            onView(withId(R.id.calculateButton)).check(matches(withText("查看我的 BMI 报告")))
            onView(withId(R.id.heightInput)).check(matches(withText("150")))
            rotate(true)
            scenario.recreate()
            onView(withId(R.id.femaleOption)).check(matches(isChecked()))
            onView(withId(R.id.ageInput)).check(matches(withText("12")))
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            onView(withId(R.id.categoryText)).check(matches(withText("严重超重")))
            onView(withId(R.id.profileSummary)).check(matches(withText("12 岁 · 女")))
            onView(withId(R.id.adviceButton)).check(matches(withText("生成建议")))
            language(false)
            onView(withId(R.id.categoryText)).check(matches(withText("Severely overweight")))
            onView(withId(R.id.bmiValue)).check(matches(withText("31.11")))
            onView(withId(R.id.adviceButton)).perform(scrollTo(), click())
            onView(withText("Connect to DeepSeek")).check(matches(isDisplayed()))
            onView(withId(android.R.id.button2)).perform(click())
            rotate(false)
            pressBack()
            waitForScreen(chinese = false, landscape = false)
            onView(withId(R.id.weightInput)).check(matches(withText("70")))
        }
    }

    @Test fun contextExamplesAndOptionsClearUpdateStoredDraft() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForScreen(chinese = false)
            onView(withId(R.id.contextHint)).perform(scrollTo(), longClick())
            onView(withText("Use child example")).perform(click())
            onView(withId(R.id.ageInput)).check(matches(withText("12")))
            onView(withId(R.id.femaleOption)).check(matches(isChecked()))
            openActionBarOverflowOrOptionsMenu(instrumentation.targetContext)
            onView(withText("Clear measurements")).perform(click())
            scenario.recreate()
            onView(withId(R.id.heightInput)).check(matches(withText("")))
            onView(withId(R.id.femaleOption)).check(matches(isNotChecked()))
            onView(withId(R.id.chartHint)).check(matches(withText(R.string.chart_hint)))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.ageInput)).check(matches(withText("")))
        }
    }

    @Test fun reportContextChartAndEditActionsWork() {
        ActivityScenario.launch(MainActivity::class.java).use {
            enterProfile()
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            onView(withId(R.id.reportContextHint)).perform(scrollTo(), longClick())
            onView(withText("View chart ranges")).perform(click())
            onView(withText("Age 12 · Girls · BMI ranges")).check(matches(isDisplayed()))
            onView(withText(R.string.got_it)).perform(click())
            onView(withId(R.id.reportContextHint)).perform(scrollTo(), longClick())
            onView(withText("Edit my details")).perform(click())
            onView(withId(R.id.weightInput)).check(matches(withText("70")))
        }
    }

    @Test fun captureFourRequiredScreens() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForScreen(chinese = false, landscape = false)
            enterProfile()
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            language(true)
            onView(withId(R.id.categoryText)).check(matches(withText("严重超重")))
            screenshot("01-report-zh-portrait.png", landscape = false)

            language(false)
            pressBack()
            rotate(true)
            onView(withId(R.id.heightInput)).check(matches(withText("150")))
            onView(withId(R.id.calculateButton)).check(matches(isDisplayed()))
            screenshot("02-main-en-landscape.png", landscape = true)

            language(true)
            openActionBarOverflowOrOptionsMenu(instrumentation.targetContext)
            onView(withText("清空身体数据")).check(matches(isDisplayed()))
            instrumentation.runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
                val menu = activity.findViewById<Toolbar>(R.id.toolbar).menu
                assertTrue("Chinese must be checked while the app is in Chinese", menu.findItem(R.id.languageChinese).isChecked)
                assertFalse(menu.findItem(R.id.languageEnglish).isChecked)
            }
            screenshot("03-options-zh-landscape.png", landscape = true, menuOpen = true)
            pressBack()

            language(false)
            rotate(false)
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            onView(withId(R.id.reportContextHint)).perform(scrollTo(), longClick())
            onView(withText("Copy BMI result")).check(matches(isDisplayed()))
            screenshot("04-context-en-portrait.png", landscape = false, menuOpen = true)
            pressBack()
            pressBack()
        }
    }

    private fun screenshot(name: String, landscape: Boolean, menuOpen: Boolean = false) {
        if (!menuOpen) instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()?.let {
                it.currentFocus?.clearFocus()
                it.findViewById<ScrollView>(R.id.root)?.scrollTo(0, 0)
            }
        }
        instrumentation.waitForIdleSync()
        onIdle()
        SystemClock.sleep(500)
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        assertEquals("Screenshot orientation", landscape, bitmap.width > bitmap.height)
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "assignment03").apply { mkdirs() }
        File(folder, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
