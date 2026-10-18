package com.homebrew.tabletopcompanion.model

import java.util.UUID

data class EquipmentItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "Gear", // Freeform category e.g. "Weapon", "Armor", "Potion", "Scroll"
    val isEquipped: Boolean = false,
    val hasArmor: Boolean = false,
    val armorSave: Int = 0, // Integer points (e.g. 2, 5, 10)
    val hasWard: Boolean = false,
    val wardSave: Int = 0,  // Integer points (e.g. 1, 5, 10)
    val hasStatModifiers: Boolean = false,
    val statModifiers: Map<String, Int>? = emptyMap(), // Key: "strength", "intelligence", etc., Value: +2, -1
    val hasAttacks: Boolean = false,
    val attackCount: Int = 1,
    val attackDamage: String = "",
    val hasSpecialEffects: Boolean = false,
    val specialEffectsText: String? = "",
    val onHandSpecialEffectsText: String? = "",
    val hasMagic: Boolean = false,
    val spellDamageModifier: Int = 0,
    val spellHealingModifier: Int = 0,
    val customProperties: Map<String, String>? = emptyMap(),
    val isConsumable: Boolean = false,
    val consumableAmount: Int = 1
) {
    val safeSpecialEffectsText: String
        get() = specialEffectsText ?: ""

    val safeOnHandSpecialEffectsText: String
        get() = onHandSpecialEffectsText ?: ""

    val safeAttackDamage: String
        get() = attackDamage ?: ""

    val safeName: String
        get() = name ?: ""

    val safeCategory: String
        get() = category ?: "Gear"
}
