package com.occamzchainsaw.straightscorer.data.game

import com.occamzchainsaw.core.models.GameState
import com.occamzchainsaw.core.repositories.ActiveGameStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Stores the current game as JSON. Writes go to a temp file first, so a crash mid-write can't corrupt the save. */
class FileActiveGameStore(
    private val file: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ActiveGameStore {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): GameState? = withContext(ioDispatcher) {
        if (!file.exists()) return@withContext null
        json.decodeFromString<GameState>(file.readText())
    }

    override suspend fun save(state: GameState?) = withContext(ioDispatcher) {
        if (state == null) {
            file.delete()
            return@withContext
        }
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(json.encodeToString(state))
        Files.move(
            temp.toPath(),
            file.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )
        Unit
    }
}
