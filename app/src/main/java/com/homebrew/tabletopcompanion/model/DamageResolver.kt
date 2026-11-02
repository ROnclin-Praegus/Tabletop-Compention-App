package com.homebrew.tabletopcompanion.model

/** The parts of a hero that damage is taken from. */
data class Vitals(
    val hp: Int,
    val boostHp: Int,
    val armor: Int,
    val ward: Int,
    val boostArmor: Int,
    val boostWard: Int
)

/**
 * Applies damage in one place for the damage dialog and the per-round effects.
 * Physical: boost armor, then armor. Magical: boost ward, then ward. Unsaveable skips both.
 * What is left goes to boost HP, then HP. Same rule as absorbDamage() in docs/hero-utils.js.
 */
fun absorbDamage(v: Vitals, amount: Int, type: DamageType): Vitals {
    if (amount <= 0) return v
    var remaining = amount
    fun take(have: Int): Int {
        val used = minOf(remaining, have.coerceAtLeast(0))
        remaining -= used
        return used
    }

    var boostArmor = v.boostArmor
    var armor = v.armor
    var boostWard = v.boostWard
    var ward = v.ward
    when (type) {
        DamageType.PHYSICAL -> {
            boostArmor -= take(boostArmor)
            armor -= take(armor)
        }
        DamageType.MAGICAL -> {
            boostWard -= take(boostWard)
            ward -= take(ward)
        }
        DamageType.UNSAVEABLE -> Unit
    }
    val boostHp = v.boostHp - take(v.boostHp)
    val hp = (v.hp - remaining).coerceAtLeast(0)
    return Vitals(hp = hp, boostHp = boostHp, armor = armor, ward = ward, boostArmor = boostArmor, boostWard = boostWard)
}
