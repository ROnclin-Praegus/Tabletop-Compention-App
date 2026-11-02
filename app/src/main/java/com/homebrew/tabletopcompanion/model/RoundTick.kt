package com.homebrew.tabletopcompanion.model

/** The hero after a round, plus the messages for effects and boosts that ran out. */
data class RoundResult(val hero: Character, val expired: List<String>)

/**
 * Plays one round for a hero: armor and ward refill, boosts reset, Spirit recharges MP,
 * per-round HP/MP effects run, and durations count down.
 * An effect that is switched off does nothing and does not count down, so it can be switched on again.
 * Same rules as tickRound() in docs/hero-utils.js.
 * [diceRollResults] maps an effect id to the rolled amount (negative = damage), for effects with dice.
 */
fun tickRound(c: Character, diceRollResults: Map<String, Int> = emptyMap()): RoundResult {
    val totalArmor = c.getTotalArmor()
    val totalWard = c.getTotalWard()
    var hp = c.currentHp
    var mp = c.currentMp
    var armor = totalArmor
    var ward = totalWard
    var boostHp = c.boostHp
    var maxBoostHp = c.maxBoostHp
    var boostHpTurns = c.boostHpTurns
    var boostArmor = c.boostArmor
    var maxBoostArmor = c.maxBoostArmor
    var boostArmorTurns = c.boostArmorTurns
    var boostWard = c.boostWard
    var maxBoostWard = c.maxBoostWard
    var boostWardTurns = c.boostWardTurns

    val expired = mutableListOf<String>()
    val maxHp = c.getEffectiveMaxHp()
    val maxMp = c.getEffectiveMaxMp()
    val spirit = c.getEffectiveSpirit()

    // 1. A boost that resets each round goes back to its full amount while it is active.
    if (maxBoostHp > 0 && c.boostHpResetOnNextRound && (boostHpTurns > 0 || (boostHpTurns == 0 && boostHp > 0))) boostHp = maxBoostHp
    if (maxBoostArmor > 0 && c.boostArmorResetOnNextRound && (boostArmorTurns > 0 || (boostArmorTurns == 0 && boostArmor > 0))) boostArmor = maxBoostArmor
    if (maxBoostWard > 0 && c.boostWardResetOnNextRound && (boostWardTurns > 0 || (boostWardTurns == 0 && boostWard > 0))) boostWard = maxBoostWard

    // 2. Spirit recharges MP (1 Spirit = +1 MP per round).
    if (maxMp > 0 && spirit > 0) mp = (mp + spirit).coerceIn(0, maxMp)

    // 3. Per-round HP/MP effects and their durations.
    val effects = c.effects.mapNotNull { effect ->
        if (!effect.enabled) return@mapNotNull effect

        val amount = if (effect.isDiceRoll && diceRollResults.containsKey(effect.id)) diceRollResults[effect.id] ?: 0 else effect.value

        if (effect.effectType == EffectType.HP_CHANGE_PER_ROUND) {
            if (amount > 0) {
                hp = (hp + amount).coerceIn(0, maxHp)
            } else if (amount < 0) {
                val after = absorbDamage(Vitals(hp, boostHp, armor, ward, boostArmor, boostWard), -amount, effect.damageType)
                hp = after.hp
                boostHp = after.boostHp
                armor = after.armor
                ward = after.ward
                boostArmor = after.boostArmor
                boostWard = after.boostWard
            }
        }
        if (effect.effectType == EffectType.MP_CHANGE_PER_ROUND && maxMp > 0) {
            mp = (mp + amount).coerceIn(0, maxMp)
        }

        when {
            effect.roundsRemaining > 1 -> effect.copy(roundsRemaining = effect.roundsRemaining - 1)
            effect.roundsRemaining == 1 -> {
                expired.add("Effect expired: ${effect.name}")
                null
            }
            else -> effect // 0 = unlimited
        }
    }

    // 4. Boost durations count down.
    if (boostHpTurns > 1) boostHpTurns -= 1
    else if (boostHpTurns == 1) {
        boostHpTurns = 0; boostHp = 0; maxBoostHp = 0
        expired.add("Boost HP duration has ended!")
    }
    if (boostArmorTurns > 1) boostArmorTurns -= 1
    else if (boostArmorTurns == 1) {
        boostArmorTurns = 0; boostArmor = 0; maxBoostArmor = 0
        expired.add("Armor Boost duration has ended!")
    }
    if (boostWardTurns > 1) boostWardTurns -= 1
    else if (boostWardTurns == 1) {
        boostWardTurns = 0; boostWard = 0; maxBoostWard = 0
        expired.add("Ward Save Boost duration has ended!")
    }

    val hero = c.copy(
        currentHp = hp, currentMp = mp,
        boostHp = boostHp, maxBoostHp = maxBoostHp, boostHpTurns = boostHpTurns,
        boostArmor = boostArmor, maxBoostArmor = maxBoostArmor, boostArmorTurns = boostArmorTurns,
        boostWard = boostWard, maxBoostWard = maxBoostWard, boostWardTurns = boostWardTurns,
        currentArmor = armor, currentWard = ward,
        effects = effects
    )
    return RoundResult(hero, expired)
}
