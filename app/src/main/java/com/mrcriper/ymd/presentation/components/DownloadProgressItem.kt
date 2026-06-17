package com.mrcriper.ymd.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrcriper.ymd.presentation.theme.YmdTheme
import com.mrcriper.ymd.presentation.viewmodel.DownloadItem

@Composable
fun DownloadProgressItem(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = null,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text(item.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Text(
                        "${item.format} · ${item.bitrate}kbps",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.status == DownloadItem.Status.RUNNING) {
                    TextButton(onClick = onPause) { Text("Pause") }
                } else if (item.status == DownloadItem.Status.PAUSED) {
                    TextButton(onClick = onResume) { Text("Resume") }
                }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadProgressItemPreview() {
    YmdTheme {
        DownloadProgressItem(
            item = DownloadItem(
                id = "1",
                title = "Sample",
                artist = "Artist",
                album = "Album",
                bitrate = 1411,
                format = "FLAC",
                progress = 0.42f,
                status = DownloadItem.Status.RUNNING,
            ),
            onPause = {}, onResume = {}, onCancel = {},
        )
    }
}
