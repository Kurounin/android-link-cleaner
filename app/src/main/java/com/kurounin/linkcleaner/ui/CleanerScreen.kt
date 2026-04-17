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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kurounin.linkcleaner.R
import com.kurounin.linkcleaner.logic.CleanResult
import com.kurounin.linkcleaner.logic.LinkCleaner
import com.kurounin.linkcleaner.logic.Platform
import kotlinx.coroutines.launch

@Composable
fun CleanerScreen(
    initialInput: String = "",
    autoClean: Boolean = false,
    linkCleaner: LinkCleaner = remember { LinkCleaner() }
) {
    var state by remember { mutableStateOf(CleanerState(input = initialInput)) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    fun runClean(text: String) {
        if (text.isBlank()) return
        state = state.copy(isCleaning = true, result = null)
        scope.launch {
            val r = linkCleaner.cleanLink(text)
            state = state.copy(isCleaning = false, result = r)
            if (r is CleanResult.Success) {
                copyToClipboard(ctx, r.cleanUrl)
                Toast.makeText(ctx, ctx.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(initialInput, autoClean) {
        if (autoClean && initialInput.isNotBlank()) {
            runClean(initialInput)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = state.input,
                onValueChange = { state = state.copy(input = it) },
                label = { Text(stringResource(R.string.hint_paste_link)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Button(
                onClick = { runClean(state.input) },
                enabled = state.input.isNotBlank() && !state.isCleaning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_clean))
            }

            if (state.isCleaning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.cleaning),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            HorizontalDivider()

            when (val r = state.result) {
                is CleanResult.Success -> SuccessCard(r, ctx)
                is CleanResult.Unchanged -> UnchangedCard(r, ctx)
                is CleanResult.Error -> ErrorCard(r, state.input, ctx) { runClean(state.input) }
                null -> Unit
            }
        }
    }
}

@Composable
private fun SuccessCard(r: CleanResult.Success, ctx: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(platformLabel(r.platform), style = MaterialTheme.typography.labelLarge)
        Text(r.cleanUrl, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { copyToClipboard(ctx, r.cleanUrl) }) {
                Text(stringResource(R.string.btn_copy))
            }
            OutlinedButton(onClick = { shareText(ctx, r.cleanUrl) }) {
                Text(stringResource(R.string.btn_share))
            }
        }
        Text(
            "${stringResource(R.string.label_removed)}: ${r.removed}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun UnchangedCard(r: CleanResult.Unchanged, ctx: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(r.reason, style = MaterialTheme.typography.labelLarge)
        Text(r.url, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = { copyToClipboard(ctx, r.url) }) {
            Text(stringResource(R.string.btn_copy))
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            r.message,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { copyToClipboard(ctx, original) }) {
                Text(stringResource(R.string.btn_copy_original))
            }
            Button(onClick = onRetry) {
                Text(stringResource(R.string.btn_retry))
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
