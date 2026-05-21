package com.occamzchainsaw.straightscorer.viewmodels

import androidx.lifecycle.ViewModel
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.core.services.GameEngine

class GameViewModel : ViewModel() {
    private val engine = GameEngine()

    val isMatchActive = engine.matchIsInProgress
    val players = engine.players
    val breakHistory = engine.breakHistory
    val targetScore = engine.targetScore
    val matchResult = engine.matchResult

    fun start(players: List<Player>, targetScore: Int) {
        engine.setupGame(players, targetScore)
        engine.startGame()
    }

    fun addPoints(points: Int = 1) {
        engine.addPoints(points)
    }

    fun miss() {
        engine.handleMiss()
    }

    fun safe() {
        engine.handleSafe()
    }

    fun foul() {
        engine.handleFoul()
    }
}