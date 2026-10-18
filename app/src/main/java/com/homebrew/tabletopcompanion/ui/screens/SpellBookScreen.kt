package com.homebrew.tabletopcompanion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homebrew.tabletopcompanion.data.SpellBooksRepository
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.model.CharacterAbility
import com.homebrew.tabletopcompanion.model.SpellBook
import com.homebrew.tabletopcompanion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellBookScreen(
    character: Character
) {
    val context = LocalContext.current
    val magicBibleAbilities = remember(character.abilities) {
        character.abilities.filter { SpellBooksRepository.isMagicOrBibleAbility(it.name) }
    }
    val equippedMagicItems = remember(character.inventory) {
        character.inventory.filter { it.isEquipped && it.hasMagic }
    }
    val totalSpellDamage = remember(equippedMagicItems) {
        equippedMagicItems.sumOf { it.spellDamageModifier }
    }
    val totalSpellHealing = remember(equippedMagicItems) {
        equippedMagicItems.sumOf { it.spellHealingModifier }
    }

    if (magicBibleAbilities.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = TextSecondary
                )
                Text(
                    text = "No Spell Book Unlocked",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Add a Magic or Bible skill (e.g. Magic: Water, Bible of Felis) from the Abilities tab to consult its grimoire and scriptures.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    var selectedIndex by remember { mutableStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, (magicBibleAbilities.size - 1).coerceAtLeast(0))
    val currentAbility = magicBibleAbilities[safeIndex]
    val spellBook = remember(currentAbility.name) {
        SpellBooksRepository.findSpellBookForAbility(currentAbility.name, context)
    }

    var searchQuery by remember { mutableStateOf("") }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
        // Book selector chips if multiple abilities
        if (magicBibleAbilities.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                magicBibleAbilities.forEachIndexed { idx, ability ->
                    val isSelected = idx == safeIndex
                    val label = if (ability.tier != null) "${ability.name} (T${ability.tier})" else ability.name
                    Surface(
                        onClick = { selectedIndex = idx },
                        color = if (isSelected) GoldAccent else DarkSurfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isSelected) GoldAccent else DarkSurfaceVariant)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (ability.name.startsWith("Magic:", ignoreCase = true)) Icons.Default.AutoAwesome else Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) DarkBackground else GoldAccent
                            )
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DarkBackground else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Header Card for Active Book
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = spellBook?.displayName ?: currentAbility.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        spellBook?.deity?.let { deity ->
                            Text(
                                text = deity,
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                color = TextSecondary
                            )
                        }
                    }

                    if (currentAbility.tier != null) {
                        Surface(
                            color = GoldAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Your Level: Tier ${currentAbility.tier}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (equippedMagicItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EQUIPPED SPELL MODIFIERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Text(
                            text = "From ${equippedMagicItems.size} equipped magic ${if (equippedMagicItems.size == 1) "item" else "items"}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = GoldAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "🔮 Dmg: ${if (totalSpellDamage > 0) "+$totalSpellDamage" else "$totalSpellDamage"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalSpellDamage > 0) GoldAccent else if (totalSpellDamage < 0) CrimsonPrimary else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = GreenHp.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, GreenHp.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "💖 Heal: ${if (totalSpellHealing > 0) "+$totalSpellHealing" else "$totalSpellHealing"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalSpellHealing > 0) GreenHp else if (totalSpellHealing < 0) CrimsonPrimary else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search spells or prayers...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldAccent,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (spellBook == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No grimoire file available yet for '${currentAbility.name}'.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else if (spellBook.type == "magic") {
            // Group and render magic spells
            val query = searchQuery.trim().lowercase()
            val filteredSpells = remember(spellBook, query) {
                if (query.isEmpty()) spellBook.spells
                else spellBook.spells.filter {
                    it.name.lowercase().contains(query) ||
                    it.description.lowercase().contains(query) ||
                    it.cost.lowercase().contains(query)
                }
            }

            val currentTier = currentAbility.tier
            val groupedByTier = remember(filteredSpells) {
                filteredSpells.groupBy { it.tier }
            }

            LazyVerticalGrid(
                columns = if (isTablet) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val tiers = (1..5).toList()
                tiers.forEach { tierNum ->
                    val spellsInTier = groupedByTier[tierNum] ?: emptyList()
                    if (spellsInTier.isNotEmpty()) {
                        val isTierUnlocked = currentTier == null || tierNum <= currentTier

                        item(key = "header_tier_$tierNum", span = { GridItemSpan(maxLineSpan) }) {
                            Surface(
                                color = if (isTierUnlocked) DarkSurfaceVariant else DarkSurface,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isTierUnlocked) GoldAccent.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TIER $tierNum SPELLS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.sp,
                                        color = if (isTierUnlocked) GoldAccent else TextSecondary
                                    )
                                    if (!isTierUnlocked) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = CrimsonPrimary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Locked (Requires Tier $tierNum)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CrimsonPrimary
                                            )
                                        }
                                    } else if (currentTier != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GreenHp,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Unlocked",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = GreenHp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(spellsInTier, key = { "spell_${it.tier}_${it.name}" }) { spell ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (isTierUnlocked) 1.0f else 0.65f),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(
                                    1.dp,
                                    if (isTierUnlocked) GoldAccent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = spell.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isTierUnlocked) GoldAccent else TextSecondary,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        if (spell.cost.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = DarkSurfaceVariant,
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    text = spell.cost,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isTierUnlocked) GoldAccent else TextSecondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = spell.description,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Faith Prayers
            val query = searchQuery.trim().lowercase()
            val filteredPrayers = remember(spellBook, query) {
                if (query.isEmpty()) spellBook.prayers
                else spellBook.prayers.filter {
                    it.name.lowercase().contains(query) ||
                    it.quote.lowercase().contains(query) ||
                    it.effect.lowercase().contains(query) ||
                    it.percentage.lowercase().contains(query)
                }
            }

            LazyVerticalGrid(
                columns = if (isTablet) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPrayers, key = { "prayer_${it.percentage}_${it.name}" }) { prayer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = GoldAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = prayer.percentage,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = prayer.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = GoldAccent
                                    )
                                }
                            }

                            if (prayer.quote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${prayer.quote}\"",
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                color = DarkBackground,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = prayer.effect,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
