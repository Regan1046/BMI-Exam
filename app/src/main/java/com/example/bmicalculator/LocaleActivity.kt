package com.example.bmicalculator

import android.app.AlertDialog
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.Menu
import android.view.MenuItem
import android.widget.Toolbar
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

/** Native options menu shared by both screens; no BMI business rules live here. */
abstract class LocalizedActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // Android 12 及以下在加载布局前应用本地保存的语言，确保 XML 文案随之切换。
        val tag = newBase.getSharedPreferences("settings", MODE_PRIVATE).getString("language", null)
        val localized = if (Build.VERSION.SDK_INT < 33 && tag != null) {
            newBase.createConfigurationContext(Configuration(newBase.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(tag))
            })
        } else newBase
        super.attachBaseContext(localized)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    protected fun setupChrome(report: Boolean = false) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setActionBar(toolbar)
        actionBar?.setTitle(R.string.app_name)
        actionBar?.setDisplayHomeAsUpEnabled(report)
        toolbar.setNavigationContentDescription(R.string.back)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.screenRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < 33) {
            val saved = getSharedPreferences("settings", MODE_PRIVATE).getString("language", null)
            if (saved != null && resources.configuration.locales[0].language != Locale.forLanguageTag(saved).language) recreate()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val chinese = resources.configuration.locales[0].language == "zh"
        // In an exclusive Android menu group, check only the active item.
        menu.findItem(if (chinese) R.id.languageChinese else R.id.languageEnglish).isChecked = true
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { finish(); true }
        R.id.languageChinese -> { changeLanguage("zh-Hans"); true }
        R.id.languageEnglish -> { changeLanguage("en"); true }
        R.id.menuAbout -> { showAbout(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun changeLanguage(tag: String) {
        // Android 13 起由系统管理应用语言。旧版保存偏好后重建页面以加载新资源。
        if (resources.configuration.locales[0].language == Locale.forLanguageTag(tag).language) return
        if (Build.VERSION.SDK_INT >= 33) {
            getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putString("language", tag).apply()
            recreate()
        }
    }

    protected fun showAbout() {
        AlertDialog.Builder(this).setTitle(R.string.about_title).setMessage(R.string.about_message)
            .setPositiveButton(R.string.got_it, null).show()
    }
}
