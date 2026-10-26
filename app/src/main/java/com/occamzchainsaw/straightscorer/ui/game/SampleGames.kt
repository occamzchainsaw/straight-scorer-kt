package com.occamzchainsaw.straightscorer.ui.game

import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.core.models.PlayerSetup
import com.occamzchainsaw.core.services.GameEngine
import com.occamzchainsaw.straightscorer.viewmodels.GameUiState

/** Realistic game states for previews and screenshot tests. */
object SampleGames {

    private fun play(setup: MatchSetup, moves: GameEngine.() -> Unit): GameUiState {
        var time = 1_700_000_000_000L
        val engine = GameEngine(clock = { time.also { time += 95_000 } })
        engine.startGame(setup)
        engine.moves()
        return GameUiState(game = engine.state.value!!, canUndo = engine.canUndo.value)
    }

    fun headToHead(): GameUiState = play(
        MatchSetup(listOf(PlayerSetup("Alice"), PlayerSetup("Bob")), targetScore = 100)
    ) {
        breakingFoul(reBreak = false)
        addPoints(3); safe()
        addPoints(12); miss()
        foul()
        addPoints(9); safe()
        addPoints(14); addPoints(4); miss()
        foul()
        addPoints(2); safe()
        addPoints(21); miss()
        addPoints(6)
    }

    fun multiPlayer(): GameUiState = play(
        MatchSetup(
            listOf(PlayerSetup("Carol"), PlayerSetup("Dave", headStart = 10), PlayerSetup("Erin"), PlayerSetup("Frank")),
            targetScore = 75,
            startingPlayerIndex = 2
        )
    ) {
        addPoints(4); miss()
        safe()
        addPoints(11); safe()
        foul()
        addPoints(7); miss()
        foul()
        addPoints(16); miss()
        addPoints(3)
    }

    fun openingBreakFoul(): GameUiState = play(
        MatchSetup(listOf(PlayerSetup("Alice"), PlayerSetup("Bob")), targetScore = 100)
    ) {}.copy(showOpeningBreakFoulDialog = true)

    fun finished(): GameUiState = play(
        MatchSetup(listOf(PlayerSetup("Alice"), PlayerSetup("Bob", headStart = 15)), targetScore = 50)
    ) {
        addPoints(8); miss()
        addPoints(5); safe()
        addPoints(17); foul()
        foul()
        addPoints(26)
    }

    fun practice(): GameUiState = play(
        MatchSetup(listOf(PlayerSetup("Alice")), targetScore = 100)
    ) {
        breakShot()
        addPoints(8); miss()
        addPoints(14); addPoints(3); safe()
        foul()
        addPoints(5)
    }

    fun practiceFinished(): GameUiState = play(
        MatchSetup(listOf(PlayerSetup("Alice")), targetScore = 100)
    ) {
        breakShot()
        addPoints(8); miss()
        addPoints(17); safe()
        addPoints(5)
        finishPractice()
    }
}
