package com.homebrew.tabletopcompanion.model

import java.util.UUID

data class CharacterNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val tag: String = "",
    val content: String = "",
    val isArchived: Boolean = false
)
