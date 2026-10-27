package com.occamzchainsaw.straightscorer.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.occamzchainsaw.straightscorer.ui.theme.outcomeColors
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol

/** Swipe either way to remove; the caller is expected to offer undo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToRemove(
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val remove by rememberUpdatedState(onRemove)
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            remove()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    val foul = MaterialTheme.outcomeColors.foul
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        gesturesEnabled = enabled,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) foul.container.copy(alpha = 0.4f)
                else foul.container,
                label = "swipe"
            )
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.large)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                OutlinedRoundedSymbol(MaterialSymbols.DELETE, tint = foul.onContainer)
            }
        }
    ) {
        content()
    }
}

