package com.walyo.browser.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "walyo_settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsStore

    private val safeData: Flow<Preferences> = store.data.catch { error ->
        if (error is IOException) {
            Log.e(TAG, "Could not read settings, using defaults", error)
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    val settings: Flow<Settings> = safeData.map { it.toSettings() }

    suspend fun setTheme(mode: ThemeMode) = store.edit { it[KEY_THEME] = mode.name }

    suspend fun setSearchEngine(engine: SearchEngine) = store.edit { it[KEY_ENGINE] = engine.name }

    suspend fun setSuggestionsEnabled(enabled: Boolean) = store.edit { it[KEY_SUGGESTIONS] = enabled }

    suspend fun setPinHash(hash: String?) = store.edit {
        if (hash == null) it.remove(KEY_PIN) else it[KEY_PIN] = hash
    }

    suspend fun setAdFreeUntil(timestamp: Long) = store.edit { it[KEY_AD_FREE_UNTIL] = timestamp }

    suspend fun setShortcuts(shortcuts: List<Shortcut>) = store.edit {
        it[KEY_SHORTCUTS] = JsonCodec.encodeShortcuts(shortcuts.take(Settings.MAX_SHORTCUTS))
    }

    /** Reads and writes the shortcuts in one atomic edit. */
    suspend fun updateShortcuts(transform: (List<Shortcut>) -> List<Shortcut>) = store.edit {
        val current = JsonCodec.decodeShortcuts(it[KEY_SHORTCUTS]) ?: Settings.DEFAULT.shortcuts
        it[KEY_SHORTCUTS] = JsonCodec.encodeShortcuts(transform(current).take(Settings.MAX_SHORTCUTS))
    }

    suspend fun isReferralChecked(): Boolean = safeData.first()[KEY_REFERRAL_CHECKED] == true

    suspend fun setReferralChecked() = store.edit { it[KEY_REFERRAL_CHECKED] = true }

    suspend fun saveTabs(tabs: List<TabSnapshot>, currentIndex: Int) = store.edit {
        it[KEY_TABS] = JsonCodec.encodeTabs(tabs)
        it[KEY_CURRENT_TAB] = currentIndex
    }

    suspend fun loadTabs(): SavedTabs {
        val prefs = safeData.first()
        return SavedTabs(JsonCodec.decodeTabs(prefs[KEY_TABS]), prefs[KEY_CURRENT_TAB] ?: 0)
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings.DEFAULT
        return Settings(
            theme = enumOrDefault(this[KEY_THEME], defaults.theme),
            searchEngine = enumOrDefault(this[KEY_ENGINE], defaults.searchEngine),
            suggestionsEnabled = this[KEY_SUGGESTIONS] ?: defaults.suggestionsEnabled,
            pinHash = this[KEY_PIN],
            adFreeUntil = this[KEY_AD_FREE_UNTIL] ?: defaults.adFreeUntil,
            shortcuts = JsonCodec.decodeShortcuts(this[KEY_SHORTCUTS]) ?: defaults.shortcuts,
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val TAG = "SettingsRepository"
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_ENGINE = stringPreferencesKey("search_engine")
        val KEY_SUGGESTIONS = booleanPreferencesKey("suggestions")
        val KEY_PIN = stringPreferencesKey("pin_hash")
        val KEY_AD_FREE_UNTIL = longPreferencesKey("ad_free_until")
        val KEY_SHORTCUTS = stringPreferencesKey("shortcuts")
        val KEY_TABS = stringPreferencesKey("tabs")
        val KEY_CURRENT_TAB = intPreferencesKey("current_tab")
        val KEY_REFERRAL_CHECKED = booleanPreferencesKey("referral_checked")
    }
}
