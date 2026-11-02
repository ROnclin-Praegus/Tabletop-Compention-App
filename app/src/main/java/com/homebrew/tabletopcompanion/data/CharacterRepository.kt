package com.homebrew.tabletopcompanion.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.model.DamageType
import com.homebrew.tabletopcompanion.model.EffectType
import com.homebrew.tabletopcompanion.model.EquipmentItem
import java.util.UUID

class CharacterRepository(context: Context) {
    private val prefs = context.getSharedPreferences("rpg_characters_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_CHARACTERS = "characters_json"
    private val KEY_CHARACTERS_BACKUP = "characters_json_corrupt_backup"
    private val KEY_CHARACTERS_BACKUP_OLDER = "characters_json_older_unreadable"
    private val KEY_PHOTO_LAST_FAILED = "photo_last_failed"
    private val KEY_PREMIUM = "is_premium"
    private val KEY_PHOTO_API_URL = "photo_api_url"
    private val KEY_PHOTO_API_KEY = "photo_api_key"
    private val KEY_PHOTO_INTERVAL_MINUTES = "photo_interval_minutes"

    fun isPremium(): Boolean {
        return prefs.getBoolean(KEY_PREMIUM, false)
    }

    fun setPremium(premium: Boolean) {
        prefs.edit().putBoolean(KEY_PREMIUM, premium).apply()
    }

    fun getPhotoApiUrl(): String {
        return prefs.getString(KEY_PHOTO_API_URL, "") ?: ""
    }

    fun setPhotoApiUrl(url: String) {
        prefs.edit().putString(KEY_PHOTO_API_URL, url).apply()
    }

    fun getPhotoApiKey(): String {
        return prefs.getString(KEY_PHOTO_API_KEY, "") ?: ""
    }

    fun setPhotoApiKey(key: String) {
        prefs.edit().putString(KEY_PHOTO_API_KEY, key).apply()
    }

    fun getPhotoIntervalMinutes(): Int {
        return prefs.getInt(KEY_PHOTO_INTERVAL_MINUTES, 15)
    }

    fun setPhotoIntervalMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_PHOTO_INTERVAL_MINUTES, minutes).apply()
    }

    // The list is read and parsed once, then kept here. Every write goes through saveCharacters().
    private var cache: List<Character>? = null

    /** True when the last background photo fetch failed, so a long outage gives one notification, not one per interval. */
    fun getPhotoLastFailed(): Boolean = prefs.getBoolean(KEY_PHOTO_LAST_FAILED, false)

    fun setPhotoLastFailed(failed: Boolean) {
        prefs.edit().putBoolean(KEY_PHOTO_LAST_FAILED, failed).apply()
    }

    fun getCharacters(): List<Character> {
        cache?.let { return it }
        val json = prefs.getString(KEY_CHARACTERS, null)
        if (json.isNullOrEmpty()) {
            val defaults = getPresetCharacters()
            saveCharacters(defaults)
            return defaults
        }
        val parsed = try {
            parseCharacters(json, gson)
        } catch (e: Exception) {
            // Keep a copy of what could not be read and show the presets. The next save replaces the
            // saved list, so the copy is the only record: the user can export it (see unreadableBackup()).
            keepBackup(json, -1)
            e.printStackTrace()
            return getPresetCharacters().also { cache = it }
        }
        if (parsed.skipped > 0) keepBackup(json, parsed.skipped) // some heroes were unreadable: keep the original text
        cache = parsed.heroes
        return parsed.heroes
    }

    /** Number of heroes that could not be read at the last load (the whole list counts as -1). 0 = all fine. */
    var unreadableNotice: Int = 0
        private set

    /** Returns the notice once (then clears it), so the UI can tell the user a single time. */
    fun takeUnreadableNotice(): Int = unreadableNotice.also { unreadableNotice = 0 }

    /**
     * Keeps a copy of the unreadable text in two slots: the newest problem, and the one before it.
     * The same text twice is stored once. Older copies (from the earlier per-hash keys) are removed.
     */
    private fun keepBackup(json: String, unreadable: Int) {
        unreadableNotice = unreadable
        val edit = prefs.edit()
        val legacyKeys = prefs.all.keys.filter { it.startsWith("${KEY_CHARACTERS_BACKUP}_") }
        val (newest, older) = nextBackupSlots(
            newest = prefs.getString(KEY_CHARACTERS_BACKUP, null),
            older = prefs.getString(KEY_CHARACTERS_BACKUP_OLDER, null),
            legacy = legacyKeys.mapNotNull { prefs.getString(it, null) },
            incoming = json
        )
        legacyKeys.forEach { edit.remove(it) }
        edit.putString(KEY_CHARACTERS_BACKUP, newest)
        older?.let { edit.putString(KEY_CHARACTERS_BACKUP_OLDER, it) }
        edit.apply()
    }

    /** The newest saved text that could not be read, or null. Lets the user export it before it is lost. */
    fun unreadableBackup(): String? = prefs.getString(KEY_CHARACTERS_BACKUP, null)

    /** Call after the user exported the backup, so the button goes away. */
    fun clearUnreadableBackup() {
        prefs.edit().remove(KEY_CHARACTERS_BACKUP).remove(KEY_CHARACTERS_BACKUP_OLDER).apply()
    }

    fun saveCharacters(characters: List<Character>) {
        cache = characters
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
                currentHp = 40,
                boostHp = 10,
                currentMp = 3,
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
                currentHp = 60,
                boostHp = 0,
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

    companion object {
        /**
         * Decides what the two backup slots hold after [incoming] (unreadable text) was found.
         * The same text as the newest slot is not stored twice; otherwise the newest moves to the older slot.
         * When no older text exists, a copy from an earlier version ([legacy], per-hash keys) is used instead.
         * Returns (newest, older).
         */
        internal fun nextBackupSlots(newest: String?, older: String?, legacy: List<String>, incoming: String): Pair<String, String?> {
            val olderOrLegacy = older ?: legacy.firstOrNull()
            return if (newest != null && newest != incoming) incoming to newest else incoming to olderOrLegacy
        }

        class Parsed(val heroes: List<Character>, val skipped: Int)

        /**
         * Reads the saved hero list. One broken hero is skipped (and counted) instead of hiding all the others.
         * Throws only if the whole text is not a hero list.
         */
        fun parseCharacters(json: String, gson: Gson = Gson()): Parsed {
            val type = object : TypeToken<List<Character?>>() {}.type
            val raw = gson.fromJson<List<Character?>>(json, type) ?: emptyList()
            val heroes = raw.mapNotNull { hero ->
                try { hero?.let { sanitizeCharacter(it) } } catch (e: Exception) { null }
            }
            return Parsed(heroes, raw.size - heroes.size)
        }

        // Gson leaves fields null when an old save lacks them, even for non-null Kotlin types.
        @Suppress("USELESS_ELVIS", "SENSELESS_COMPARISON", "UNNECESSARY_SAFE_CALL")
        internal fun sanitizeCharacter(c: Character): Character {
            val cleanInventory = (c.inventory ?: emptyList()).filterNotNull().map { item ->
                item.copy(
                    id = item.id ?: UUID.randomUUID().toString(),
                    name = item.name ?: "",
                    category = item.category ?: "Gear",
                    attackDamage = item.attackDamage ?: "",
                    specialEffectsText = item.specialEffectsText ?: "",
                    onHandSpecialEffectsText = item.onHandSpecialEffectsText ?: "",
                    customProperties = item.customProperties ?: emptyMap(),
                    statModifiers = item.statModifiers ?: emptyMap()
                )
            }
            val cleanEffects = (c.effects ?: emptyList()).filterNotNull().map { eff ->
                eff.copy(
id = eff.id ?: UUID.randomUUID().toString(),
                    name = eff.name ?: "",
                    diceFormula = eff.diceFormula ?: "",
                    changeMode = eff.changeMode ?: "DMG",
                    // Gson gives null for names it does not know; copy() would throw on a null enum.
                    effectType = eff.effectType ?: EffectType.STAT_MODIFIER,
                    damageType = eff.damageType ?: DamageType.UNSAVEABLE,
                    statModifiers = eff.statModifiers ?: emptyMap(),
                    isEnabled = eff.isEnabled ?: true
                )
            }
            val clean = c.copy(
                id = c.id ?: UUID.randomUUID().toString(), // a hand-written file may have no id
                name = c.name ?: "",
                race = c.race ?: "",
                inventory = cleanInventory,
                effects = cleanEffects,
                abilities = (c.abilities ?: emptyList()).filterNotNull().map {
                    it.copy(id = it.id ?: UUID.randomUUID().toString(), name = it.name ?: "", description = it.description ?: "")
                },
                notes = (c.notes ?: emptyList()).filterNotNull().map {
                    it.copy(id = it.id ?: UUID.randomUUID().toString(), title = it.title ?: "", tag = it.tag ?: "", content = it.content ?: "")
                },
                // A picture is a file on this phone or inline image data. A web address would be fetched on open.
                imageUri = c.imageUri?.takeIf { it.startsWith("content://") || it.startsWith("file://") || it.startsWith("data:image/") }
            )
            // The real maximums come from Endurance and Intelligence. Old saves and imports can hold
            // a current value above them (saves from older versions also stored a maxHp/maxMp, now ignored). A max of 0 is left alone.
            val maxHp = clean.getEffectiveMaxHp()
            val maxMp = clean.getEffectiveMaxMp()
            return clean.copy(
                currentHp = if (maxHp > 0) clean.currentHp.coerceAtMost(maxHp) else clean.currentHp,
                currentMp = if (maxMp > 0) clean.currentMp.coerceAtMost(maxMp) else clean.currentMp
            )
        }
    }
}
