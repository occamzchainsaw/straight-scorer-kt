package com.occamzchainsaw.core.rules

import com.occamzchainsaw.core.models.Break
import com.occamzchainsaw.core.models.BreakEndAction
import com.occamzchainsaw.core.models.GameAction
import com.occamzchainsaw.core.models.GameSettings
import com.occamzchainsaw.core.models.GameState
import com.occamzchainsaw.core.models.GameStatus
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.core.models.TableState

/**
 * Pure 14.1 rules: every action takes a [GameState] and returns the next one.
 * Actions on a finished game (or invalid actions) return the state unchanged.
 */
object GameReducer {
    const val RACK_SIZE = 15
    const val FOUL_PENALTY = 1
    const val BREAKING_FOUL_PENALTY = 2
    const val THIRD_FOUL_PENALTY = 16

    fun reduce(state: GameState, action: GameAction, settings: GameSettings, now: Long): GameState {
        if (!state.isInProgress) return state

        return when (action) {
            is GameAction.AddPoints -> addPoints(state, action.points, now)
            GameAction.Miss -> endVisit(state, BreakEndAction.Miss, settings.countMissesInAverage)
            GameAction.Safe -> endVisit(state, BreakEndAction.Safe, settings.countSafetiesInAverage)
            GameAction.Foul -> foul(state, settings)
            is GameAction.BreakingFoul -> breakingFoul(state, action.reBreak)
        }
    }

    private fun addPoints(state: GameState, points: Int, now: Long): GameState {
        if (points <= 0) return state

        val player = state.playerAtTable
        val awarded = minOf(points, state.targetScore - player.score)
        val table = state.table.pocketBalls(awarded)
        val scored = player.copy(
            score = player.score + awarded,
            currentBreak = player.currentBreak + awarded,
            consecutiveFouls = 0
        )

        if (scored.score < state.targetScore) {
            return state.withPlayer(scored).copy(table = table)
        }

        return state.withPlayer(scored.closeVisit(countInAverage = true)).copy(
            table = table,
            breaks = state.breaks + scored.toBreak(BreakEndAction.Win, table.rackNumber),
            status = GameStatus.Finished(scored.id),
            finishedAt = now
        )
    }

    private fun endVisit(state: GameState, action: BreakEndAction, countInAverage: Boolean): GameState {
        val player = state.playerAtTable
        val closed = player.closeVisit(countInAverage).copy(consecutiveFouls = 0)

        return state.withPlayer(closed).copy(
            atTableIndex = state.nextIndex(),
            table = state.table.copy(isOpeningBreak = false),
            breaks = state.breaks + player.toBreak(action, state.table.rackNumber)
        )
    }

    /**
     * Standard foul. The third consecutive one costs 16 in total and (by default) re-racks all 15 balls,
     * with the offender breaking again under the opening break requirements (rules 12, 12.1).
     */
    private fun foul(state: GameState, settings: GameSettings): GameState {
        val player = state.playerAtTable
        val fouls = player.consecutiveFouls + 1
        val isThirdFoul = fouls >= 3
        val penalty = if (isThirdFoul) THIRD_FOUL_PENALTY else FOUL_PENALTY

        val closed = player.closeVisit(countInAverage = !isThirdFoul && settings.countMissesInAverage).copy(
            score = player.score - penalty,
            consecutiveFouls = if (isThirdFoul) 0 else fouls,
            totalFouls = player.totalFouls + 1
        )
        val historyEntry = player.toBreak(
            if (isThirdFoul) BreakEndAction.ThirdFoul else BreakEndAction.Foul,
            state.table.rackNumber,
            penalty
        )

        val offenderReBreaks = isThirdFoul && settings.resetRackOnThirdFoul
        return state.withPlayer(closed).copy(
            atTableIndex = if (offenderReBreaks) state.atTableIndex else state.nextIndex(),
            table = if (offenderReBreaks) {
                TableState(rackNumber = state.table.rackNumber + 1, isOpeningBreak = true)
            } else {
                state.table.copy(isOpeningBreak = false)
            },
            breaks = state.breaks + historyEntry
        )
    }

    /**
     * Breaking foul (rules 4, 11): -2, not counted towards consecutive fouls. The incoming player
     * either accepts the table or makes the breaker break again.
     */
    private fun breakingFoul(state: GameState, reBreak: Boolean): GameState {
        if (!state.table.isOpeningBreak) return state

        val player = state.playerAtTable
        val penalised = player.copy(
            score = player.score - BREAKING_FOUL_PENALTY,
            currentBreak = 0,
            totalFouls = player.totalFouls + 1
        )

        return state.withPlayer(penalised).copy(
            atTableIndex = if (reBreak) state.atTableIndex else state.nextIndex(),
            table = state.table.copy(isOpeningBreak = reBreak),
            breaks = state.breaks + player.toBreak(
                BreakEndAction.BreakingFoul,
                state.table.rackNumber,
                BREAKING_FOUL_PENALTY
            )
        )
    }

    /** Pockets balls one at a time; when only one ball is left the other 14 are re-racked. */
    private fun TableState.pocketBalls(count: Int): TableState {
        var balls = ballsOnTable
        var rack = rackNumber
        repeat(count) {
            balls--
            if (balls == 1) {
                balls = RACK_SIZE
                rack++
            }
        }
        return copy(ballsOnTable = balls, rackNumber = rack, isOpeningBreak = false)
    }

    /** Moves the current break into the player's stats. Breaks with points always count. */
    private fun Player.closeVisit(countInAverage: Boolean): Player {
        val counted = currentBreak > 0 || countInAverage
        return copy(
            currentBreak = 0,
            highestBreak = maxOf(highestBreak, currentBreak),
            breakSum = if (counted) breakSum + currentBreak else breakSum,
            breakCount = if (counted) breakCount + 1 else breakCount
        )
    }

    private fun Player.toBreak(action: BreakEndAction, rackNumber: Int, penalty: Int = 0) = Break(
        playerId = id,
        playerName = name,
        points = currentBreak,
        penalty = penalty,
        endAction = action,
        rackNumber = rackNumber
    )

    private fun GameState.withPlayer(player: Player) = copy(
        players = players.map { if (it.id == player.id) player else it }
    )

    private fun GameState.nextIndex() = (atTableIndex + 1) % players.size
}
