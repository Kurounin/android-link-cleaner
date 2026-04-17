package com.kurounin.linkcleaner.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kurounin.linkcleaner.R
import com.kurounin.linkcleaner.logic.CleanResult
import com.kurounin.linkcleaner.logic.LinkCleaner
import com.kurounin.linkcleaner.logic.Platform
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    initialInput: String = "",
    autoClean: Boolean = false,
    linkCleaner: LinkCleaner = remember { LinkCleaner() }
) {
    var state by remember { mutableStateOf(CleanerState(input = initialInput)) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val toastCopiedMessage = stringResource(R.string.toast_copied)

    fun runClean(text: String) {
        if (text.isBlank()) return
        state = state.copy(isCleaning = true, result = null)
        scope.launch {
            val r = linkCleaner.cleanLink(text)
            state = state.copy(isCleaning = false, result = r)
            if (r is CleanResult.Success) {
                copyToClipboard(ctx, r.cleanUrl)
                Toast.makeText(ctx, toastCopiedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(initialInput, autoClean) {
        if (autoClean && initialInput.isNotBlank()) {
            runClean(initialInput)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InputSection(
                input = state.input,
                isCleaning = state.isCleaning,
                onInputChange = { state = state.copy(input = it) },
                onClean = { runClean(state.input) },
                onPaste = {
                    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.primaryClip?.getItemAt(0)?.text?.let {
                        state = state.copy(input = it.toString())
                    }
                }
            )

            if (state.isCleaning) {
                CleaningIndicator()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            when (val r = state.result) {
                is CleanResult.Success -> SuccessCard(r, ctx)
                is CleanResult.Unchanged -> UnchangedCard(r, ctx)
                is CleanResult.Error -> ErrorCard(r, state.input, ctx) { runClean(state.input) }
                null -> if (!state.isCleaning) EmptyState()
            }
        }
    }
}

@Composable
private fun InputSection(
    input: String,
    isCleaning: Boolean,
    onInputChange: (String) -> Unit,
    onClean: () -> Unit,
    onPaste: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = { Text(stringResource(R.string.hint_paste_link)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row {
                    if (input.isNotEmpty()) {
                        IconButton(onClick = { onInputChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.cd_clear))
                        }
                    }
                    IconButton(onClick = onPaste) {
                        Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.cd_paste))
                    }
                }
            },
            shape = MaterialTheme.shapes.medium
        )

        Button(
            onClick = onClean,
            enabled = input.isNotBlank() && !isCleaning,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_clean))
        }
    }
}

@Composable
private fun CleaningIndicator() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(R.string.cleaning),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Link,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = themeColor()
        )
        Text(
            stringResource(R.string.empty_state_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun themeColor(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)

@Composable
private fun SuccessCard(r: CleanResult.Success, ctx: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    platformLabel(r.platform),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                r.cleanUrl,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { copyToClipboard(ctx, r.cleanUrl) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_copy))
                }
                OutlinedButton(
                    onClick = { shareText(ctx, r.cleanUrl) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_share))
                }
            }

            if (r.removed.isNotBlank()) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "${stringResource(R.string.label_removed)}: ${r.removed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun UnchangedCard(r: CleanResult.Unchanged, ctx: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.label_unchanged),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(r.reason, style = MaterialTheme.typography.bodyMedium)
            Text(r.url, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = { copyToClipboard(ctx, r.url) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_copy))
            }
        }
    }
}

@Composable
private fun ErrorCard(
    r: CleanResult.Error,
    original: String,
    ctx: Context,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    r.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { copyToClipboard(ctx, original) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_copy_original))
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_retry))
                }
            }
        }
    }
}

private fun platformLabel(p: Platform): String = when (p) {
    Platform.TIKTOK -> "TikTok"
    Platform.INSTAGRAM -> "Instagram"
    Platform.YOUTUBE -> "YouTube"
    Platform.OTHER -> "Other"
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Clean URL", text))
}

private fun shareText(ctx: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(intent, null))
}
