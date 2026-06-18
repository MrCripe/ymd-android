package com.mrcriper.ymd.presentation.screens.home

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrcriper.ymd.R
import com.mrcriper.ymd.domain.model.DownloadQuality
import com.mrcriper.ymd.domain.model.LyricFormat
import com.mrcriper.ymd.presentation.components.QualityChip
import com.mrcriper.ymd.presentation.components.SettingsGroup
import com.mrcriper.ymd.presentation.components.UrlTypeChip
import com.mrcriper.ymd.presentation.theme.YmdTheme
import com.mrcriper.ymd.presentation.viewmodel.HomeUiState
import com.mrcriper.ymd.presentation.viewmodel.HomeViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAuth: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDownload: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    HomeContent(
        state = state,
        onUrlChange = viewModel::onUrlChange,
        onQuality = viewModel::updateQuality,
        onLyric = viewModel::updateLyrics,
        onSetting = viewModel::update,
        onStart = {
            viewModel.startDownload()
            onOpenDownload()
        },
        onOpenAuth = onOpenAuth,
        onOpenSettings = onOpenSettings,
        onOpenAbout = onOpenAbout,
        onOpenFavorites = {
            val login = state.settings.yandexLogin.ifBlank { "MrCriper10" }
            val url = "https://music.yandex.ru/users/$login/playlists/3"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    onUrlChange: (String) -> Unit,
    onQuality: (DownloadQuality) -> Unit,
    onLyric: (LyricFormat) -> Unit,
    onSetting: ((com.mrcriper.ymd.data.local.datastore.AppSettings) -> com.mrcriper.ymd.data.local.datastore.AppSettings) -> Unit,
    onStart: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenFavorites: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenAuth) { Icon(Icons.Filled.Lock, contentDescription = null) }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = null) }
                    IconButton(onClick = onOpenAbout) { Icon(Icons.Filled.Info, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.url,
                onValueChange = onUrlChange,
                label = { Text(stringResource(R.string.home_url_hint)) },
                trailingIcon = { UrlTypeChip(state.detectedType) },
                modifier = Modifier.fillMaxWidth(),
            )
            SettingsGroup(title = stringResource(R.string.home_quality)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DownloadQuality.entries.forEach { q ->
                        QualityChip(quality = q, selected = state.settings.quality == q, onSelect = onQuality)
                    }
                }
            }
            SettingsGroup(title = stringResource(R.string.home_lyrics)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LyricFormat.entries.forEach { l ->
                        FilterChip(selected = state.settings.lyricsFormat == l, onClick = { onLyric(l) }, label = { Text(l.label) })
                    }
                }
            }
            SettingsGroup(title = "Options") {
                SwitchRow(
                    title = stringResource(R.string.home_skip_existing),
                    checked = state.settings.skipExisting,
                ) { onSetting { it.copy(skipExisting = !it.skipExisting) } }
                SwitchRow(
                    title = stringResource(R.string.home_only_music),
                    checked = state.settings.onlyMusic,
                ) { onSetting { it.copy(onlyMusic = !it.onlyMusic) } }
                SwitchRow(
                    title = stringResource(R.string.home_stick_to_artist),
                    checked = state.settings.stickToArtist,
                ) { onSetting { it.copy(stickToArtist = !it.stickToArtist) } }
                SwitchRow(
                    title = stringResource(R.string.home_unsafe_path),
                    checked = state.settings.unsafePath,
                ) { onSetting { it.copy(unsafePath = !it.unsafePath) } }
            }
            SettingsGroup(title = stringResource(R.string.home_cover_resolution)) {
                Slider(
                    value = state.settings.coverResolution.toFloat(),
                    onValueChange = { v -> onSetting { it.copy(coverResolution = v.toInt().coerceIn(100, 4000)) } },
                    valueRange = 100f..4000f,
                )
                Text("${state.settings.coverResolution}px")
            }
            SettingsGroup(title = stringResource(R.string.home_delay)) {
                Slider(
                    value = state.settings.requestDelaySeconds.toFloat(),
                    onValueChange = { v -> onSetting { it.copy(requestDelaySeconds = v.toInt()) } },
                    valueRange = 0f..10f,
                    steps = 10,
                )
                Text("${state.settings.requestDelaySeconds}s")
            }
            SettingsGroup(title = stringResource(R.string.home_advanced)) {
                Column {
                    Text(stringResource(R.string.home_timeout))
                    Slider(
                        value = state.settings.timeoutSeconds.toFloat(),
                        onValueChange = { v -> onSetting { it.copy(timeoutSeconds = v.toInt().coerceIn(1, 120)) } },
                        valueRange = 1f..120f,
                    )
                    Text("${state.settings.timeoutSeconds}s")
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.home_retries))
                    Slider(
                        value = state.settings.retries.toFloat(),
                        onValueChange = { v -> onSetting { it.copy(retries = v.toInt()) } },
                        valueRange = 0f..50f,
                        steps = 50,
                    )
                    Text(if (state.settings.retries == 0) "∞" else state.settings.retries.toString())
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.home_retry_delay))
                    Slider(
                        value = state.settings.retryDelaySeconds.toFloat(),
                        onValueChange = { v -> onSetting { it.copy(retryDelaySeconds = v.toInt()) } },
                        valueRange = 0f..30f,
                    )
                    Text("${state.settings.retryDelaySeconds}s")
                }
            }
            OutlinedTextField(
                value = state.settings.pathPattern,
                onValueChange = { v -> onSetting { it.copy(pathPattern = v) } },
                label = { Text(stringResource(R.string.home_path_pattern)) },
                supportingText = { Text("#album-artist/#album/#number - #title") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_start_download))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenFavorites,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(),
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("My favorites")
            }
            state.message?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(msg, modifier = Modifier.padding(12.dp))
                }
            }
            state.lastApiResult?.let { r ->
                Card(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(r, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    YmdTheme {
        HomeContent(
            state = HomeUiState(url = "https://music.yandex.ru/album/123"),
            onUrlChange = {}, onQuality = {}, onLyric = {}, onSetting = {}, onStart = {},
            onOpenAuth = {}, onOpenSettings = {}, onOpenAbout = {}, onOpenFavorites = {},
        )
    }
}
