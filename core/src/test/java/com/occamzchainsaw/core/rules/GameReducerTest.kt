package com.occamzchainsaw.core.rules

import com.occamzchainsaw.core.models.BreakEndAction
import com.occamzchainsaw.core.models.GameAction
import com.occamzchainsaw.core.models.GameSettings
import com.occamzchainsaw.core.models.GameState
import com.occamzchainsaw.core.models.GameStatus
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.core.models.TableState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameReducerTest {

    private fun game(
        playerCount: Int = 2,
        targetScore: Int = 100,
        table: TableState = TableState(),
        players: List<Player> = (0 until playerCount).map { Player(id = it, name = "P$it") }
    ) = GameState(
        players = players,
        atTableIndex = 0,
        targetScore = targetScore,
        table = table,
        startedAt = 0L
    )

    private fun GameState.apply(
        vararg actions: GameAction,
        settings: GameSettings = GameSettings()
    ): GameState = actions.fold(this) { state, action -> GameReducer.reduce(state, action, settings, now = 42L) }

    // region adding points

    @Test
    fun addPoints_increasesScoreAndBreak_andEndsOpeningBreak() {
        val state = game().apply(GameAction.AddPoints(1))

        assertEquals(1, state.players[0].score)
        assertEquals(1, state.players[0].currentBreak)
        assertEquals(14, state.table.ballsOnTable)
        assertFalse(state.table.isOpeningBreak)
        assertEquals(0, state.atTableIndex)
    }

    @Test
    fun addPoints_removesOneBallPerPoint() {
        val state = game().apply(GameAction.AddPoints(5))

        assertEquals(10, state.table.ballsOnTable)
    }

    @Test
    fun addPoints_reracksWhenOneBallIsLeft() {
        val state = game().apply(GameAction.AddPoints(14))

        assertEquals(15, state.table.ballsOnTable)
        assertEquals(2, state.table.rackNumber)
    }

    @Test
    fun addPoints_spanningARerack_continuesInTheNewRack() {
        val state = game(table = TableState(ballsOnTable = 3)).apply(GameAction.AddPoints(4))

        // 3 -> 2 -> 1 (rerack to 15) -> 14 -> 13
        assertEquals(13, state.table.ballsOnTable)
        assertEquals(2, state.table.rackNumber)
    }

    @Test
    fun addPoints_resetsConsecutiveFouls() {
        val players = listOf(Player(id = 0, name = "A", consecutiveFouls = 2), Player(id = 1, name = "B"))
        val state = game(players = players).apply(GameAction.AddPoints(1))

        assertEquals(0, state.players[0].consecutiveFouls)
    }

    @Test
    fun addPoints_zeroOrNegative_isIgnored() {
        val initial = game()

        assertSame(initial, initial.apply(GameAction.AddPoints(0)))
        assertSame(initial, initial.apply(GameAction.AddPoints(-3)))
    }

    // endregion

    // region winning

    @Test
    fun reachingTarget_finishesGame_andRecordsWinningBreak() {
        val state = game(targetScore = 10).apply(GameAction.AddPoints(10))

        assertEquals(GameStatus.Finished(winnerId = 0), state.status)
        assertEquals(42L, state.finishedAt)
        assertEquals(10, state.players[0].score)
        assertEquals(10, state.players[0].highestBreak)
        assertEquals(1, state.players[0].breakCount)
        assertEquals(10f, state.players[0].averageBreak)
        assertEquals(BreakEndAction.Win, state.breaks.last().endAction)
        assertEquals(10, state.breaks.last().points)
    }

    @Test
    fun pointsOverTarget_areClampedToTarget() {
        val players = listOf(Player(id = 0, name = "A", headStart = 8), Player(id = 1, name = "B"))
        val state = game(players = players, targetScore = 10).apply(GameAction.AddPoints(5))

        assertEquals(10, state.players[0].score)
        assertEquals(2, state.breaks.last().points)
        assertEquals(13, state.table.ballsOnTable)
    }

    @Test
    fun finishedGame_ignoresFurtherActions() {
        val finished = game(targetScore = 10).apply(GameAction.AddPoints(10))

        assertSame(finished, finished.apply(GameAction.Miss))
        assertSame(finished, finished.apply(GameAction.Foul))
        assertSame(finished, finished.apply(GameAction.AddPoints(1)))
    }

    @Test
    fun headStart_countsTowardsTarget() {
        val players = listOf(Player(id = 0, name = "A", headStart = 95), Player(id = 1, name = "B"))
        val state = game(players = players).apply(GameAction.AddPoints(5))

        assertEquals(GameStatus.Finished(0), state.status)
    }

    // endregion

    // region miss and safe

    @Test
    fun miss_passesTurn_andRecordsBreak() {
        val state = game().apply(GameAction.AddPoints(3), GameAction.Miss)

        assertEquals(1, state.atTableIndex)
        assertEquals(0, state.players[0].currentBreak)
        assertEquals(3, state.players[0].highestBreak)
        assertEquals(1, state.players[0].breakCount)
        assertEquals(BreakEndAction.Miss, state.breaks.single().endAction)
        assertEquals(3, state.breaks.single().points)
        assertEquals("P0", state.breaks.single().playerName)
    }

    @Test
    fun turnRotation_wrapsAroundThreePlayers() {
        val state = game(playerCount = 3).apply(GameAction.Miss, GameAction.Miss)
        assertEquals(2, state.atTableIndex)
        assertEquals(0, state.apply(GameAction.Miss).atTableIndex)
    }

    @Test
    fun pointlessMiss_countsTowardsAverage_byDefault() {
        val state = game().apply(GameAction.Miss)

        assertEquals(1, state.players[0].breakCount)
        assertEquals(0f, state.players[0].averageBreak)
    }

    @Test
    fun pointlessMiss_isIgnoredInAverage_whenDisabled() {
        val state = game().apply(GameAction.Miss, settings = GameSettings(countMissesInAverage = false))

        assertEquals(0, state.players[0].breakCount)
        assertEquals(BreakEndAction.Miss, state.breaks.single().endAction)
    }

    @Test
    fun pointlessSafe_isIgnoredInAverage_byDefault_butStillInHistory() {
        val state = game().apply(GameAction.Safe)

        assertEquals(0, state.players[0].breakCount)
        assertEquals(0f, state.players[0].averageBreak)
        assertEquals(BreakEndAction.Safe, state.breaks.single().endAction)
        assertEquals("P0", state.breaks.single().playerName)
        assertEquals(1, state.atTableIndex)
    }

    @Test
    fun pointlessSafe_countsTowardsAverage_whenEnabled() {
        val state = game().apply(GameAction.Safe, settings = GameSettings(countSafetiesInAverage = true))

        assertEquals(1, state.players[0].breakCount)
    }

    @Test
    fun safeWithPoints_alwaysCountsTowardsAverage() {
        val state = game().apply(GameAction.AddPoints(4), GameAction.Safe)

        assertEquals(1, state.players[0].breakCount)
        assertEquals(4f, state.players[0].averageBreak)
    }

    @Test
    fun missAndSafe_resetConsecutiveFouls() {
        val players = listOf(Player(id = 0, name = "A", consecutiveFouls = 2), Player(id = 1, name = "B"))

        assertEquals(0, game(players = players).apply(GameAction.Miss).players[0].consecutiveFouls)
        assertEquals(0, game(players = players).apply(GameAction.Safe).players[0].consecutiveFouls)
    }

    // endregion

    // region fouls

    @Test
    fun standardFoul_costsOnePoint_andPassesTurn() {
        val state = game().apply(GameAction.Foul)

        assertEquals(-1, state.players[0].score)
        assertEquals(1, state.players[0].consecutiveFouls)
        assertEquals(1, state.players[0].totalFouls)
        assertEquals(1, state.atTableIndex)
        assertFalse(state.table.isOpeningBreak)
        assertEquals(BreakEndAction.Foul, state.breaks.single().endAction)
        assertEquals(1, state.breaks.single().penalty)
    }

    @Test
    fun foulAfterPottingBalls_keepsBreakPoints_andRecordsNetPoints() {
        val state = game().apply(GameAction.AddPoints(5), GameAction.Foul)

        assertEquals(4, state.players[0].score)
        assertEquals(5, state.players[0].highestBreak)
        assertEquals(5f, state.players[0].averageBreak)
        assertEquals(4, state.breaks.single().netPoints)
        assertEquals(1, state.players[0].consecutiveFouls)
    }

    @Test
    fun pointlessFoul_followsMissAverageSetting() {
        assertEquals(1, game().apply(GameAction.Foul).players[0].breakCount)
        assertEquals(
            0,
            game().apply(GameAction.Foul, settings = GameSettings(countMissesInAverage = false)).players[0].breakCount
        )
    }

    @Test
    fun thirdConsecutiveFoul_costsSixteen_reracks_andOffenderBreaksAgain() {
        val players = listOf(Player(id = 0, name = "A", consecutiveFouls = 2), Player(id = 1, name = "B"))
        val state = game(players = players, table = TableState(ballsOnTable = 7, rackNumber = 3))
            .apply(GameAction.Foul)

        assertEquals(-16, state.players[0].score)
        assertEquals(0, state.players[0].consecutiveFouls)
        assertEquals(1, state.players[0].totalFouls)
        assertEquals(0, state.players[0].breakCount)
        assertEquals(TableState(ballsOnTable = 15, rackNumber = 4, isOpeningBreak = true), state.table)
        assertEquals(0, state.atTableIndex)
        assertEquals(BreakEndAction.ThirdFoul, state.breaks.single().endAction)
        assertEquals(-16, state.breaks.single().netPoints)
    }

    @Test
    fun thirdConsecutiveFoul_withoutRerack_passesTurn() {
        val players = listOf(Player(id = 0, name = "A", consecutiveFouls = 2), Player(id = 1, name = "B"))
        val state = game(players = players, table = TableState(ballsOnTable = 7, isOpeningBreak = false))
            .apply(GameAction.Foul, settings = GameSettings(resetRackOnThirdFoul = false))

        assertEquals(-16, state.players[0].score)
        assertEquals(7, state.table.ballsOnTable)
        assertFalse(state.table.isOpeningBreak)
        assertEquals(1, state.atTableIndex)
    }

    @Test
    fun threeFoulsAcrossTurns_triggerThirdFoulRule() {
        val state = game().apply(
            GameAction.Foul, GameAction.Miss,
            GameAction.Foul, GameAction.Miss,
            GameAction.Foul
        )

        assertEquals(-16 - 2, state.players[0].score)
        assertEquals(BreakEndAction.ThirdFoul, state.breaks.last().endAction)
    }

    @Test
    fun legalShotBetweenFouls_resetsCount() {
        val state = game().apply(
            GameAction.Foul, GameAction.Miss,
            GameAction.Miss, GameAction.Miss,
            GameAction.Foul
        )

        assertEquals(1, state.players[0].consecutiveFouls)
        assertEquals(-2, state.players[0].score)
    }

    // endregion

    // region breaking foul

    @Test
    fun breakingFoul_acceptedTable_costsTwo_andPassesTurn() {
        val state = game().apply(GameAction.BreakingFoul(reBreak = false))

        assertEquals(-2, state.players[0].score)
        assertEquals(0, state.players[0].consecutiveFouls)
        assertEquals(1, state.players[0].totalFouls)
        assertEquals(0, state.players[0].breakCount)
        assertEquals(1, state.atTableIndex)
        assertFalse(state.table.isOpeningBreak)
        assertEquals(BreakEndAction.BreakingFoul, state.breaks.single().endAction)
        assertEquals(-2, state.breaks.single().netPoints)
    }

    @Test
    fun breakingFoul_reBreak_keepsBreakerAtTable() {
        val state = game().apply(
            GameAction.BreakingFoul(reBreak = true),
            GameAction.BreakingFoul(reBreak = true)
        )

        assertEquals(-4, state.players[0].score)
        assertEquals(0, state.atTableIndex)
        assertTrue(state.table.isOpeningBreak)
        assertEquals(15, state.table.ballsOnTable)
    }

    @Test
    fun breakingFoul_outsideOpeningBreak_isIgnored() {
        val started = game().apply(GameAction.Miss)

        assertSame(started, started.apply(GameAction.BreakingFoul(reBreak = false)))
    }

    @Test
    fun breakingFoul_isAvailableAgain_afterThirdFoulRerack() {
        val players = listOf(Player(id = 0, name = "A", consecutiveFouls = 2), Player(id = 1, name = "B"))
        val state = game(players = players, table = TableState(isOpeningBreak = false))
            .apply(GameAction.Foul, GameAction.BreakingFoul(reBreak = false))

        assertEquals(-18, state.players[0].score)
        assertEquals(1, state.atTableIndex)
    }

    // endregion
}
