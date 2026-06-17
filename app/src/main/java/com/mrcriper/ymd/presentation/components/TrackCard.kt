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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrcriper.ymd.domain.model.Album
import com.mrcriper.ymd.domain.model.Artist
import com.mrcriper.ymd.domain.model.Track
import com.mrcriper.ymd.presentation.theme.YmdTheme

@Composable
fun TrackCard(
    track: Track,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = track.coverUri,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.size(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.fullTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    text = track.primaryArtist?.name.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
                track.primaryAlbum?.let {
                    Text(
                        text = it.title,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackCardPreview() {
    YmdTheme {
        TrackCard(
            track = Track(
                id = "1",
                title = "Sample Track",
                artists = listOf(Artist(id = "1", name = "Artist Name")),
                albums = listOf(Album(id = "1", title = "Album Title")),
                coverUri = null,
            ),
        )
    }
}
