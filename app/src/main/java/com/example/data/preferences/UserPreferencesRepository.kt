package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qr_settings")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_DARK_MODE = intPreferencesKey("dark_mode") // 0: System, 1: Light, 2: Dark
        val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
        val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        val KEY_AUTO_OPEN_URL = booleanPreferencesKey("auto_open_url")
        val KEY_LANGUAGE = stringPreferencesKey("language")
    }

    val darkModeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE] ?: 0
    }

    val vibrationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_VIBRATION] ?: true
    }

    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SOUND] ?: true
    }

    val autoOpenUrlFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_OPEN_URL] ?: false
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "en"
    }

    suspend fun setDarkMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = mode
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VIBRATION] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SOUND] = enabled
        }
    }

    suspend fun setAutoOpenUrl(autoOpen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_OPEN_URL] = autoOpen
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = lang
        }
    }
}
