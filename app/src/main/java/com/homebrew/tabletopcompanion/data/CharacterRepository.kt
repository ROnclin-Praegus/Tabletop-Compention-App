package com.homebrew.tabletopcompanion.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.model.EquipmentItem

class CharacterRepository(context: Context) {
    private val prefs = context.getSharedPreferences("rpg_characters_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_CHARACTERS = "characters_json"
    private val KEY_PREMIUM = "is_premium"

    fun isPremium(): Boolean {
        return prefs.getBoolean(KEY_PREMIUM, false)
    }

    fun setPremium(premium: Boolean) {
        prefs.edit().putBoolean(KEY_PREMIUM, premium).apply()
    }

    fun getCharacters(): List<Character> {
        val json = prefs.getString(KEY_CHARACTERS, null)
        if (json.isNullOrEmpty()) {
            val defaults = getPresetCharacters()
            saveCharacters(defaults)
            return defaults
        }
        return try {
            val type = object : TypeToken<List<Character>>() {}.type
            gson.fromJson<List<Character>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            getPresetCharacters()
        }
    }

    fun saveCharacters(characters: List<Character>) {
        val json = gson.toJson(characters)
        prefs.edit().putString(KEY_CHARACTERS, json).apply()
    }

    fun addCharacter(character: Character) {
        val list = getCharacters().toMutableList()
        list.add(0, character)
        saveCharacters(list)
    }

    fun updateCharacter(updated: Character) {
        val list = getCharacters().toMutableList()
        val index = list.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            list[index] = updated
            saveCharacters(list)
        }
    }

    fun deleteCharacter(characterId: String) {
        val list = getCharacters().filter { it.id != characterId }
        saveCharacters(list)
    }

    private fun getPresetCharacters(): List<Character> {
        return listOf(
            Character(
                name = "Ignis Firebrand",
                race = "Dragonkin",
                maxHp = 50,
                currentHp = 50,
                boostHp = 10,
                maxMp = 40,
                currentMp = 40,
                gold = 150,
                strength = 5,
                intelligence = 3,
                endurance = 4,
                spirit = 2,
                finesse = 1,
                charisma = 3,
                inventory = listOf(
                    EquipmentItem(
                        name = "Dragon Cuirass",
                        category = "Armor",
                        isEquipped = true,
                        armorSave = 10,
                        wardSave = 5
                    ),
                    EquipmentItem(
                        name = "Flame Blade",
                        category = "Weapon",
                        isEquipped = true,
                        armorSave = 0,
                        wardSave = 0
                    )
                )
            ),
            Character(
                name = "Gorgar Steel-Shield",
                race = "Barbarian",
                maxHp = 65,
                currentHp = 65,
                boostHp = 0,
                maxMp = 0,
                strength = 8,
                intelligence = 0,
                endurance = 6,
                spirit = 1,
                finesse = 2,
                charisma = 1,
                inventory = listOf(
                    EquipmentItem(
                        name = "Tower Shield of Aegis",
                        category = "Shield",
                        isEquipped = true,
                        armorSave = 8,
                        wardSave = 3
                    )
                )
            )
        )
    }
}
