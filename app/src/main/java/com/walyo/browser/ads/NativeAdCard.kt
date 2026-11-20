package com.walyo.browser.ads

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.walyo.browser.R
import com.walyo.browser.ui.theme.WalyoColors
import kotlinx.coroutines.awaitCancellation

private const val TAG = "NativeAdCard"

private data class AdPalette(
    val surface: Int,
    val outline: Int,
    val text: Int,
    val muted: Int,
    val ctaStart: Int,
    val ctaEnd: Int,
    val badge: Int,
)

/** AdMob native ad styled to match the Walyo home page. Renders nothing until an ad loads. */
@Composable
fun NativeAdCard(adUnitId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    LaunchedEffect(adUnitId) {
        MobileAdsGate.awaitReady()
        var disposed = false
        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                if (disposed) {
                    ad.destroy()
                } else {
                    nativeAd?.destroy()
                    nativeAd = ad
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.code} ${error.message}")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                    .build(),
            )
            .build()
        loader.loadAd(AdRequest.Builder().build())
        try {
            awaitCancellation()
        } finally {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return
    val scheme = MaterialTheme.colorScheme
    val palette = AdPalette(
        surface = scheme.surfaceContainer.toArgb(),
        outline = scheme.outline.toArgb(),
        text = scheme.onSurface.toArgb(),
        muted = scheme.onSurfaceVariant.toArgb(),
        ctaStart = WalyoColors.Violet.toArgb(),
        ctaEnd = WalyoColors.Cyan.toArgb(),
        badge = WalyoColors.Amber.toArgb(),
    )
    AndroidView(
        modifier = modifier,
        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.native_ad, null) as NativeAdView },
        update = { view -> view.bind(ad, palette) },
    )
}

private fun NativeAdView.bind(ad: NativeAd, palette: AdPalette) {
    val density = resources.displayMetrics.density
    val root = findViewById<LinearLayout>(R.id.ad_root)
    val badge = findViewById<TextView>(R.id.ad_badge)
    val advertiser = findViewById<TextView>(R.id.ad_advertiser)
    val media = findViewById<MediaView>(R.id.ad_media)
    val icon = findViewById<ImageView>(R.id.ad_icon)
    val headline = findViewById<TextView>(R.id.ad_headline)
    val body = findViewById<TextView>(R.id.ad_body)
    val stars = findViewById<RatingBar>(R.id.ad_stars)
    val store = findViewById<TextView>(R.id.ad_store)
    val cta = findViewById<TextView>(R.id.ad_cta)

    root.background = GradientDrawable().apply {
        cornerRadius = 20 * density
        setColor(palette.surface)
        setStroke(density.toInt().coerceAtLeast(1), palette.outline)
    }
    badge.background = GradientDrawable().apply {
        cornerRadius = 4 * density
        setColor(palette.badge)
    }
    cta.background = GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(palette.ctaStart, palette.ctaEnd),
    ).apply { cornerRadius = 24 * density }

    headline.setTextColor(palette.text)
    listOf(advertiser, body, store).forEach { it.setTextColor(palette.muted) }
    stars.progressTintList = ColorStateList.valueOf(WalyoColors.Amber.toArgb())

    headlineView = headline
    headline.text = ad.headline

    bodyView = body
    body.setTextOrHide(ad.body)

    advertiserView = advertiser
    advertiser.text = ad.advertiser.orEmpty()

    storeView = store
    store.setTextOrHide(ad.store)

    callToActionView = cta
    cta.setTextOrHide(ad.callToAction)

    iconView = icon
    val iconDrawable = ad.icon?.drawable
    icon.setImageDrawable(iconDrawable)
    icon.visibility = if (iconDrawable == null) View.GONE else View.VISIBLE

    starRatingView = stars
    val rating = ad.starRating
    stars.visibility = if (rating == null) View.GONE else View.VISIBLE
    rating?.let { stars.rating = it.toFloat() }

    mediaView = media
    ad.mediaContent?.let { media.setMediaContent(it) }

    setNativeAd(ad)
}

private fun TextView.setTextOrHide(value: String?) {
    text = value.orEmpty()
    visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
}
