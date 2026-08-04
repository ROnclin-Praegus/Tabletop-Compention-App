package com.homebrew.tabletopcompanion.model

import java.util.UUID

enum class EffectType {
    STAT_MODIFIER,
    HP_CHANGE_PER_ROUND,
    MP_CHANGE_PER_ROUND
}

enum class DamageType {
    PHYSICAL,   // Absorbed by Armor first, then Boost HP, then HP
    MAGICAL,    // Absorbed by Ward Save first, then Boost HP, then HP
    UNSAVEABLE  // Bypasses Armor & Ward, absorbed by Boost HP first, then HP
}

data class CharacterEffect(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val effectType: EffectType = EffectType.STAT_MODIFIER,
    val targetStat: String? = null, // "strength", "intelligence", "endurance", "spirit", "finesse", "charisma"
    val value: Int = 0, // e.g. +3, -5
    val roundsRemaining: Int = 0, // 0 = Unlimited, > 0 = remaining rounds
    val damageType: DamageType = DamageType.UNSAVEABLE // For negative HP_CHANGE_PER_ROUND
)

