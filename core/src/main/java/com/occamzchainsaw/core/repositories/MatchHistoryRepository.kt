package com.occamzchainsaw.core.repositories

import com.occamzchainsaw.core.models.MatchResult
import kotlinx.coroutines.flow.Flow

interface MatchHistoryRepository {
    /** All saved matches, most recently finished first. */
    fun observeMatches(): Flow<List<MatchResult>>

    suspend fun getMatch(id: String): MatchResult?

    /** Inserts the result, or replaces an existing one with the same id. */
    suspend fun save(result: MatchResult)

    suspend fun delete(id: String)

    suspend fun clear()
}
