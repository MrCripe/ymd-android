package com.mrcriper.ymd.presentation.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrcriper.ymd.R
import com.mrcriper.ymd.presentation.components.SettingsGroup
import com.mrcriper.ymd.presentation.theme.YmdTheme
import com.mrcriper.ymd.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenAuth: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            // Persist read+write permission so we can write into the picked folder later.
            runCatching {
                it.let { u ->
                    viewModel.update { s ->
                        s.copy(downloadPath = u.toString())
                    }
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_settings)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsGroup(title = "Download folder") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings.downloadPath?.let {
                                // Show just the last path segment for readability
                                it.substringAfterLast('/').ifBlank { it }.take(60)
                            } ?: "Not set",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = settings.downloadPath ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = { directoryPicker.launch(null) }) {
                        Icon(Icons.Filled.Folder, contentDescription = "Pick folder")
                    }
                }
                if (settings.downloadPath != null) {
                    TextButton(onClick = { viewModel.update { it.copy(downloadPath = null) } }) {
                        Text("Clear")
                    }
                }
            }
            SettingsGroup(title = stringResource(R.string.settings_download_group)) {
                RowSetting("Skip existing", settings.skipExisting) { viewModel.update { it.copy(skipExisting = !it.skipExisting) } }
                RowSetting("Embed cover", settings.embedCover) { viewModel.update { it.copy(embedCover = !it.embedCover) } }
                RowSetting("Only music", settings.onlyMusic) { viewModel.update { it.copy(onlyMusic = !it.onlyMusic) } }
                RowSetting("Unsafe path", settings.unsafePath) { viewModel.update { it.copy(unsafePath = !it.unsafePath) } }
                Spacer(Modifier.padding(4.dp))
                Text("Cover resolution: ${settings.coverResolution}px")
                Slider(
                    value = settings.coverResolution.toFloat(),
                    onValueChange = { v -> viewModel.update { it.copy(coverResolution = v.toInt().coerceIn(100, 4000)) } },
                    valueRange = 100f..4000f,
                )
            }
            SettingsGroup(title = stringResource(R.string.settings_network_group)) {
                Text("Timeout: ${settings.timeoutSeconds}s")
                Slider(
                    value = settings.timeoutSeconds.toFloat(),
                    onValueChange = { v -> viewModel.update { it.copy(timeoutSeconds = v.toInt().coerceIn(1, 120)) } },
                    valueRange = 1f..120f,
                )
                Text("Retries: ${if (settings.retries == 0) "∞" else settings.retries}")
                Slider(
                    value = settings.retries.toFloat(),
                    onValueChange = { v -> viewModel.update { it.copy(retries = v.toInt()) } },
                    valueRange = 0f..50f,
                )
                Text("Retry delay: ${settings.retryDelaySeconds}s")
                Slider(
                    value = settings.retryDelaySeconds.toFloat(),
                    onValueChange = { v -> viewModel.update { it.copy(retryDelaySeconds = v.toInt()) } },
                    valueRange = 0f..30f,
                )
            }
            SettingsGroup(title = stringResource(R.string.settings_account_group)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Active account: ${settings.activeAccountKey ?: "none"}")
                    TextButton(onClick = onOpenAuth) { Text("Manage") }
                }
                Text("Yandex login: ${settings.yandexLogin}")
            }
            SettingsGroup(title = stringResource(R.string.settings_appearance_group)) {
                Text("Theme: System")
                RowSetting("Dynamic colors", true) {}
            }
            SettingsGroup(title = stringResource(R.string.settings_about_group)) {
                TextButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) { Text("About YMD") }
            }
        }
    }
}

@Composable
private fun RowSetting(title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Suppress("unused")
private fun keepImport(intent: Intent) = intent

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    YmdTheme { SettingsScreen(onOpenAuth = {}, onOpenAbout = {}) }
}
