package com.homebrew.tabletopcompanion

import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.model.EquipmentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterGearTest {
    private fun hero(currentArmor: Int? = null, inventory: List<EquipmentItem> = emptyList()) =
        Character(name = "T", race = "x", currentHp = 60, endurance = 6, currentArmor = currentArmor, inventory = inventory)

    private val plate = EquipmentItem(name = "Plate", isEquipped = true, hasArmor = true, armorSave = 6)
    private val shieldOfEndurance = EquipmentItem(name = "Shield", isEquipped = true, statModifiers = mapOf("endurance" to 3), hasStatModifiers = true)

    @Test fun effectiveMaxHpCountsGearThatChangesEndurance() {
        // The hero list shows this number; the starting HP given in the constructor (60) is not what the Use screen shows.
        val h = hero(inventory = listOf(shieldOfEndurance))
        assertEquals(90, h.getEffectiveMaxHp())
    }

    @Test fun damagedArmorStaysDamagedWhenGearIsRemovedAndPutBack() {
        val damaged = hero(currentArmor = 0, inventory = listOf(plate))
        val without = damaged.withInventory(listOf(plate.copy(isEquipped = false)))
        val again = without.withInventory(listOf(plate))
        assertEquals(0, without.currentArmor)
        assertEquals(0, again.currentArmor) // not healed by the swap
    }

    @Test fun untouchedArmorStaysFullWhenNewArmorIsEquipped() {
        val fresh = hero(currentArmor = null, inventory = emptyList())
        val equipped = fresh.withInventory(listOf(plate))
        assertNull(equipped.currentArmor)
        assertEquals(6, equipped.getEffectiveCurrentArmor())
    }

    @Test fun armorIsCappedToTheNewTotal() {
        val h = hero(currentArmor = 6, inventory = listOf(plate))
        assertEquals(0, h.withInventory(emptyList()).currentArmor)
    }
}
