package com.homebrew.tabletopcompanion.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.homebrew.tabletopcompanion.model.SpellBook
import java.io.InputStreamReader

object SpellBooksRepository {
    private var cachedBooks: Map<String, SpellBook>? = null
    private val BIBLE_OR_FAITH = Regex("\\b(bible|faith)\\b")

    fun getSpellBooks(context: Context): Map<String, SpellBook> {
        cachedBooks?.let { return it }
        return try {
            val inputStream = context.assets.open("spellbooks.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<Map<String, SpellBook>>() {}.type
            val map: Map<String, SpellBook> = Gson().fromJson(reader, type)
            reader.close()
            cachedBooks = map
            map
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    fun isMagicOrBibleAbility(abilityName: String): Boolean {
        val clean = abilityName.trim().lowercase()
        // "bible" and "faith" count only as whole words, so "Faithful Blade" is not magic.
        return clean.startsWith("magic:") || BIBLE_OR_FAITH.containsMatchIn(clean)
    }

    fun findSpellBookForAbility(abilityName: String, context: Context): SpellBook? {
        val books = getSpellBooks(context)
        val clean = abilityName.trim().lowercase()

        // 1. Direct name match
        for ((_, book) in books) {
            if (book.name.trim().lowercase() == clean) return book
        }

        // 2. Starts with / prefix match
        for ((_, book) in books) {
            val bookNameClean = book.name.trim().lowercase()
            if (bookNameClean.isNotEmpty() && clean.length >= 3 && (clean.startsWith(bookNameClean) || bookNameClean.startsWith(clean))) return book
        }

        // 3. Alias match
        for ((_, book) in books) {
            for (alias in book.aliases) {
                val aliasClean = alias.trim().lowercase()
                if (clean == aliasClean || (aliasClean.isNotEmpty() && clean.length >= 3 && (clean.startsWith(aliasClean) || clean.contains(aliasClean) || aliasClean.contains(clean)))) {
                    return book
                }
            }
        }

        // 4. Deity keyword match
        if (clean.contains("felis")) return books["Bible of Felis"]
        if (clean.contains("illumus")) return books["Bible of Illumus"]
        if (clean.contains("zuul")) return books["Bible of Zuul"]

        // 5. General faith / bible fallback to first available faith book
        if (clean == "faith" || clean.startsWith("faith:") || clean == "bible" || clean.startsWith("bible:")) {
            return books["Bible of Felis"] ?: books.values.firstOrNull { it.type == "faith" }
        }

        return null
    }
}
