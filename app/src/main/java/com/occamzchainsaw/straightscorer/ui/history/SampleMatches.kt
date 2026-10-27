package com.occamzchainsaw.straightscorer.ui.history

import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.core.models.PlayerSetup
import com.occamzchainsaw.core.rules.toMatchResult
import com.occamzchainsaw.core.services.GameEngine

/** Finished matches for previews and screenshot tests. */
object SampleMatches {
    private const val DAY = 86_400_000L
    private const val START = 1_792_000_000_000L // late October 2026

    private fun play(startedAt: Long, setup: MatchSetup, moves: GameEngine.() -> Unit): MatchResult {
        var time = startedAt
        val engine = GameEngine(clock = { time.also { time += 140_000 } })
        engine.startGame(setup)
        engine.moves()
        return engine.state.value!!.toMatchResult()!!
    }

    fun headToHead(startedAt: Long = START): MatchResult = play(
        startedAt,
        MatchSetup(listOf(PlayerSetup("Alice"), PlayerSetup("Bob")), targetScore = 50)
    ) {
        breakShot()
        addPoints(9); miss()
        addPoints(14); addPoints(2); safe()
        foul()
        addPoints(6); miss()
        addPoints(21); safe()
        addPoints(28)
    }

    fun multiPlayer(startedAt: Long = START - 2 * DAY): MatchResult = play(
        startedAt,
        MatchSetup(listOf(PlayerSetup("Carol"), PlayerSetup("Dave"), PlayerSetup("Erin")), targetScore = 30)
    ) {
        breakShot()
        addPoints(7); miss()
        addPoints(4); safe()
        addPoints(12); miss()
        foul()
        addPoints(26)
    }

    fun practice(startedAt: Long = START - 5 * DAY): MatchResult = play(
        startedAt,
        MatchSetup(listOf(PlayerSetup("Alice")), targetScore = 100)
    ) {
        breakShot()
        addPoints(11); miss()
        addPoints(23); safe()
        addPoints(6)
        finishPractice()
    }

    fun list(): List<MatchResult> = listOf(
        headToHead(),
        multiPlayer(),
        practice(),
        headToHead(START - 35 * DAY)
    )
}
