package com.walyo.browser.data

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Small org.json helpers for storing lists inside DataStore preferences. */
internal object JsonCodec {
    private const val TAG = "JsonCodec"

    fun encodeShortcuts(items: List<Shortcut>): String = JSONArray().apply {
        items.forEach { put(JSONObject().put("title", it.title).put("url", it.url)) }
    }.toString()

    fun decodeShortcuts(json: String?): List<Shortcut>? = decode(json) { obj ->
        Shortcut(obj.getString("title"), obj.getString("url"))
    }

    fun encodeHistory(items: List<HistoryEntry>): String = JSONArray().apply {
        items.forEach {
            put(JSONObject().put("title", it.title).put("url", it.url).put("at", it.visitedAt))
        }
    }.toString()

    fun decodeHistory(json: String?): List<HistoryEntry> = decode(json) { obj ->
        HistoryEntry(obj.getString("title"), obj.getString("url"), obj.getLong("at"))
    }.orEmpty()

    fun encodeTabs(items: List<TabSnapshot>): String = JSONArray().apply {
        items.forEach { put(JSONObject().put("url", it.url ?: JSONObject.NULL).put("title", it.title)) }
    }.toString()

    fun decodeTabs(json: String?): List<TabSnapshot> = decode(json) { obj ->
        TabSnapshot(if (obj.isNull("url")) null else obj.getString("url"), obj.optString("title"))
    }.orEmpty()

    private fun <T> decode(json: String?, mapper: (JSONObject) -> T): List<T>? {
        if (json.isNullOrBlank()) return null
        return try {
            val array = JSONArray(json)
            List(array.length()) { index -> mapper(array.getJSONObject(index)) }
        } catch (e: JSONException) {
            Log.e(TAG, "Corrupted stored list, ignoring it", e)
            null
        }
    }
}
