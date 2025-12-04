package com.kiryusha.media.activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kiryusha.media.utils.AppPreferences
import com.kiryusha.media.utils.LocaleManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val appPreferences = AppPreferences(newBase)
        var languageCode = "en"

        try {
            kotlinx.coroutines.runBlocking {
                languageCode = appPreferences.getLanguage().first()
            }
        } catch (e: Exception) {
        }

        val context = LocaleManager.setLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }
}
