package com.walyo.browser.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Fetches search suggestions for the address bar. Failures return an empty list. */
object SuggestionsClient {
    private const val TAG = "SuggestionsClient"
    private const val TIMEOUT_MS = 3_000
    private const val MAX_RESULTS = 6
    private const val ENDPOINT =
        "https://suggestqueries.google.com/complete/search?client=firefox&ie=UTF-8&oe=UTF-8&q="

    suspend fun fetch(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val connection = URL(ENDPOINT + URLEncoder.encode(query, "UTF-8"))
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parse(body)
        } catch (e: IOException) {
            Log.w(TAG, "Suggestions request failed", e)
            emptyList()
        } catch (e: org.json.JSONException) {
            Log.w(TAG, "Unexpected suggestions response", e)
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(body: String): List<String> {
        val results = JSONArray(body).getJSONArray(1)
        return List(minOf(results.length(), MAX_RESULTS)) { results.getString(it) }
    }
}
