package com.occamzchainsaw.straightscorer.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.data.settings.AppSettings
import com.occamzchainsaw.straightscorer.data.settings.ThemeMode
import com.occamzchainsaw.straightscorer.viewmodels.AppViewModelProvider
import com.occamzchainsaw.straightscorer.viewmodels.SettingsViewModel
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol

private val ReadingWidth = 680.dp

@Immutable
data class SettingsActions(
    val onThemeMode: (ThemeMode) -> Unit = {},
    val onDynamicColor: (Boolean) -> Unit = {},
    val onDefaultTargetScore: (Int) -> Unit = {},
    val onResetRackOnThirdFoul: (Boolean) -> Unit = {},
    val onCountMisses: (Boolean) -> Unit = {},
    val onCountSafeties: (Boolean) -> Unit = {},
    val onKeepScreenOn: (Boolean) -> Unit = {},
    val onOpenLicenses: () -> Unit = {}
)

@Composable
fun SettingsScreen(
    onOpenLicenses: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val settings by viewModel.settings.collectAsState()
    val actions = remember(viewModel, onOpenLicenses) {
        SettingsActions(
            onThemeMode = viewModel::setThemeMode,
            onDynamicColor = viewModel::setDynamicColor,
            onDefaultTargetScore = viewModel::setDefaultTargetScore,
            onResetRackOnThirdFoul = viewModel::setResetRackOnThirdFoul,
            onCountMisses = viewModel::setCountMissesInAverage,
            onCountSafeties = viewModel::setCountSafetiesInAverage,
            onKeepScreenOn = viewModel::setKeepScreenOn,
            onOpenLicenses = onOpenLicenses
        )
    }
    SettingsContent(settings, actions, versionName = rememberVersionName())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settings: AppSettings,
    actions: SettingsActions,
    versionName: String,
    modifier: Modifier = Modifier,
    dynamicColorAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    initiallyEditingTarget: Boolean = false
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var editingTarget by rememberSaveable { mutableStateOf(initiallyEditingTarget) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }, scrollBehavior = scrollBehavior)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "appearance") {
                SettingsSection(stringResource(R.string.settings_appearance)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_theme)) },
                        supportingContent = {
                            ThemeSelector(
                                selected = settings.themeMode,
                                onSelect = actions.onThemeMode,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        },
                        leadingContent = { SettingIcon(MaterialSymbols.PALETTE) },
                        colors = sectionItemColors()
                    )
                    SectionDivider()
                    SwitchSetting(
                        symbol = MaterialSymbols.COLORIZE,
                        title = stringResource(R.string.settings_dynamic_color),
                        text = stringResource(
                            if (dynamicColorAvailable) R.string.settings_dynamic_color_text
                            else R.string.settings_dynamic_color_unavailable
                        ),
                        checked = settings.dynamicColor && dynamicColorAvailable,
                        enabled = dynamicColorAvailable,
                        onCheckedChange = actions.onDynamicColor
                    )
                }
            }
            item(key = "game") {
                SettingsSection(stringResource(R.string.settings_game), footer = stringResource(R.string.settings_rules_note)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_default_target)) },
                        supportingContent = { Text(stringResource(R.string.settings_default_target_text)) },
                        leadingContent = { SettingIcon(MaterialSymbols.TROPHY) },
                        trailingContent = {
                            Text(
                                settings.defaultTargetScore.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = sectionItemColors(),
                        modifier = Modifier.clickable(role = Role.Button) { editingTarget = true }
                    )
                    SectionDivider()
                    SwitchSetting(
                        symbol = MaterialSymbols.ATR,
                        title = stringResource(R.string.settings_rerack),
                        text = stringResource(R.string.settings_rerack_text),
                        checked = settings.game.resetRackOnThirdFoul,
                        onCheckedChange = actions.onResetRackOnThirdFoul
                    )
                    SectionDivider()
                    SwitchSetting(
                        symbol = MaterialSymbols.CIRCLE,
                        title = stringResource(R.string.settings_count_misses),
                        text = stringResource(R.string.settings_count_misses_text),
                        checked = settings.game.countMissesInAverage,
                        onCheckedChange = actions.onCountMisses
                    )
                    SectionDivider()
                    SwitchSetting(
                        symbol = MaterialSymbols.SHIELD,
                        title = stringResource(R.string.settings_count_safeties),
                        text = stringResource(R.string.settings_count_safeties_text),
                        checked = settings.game.countSafetiesInAverage,
                        onCheckedChange = actions.onCountSafeties
                    )
                }
            }
            item(key = "display") {
                SettingsSection(stringResource(R.string.settings_display)) {
                    SwitchSetting(
                        symbol = MaterialSymbols.LIGHT_MODE,
                        title = stringResource(R.string.settings_keep_screen_on),
                        text = stringResource(R.string.settings_keep_screen_on_text),
                        checked = settings.keepScreenOn,
                        onCheckedChange = actions.onKeepScreenOn
                    )
                }
            }
            item(key = "about") {
                SettingsSection(stringResource(R.string.settings_about)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_version)) },
                        supportingContent = { Text(versionName) },
                        leadingContent = { SettingIcon(MaterialSymbols.INFO) },
                        colors = sectionItemColors()
                    )
                    SectionDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_licenses)) },
                        supportingContent = { Text(stringResource(R.string.settings_licenses_text)) },
                        leadingContent = { SettingIcon(MaterialSymbols.DESCRIPTION) },
                        trailingContent = { OutlinedRoundedSymbol(MaterialSymbols.CHEVRON_RIGHT) },
                        colors = sectionItemColors(),
                        modifier = Modifier.clickable(role = Role.Button, onClick = actions.onOpenLicenses)
                    )
                }
            }
        }
    }

    if (editingTarget) {
        TargetScoreDialog(
            current = settings.defaultTargetScore,
            onSave = { score ->
                editingTarget = false
                actions.onDefaultTargetScore(score)
            },
            onDismiss = { editingTarget = false }
        )
    }
}

@Composable
private fun SettingsSection(title: String, footer: String? = null, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = ReadingWidth)
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(vertical = 4.dp)) { content() }
        }
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
        }
    }
}

@Composable
private fun SwitchSetting(
    symbol: String,
    title: String,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(text) },
        leadingContent = { SettingIcon(symbol) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = sectionItemColors(),
        modifier = Modifier.clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val labels = mapOf(
        ThemeMode.System to R.string.settings_theme_system,
        ThemeMode.Light to R.string.settings_theme_light,
        ThemeMode.Dark to R.string.settings_theme_dark
    )
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                label = { Text(stringResource(labels.getValue(mode))) }
            )
        }
    }
}

@Composable
private fun TargetScoreDialog(current: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(current.toString()) }
    val value = text.toIntOrNull()
    val valid = value != null && value in MatchSetup.MIN_TARGET_SCORE..MatchSetup.MAX_TARGET_SCORE

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { OutlinedRoundedSymbol(MaterialSymbols.TROPHY) },
        title = { Text(stringResource(R.string.settings_target_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> if (input.length <= 4 && input.all(Char::isDigit)) text = input },
                isError = !valid,
                supportingText = if (!valid) {
                    {
                        Text(
                            stringResource(
                                R.string.settings_target_dialog_error,
                                MatchSetup.MIN_TARGET_SCORE,
                                MatchSetup.MAX_TARGET_SCORE
                            )
                        )
                    }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value!!) }, enabled = valid) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun SettingIcon(symbol: String) {
    OutlinedRoundedSymbol(symbol, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun sectionItemColors() = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)

@Composable
private fun rememberVersionName(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }
}
