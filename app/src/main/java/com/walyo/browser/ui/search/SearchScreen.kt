package com.walyo.browser.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walyo.browser.data.HistoryEntry
import com.walyo.browser.data.SuggestionsClient
import com.walyo.browser.util.UrlUtils
import kotlinx.coroutines.delay

private const val SUGGEST_DEBOUNCE_MS = 200L
private const val HISTORY_MATCHES = 5

@Composable
fun SearchScreen(
    initialText: String,
    suggestionsEnabled: Boolean,
    history: List<HistoryEntry>,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var field by remember { mutableStateOf(TextFieldValue(initialText, TextRange(0, initialText.length))) }
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    val focusRequester = remember { FocusRequester() }
    val query = field.text.trim()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(query, suggestionsEnabled) {
        if (!suggestionsEnabled || query.isEmpty() || UrlUtils.isWebUrl(query)) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(SUGGEST_DEBOUNCE_MS)
        suggestions = SuggestionsClient.fetch(query)
    }
    val historyMatches = remember(query, history) {
        history.filter {
            query.isEmpty() || it.url.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true)
        }.take(HISTORY_MATCHES)
    }
    BackHandler(onBack = onDismiss)

    fun submit(text: String) {
        if (text.isNotBlank()) onSubmit(text)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            SearchField(
                value = field,
                onValueChange = { field = it },
                onSubmit = { submit(field.text) },
                onClear = { field = TextFieldValue("") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
                    .focusRequester(focusRequester),
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(suggestions) { suggestion ->
                SuggestionRow(
                    icon = Icons.Rounded.Search,
                    title = suggestion,
                    subtitle = null,
                    onClick = { submit(suggestion) },
                    onFill = { field = TextFieldValue(suggestion, TextRange(suggestion.length)) },
                )
            }
            items(historyMatches, key = { "h_" + it.url }) { entry ->
                SuggestionRow(
                    icon = Icons.Rounded.History,
                    title = entry.title,
                    subtitle = UrlUtils.displayUrl(entry.url),
                    onClick = { submit(entry.url) },
                    onFill = { field = TextFieldValue(entry.url, TextRange(entry.url.length)) },
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surfaceVariant)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.text.isEmpty()) {
                        Text("Search or type address", color = colors.onSurfaceVariant)
                    }
                    inner()
                }
                if (value.text.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = colors.onSurfaceVariant)
                    }
                }
            }
        },
    )
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    onFill: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        IconButton(onClick = onFill) {
            Icon(Icons.Rounded.NorthWest, contentDescription = "Edit", tint = colors.onSurfaceVariant)
        }
    }
}
