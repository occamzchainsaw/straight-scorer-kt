package com.occamzchainsaw.straightscorer.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.ui.theme.ScorerMotion
import com.occamzchainsaw.straightscorer.ui.theme.outcomeColors

val ToolbarHeight = 72.dp

/**
 * Floating toolbar with the scoring actions, colour-coded like the history.
 * Long-pressing +1 also opens the multi-point sheet.
 */
@Composable
fun ActionToolbar(
    enabled: Boolean,
    isOpeningBreak: Boolean,
    onAddPoint: () -> Unit,
    onOpenAddPoints: () -> Unit,
    onBreakShot: () -> Unit,
    onSafe: () -> Unit,
    onMiss: () -> Unit,
    onFoul: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outcomes = MaterialTheme.outcomeColors
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .widthIn(max = 560.dp)
            .height(ToolbarHeight),
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(
                label = stringResource(R.string.action_add_point),
                container = scheme.primary,
                content = scheme.onPrimary,
                enabled = enabled,
                onClick = onAddPoint,
                onLongClick = onOpenAddPoints,
                emphasized = true,
                modifier = Modifier.weight(1.35f)
            )
            ToolbarButton(
                label = stringResource(R.string.action_add_points),
                description = stringResource(R.string.action_add_points_description),
                container = scheme.secondaryContainer,
                content = scheme.onSecondaryContainer,
                enabled = enabled,
                onClick = onOpenAddPoints,
                modifier = Modifier.weight(0.8f)
            )
            // on the opening break only a legal break (or a pot / foul) makes sense
            AnimatedContent(
                targetState = isOpeningBreak,
                transitionSpec = {
                    fadeIn(tween(ScorerMotion.MEDIUM)) togetherWith fadeOut(tween(ScorerMotion.SHORT))
                },
                modifier = Modifier.weight(2f),
                label = "middle actions"
            ) { openingBreak ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (openingBreak) {
                        ToolbarButton(
                            label = stringResource(R.string.action_break_shot),
                            description = stringResource(R.string.action_break_shot_description),
                            container = outcomes.breakShot.container,
                            content = outcomes.breakShot.onContainer,
                            enabled = enabled,
                            onClick = onBreakShot,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        ToolbarButton(
                            label = stringResource(R.string.action_safe),
                            container = outcomes.safe.container,
                            content = outcomes.safe.onContainer,
                            enabled = enabled,
                            onClick = onSafe,
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarButton(
                            label = stringResource(R.string.action_miss),
                            container = outcomes.miss.container,
                            content = outcomes.miss.onContainer,
                            enabled = enabled,
                            onClick = onMiss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            ToolbarButton(
                label = stringResource(R.string.action_foul),
                container = outcomes.foul.container,
                content = outcomes.foul.onContainer,
                enabled = enabled,
                onClick = onFoul,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarButton(
    label: String,
    container: Color,
    content: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    onLongClick: (() -> Unit)? = null,
    emphasized: Boolean = false
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .alpha(if (enabled) 1f else 0.38f),
        shape = RoundedCornerShape(if (emphasized) 20.dp else 16.dp),
        color = container,
        contentColor = content
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
        ) {
            Text(
                text = label,
                style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
