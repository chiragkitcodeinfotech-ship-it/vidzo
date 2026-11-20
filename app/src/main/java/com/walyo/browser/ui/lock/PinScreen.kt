package com.walyo.browser.ui.lock

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walyo.browser.ui.components.WalyoLogo
import com.walyo.browser.ui.theme.WalyoColors
import com.walyo.browser.util.PinHasher
import kotlinx.coroutines.delay

enum class PinMode { UNLOCK, CREATE, VERIFY }

private const val MAX_ATTEMPTS = 5
private const val COOLDOWN_MS = 30_000L
private val KEYS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")

/**
 * Full-screen PIN pad.
 * UNLOCK / VERIFY check against [storedHash]; CREATE asks twice and returns the new hash.
 */
@Composable
fun PinScreen(
    mode: PinMode,
    storedHash: String?,
    onSuccess: (newHash: String?) -> Unit,
    onCancel: (() -> Unit)?,
) {
    val activity = LocalActivity.current
    var entered by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var failures by remember { mutableIntStateOf(0) }
    var coolingDown by remember { mutableStateOf(false) }

    LaunchedEffect(coolingDown) {
        if (coolingDown) {
            delay(COOLDOWN_MS)
            coolingDown = false
            failures = 0
            error = null
        }
    }

    BackHandler {
        if (onCancel != null) onCancel() else activity?.moveTaskToBack(true)
    }

    fun checkPin(pin: String) {
        when (mode) {
            PinMode.CREATE -> {
                val first = firstPin
                when {
                    first == null -> firstPin = pin
                    first == pin -> onSuccess(PinHasher.hash(pin))
                    else -> {
                        firstPin = null
                        error = "PINs didn't match. Try again."
                    }
                }
            }
            PinMode.UNLOCK, PinMode.VERIFY -> {
                if (PinHasher.verify(pin, storedHash)) {
                    onSuccess(null)
                } else {
                    failures++
                    if (failures >= MAX_ATTEMPTS) {
                        coolingDown = true
                        error = "Too many attempts. Wait 30 seconds."
                    } else {
                        error = "Wrong PIN"
                    }
                }
            }
        }
        entered = ""
    }

    fun onKey(key: String) {
        if (coolingDown) return
        when (key) {
            "⌫" -> entered = entered.dropLast(1)
            "" -> Unit
            else -> if (entered.length < PinHasher.PIN_LENGTH) {
                error = null
                entered += key
                if (entered.length == PinHasher.PIN_LENGTH) checkPin(entered)
            }
        }
    }

    val title = when {
        mode == PinMode.CREATE && firstPin == null -> "Create a 4-digit PIN"
        mode == PinMode.CREATE -> "Confirm your PIN"
        mode == PinMode.VERIFY -> "Enter your current PIN"
        else -> "Enter PIN to unlock Vidzo"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Swallow touches so nothing behind the lock screen is clickable.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WalyoLogo(size = 64.dp)
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        PinDots(filled = entered.length)
        Spacer(Modifier.height(16.dp))
        Text(
            error.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.height(22.dp),
        )
        Spacer(Modifier.height(16.dp))
        Keypad(enabled = !coolingDown, onKey = ::onKey)
        if (onCancel != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun PinDots(filled: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(PinHasher.PIN_LENGTH) { index ->
            val modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
            Box(
                if (index < filled) {
                    modifier.background(WalyoColors.Brand)
                } else {
                    modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                },
            )
        }
    }
}

@Composable
private fun Keypad(enabled: Boolean, onKey: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        KEYS.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { key -> KeyButton(key, enabled, onKey) }
            }
        }
    }
}

@Composable
private fun KeyButton(key: String, enabled: Boolean, onKey: (String) -> Unit) {
    val base = Modifier
        .size(72.dp)
        .clip(CircleShape)
    if (key.isEmpty()) {
        Spacer(base)
        return
    }
    Box(
        base
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled) { onKey(key) },
        contentAlignment = Alignment.Center,
    ) {
        if (key == "⌫") {
            Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Delete")
        } else {
            Text(key, fontSize = 26.sp, style = MaterialTheme.typography.titleLarge)
        }
    }
}
