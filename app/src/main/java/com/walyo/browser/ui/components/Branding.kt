package com.walyo.browser.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.walyo.browser.R
import com.walyo.browser.ui.theme.WalyoColors
import com.walyo.browser.util.UrlUtils

@Composable
fun WalyoLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = "Vidzo",
        modifier = modifier.size(size),
    )
}

@Composable
fun WalyoWordmark(fontSize: TextUnit = 30.sp, modifier: Modifier = Modifier) {
    Text(
        text = "Vidzo",
        modifier = modifier,
        style = TextStyle(
            brush = WalyoColors.Brand,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
        ),
    )
}

/** Site icon loaded from the favicon service, falling back to the first letter of the host. */
@Composable
fun Favicon(url: String, size: Dp, modifier: Modifier = Modifier) {
    val host = UrlUtils.displayHost(url)
    var failed by remember(host) { mutableStateOf(false) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (failed || host.isBlank()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(size * 0.3f))
                    .background(WalyoColors.Brand),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = host.firstOrNull()?.uppercase() ?: "•",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.5f).sp,
                )
            }
        } else {
            AsyncImage(
                model = UrlUtils.faviconUrl(url),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(size * 0.2f)),
                onError = { failed = true },
            )
        }
    }
}

/** Small gradient circle holding an icon — used on promo cards and empty states. */
@Composable
fun GradientBadge(size: Dp = 36.dp, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .background(WalyoColors.Brand),
        contentAlignment = Alignment.Center,
    ) { content() }
}
