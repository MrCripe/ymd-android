package com.mrcriper.ymd.presentation.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mrcriper.ymd.data.remote.api.YandexEntity
import com.mrcriper.ymd.presentation.theme.YmdTheme

@Composable
fun UrlTypeChip(entity: YandexEntity?, modifier: Modifier = Modifier) {
    val label = when (entity) {
        is YandexEntity.Track -> "Track · ${entity.trackId}"
        is YandexEntity.Album -> "Album · ${entity.albumId}"
        is YandexEntity.Artist -> "Artist · ${entity.artistId}"
        is YandexEntity.Playlist -> "Playlist · ${entity.owner}/${entity.kind}"
        is YandexEntity.RawId -> "${entity.kind} · ${entity.id}"
        null -> "Unknown"
    }
    AssistChip(
        onClick = {},
        enabled = entity != null,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun UrlTypeChipPreview() {
    YmdTheme { UrlTypeChip(YandexEntity.Track("123456")) }
}
