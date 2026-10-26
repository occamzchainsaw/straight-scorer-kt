package com.occamzchainsaw.core.repositories

import com.occamzchainsaw.core.models.MatchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeMatchHistoryRepository : MatchHistoryRepository {
    val matches = MutableStateFlow<Map<String, MatchResult>>(emptyMap())
    var saveCount = 0
        private set

    override fun observeMatches(): Flow<List<MatchResult>> =
        matches.map { all -> all.values.sortedByDescending { it.finishedAt } }

    override suspend fun getMatch(id: String) = matches.value[id]

    override suspend fun save(result: MatchResult) {
        saveCount++
        matches.update { it + (result.id to result) }
    }

    override suspend fun delete(id: String) = matches.update { it - id }

    override suspend fun clear() = matches.update { emptyMap() }
}
