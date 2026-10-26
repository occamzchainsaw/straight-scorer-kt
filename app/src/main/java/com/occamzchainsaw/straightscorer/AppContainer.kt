package com.occamzchainsaw.straightscorer

import android.content.Context
import com.occamzchainsaw.core.models.GameSettings
import com.occamzchainsaw.core.repositories.MatchHistoryRepository
import com.occamzchainsaw.core.services.ActiveGamePersister
import com.occamzchainsaw.core.services.GameEngine
import com.occamzchainsaw.core.services.MatchRecorder
import com.occamzchainsaw.straightscorer.data.game.FileActiveGameStore
import com.occamzchainsaw.straightscorer.data.history.AppDatabase
import com.occamzchainsaw.straightscorer.data.history.RoomMatchHistoryRepository
import com.occamzchainsaw.straightscorer.data.rules.AssetRulesRepository
import com.occamzchainsaw.straightscorer.data.rules.RulesRepository
import com.occamzchainsaw.straightscorer.data.settings.DataStoreSettingsRepository
import com.occamzchainsaw.straightscorer.data.settings.SettingsRepository
import com.occamzchainsaw.straightscorer.data.settings.settingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** App-wide singletons. Created once in [StraightScorerApplication]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database by lazy { AppDatabase.create(appContext) }

    val matchHistoryRepository: MatchHistoryRepository by lazy {
        RoomMatchHistoryRepository(database.matchDao())
    }

    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(appContext.settingsDataStore)

    val rulesRepository: RulesRepository = AssetRulesRepository(appContext.assets)

    private val gameSettings = settingsRepository.settings
        .map { it.game }
        .stateIn(appScope, SharingStarted.Eagerly, GameSettings())

    /** Shared by the setup and game screens. */
    val gameEngine = GameEngine(settings = { gameSettings.value })

    val matchRecorder = MatchRecorder(gameEngine, matchHistoryRepository)

    private val activeGamePersister = ActiveGamePersister(
        gameEngine,
        FileActiveGameStore(File(appContext.filesDir, "active_game.json"))
    )

    init {
        appScope.launch { activeGamePersister.run() }
        appScope.launch { matchRecorder.run() }
    }
}
