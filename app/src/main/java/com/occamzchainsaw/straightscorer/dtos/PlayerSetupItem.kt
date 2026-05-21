package com.occamzchainsaw.straightscorer.dtos

import java.util.UUID

data class PlayerSetupItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val headStart: String = "0",
    val isStarting: Boolean = false
)
