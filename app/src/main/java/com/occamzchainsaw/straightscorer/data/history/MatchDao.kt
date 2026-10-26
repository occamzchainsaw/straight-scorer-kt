package com.occamzchainsaw.straightscorer.data.history

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY finished_at DESC")
    fun observeAll(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getById(id: String): MatchEntity?

    @Upsert
    suspend fun upsert(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM matches")
    suspend fun deleteAll()
}
