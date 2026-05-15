package com.occamzchainsaw.core.services

import com.occamzchainsaw.core.models.Break
import com.occamzchainsaw.core.models.BreakEndAction
import com.occamzchainsaw.core.models.Player
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameEngineTest {
    private lateinit var engine: GameEngine

    @Before
    fun setUp() {
        engine = GameEngine()
    }

    @Test
    fun setupGame() {
        val player1 = Player(name = "player1", isStarting = true, isAtTable = true,)
        val player2 = Player(name = "player2")
        val players = listOf(player1, player2)

        engine.setupGame(players, targetScore = 50)

        val statePlayers = engine.players.value
        assertEquals(2, statePlayers.size)
        assertEquals(50, engine.targetScore.value)
        assertTrue("Player 1 is at the table", statePlayers[0].isAtTable)
        assertTrue("Player 1 is starting", statePlayers[0].isStarting)
    }

    @Test
    fun addPoints() {
        val player1 = Player(name = "player1", isStarting = true, isAtTable = true, score = 10)
        val player2 = Player(name = "player2")
        val players = listOf(player1, player2)
        engine.setupGame(players, targetScore = 100)

        engine.addPoints()

        val updatedActivePlayer = engine.players.value.first { it.isAtTable }
        assertEquals("Score should be incremented by 1", 11, updatedActivePlayer.score)
        assertEquals("Break should increment by 1", 1, updatedActivePlayer.currentBreak)
    }

    @Test
    fun handleMiss() {
        val player1 = Player(name = "player1", isStarting = true, isAtTable = true, score = 10)
        val player2 = Player(name = "player2")
        val players = listOf(player1, player2)
        engine.setupGame(players, targetScore = 100)

        engine.handleMiss()

        val updatedPlayer1 = engine.players.value[0]
        val updatedPlayer2 = engine.players.value[1]
        val breakHistory = engine.breakHistory.value
        val expectedBreak = Break(
            player = player1,
            pointsScored = 0,
            breakEndAction = BreakEndAction.Miss
        )
        assertEquals("Break count should be incremented by 1", 1, updatedPlayer1.breakCount)
        assertEquals("Current break should be reset to 0", 0, updatedPlayer1.currentBreak)
        assertEquals("Player2 should be at the table now", true, updatedPlayer2.isAtTable)
        assertEquals("Last historical break should be by Player1, 0 points, ended with a Miss", expectedBreak, breakHistory.last())
    }

    @Test
    fun handleSafe() {
        val player1 = Player(name = "player1", isStarting = true, isAtTable = true, currentBreak = 12)
        val player2 = Player(name = "player2")
        val players = listOf(player1, player2)
        engine.setupGame(players, targetScore = 100)

        engine.handleSafe()

        val updatedPlayer1 = engine.players.value[0]
        val updatedPlayer2 = engine.players.value[1]
        val breakHistory = engine.breakHistory.value
        val expectedBreak = Break(
            player = player1,
            pointsScored = 12,
            breakEndAction = BreakEndAction.Safe
        )
        assertEquals("Break count should be incremented by 1", 1, updatedPlayer1.breakCount)
        assertEquals("Current break should be reset to 0", 0, updatedPlayer1.currentBreak)
        assertEquals("Player2 should be at the table now", true, updatedPlayer2.isAtTable)
        assertEquals("Last historical break should be by Player1, 12 points, ended with a Safe", expectedBreak, breakHistory.last())
    }

    @Test
    fun handleFoul() {
        val player1 = Player(name = "player1", isStarting = true, isAtTable = true)
        val player2 = Player(name = "player2")
        val players = listOf(player1, player2)
        engine.setupGame(players, targetScore = 100)

        engine.handleFoul()

        val updatedPlayer1 = engine.players.value[0]
        val updatedPlayer2 = engine.players.value[1]
        val breakHistory = engine.breakHistory.value
        val expectedBreak = Break(
            player = player1,
            pointsScored = -2,
            breakEndAction = BreakEndAction.Foul
        )
        assertEquals("Break count should be incremented by 1", 1, updatedPlayer1.breakCount)
        assertEquals("Current break should be reset to 0", 0, updatedPlayer1.currentBreak)
        assertEquals("Player 1's total fouls should be incremented by 1", 1, updatedPlayer1.totalFouls)
        assertEquals("Player 1's consecutive fouls should be incremented by 1", 1, updatedPlayer1.consecutiveFouls)
        assertEquals("Player2 should be at the table now", true, updatedPlayer2.isAtTable)
        assertEquals("Last historical break should be by Player1, -2 points, ended with a Foul (on break)", expectedBreak, breakHistory.last())
    }

}