package com.homebrew.tabletopcompanion.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

enum class EffectType {
    STAT_MODIFIER,
    HP_CHANGE_PER_ROUND,
    MP_CHANGE_PER_ROUND
}

enum class DamageType {
    @SerializedName(value = "PHYSICAL", alternate = ["physical"])
    PHYSICAL,   // Absorbed by Armor first, then Boost HP, then HP
    @SerializedName(value = "MAGICAL", alternate = ["magical"])
    MAGICAL,    // Absorbed by Ward Save first, then Boost HP, then HP
    @SerializedName(value = "UNSAVEABLE", alternate = ["unsaveable"])
    UNSAVEABLE  // Bypasses Armor & Ward, absorbed by Boost HP first, then HP
}

data class CharacterEffect(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val effectType: EffectType = EffectType.STAT_MODIFIER,
    val targetStat: String? = null, // "strength", "intelligence", "endurance", "spirit", "finesse", "charisma"
    val value: Int = 0, // e.g. +3, -5
    val statModifiers: Map<String, Int>? = emptyMap(),
    val roundsRemaining: Int = 0, // 0 = Unlimited, > 0 = remaining rounds
    val damageType: DamageType = DamageType.UNSAVEABLE, // For negative HP_CHANGE_PER_ROUND
    val isDiceRoll: Boolean = false,
    val diceFormula: String = "",
    val changeMode: String = "DMG", // "DMG", "HEAL", "MIXED"
    val isEnabled: Boolean? = true
) {
    val enabled: Boolean
        get() = isEnabled != false

    val safeName: String
        get() = name ?: ""

    val safeDiceFormula: String
        get() = diceFormula ?: ""

    val safeChangeMode: String
        get() = changeMode ?: "DMG"
}
