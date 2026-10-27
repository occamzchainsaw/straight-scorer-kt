package com.occamzchainsaw.straightscorer.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.occamzchainsaw.straightscorer.R
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val APACHE_2 = "Apache License 2.0"
private const val APACHE_2_URL = "https://www.apache.org/licenses/LICENSE-2.0"

@Immutable
data class LicenseEntry(
    val name: String,
    val author: String,
    val license: String,
    val url: String? = null,
    /** Bundled licence text under assets/, shown inline. */
    val assetPath: String? = null
)

/** Third-party components shipped in the app. Licences checked against each project's published POM. */
val ThirdPartyLicenses = listOf(
    LicenseEntry("Montserrat", "The Montserrat.Git Project Authors", "SIL Open Font License 1.1", assetPath = "licenses/montserrat_ofl.txt"),
    LicenseEntry("AndroidX, Jetpack Compose and Material 3", "The Android Open Source Project", APACHE_2, APACHE_2_URL),
    LicenseEntry("Kotlin, kotlinx.coroutines and kotlinx.serialization", "JetBrains", APACHE_2, APACHE_2_URL),
    LicenseEntry("JetBrains Markdown", "JetBrains", APACHE_2, APACHE_2_URL),
    LicenseEntry("MaterialKolor", "Jordon de Hoog", "MIT License", "https://github.com/jordond/materialkolor/blob/master/LICENSE"),
    LicenseEntry("Reorderable", "Calvin Liang", APACHE_2, APACHE_2_URL),
    LicenseEntry(
        "Compose Material Symbols",
        "Clément Vicart",
        APACHE_2,
        "https://github.com/ClementVicart/compose-material-symbols/blob/main/LICENSE"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit, entries: List<LicenseEntry> = ThirdPartyLicenses) {
    val backDescription = stringResource(R.string.action_back)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = backDescription }) {
                        FilledRoundedSymbol(MaterialSymbols.ARROW_BACK)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.name }) { entry ->
                LicenseCard(entry, Modifier.widthIn(max = 680.dp))
            }
        }
    }
}

@Composable
private fun LicenseCard(entry: LicenseEntry, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    var expanded by rememberSaveable(entry.name) { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = entry.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = entry.license,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            when {
                entry.assetPath != null -> {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(stringResource(if (expanded) R.string.licenses_hide_text else R.string.licenses_show_text))
                    }
                    AnimatedVisibility(expanded) { AssetText(entry.assetPath) }
                }
                entry.url != null -> TextButton(onClick = { runCatching { uriHandler.openUri(entry.url) } }) {
                    Text(stringResource(R.string.licenses_open))
                }
            }
        }
    }
}

@Composable
private fun AssetText(path: String) {
    val context = LocalContext.current
    val text by produceState("", path) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrDefault("")
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
    )
}
