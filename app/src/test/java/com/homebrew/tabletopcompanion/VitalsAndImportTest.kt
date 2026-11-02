package com.homebrew.tabletopcompanion

import com.google.gson.Gson
import com.homebrew.tabletopcompanion.data.CharacterRepository
import com.homebrew.tabletopcompanion.model.Character
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VitalsAndImportTest {
    private fun hero(endurance: Int, hp: Int, intelligence: Int = 0, mp: Int = 0) =
        Character(name = "T", race = "R", currentHp = hp, currentMp = mp, endurance = endurance, intelligence = intelligence)

    @Test fun raisingEnduranceRaisesCurrentHpByTheSameAmount() {
        val before = hero(endurance = 3, hp = 20) // max 30
        val after = before.withVitalsAdjustedTo(before.copy(endurance = 5)) // max 50
        assertEquals(40, after.currentHp)
    }

    @Test fun loweringEnduranceNeverLeavesHpAboveTheNewMax() {
        val before = hero(endurance = 5, hp = 50)
        assertEquals(30, before.withVitalsAdjustedTo(before.copy(endurance = 3)).currentHp)
    }

    @Test fun loweringEnduranceNeverGoesBelowZero() {
        val before = hero(endurance = 5, hp = 5)
        assertEquals(0, before.withVitalsAdjustedTo(before.copy(endurance = 0)).currentHp)
    }

    @Test fun mpFollowsIntelligence() {
        val before = hero(endurance = 1, hp = 10, intelligence = 2, mp = 2)
        assertEquals(5, before.withVitalsAdjustedTo(before.copy(intelligence = 5)).currentMp)
    }

    @Test fun loadedHeroWithHpAboveItsRealMaxIsCapped() {
        // Old save: current HP 50, but endurance 4 means the real max is 40.
        val clean = CharacterRepository.sanitizeCharacter(hero(endurance = 4, hp = 50, intelligence = 3, mp = 40))
        assertEquals(40, clean.currentHp)
        assertEquals(3, clean.currentMp)
    }

    @Test fun heroWithZeroEnduranceKeepsItsHp() {
        assertEquals(5, CharacterRepository.sanitizeCharacter(hero(endurance = 0, hp = 5)).currentHp)
    }

    @Test fun importedFileWithoutListsGetsEmptyLists() {
        // Same steps as the import button: Gson, then sanitize.
        val raw = Gson().fromJson("""{"name":"Friend","race":"Elf","maxHp":10,"maxMp":0}""", Character::class.java)
        val clean = CharacterRepository.sanitizeCharacter(raw)
        assertTrue(clean.inventory.isEmpty() && clean.effects.isEmpty() && clean.abilities.isEmpty() && clean.notes.isEmpty())
        assertEquals(0, clean.getTotalArmor()) // used to crash on a null inventory
    }

    @Test fun partsWithMissingFieldsAreFilledIn() {
        // A hand-written file: no ids, no texts. Used to crash the cleanup or the screen that draws them.
        val json = """{"id":"h","name":"A","race":"x","currentHp":1,
            "abilities":[{"name":"Magic: Fire"}],"notes":[{}],
            "inventory":[{"name":"Sword"}],"effects":[{"effectType":"HP_CHANGE_PER_ROUND"}]}"""
        val clean = CharacterRepository.sanitizeCharacter(Gson().fromJson(json, Character::class.java))
        assertEquals("", clean.abilities.single().description)
        assertTrue(clean.abilities.single().id.isNotEmpty() && clean.notes.single().id.isNotEmpty())
        assertTrue(clean.inventory.single().id.isNotEmpty() && clean.effects.single().id.isNotEmpty())
        assertEquals("", clean.notes.single().content)
    }

    @Test fun onlyPicturesOnThisPhoneOrInlineDataAreKept() {
        fun uri(u: String?) = CharacterRepository.sanitizeCharacter(hero(endurance = 1, hp = 1).copy(imageUri = u)).imageUri
        assertEquals("content://media/1", uri("content://media/1"))
        assertEquals("data:image/jpeg;base64,AA", uri("data:image/jpeg;base64,AA"))
        assertEquals(null, uri("https://example.com/p.png"))
    }

    @Test fun goldStopsAtTheLargestNumberInsteadOfWrapping() {
        val rich = hero(endurance = 1, hp = 1).copy(gold = 2_000_000_000)
        assertEquals(Int.MAX_VALUE, rich.goldPlus(2_000_000_000))
        assertEquals(5, hero(endurance = 1, hp = 1).goldPlus(5))
    }
}
