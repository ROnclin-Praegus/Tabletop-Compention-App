package com.homebrew.tabletopcompanion

import com.homebrew.tabletopcompanion.data.CharacterRepository.Companion.nextBackupSlots
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupSlotsTest {
    @Test fun firstProblemGoesToTheNewestSlot() {
        assertEquals("A" to null, nextBackupSlots(null, null, emptyList(), "A"))
    }

    @Test fun sameTextTwiceIsKeptOnceAndKeepsTheOlderSlot() {
        assertEquals("A" to "Z", nextBackupSlots("A", "Z", emptyList(), "A"))
    }

    @Test fun aNewProblemPushesTheNewestToTheOlderSlot() {
        assertEquals("B" to "A", nextBackupSlots("A", "Z", emptyList(), "B"))
    }

    @Test fun anEarlierVersionsCopyFillsAnEmptyOlderSlot() {
        assertEquals("B" to "old", nextBackupSlots(null, null, listOf("old"), "B"))
        assertEquals("A" to "old", nextBackupSlots("A", null, listOf("old"), "A"))
    }
}
