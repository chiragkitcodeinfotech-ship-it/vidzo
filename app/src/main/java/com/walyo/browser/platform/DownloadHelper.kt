package com.walyo.browser.platform

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.core.content.ContextCompat

data class DownloadRequest(
    val url: String,
    val userAgent: String,
    val contentDisposition: String?,
    val mimeType: String?,
)

object DownloadHelper {

    fun needsStoragePermission(context: Context): Boolean =
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED

    /** Hands the file to Android's DownloadManager. Returns the file name. */
    fun enqueue(context: Context, request: DownloadRequest): String {
        val fileName = URLUtil.guessFileName(request.url, request.contentDisposition, request.mimeType)
        val systemRequest = DownloadManager.Request(Uri.parse(request.url)).apply {
            request.mimeType?.let(::setMimeType)
            CookieManager.getInstance().getCookie(request.url)?.let { addRequestHeader("Cookie", it) }
            addRequestHeader("User-Agent", request.userAgent)
            setTitle(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        context.getSystemService(DownloadManager::class.java).enqueue(systemRequest)
        return fileName
    }
}
