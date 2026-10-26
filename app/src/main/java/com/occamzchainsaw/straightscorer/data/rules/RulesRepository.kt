package com.occamzchainsaw.straightscorer.data.rules

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface RulesRepository {
    /** The 14.1 rules as markdown. */
    suspend fun loadRules(): String
}

class AssetRulesRepository(private val assets: AssetManager) : RulesRepository {
    override suspend fun loadRules(): String = withContext(Dispatchers.IO) {
        assets.open(RULES_FILE).bufferedReader().use { it.readText() }
    }

    private companion object {
        const val RULES_FILE = "rules.md"
    }
}
