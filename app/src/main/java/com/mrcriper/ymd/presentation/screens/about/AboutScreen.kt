package com.mrcriper.ymd.presentation.screens.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mrcriper.ymd.BuildConfig
import com.mrcriper.ymd.R
import com.mrcriper.ymd.presentation.theme.YmdTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            Text("YMD", style = MaterialTheme.typography.displaySmall)
            Text(
                "Yandex Music Downloader — native Android rewrite of yandex-music-downloader-realflac.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.padding(8.dp))
            Text(stringResource(R.string.about_version) + ": ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            Spacer(Modifier.padding(8.dp))
            Text(stringResource(R.string.about_github) + ": github.com/MrCripe/yandex-music-downloader-realflac")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutPreview() {
    YmdTheme { AboutScreen(onBack = {}) }
}
