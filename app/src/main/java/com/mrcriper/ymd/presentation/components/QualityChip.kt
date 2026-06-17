package com.mrcriper.ymd.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mrcriper.ymd.domain.model.DownloadQuality
import com.mrcriper.ymd.presentation.theme.YmdTheme

@Composable
fun QualityChip(
    quality: DownloadQuality,
    selected: Boolean,
    onSelect: (DownloadQuality) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = { onSelect(quality) },
        label = { Text(quality.label) },
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun QualityChipPreview() {
    YmdTheme {
        Row {
            DownloadQuality.entries.forEach { q ->
                QualityChip(quality = q, selected = q == DownloadQuality.BEST, onSelect = {})
            }
        }
    }
}
