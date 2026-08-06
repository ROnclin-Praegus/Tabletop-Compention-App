package com.homebrew.tabletopcompanion.model

import java.util.UUID

data class Character(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val race: String,
    var level: Int = 1,
    val maxHp: Int,
    var currentHp: Int = maxHp,
    var boostHp: Int = 0,
    var maxBoostHp: Int = 0,
    var boostHpTurns: Int = 0, // 0 = unlimited/expired
    var boostHpResetOnNextRound: Boolean = true,
    var currentArmor: Int? = null,
    var currentWard: Int? = null,
    val maxMp: Int,
    var currentMp: Int = maxMp,
    val hideMp: Boolean = false,
    var gold: Int = 0,
    val imageUri: String? = null,
    // STATS
    val strength: Int = 0,
    val intelligence: Int = 0,
    val endurance: Int = 0,
    val spirit: Int = 0,
    val finesse: Int = 0,
    val charisma: Int = 0,
    // INVENTORY, EQUIPMENT, ABILITIES, NOTES & EFFECTS
    val inventory: List<EquipmentItem> = emptyList(),
    val abilities: List<CharacterAbility> = emptyList(),
    val notes: List<CharacterNote> = emptyList(),
    val effects: List<CharacterEffect> = emptyList()
) {
    fun getTotalArmor(): Int = inventory
        .filter { it.isEquipped && (it.hasArmor || it.armorSave > 0) }
        .sumOf { it.armorSave }

    fun getTotalWard(): Int = inventory
        .filter { it.isEquipped && (it.hasWard || it.wardSave > 0) }
        .sumOf { it.wardSave }

    fun getEffectiveCurrentArmor(): Int = currentArmor ?: getTotalArmor()
    fun getEffectiveCurrentWard(): Int = currentWard ?: getTotalWard()

    // Calculate total stat modifier from equipped gear + active stat effects
    fun getStatModifier(statKey: String): Int {
        val keyLower = statKey.lowercase()

        // 1. From equipped gear
        val gearMod = inventory
            .filter { it.isEquipped && (it.hasStatModifiers || it.statModifiers.isNotEmpty()) }
            .sumOf { it.statModifiers[keyLower] ?: 0 }

        // 2. From active stat effects
        val effectMod = effects
            .filter { it.effectType == EffectType.STAT_MODIFIER && it.targetStat?.lowercase() == keyLower }
            .sumOf { it.value }

        return gearMod + effectMod
    }

    fun getEffectiveMaxHp(): Int {
        val totalEnd = endurance + getStatModifier("endurance")
        return (totalEnd * 10).coerceAtLeast(0)
    }

    fun getEffectiveMaxMp(): Int {
        if (hideMp) return 0
        val totalInt = intelligence + getStatModifier("intelligence")
        return (totalInt * 1).coerceAtLeast(0)
    }

    fun getEffectiveSpirit(): Int {
        val totalSpi = spirit + getStatModifier("spirit")
        return totalSpi.coerceAtLeast(0)
    }

    fun formatAbilityDescription(rawDesc: String): String {
        if (rawDesc.isBlank()) return ""
        val strTotal = strength + getStatModifier("strength")
        val intTotal = intelligence + getStatModifier("intelligence")
        val endTotal = endurance + getStatModifier("endurance")
        val spiTotal = spirit + getStatModifier("spirit")
        val finTotal = finesse + getStatModifier("finesse")
        val chaTotal = charisma + getStatModifier("charisma")

        var res = rawDesc
        res = res.replace(Regex("(?i)\\{STR\\}|\\{STRENGTH\\}"), strTotal.toString())
        res = res.replace(Regex("(?i)\\{INT\\}|\\{INTELLIGENCE\\}"), intTotal.toString())
        res = res.replace(Regex("(?i)\\{END\\}|\\{ENDURANCE\\}"), endTotal.toString())
        res = res.replace(Regex("(?i)\\{SPI\\}|\\{SPIRIT\\}"), spiTotal.toString())
        res = res.replace(Regex("(?i)\\{FIN\\}|\\{FINESSE\\}"), finTotal.toString())
        res = res.replace(Regex("(?i)\\{CHA\\}|\\{CHARISMA\\}"), chaTotal.toString())
        return res
    }
}
