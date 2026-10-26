package com.occamzchainsaw.straightscorer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occamzchainsaw.straightscorer.viewmodels.AppViewModelProvider
import com.occamzchainsaw.straightscorer.viewmodels.RulesUiState
import com.occamzchainsaw.straightscorer.viewmodels.RulesViewModel

// Placeholder rendering (raw markdown) until the screen is redesigned.
@Composable
fun rulesScreen(viewModel: RulesViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            RulesUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            RulesUiState.Error -> Text("Couldn't load the rules.", modifier = Modifier.align(Alignment.Center))
            is RulesUiState.Loaded -> Text(
                text = state.markdown,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            )
        }
    }
}
