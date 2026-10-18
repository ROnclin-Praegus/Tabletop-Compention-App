package com.homebrew.tabletopcompanion.model

data class SpellBookSpell(
    val tier: Int,
    val name: String,
    val cost: String,
    val description: String
)

data class BiblePrayer(
    val percentage: String,
    val name: String,
    val quote: String,
    val effect: String
)

data class SpellBook(
    val type: String, // "magic" or "faith"
    val name: String,
    val displayName: String,
    val deity: String? = null,
    val aliases: List<String> = emptyList(),
    val spells: List<SpellBookSpell> = emptyList(),
    val prayers: List<BiblePrayer> = emptyList()
)
