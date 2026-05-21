package com.occamzchainsaw.straightscorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol

@Composable
fun SwipeBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .background(Color.Red.copy(alpha = 0.8f), shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        OutlinedRoundedSymbol(MaterialSymbols.DELETE)
    }
}