package com.homebrew.tabletopcompanion.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.model.ImportedSkill

object SkillsRepository {
    private var cachedSkills: List<ImportedSkill>? = null

    fun getSkills(context: Context): List<ImportedSkill> {
        cachedSkills?.let { return it }
        return try {
            val jsonString = context.assets.open("skills.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<ImportedSkill>>() {}.type
            val list: List<ImportedSkill> = Gson().fromJson(jsonString, type)
            cachedSkills = list
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getCategories(context: Context): List<String> {
        val skills = getSkills(context)
        return skills.map { it.category }.distinct()
    }

    data class SubSpell(val name: String, val description: String)

    val parentSubSpellsMap: Map<String, List<SubSpell>> = mapOf(
        "Reaction spells" to listOf(
            SubSpell("Reaction Spell: Magic sensing (1MP)", "Detect and trace sources of magic."),
            SubSpell("Reaction Spell: Dispel (1MP)", "Cancel a magical effect (burning, curse, slow, petrification) on a person. Or use 2MP to cast it as an area-of-effect."),
            SubSpell("Reaction Spell: Diffuse (xMP)", "A last-ditch defense spell that can be cast as a reaction. Reduce the damage of an incoming hit by 5+xd6. Where x is the amount of mana input."),
            SubSpell("Reaction Spell: Magical device (1MP)", "Channel magic to use magical machinery.")
        )
    )

    fun getSubSpellsForParent(parentName: String): List<SubSpell> {
        val cleanParent = normalize(parentName)
        return parentSubSpellsMap.entries
            .firstOrNull { normalize(it.key) == cleanParent }
            ?.value ?: emptyList()
    }

    fun isSubSpell(abilityName: String): Boolean {
        val clean = normalize(abilityName)
        return clean.startsWith("reaction spell:") || clean.startsWith("reaction spell -")
    }

    fun getParentNameForSubSpell(abilityName: String): String? {
        val clean = normalize(abilityName)
        if (clean.startsWith("reaction spell:") || clean.startsWith("reaction spell -")) {
            return "Reaction spells"
        }
        return null
    }

    fun isSkillAlreadyAdded(skill: ImportedSkill, character: Character): Boolean {
        val skillNameLower = normalize(skill.name)
        return character.abilities.any { ability ->
            val clean = normalize(ability.name)
            clean == skillNameLower || clean.startsWith("$skillNameLower -") || clean.startsWith("$skillNameLower:")
        }
    }

    private fun normalize(str: String): String =
        str.trim().lowercase().replace(Regex("\\s+"), " ")

    fun isSkillUnlocked(skill: ImportedSkill, character: Character): Pair<Boolean, String?> {
        val charAbilityNames = character.abilities.map { normalize(it.name) }

        // 1. Check required stats
        for ((statName, minVal) in skill.requiredStats) {
            val statVal = when (statName.lowercase()) {
                "strength" -> character.strength
                "intelligence" -> character.intelligence
                "endurance" -> character.endurance
                "spirit" -> character.spirit
                "finesse" -> character.finesse
                "charisma" -> character.charisma
                else -> 0
            }
            if (statVal < minVal) {
                val statLabel = statName.uppercase().take(3)
                return Pair(false, "Requires $statLabel $minVal (Base: $statVal)")
            }
        }

        // 2. Check required skills
        for (req in skill.requiredSkills) {
            val reqLower = normalize(req)
            val hasSkill = charAbilityNames.any { ability ->
                ability == reqLower || ability.startsWith("$reqLower -") || ability.startsWith("$reqLower:")
            }
            if (!hasSkill) {
                return Pair(false, "Requires: $req")
            }
        }

        // 3. Check special requirements
        when (skill.specialReq) {
            "any_ranged_spec" -> {
                val rangedSpecs = listOf("bow specialization", "crossbow specialization", "pistol specialization", "rifle specialization")
                val hasRanged = charAbilityNames.any { ability -> rangedSpecs.any { ability.contains(it) } }
                if (!hasRanged) {
                    return Pair(false, "Requires: Any ranged weapon specialization")
                }
            }
            "any_ranged_expert" -> {
                val rangedExperts = listOf("bow specialization (expert)", "crossbow specialization (expert)", "pistol specialization (expert)", "rifle specialization (expert)")
                val hasRangedExpert = charAbilityNames.any { ability -> rangedExperts.any { ability.contains(it) } }
                if (!hasRangedExpert) {
                    return Pair(false, "Requires: Any ranged weapon (Expert)")
                }
            }
            "bow_or_crossbow_expert" -> {
                val bowCrossExperts = listOf("bow specialization (expert)", "crossbow specialization (expert)")
                val hasBowCrossExpert = charAbilityNames.any { ability -> bowCrossExperts.any { ability.contains(it) } }
                if (!hasBowCrossExpert) {
                    return Pair(false, "Requires: Bow or Crossbow (Expert)")
                }
            }
            "any_weapon_expert" -> {
                val hasAnyExpert = charAbilityNames.any { it.contains("specialization (expert)") || it.contains("(expert)") }
                if (!hasAnyExpert) {
                    return Pair(false, "Requires: Any weapon specialization (Expert)")
                }
            }
            "melee_weapon_expert" -> {
                val meleeSpecs = listOf(
                    "dagger", "one-handed swords", "one-handed blunt", "one-handed axe",
                    "two-handed swords", "two-handed axe", "two-handed blunt", "polearm", "exotic"
                )
                val hasMeleeExpert = charAbilityNames.any { ability ->
                    ability.contains("(expert)") && meleeSpecs.any { ability.contains(it) }
                }
                if (!hasMeleeExpert) {
                    return Pair(false, "Requires: Any melee weapon specialization (Expert)")
                }
            }
            "any_bible" -> {
                val hasBible = charAbilityNames.any { it.startsWith("bible of") || it.contains("bible") }
                if (!hasBible) {
                    return Pair(false, "Requires: A Bible (Faith)")
                }
            }
        }

        return Pair(true, null)
    }
}
