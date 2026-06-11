package com.occamzchainsaw.straightscorer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.occamzchainsaw.straightscorer.dtos.PlayerSetupItem
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol

@Composable
fun playerEditCard(
    item: PlayerSetupItem,
    onItemUpdated: (PlayerSetupItem) -> Unit,
    onDelete: (() -> Unit)? = null,
    showErrors: Boolean = false,
    modifier: Modifier = Modifier
) {
    var headStartValue by remember {
        mutableStateOf(TextFieldValue(item.headStart))
    }

    val nameIsError = showErrors && item.name.isBlank()

    OutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = item.name,
                    onValueChange = { onItemUpdated(item.copy(name = it)) },
                    label = { Text("Name") },
                    isError = nameIsError,
                    supportingText = if (nameIsError) {
                        { Text("Name required") }
                    } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        OutlinedRoundedSymbol(
                            icon = MaterialSymbols.DELETE,
                            size = 36.dp,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focus ->
                            if (focus.isFocused) {
                                headStartValue = headStartValue.copy(
                                    selection = TextRange(0, headStartValue.text.length)
                                )
                            }
                        },
                    value = headStartValue,
                    onValueChange = { newValue ->
                        if (newValue.text.isEmpty() || newValue.text.all { it.isDigit() }) {
                            headStartValue = newValue
                            onItemUpdated(item.copy(headStart = newValue.text))
                        }
                    },
                    label = { Text("Head Start") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Breaks Off", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = item.isStarting,
                        onCheckedChange = { onItemUpdated(item.copy(isStarting = it)) }
                    )
                }
            }
        }
    }
}
