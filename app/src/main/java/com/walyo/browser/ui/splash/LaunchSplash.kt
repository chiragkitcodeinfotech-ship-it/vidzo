package com.walyo.browser.ui.splash

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walyo.browser.R
import com.walyo.browser.ui.theme.WalyoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashBackground = Color(0xFF0D0F14)
private const val TAGLINE = "WATCH · BROWSE · ENJOY"
private const val WORDMARK = "Vidzo"

// Timeline (ms from launch). The whole intro settles in ~2.2 s.
private const val RIPPLE_AT = 450L
private const val WORDMARK_AT = 650L
private const val SHINE_AT = 1_000L
private const val TAGLINE_AT = 1_350L

/**
 * Animated launch screen shown over the app while the launch ad loads:
 * drifting brand glows, logo springs in with a "play" ripple and a glossy shine,
 * the wordmark tracks in, the tagline fades in, and a progress bar fills over [durationMs].
 */
@Composable
fun LaunchSplash(
    durationMs: Int,
    onFirstFrame: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-12f) }
    val ripple = remember { Animatable(0f) }
    val shine = remember { Animatable(0f) }
    val wordmark = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        withFrameNanos { } // first frame is on screen
        onFirstFrame()
        val bouncy = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
        launch { progress.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing)) }
        launch { logoAlpha.animateTo(1f, tween(300)) }
        launch { logoScale.animateTo(1f, bouncy) }
        launch { logoRotation.animateTo(0f, bouncy) }
        launch { delay(RIPPLE_AT); ripple.animateTo(1f, tween(1_100, easing = LinearOutSlowInEasing)) }
        launch { delay(WORDMARK_AT); wordmark.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch { delay(SHINE_AT); shine.animateTo(1f, tween(750, easing = FastOutSlowInEasing)) }
        launch { delay(TAGLINE_AT); tagline.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
    }

    val ambient = rememberInfiniteTransition(label = "ambient")
    val drift by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift",
    )

    Box(
        modifier
            .fillMaxSize()
            .background(SplashBackground)
            .drawBehind { drawAmbientGlows(drift) },
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                LogoHalo(alpha = logoAlpha.value, ripple = ripple.value)
                Box(
                    Modifier
                        .size(96.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            rotationZ = logoRotation.value
                            alpha = logoAlpha.value
                        }
                        .clip(RoundedCornerShape(26))
                        .drawWithContent {
                            drawContent()
                            drawShine(shine.value)
                        },
                ) {
                    Image(painterResource(R.drawable.ic_logo), contentDescription = null, Modifier.fillMaxSize())
                }
            }
            Text(
                text = WORDMARK,
                modifier = Modifier.graphicsLayer {
                    alpha = wordmark.value
                    translationY = (1f - wordmark.value) * 18.dp.toPx()
                },
                style = TextStyle(
                    brush = WalyoColors.Brand,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = lerp(14f, -0.5f, wordmark.value).sp,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = TAGLINE,
                modifier = Modifier.graphicsLayer { alpha = tagline.value },
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = lerp(7f, 3f, tagline.value).sp,
                ),
            )
        }
        ProgressLine(
            progress = progress.value,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
        )
    }
}

/** Soft glow behind the logo plus a ring that expands and fades like a "play" ripple. */
@Composable
private fun LogoHalo(alpha: Float, ripple: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(WalyoColors.Violet.copy(alpha = 0.55f * alpha), Color.Transparent),
                center = center,
                radius = size.minDimension / 2,
            ),
            radius = size.minDimension / 2,
            center = center,
        )
        if (ripple in 0.001f..0.999f) {
            drawCircle(
                color = WalyoColors.Cyan.copy(alpha = 0.7f * (1f - ripple)),
                radius = lerp(48.dp.toPx(), size.minDimension / 2, ripple),
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun ProgressLine(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(120.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(WalyoColors.Brand),
        )
    }
}

/** Two large brand-coloured glows that slowly drift, so the screen never looks frozen. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAmbientGlows(drift: Float) {
    val radius = size.maxDimension * 0.55f
    val violetCenter = Offset(size.width * lerp(0.1f, 0.3f, drift), size.height * lerp(0.15f, 0.25f, drift))
    val cyanCenter = Offset(size.width * lerp(0.9f, 0.7f, drift), size.height * lerp(0.85f, 0.75f, drift))
    drawGlow(WalyoColors.Violet.copy(alpha = 0.28f), violetCenter, radius)
    drawGlow(WalyoColors.Cyan.copy(alpha = 0.18f), cyanCenter, radius)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlow(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}

/** Diagonal glossy light band sweeping across the logo once ([progress] 0 → 1). */
private fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.drawShine(progress: Float) {
    if (progress <= 0f || progress >= 1f) return
    val band = size.width * 0.35f
    val x = lerp(-band, size.width + band, progress)
    drawRect(
        brush = Brush.linearGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
            start = Offset(x - band, 0f),
            end = Offset(x + band, size.height),
        ),
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction
