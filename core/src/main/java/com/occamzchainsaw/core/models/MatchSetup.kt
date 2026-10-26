package com.occamzchainsaw.core.models

data class PlayerSetup(
    val name: String,
    val headStart: Int = 0
)

data class MatchSetup(
    val players: List<PlayerSetup>,
    val targetScore: Int,
    val startingPlayerIndex: Int = 0
) {
    enum class Error {
        NoPlayers,
        TooManyPlayers,
        TargetScoreOutOfRange,
        InvalidName,
        DuplicateNames,
        HeadStartOutOfRange,
        InvalidStartingPlayer
    }

    fun validate(): Set<Error> = buildSet {
        if (players.isEmpty()) add(Error.NoPlayers)
        if (players.size > MAX_PLAYERS) add(Error.TooManyPlayers)
        if (targetScore !in MIN_TARGET_SCORE..MAX_TARGET_SCORE) add(Error.TargetScoreOutOfRange)
        if (players.any { it.name.trim().length !in 1..MAX_NAME_LENGTH }) add(Error.InvalidName)
        if (players.map { it.name.trim().lowercase() }.toSet().size < players.size) add(Error.DuplicateNames)
        if (players.any { it.headStart < 0 || it.headStart >= targetScore }) add(Error.HeadStartOutOfRange)
        if (startingPlayerIndex !in players.indices) add(Error.InvalidStartingPlayer)
    }

    val isValid: Boolean
        get() = validate().isEmpty()

    companion object {
        const val MIN_TARGET_SCORE = 10
        const val MAX_TARGET_SCORE = 1000
        const val MAX_PLAYERS = 10
        const val MAX_NAME_LENGTH = 25
    }
}
