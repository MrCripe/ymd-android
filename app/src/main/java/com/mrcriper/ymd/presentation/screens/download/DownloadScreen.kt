package com.mrcriper.ymd.presentation.screens.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrcriper.ymd.R
import com.mrcriper.ymd.presentation.components.DownloadProgressItem
import com.mrcriper.ymd.presentation.theme.YmdTheme
import com.mrcriper.ymd.presentation.viewmodel.DownloadItem
import com.mrcriper.ymd.presentation.viewmodel.DownloadUiState
import com.mrcriper.ymd.presentation.viewmodel.DownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_download)) }) },
    ) { padding ->
        DownloadList(
            state = state,
            paddingValues = padding,
            onPause = viewModel::pause,
            onResume = viewModel::resume,
            onCancel = viewModel::cancel,
            onCancelAll = viewModel::cancelAll,
        )
    }
}

@Composable
private fun DownloadList(
    state: DownloadUiState,
    paddingValues: PaddingValues,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onCancelAll: () -> Unit,
) {
    if (state.items.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp)) {
            Text(stringResource(R.string.download_empty), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    Column(modifier = Modifier.padding(paddingValues)) {
        OutlinedButton(onClick = onCancelAll, modifier = Modifier.padding(16.dp)) {
            Text("Cancel all")
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(state.items, key = { it.id }) { item ->
                DownloadProgressItem(
                    item = item,
                    onPause = { onPause(item.id) },
                    onResume = { onResume(item.id) },
                    onCancel = { onCancel(item.id) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadEmptyPreview() {
    YmdTheme { DownloadList(state = DownloadUiState(), PaddingValues(0.dp), {}, {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun DownloadListPreview() {
    YmdTheme {
        DownloadList(
            state = DownloadUiState(
                items = listOf(
                    DownloadItem("1", "Title", "Artist", "Album", 1411, "FLAC", 0.5f, DownloadItem.Status.RUNNING),
                ),
            ),
            paddingValues = PaddingValues(0.dp),
            onPause = {}, onResume = {}, onCancel = {}, onCancelAll = {},
        )
    }
}
