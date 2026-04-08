package de.mysportsmate.officebreak.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.datastore.preferences.core.stringPreferencesKey
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.Locale

object LocaleHelper {

    fun createLocalizedContext(context: Context, language: String): Context {
        val locale = resolveLocale(language)

        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }

        return context.createConfigurationContext(config)
    }

    fun applyLocaleToContext(context: Context): Context {
        val language = readLanguagePreference(context)
        return createLocalizedContext(context, language)
    }

    fun resolveLocale(language: String): Locale {
        return if (language == SettingsRepository.LANGUAGE_SYSTEM) {
            Resources.getSystem().configuration.locales[0]
        } else {
            Locale.forLanguageTag(language)
        }
    }

    private fun readLanguagePreference(context: Context): String {
        return try {
            runBlocking {
                withTimeout(1000L) {
                    context.dataStore.data.map { prefs ->
                        prefs[stringPreferencesKey("language")] ?: SettingsRepository.LANGUAGE_SYSTEM
                    }.first()
                }
            }
        } catch (_: Exception) {
            SettingsRepository.LANGUAGE_SYSTEM
        }
    }
}
