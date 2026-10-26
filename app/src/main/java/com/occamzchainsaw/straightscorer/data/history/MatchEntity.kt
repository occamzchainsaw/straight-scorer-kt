package com.occamzchainsaw.straightscorer.data.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.occamzchainsaw.core.models.Break
import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.core.models.PlayerMatchSummary
import kotlinx.serialization.json.Json

@Entity(
    tableName = "matches",
    indices = [Index("finished_at")]
)
data class MatchEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long,
    @ColumnInfo(name = "target_score") val targetScore: Int,
    val racks: Int,
    @ColumnInfo(name = "winner_name") val winnerName: String?,
    /** JSON list of [PlayerMatchSummary]. */
    @ColumnInfo(name = "players_json") val playersJson: String,
    /** JSON list of [Break]. */
    @ColumnInfo(name = "breaks_json") val breaksJson: String
)

private val json = Json { ignoreUnknownKeys = true }

fun MatchResult.toEntity() = MatchEntity(
    id = id,
    startedAt = startedAt,
    finishedAt = finishedAt,
    targetScore = targetScore,
    racks = racks,
    winnerName = winner?.name,
    playersJson = json.encodeToString(players),
    breaksJson = json.encodeToString(breaks)
)

fun MatchEntity.toMatchResult() = MatchResult(
    id = id,
    startedAt = startedAt,
    finishedAt = finishedAt,
    targetScore = targetScore,
    racks = racks,
    players = json.decodeFromString<List<PlayerMatchSummary>>(playersJson),
    breaks = json.decodeFromString<List<Break>>(breaksJson)
)
