package com.occamzchainsaw.core.services

import com.occamzchainsaw.core.models.Break
import com.occamzchainsaw.core.models.BreakEndAction
import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.core.models.PlayerMatchSummary
import com.occamzchainsaw.core.models.TableState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.collections.emptyList

class GameEngine {
    private val _players = MutableStateFlow<List<Player>>(emptyList<Player>())
    private val _breakHistory = MutableStateFlow<List<Break>>(emptyList<Break>())
    private val _tableState = MutableStateFlow<TableState>(TableState())
    private val _targetScore = MutableStateFlow(100)
    private val _matchResult = MutableStateFlow(MatchResult())

    private var _breakOffTaken = false
    private var _gameFinished = false

    val players = _players.asStateFlow()
    var breakHistory = _breakHistory.asStateFlow()
    val targetScore = _targetScore.asStateFlow()
    val matchResult = _matchResult.asStateFlow()

    fun setupGame(players: List<Player>, targetScore: Int) {
        _players.value = players
        _breakHistory.value = emptyList()
        _tableState.value = TableState()
        _targetScore.value = targetScore
        _breakOffTaken = false
        _gameFinished = false
    }

    fun addPoints(points: Int = 1) {
        if (_gameFinished) return

        var newBreak = 0
        _players.update { currentList ->
            currentList.map { player ->
                if (player.isAtTable) {
                    val newScore = player.score + points
                    newBreak = player.currentBreak + points

                    if (newScore >= _targetScore.value) {
                        _gameFinished = true

                        val newBreakCount = player.breakCount + 1
                        val newBreakSum = player.breakSum + newBreak
                        player.copy(
                            score = newScore,
                            currentBreak = newBreak,
                            averageBreak = newBreakSum.toFloat() / newBreakCount.toFloat(),
                            highestBreak = maxOf(player.highestBreak, newBreak)
                        )
                    } else {
                        player.copy(
                            score = newScore,
                            currentBreak = newBreak
                        )
                    }
                } else {
                    player
                }
            }
        }

        _tableState.update { currentState ->
            val ballsRemaining = currentState.ballsOnTable - 1

            if (ballsRemaining == 1) {
                currentState.copy(
                    ballsOnTable = 15,
                    rackNumber = currentState.rackNumber + 1
                )
            } else {
                currentState.copy(
                    ballsOnTable = ballsRemaining
                )
            }
        }

        if (_gameFinished) {
            _breakHistory.update { currentList ->
                currentList + Break(
                    player = _players.value.first { it.isAtTable },
                    pointsScored = newBreak,
                    breakEndAction = BreakEndAction.Win
                )
            }

            val playerSummaries = _players.value.map { player ->
                PlayerMatchSummary(
                    name = player.name,
                    isWinner = player.isAtTable,
                    score = player.score,
                    highestBreak = player.highestBreak,
                    averageBreak = player.averageBreak,
                    totalFouls = player.totalFouls,
                )
            }
            _matchResult.value = MatchResult(
                date = System.currentTimeMillis(),
                winner = playerSummaries.firstOrNull { it.isWinner },
                players = playerSummaries
            )
        }
    }

    fun handleMiss() {
        if (_gameFinished) return

        var newHistoricalBreak = Break()
        _players.update { currentList ->
            _breakOffTaken = true
            val activeIndex = currentList.indexOfFirst { it.isAtTable }
            if (activeIndex == -1) return@update currentList

            val nextIndex = (activeIndex + 1) % currentList.size
            currentList.mapIndexed { index, player ->
                when (index) {
                    activeIndex -> {
                        val newBreakCount = player.breakCount + 1
                        val newBreakSum = player.breakSum + player.currentBreak
                        newHistoricalBreak = Break(
                            player = player,
                            pointsScored = player.currentBreak,
                            breakEndAction = BreakEndAction.Miss
                        )

                        player.copy(
                            breakCount = newBreakCount,
                            breakSum = newBreakSum,
                            averageBreak = newBreakSum.toFloat() / newBreakCount.toFloat(),
                            highestBreak = maxOf(player.highestBreak, player.currentBreak),
                            currentBreak = 0,
                            consecutiveFouls = 0,
                            isAtTable = false
                        )
                    }
                    nextIndex -> {
                        player.copy(isAtTable = true)
                    }
                    else -> {
                        player
                    }
                }
            }
        }

        _breakHistory.update { currentHistory ->
            currentHistory + newHistoricalBreak
        }

        _breakOffTaken = true
    }

    fun handleSafe() {
        if (_gameFinished) return

        var newHistoricalBreak = Break()
        _players.update { currentList ->
            val activeIndex = currentList.indexOfFirst { it.isAtTable }
            if (activeIndex == -1) return@update currentList

            val nextIndex = (activeIndex + 1) % currentList.size

            currentList.mapIndexed { index, player ->
                when (index) {
                    activeIndex -> {
                        var newBreakCount = player.breakCount
                        var newBreakSum = player.breakSum

                        if (player.currentBreak > 0) {
                            newBreakCount++
                            newBreakSum += player.currentBreak
                            newHistoricalBreak = Break(
                                player = player,
                                pointsScored = player.currentBreak,
                                breakEndAction = BreakEndAction.Safe
                            )
                        }

                        player.copy(
                            breakCount = newBreakCount,
                            breakSum = newBreakSum,
                            averageBreak = newBreakSum.toFloat() / newBreakCount.toFloat(),
                            highestBreak = maxOf(player.highestBreak, player.currentBreak),
                            currentBreak = 0,
                            consecutiveFouls = 0,
                            isAtTable = false
                        )
                    }
                    nextIndex -> {
                        player.copy(isAtTable = true)
                    }
                    else -> {
                        player
                    }
                }
            }
        }

        _breakHistory.update { currentList ->
            currentList + newHistoricalBreak
        }

        _breakOffTaken = true
    }

    fun handleFoul() {
        if (_gameFinished) return

        var isThirdFoul = false
        var newHistoricalBreak = Break()
        _players.update { currentList ->
            val activeIndex = currentList.indexOfFirst { it.isAtTable }
            if (activeIndex == -1) return@update currentList

            val nextIndex = (activeIndex + 1) % currentList.size

            currentList.mapIndexed { index, player ->
                when (index) {
                    activeIndex -> {
                        var newConsecutiveFouls = player.consecutiveFouls + 1
                        var newBreak = player.currentBreak

                        if (newConsecutiveFouls == 3) {
                            newBreak -= 16
                            newConsecutiveFouls = 0
                            isThirdFoul = true
                        }
                        else if (player.isStarting && !_breakOffTaken) {
                            newBreak -= 2
                        }
                        else {
                            newBreak -= 1
                        }

                        newHistoricalBreak = Break(
                            player = player,
                            pointsScored = newBreak,
                            breakEndAction = if (isThirdFoul) BreakEndAction.ThirdFoul else BreakEndAction.Foul
                        )

                        val newBreakCount = player.breakCount + 1
                        val newBreakSum = player.breakSum + newBreak

                        player.copy(
                            score = player.score + newBreak,
                            consecutiveFouls = newConsecutiveFouls,
                            totalFouls = player.totalFouls + 1,
                            breakCount = newBreakCount,
                            breakSum = newBreakSum,
                            averageBreak = newBreakSum.toFloat() / newBreakCount.toFloat(),
                            highestBreak = maxOf(player.highestBreak, newBreak),
                            currentBreak = 0,
                            isAtTable = false
                        )
                    }
                    nextIndex -> {
                        player.copy(isAtTable = true)
                    }
                    else -> {
                        player
                    }
                }
            }
        }

        if (isThirdFoul) {
            _tableState.update { currentState ->
                currentState.copy(
                    ballsOnTable = 15,
                    rackNumber = currentState.rackNumber + 1
                )
            }
        }

        _breakHistory.update { currentList ->
            currentList + newHistoricalBreak
        }

        _breakOffTaken = true
    }
}