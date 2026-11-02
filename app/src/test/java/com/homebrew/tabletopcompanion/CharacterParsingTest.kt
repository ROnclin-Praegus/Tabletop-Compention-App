package com.homebrew.tabletopcompanion

import com.homebrew.tabletopcompanion.data.CharacterRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterParsingTest {
    @Test fun oldSaveWithoutListsLoadsWithEmptyLists() {
        // An old save has no "inventory", "effects", "abilities" or "notes". Gson leaves them null.
        val parsed = CharacterRepository.parseCharacters("""[{"id":"1","name":"Old","race":"Elf","maxHp":5,"maxMp":0}]""")
        assertEquals(0, parsed.skipped)
        val hero = parsed.heroes.single()
        assertTrue(hero.inventory.isEmpty() && hero.effects.isEmpty() && hero.abilities.isEmpty() && hero.notes.isEmpty())
    }

    @Test fun oneBrokenEntryDoesNotHideTheOthers() {
        val parsed = CharacterRepository.parseCharacters(
            """[{"id":"1","name":"A","race":"x","maxHp":1,"maxMp":0}, null, {"id":"2","name":"B","race":"x","maxHp":1,"maxMp":0,"inventory":[null]}]"""
        )
        assertEquals(listOf("A", "B"), parsed.heroes.map { it.name })
        assertEquals(1, parsed.skipped)
        assertTrue(parsed.heroes[1].inventory.isEmpty())
    }

    @Test(expected = Exception::class) fun textThatIsNotAHeroListThrows() {
        CharacterRepository.parseCharacters("{not json")
    }
}

class WebHeroFormatTest {
    private fun effectJson(damageType: String) =
        """[{"id":"1","name":"W","race":"x","maxHp":5,"maxMp":0,"effects":[{"id":"e","name":"p","effectType":"HP_CHANGE_PER_ROUND","value":-3,"damageType":"$damageType","roundsRemaining":3}]}]"""

    @Test fun damageTypeFromTheWebAppKeepsTheHero() {
        // The web app writes lowercase names. This used to make Android drop the whole hero.
        val parsed = CharacterRepository.parseCharacters(effectJson("physical"))
        assertEquals(0, parsed.skipped)
        assertEquals(com.homebrew.tabletopcompanion.model.DamageType.PHYSICAL, parsed.heroes.single().effects.single().damageType)
    }

    @Test fun damageTypeFromAndroidStillWorks() {
        val parsed = CharacterRepository.parseCharacters(effectJson("MAGICAL"))
        assertEquals(com.homebrew.tabletopcompanion.model.DamageType.MAGICAL, parsed.heroes.single().effects.single().damageType)
    }

    @Test fun unknownDamageTypeBecomesUnsaveableInsteadOfDroppingTheHero() {
        val parsed = CharacterRepository.parseCharacters(effectJson("fire"))
        assertEquals(0, parsed.skipped)
        assertEquals(com.homebrew.tabletopcompanion.model.DamageType.UNSAVEABLE, parsed.heroes.single().effects.single().damageType)
    }
}
