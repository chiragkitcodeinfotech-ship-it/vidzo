package com.walyo.browser.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.historyStore: DataStore<Preferences> by preferencesDataStore(name = "walyo_history")

/** Browsing history, newest first, de-duplicated by URL and capped at [MAX_ENTRIES]. */
class HistoryRepository(context: Context) {

    private val store = context.applicationContext.historyStore

    val history: Flow<List<HistoryEntry>> = store.data
        .catch { error ->
            if (error is IOException) {
                Log.e(TAG, "Could not read history", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { JsonCodec.decodeHistory(it[KEY_HISTORY]) }

    suspend fun record(url: String, title: String, visitedAt: Long = System.currentTimeMillis()) =
        store.edit { prefs ->
            val existing = JsonCodec.decodeHistory(prefs[KEY_HISTORY])
            val updated = listOf(HistoryEntry(title, url, visitedAt)) +
                existing.filterNot { it.url == url }
            prefs[KEY_HISTORY] = JsonCodec.encodeHistory(updated.take(MAX_ENTRIES))
        }

    suspend fun remove(url: String) = store.edit { prefs ->
        val remaining = JsonCodec.decodeHistory(prefs[KEY_HISTORY]).filterNot { it.url == url }
        prefs[KEY_HISTORY] = JsonCodec.encodeHistory(remaining)
    }

    suspend fun clear() = store.edit { it.remove(KEY_HISTORY) }

    private companion object {
        const val TAG = "HistoryRepository"
        const val MAX_ENTRIES = 300
        val KEY_HISTORY = stringPreferencesKey("entries")
    }
}
