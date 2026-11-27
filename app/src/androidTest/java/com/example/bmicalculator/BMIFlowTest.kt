package com.example.bmicalculator

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList

import android.graphics.Bitmap
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import android.widget.ScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BMIFlowTest {
    @Before fun clearTestProfile() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        context.getSharedPreferences("body_data", 0).edit().clear().commit()
        context.getSharedPreferences("settings", 0).edit().clear().commit()
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("en")
        }
        instrumentation.waitForIdleSync()
    }

    private fun enterProfile(age: String = "25", height: String = "170", weight: String = "65") {
        onView(withId(R.id.heightInput)).perform(scrollTo(), replaceText(height), closeSoftKeyboard())
        onView(withId(R.id.weightInput)).perform(scrollTo(), replaceText(weight), closeSoftKeyboard())
        onView(withId(R.id.ageInput)).perform(scrollTo(), replaceText(age), closeSoftKeyboard())
        onView(withId(R.id.femaleOption)).perform(scrollTo(), click())
    }

    @Test fun emptyInputsShowErrors() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            onView(withId(R.id.heightInput)).check(matches(hasErrorText("Enter a height from 30 to 250 cm.")))
        }
    }

    @Test fun ageBoundaryAndRotationPreserveTheForm() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            enterProfile("18")
            onView(withId(R.id.chartHint)).check(matches(withText(R.string.child_hint)))
            scenario.recreate()
            onView(withId(R.id.ageInput)).check(matches(withText("18")))
            onView(withId(R.id.femaleOption)).check(matches(isChecked()))
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            onView(withId(R.id.reportGroup)).check(matches(withText(R.string.child_report)))
            onView(withId(R.id.categoryText)).check(matches(withText(R.string.acceptable_weight)))
            pressBack()
            onView(withId(R.id.ageInput)).perform(scrollTo(), replaceText("19"), closeSoftKeyboard())
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            onView(withId(R.id.reportGroup)).check(matches(withText(R.string.adult_report)))
            onView(withId(R.id.categoryText)).check(matches(withText(R.string.normal_range)))
            pressBack()
        }
    }

    @Test fun captureBothScreensAndCheckSavedProfile() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            enterProfile("12", "150", "70")
            scenario.onActivity {
                it.currentFocus?.clearFocus()
                it.findViewById<ScrollView>(R.id.root).scrollTo(0, 0)
            }
            screenshot("01-input.png")
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            onView(withId(R.id.bmiValue)).check(matches(withText("31.11")))
            onView(withId(R.id.categoryText)).check(matches(withText(R.string.severely_overweight)))
            onView(withId(R.id.warningBadge)).check(matches(isDisplayed()))
            screenshot("02-report.png")
            onView(withId(R.id.editButton)).perform(scrollTo(), click())
            onView(withId(R.id.heightInput)).check(matches(withText("150")))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.heightInput)).check(matches(withText("150")))
            onView(withId(R.id.ageInput)).check(matches(withText("12")))
            onView(withId(R.id.femaleOption)).check(matches(isChecked()))
        }
    }

    @Test fun severeUnderweightHasWarningAndReferenceRanges() {
        ActivityScenario.launch(MainActivity::class.java).use {
            enterProfile("12", "150", "24")
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            onView(withId(R.id.categoryText)).check(matches(withText(R.string.severely_underweight)))
            onView(withId(R.id.warningBadge)).check(matches(isDisplayed()))
            onView(withId(R.id.chartButton)).perform(scrollTo(), click())
            onView(withText("Age 12 · Girls · BMI ranges")).check(matches(isDisplayed()))
            onView(withText(R.string.got_it)).perform(click())
            pressBack()
        }
    }

    @Test fun changingGenderChangesTheChildCategory() {
        ActivityScenario.launch(MainActivity::class.java).use {
            enterProfile("12", "150", "54")
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            onView(withId(R.id.categoryText)).check(matches(withText(R.string.overweight)))
            pressBack()
            onView(withId(R.id.maleOption)).perform(scrollTo(), click())
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            onView(withId(R.id.categoryText)).check(matches(withText(R.string.acceptable_weight)))
            pressBack()
        }
    }

    @Test fun unsupportedChildAgeShowsAnHonestLimit() {
        ActivityScenario.launch(MainActivity::class.java).use {
            enterProfile("5", "110", "20")
            onView(withId(R.id.chartHint)).check(matches(withText(R.string.unsupported_hint)))
            onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
            onView(withId(R.id.categoryText)).check(matches(withText(R.string.outside_age_range)))
            onView(withId(R.id.chartButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            pressBack()
        }
    }

    @Test fun adultObeseReportRemainsUsableInLandscape() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, ReportActivity::class.java).putExtra("age", 25)
            .putExtra("height", 170.0).putExtra("weight", 90.0).putExtra("gender", "MALE")
        ActivityScenario.launch<ReportActivity>(intent).use { scenario ->
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            val deadline = SystemClock.uptimeMillis() + 5000
            var landscapeReady = false
            while (!landscapeReady && SystemClock.uptimeMillis() < deadline) {
                scenario.onActivity {
                    landscapeReady = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                        it.window.decorView.width > it.window.decorView.height
                }
                if (!landscapeReady) SystemClock.sleep(50)
            }
            org.junit.Assert.assertTrue("Landscape layout must settle before tapping", landscapeReady)
            scenario.recreate()
            onView(withId(R.id.categoryText)).check(matches(withText(R.string.obese)))
            onView(withId(R.id.chartButton)).perform(scrollTo())
            // Wait for the display/scroll animation before injecting rotated touch coordinates.
            SystemClock.sleep(400)
            onView(withId(R.id.chartButton)).check(matches(isDisplayed())).perform(click())
            onView(withText(R.string.adult_chart_title)).check(matches(isDisplayed()))
            onView(withText(R.string.got_it)).perform(click())
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        }
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        onIdle()
        // UI idleness does not include the platform radio-button ripple animation.
        SystemClock.sleep(400)
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
