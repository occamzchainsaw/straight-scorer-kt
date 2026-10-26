package com.occamzchainsaw.core.rules

import com.occamzchainsaw.core.models.GameState
import com.occamzchainsaw.core.models.GameStatus
import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.core.models.PlayerMatchSummary

/** Returns null while the game is still in progress. */
fun GameState.toMatchResult(): MatchResult? {
    val finished = status as? GameStatus.Finished ?: return null
    return MatchResult(
        id = id,
        startedAt = startedAt,
        finishedAt = finishedAt ?: startedAt,
        targetScore = targetScore,
        racks = table.rackNumber,
        players = players.map { player ->
            PlayerMatchSummary(
                name = player.name,
                headStart = player.headStart,
                finalScore = player.score,
                highestBreak = player.highestBreak,
                averageBreak = player.averageBreak,
                breakCount = player.breakCount,
                totalFouls = player.totalFouls,
                isWinner = player.id == finished.winnerId
            )
        },
        breaks = breaks
    )
}
