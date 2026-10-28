package com.occamzchainsaw.straightscorer.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.ui.game.formatScore
import com.occamzchainsaw.straightscorer.ui.theme.outcomeColors
import com.occamzchainsaw.straightscorer.ui.theme.tabularNumbers
import com.occamzchainsaw.straightscorer.ui.theme.cardContainer

@Immutable
data class StatsRowData(
    val name: String,
    val score: Int,
    val highestBreak: Int,
    val averageBreak: Float,
    val fouls: Int,
    /** The winner's row gets the win colour. */
    val highlighted: Boolean = false
)

/** Score / high break / average / fouls per player, used by the result sheet and match details. */
@Composable
fun StatsTable(rows: List<StatsRowData>, modifier: Modifier = Modifier) {
    val win = MaterialTheme.outcomeColors.win
    val locale = currentLocale()
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.cardContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            TableRow(
                name = stringResource(R.string.result_player),
                values = listOf(
                    stringResource(R.string.result_score),
                    stringResource(R.string.result_high_break),
                    stringResource(R.string.result_average),
                    stringResource(R.string.result_fouls)
                ),
                isHeader = true
            )
            HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            rows.forEach { row ->
                TableRow(
                    name = row.name,
                    values = listOf(
                        formatScore(row.score),
                        row.highestBreak.toString(),
                        formatAverage(row.averageBreak, locale),
                        row.fouls.toString()
                    ),
                    emphasized = row.highlighted,
                    modifier = if (row.highlighted) {
                        Modifier
                            .padding(horizontal = 4.dp)
                            .background(win.container, MaterialTheme.shapes.medium)
                    } else {
                        Modifier.padding(horizontal = 4.dp)
                    }
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    name: String,
    values: List<String>,
    modifier: Modifier = Modifier,
    isHeader: Boolean = false,
    emphasized: Boolean = false
) {
    val style = if (isHeader) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium.tabularNumbers()
    val color = when {
        isHeader -> MaterialTheme.colorScheme.onSurfaceVariant
        emphasized -> MaterialTheme.outcomeColors.win.onContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val weight = if (emphasized) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = style,
            color = color,
            fontWeight = weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        values.forEach { value ->
            Text(
                text = value,
                style = style,
                color = color,
                fontWeight = weight,
                textAlign = TextAlign.End,
                modifier = Modifier.width(52.dp)
            )
        }
    }
}
