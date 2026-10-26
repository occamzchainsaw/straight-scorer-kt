package com.occamzchainsaw.straightscorer.data.history

import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.core.repositories.MatchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMatchHistoryRepository(private val dao: MatchDao) : MatchHistoryRepository {
    override fun observeMatches(): Flow<List<MatchResult>> =
        dao.observeAll().map { matches -> matches.map { it.toMatchResult() } }

    override suspend fun getMatch(id: String): MatchResult? = dao.getById(id)?.toMatchResult()

    override suspend fun save(result: MatchResult) = dao.upsert(result.toEntity())

    override suspend fun delete(id: String) = dao.deleteById(id)

    override suspend fun clear() = dao.deleteAll()
}
