package com.homebrew.tabletopcompanion.model

import java.util.UUID

data class CharacterAbility(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String
)
