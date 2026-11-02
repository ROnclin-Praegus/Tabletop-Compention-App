package com.homebrew.tabletopcompanion.model

import java.util.UUID

data class Character(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val race: String,
    var level: Int = 1,
    var currentHp: Int,
    var boostHp: Int = 0,
    var maxBoostHp: Int = 0,
    var boostHpTurns: Int = 0, // 0 = unlimited/expired
    var boostHpResetOnNextRound: Boolean = true,
    var currentArmor: Int? = null,
    var boostArmor: Int = 0,
    var maxBoostArmor: Int = 0,
    var boostArmorTurns: Int = 0,
    var boostArmorResetOnNextRound: Boolean = true,
    var currentWard: Int? = null,
    var boostWard: Int = 0,
    var maxBoostWard: Int = 0,
    var boostWardTurns: Int = 0,
    var boostWardResetOnNextRound: Boolean = true,
    var currentMp: Int = 0,
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
    val effects: List<CharacterEffect> = emptyList(),
    val onHandWeaponId: String? = null,
    val offHandWeaponId: String? = null
) {
    fun getTotalArmor(): Int = inventory
        .filter { it.isEquipped && (it.hasArmor || it.armorSave > 0) }
        .sumOf { it.armorSave }

    fun getTotalWard(): Int = inventory
        .filter { it.isEquipped && (it.hasWard || it.wardSave > 0) }
        .sumOf { it.wardSave }

    /**
     * Returns this hero with a new inventory. Armor and ward that already took damage stay damaged
     * (they are only capped to the new total), so changing gear mid-round does not heal them.
     * Armor that is still untouched (null = "full") stays full, so newly equipped armor counts at once.
     */
    fun withInventory(newInventory: List<EquipmentItem>): Character {
        val changed = copy(inventory = newInventory)
        return changed.copy(
            currentArmor = currentArmor?.coerceAtMost(changed.getTotalArmor()),
            currentWard = currentWard?.coerceAtMost(changed.getTotalWard())
        )
    }

    /** Gold after adding [amount], stopping at the largest Int instead of wrapping to a negative number. */
    fun goldPlus(amount: Int): Int = (gold.toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    fun getEffectiveCurrentArmor(): Int = currentArmor ?: getTotalArmor()
    fun getEffectiveCurrentWard(): Int = currentWard ?: getTotalWard()

    // Calculate total stat modifier from equipped gear + active stat effects
    fun getStatModifier(statKey: String): Int {
        val keyLower = statKey.lowercase()

        // 1. From equipped gear
        val gearMod = inventory
            .filter { it.isEquipped && (it.hasStatModifiers || it.statModifiers?.isNotEmpty() == true) }
            .sumOf { it.statModifiers?.get(keyLower) ?: 0 }

        // 2. From active stat effects
        val effectMod = effects
            .filter { it.enabled && it.effectType == EffectType.STAT_MODIFIER }
            .sumOf { effect ->
                val legacyVal = if (effect.targetStat?.lowercase() == keyLower) effect.value else 0
                val mapVal = effect.statModifiers?.get(keyLower) ?: 0
                legacyVal + mapVal
            }

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

    /**
     * [changed] is this hero after a stat or gear change. Max HP/MP moved, so current HP/MP move by the
     * same amount (never below 0 or above the new max). Used by the Use screen and the edit dialog.
     */
    fun withVitalsAdjustedTo(changed: Character): Character {
        val newMaxHp = changed.getEffectiveMaxHp()
        val newMaxMp = changed.getEffectiveMaxMp()
        return changed.copy(
            currentHp = (currentHp + newMaxHp - getEffectiveMaxHp()).coerceIn(0, newMaxHp),
            currentMp = (currentMp + newMaxMp - getEffectiveMaxMp()).coerceIn(0, newMaxMp)
        )
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
