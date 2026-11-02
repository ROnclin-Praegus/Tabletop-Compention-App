package com.homebrew.tabletopcompanion

import com.homebrew.tabletopcompanion.data.SpellBooksRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpellBooksRepositoryTest {
    @Test fun bibleAndFaithMatchAsWholeWords() {
        assertTrue(SpellBooksRepository.isMagicOrBibleAbility("Bible of Felis"))
        assertTrue(SpellBooksRepository.isMagicOrBibleAbility("Faith: Zuul"))
        assertTrue(SpellBooksRepository.isMagicOrBibleAbility("Magic: Water"))
    }

    @Test fun lookalikeWordsAreNotMagic() {
        assertFalse(SpellBooksRepository.isMagicOrBibleAbility("Faithful Blade"))
        assertFalse(SpellBooksRepository.isMagicOrBibleAbility("Dagger specialization"))
    }
}
