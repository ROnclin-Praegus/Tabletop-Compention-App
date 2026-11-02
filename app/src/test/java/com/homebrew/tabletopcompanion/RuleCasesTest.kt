package com.homebrew.tabletopcompanion

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.homebrew.tabletopcompanion.data.CharacterRepository
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.model.DamageType
import com.homebrew.tabletopcompanion.model.Vitals
import com.homebrew.tabletopcompanion.model.absorbDamage
import com.homebrew.tabletopcompanion.model.tickRound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Runs docs/rule-cases.json, the same list docs/tests.html runs against hero-utils.js. */
class RuleCasesTest {
    private val gson = Gson()

    // Gradle runs unit tests with the app/ folder as working directory.
    private val cases = gson.fromJson(File("../docs/rule-cases.json").readText(), JsonObject::class.java)

    @Test fun damageCasesMatchTheSharedList() {
        val list = cases.getAsJsonArray("absorbDamage")
        assertTrue(list.size() > 0)
        for (el in list) {
            val case = el.asJsonObject
            val start = gson.fromJson(case.get("start"), Vitals::class.java)
            val expect = gson.fromJson(case.get("expect"), Vitals::class.java)
            val type = DamageType.valueOf(case.get("type").asString.uppercase())
            assertEquals(case.get("name").asString, expect, absorbDamage(start, case.get("amount").asInt, type))
        }
    }

    @Test fun roundCasesMatchTheSharedList() {
        val list = cases.getAsJsonArray("tickRound")
        assertTrue(list.size() > 0)
        for (el in list) {
            val case = el.asJsonObject
            val name = case.get("name").asString
            // Same steps as loading a saved hero: Gson, then sanitize (fills what the JSON leaves out).
            val hero = CharacterRepository.sanitizeCharacter(gson.fromJson(case.get("hero"), Character::class.java))
            val rolls = case.getAsJsonObject("diceRolls")?.entrySet()?.associate { it.key to it.value.asInt } ?: emptyMap()
            val result = tickRound(hero, rolls)
            val expect = case.getAsJsonObject("expect")

            assertEquals(name, expect.get("hp").asInt, result.hero.currentHp)
            expect.get("mp")?.let { assertEquals(name, it.asInt, result.hero.currentMp) }
            expect.get("boostHp")?.let { assertEquals(name, it.asInt, result.hero.boostHp) }
            assertEquals(name, expect.getAsJsonArray("expired").map { it.asString }, result.expired)
            val effects = expect.getAsJsonArray("effects").map { it.asJsonObject.get("id").asString to it.asJsonObject.get("roundsRemaining").asInt }
            assertEquals(name, effects, result.hero.effects.map { it.id to it.roundsRemaining })
        }
    }
}
