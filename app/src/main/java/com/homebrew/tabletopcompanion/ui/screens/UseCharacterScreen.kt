package com.homebrew.tabletopcompanion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.homebrew.tabletopcompanion.model.tickRound
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.model.CharacterAbility
import com.homebrew.tabletopcompanion.model.CharacterEffect
import com.homebrew.tabletopcompanion.model.CharacterNote
import com.homebrew.tabletopcompanion.model.Vitals
import com.homebrew.tabletopcompanion.model.absorbDamage
import com.homebrew.tabletopcompanion.model.DamageType
import com.homebrew.tabletopcompanion.model.EffectType
import com.homebrew.tabletopcompanion.model.EquipmentItem
import com.homebrew.tabletopcompanion.data.SkillsRepository
import com.homebrew.tabletopcompanion.ui.theme.*

enum class AdjustmentType {
    DAMAGE_PROMPT,
    HEAL,
    BOOST,
    SPEND_MP,
    RECOVER_MP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UseCharacterScreen(
    character: Character,
    onBack: () -> Unit,
    onCharacterUpdated: (Character) -> Unit
) {
    var activeCharacter by remember { mutableStateOf(character) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Active Tab state: 0 = Vitals, 1 = Abilities, 2 = Equipment, 3 = Inventory, 4 = Notes
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog states
    var pendingAdjustmentType by remember { mutableStateOf<AdjustmentType?>(null) }
    var showAddEquipmentDialog by remember { mutableStateOf(false) }
    var collapsedEquippedCategories by remember {
        mutableStateOf(character.inventory.filter { it.isEquipped }.map { it.category }.toSet())
    }
    var collapsedUnequippedCategories by remember {
        mutableStateOf(character.inventory.filter { !it.isEquipped }.map { it.category }.toSet())
    }
    var showEditCharacterDialog by remember { mutableStateOf(false) }
    var showAddEffectDialog by remember { mutableStateOf(false) }
    var showAddAbilityDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showArchivedNotes by remember { mutableStateOf(false) }
    var showGoldDialog by remember { mutableStateOf(false) }
    var showLevelUpDialog by remember { mutableStateOf(false) }
    var showLevelUpAnimation by remember { mutableStateOf(false) }
    var previousLevelForAnimation by remember { mutableIntStateOf(activeCharacter.level) }
    var itemToEdit by remember { mutableStateOf<EquipmentItem?>(null) }
    var abilityToEdit by remember { mutableStateOf<CharacterAbility?>(null) }
    var noteToEdit by remember { mutableStateOf<CharacterNote?>(null) }
    var effectToEdit by remember { mutableStateOf<CharacterEffect?>(null) }
    var collapsedParentAbilityNames by remember { mutableStateOf(setOf<String>()) }
    var showSelectOnHandDialog by remember { mutableStateOf(false) }
    var selectOnHandTargetIsOffHand by remember { mutableStateOf(false) }

    var recentChangeArmor by remember { mutableStateOf<Int?>(null) }
    var recentChangeWard by remember { mutableStateOf<Int?>(null) }
    var recentChangeHp by remember { mutableStateOf<Int?>(null) }
    var recentDamageTrigger by remember { mutableLongStateOf(0L) }

    var activeExpiredNotification by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeExpiredNotification) {
        if (activeExpiredNotification != null) {
            kotlinx.coroutines.delay(5000)
            activeExpiredNotification = null
        }
    }

    val totalArmor = activeCharacter.getTotalArmor()
    val totalWard = activeCharacter.getTotalWard()

    val currentArmor = activeCharacter.getEffectiveCurrentArmor()
    val currentWard = activeCharacter.getEffectiveCurrentWard()

    val effectiveMaxHp = activeCharacter.getEffectiveMaxHp()
    val effectiveMaxMp = activeCharacter.getEffectiveMaxMp()
    val effectiveSpirit = activeCharacter.getEffectiveSpirit()

    var showDiceRollPromptDialog by remember { mutableStateOf(false) }

    val activeDiceEffects = remember(activeCharacter.effects) {
        activeCharacter.effects.filter {
            it.enabled && it.isDiceRoll && (it.effectType == EffectType.HP_CHANGE_PER_ROUND || it.effectType == EffectType.MP_CHANGE_PER_ROUND)
        }
    }

    fun executeRoundTick(diceRollResults: Map<String, Int> = emptyMap()) {
        // The rules live in tickRound() (model/RoundTick.kt, unit tested). This only shows the result.
        val result = tickRound(activeCharacter, diceRollResults)
        val updated = result.hero

        if (result.expired.isNotEmpty()) {
            activeExpiredNotification = result.expired.joinToString("\n")
        }

        val changeArmor = (updated.getEffectiveCurrentArmor() + updated.boostArmor) - (totalArmor + activeCharacter.boostArmor)
        val changeWard = (updated.getEffectiveCurrentWard() + updated.boostWard) - (totalWard + activeCharacter.boostWard)
        val changeHp = (updated.currentHp + updated.boostHp) - (activeCharacter.currentHp + activeCharacter.boostHp)

        if (changeArmor != 0 || changeWard != 0 || changeHp != 0) {
            recentChangeArmor = if (changeArmor != 0) changeArmor else null
            recentChangeWard = if (changeWard != 0) changeWard else null
            recentChangeHp = if (changeHp != 0) changeHp else null
            recentDamageTrigger = System.currentTimeMillis()
        }

        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun triggerNextRound() {
        if (activeDiceEffects.isNotEmpty()) {
            showDiceRollPromptDialog = true
        } else {
            executeRoundTick()
        }
    }


    fun applyDamageWithType(amount: Int, type: DamageType) {
        if (amount <= 0) return
        val after = absorbDamage(
            Vitals(
                hp = activeCharacter.currentHp,
                boostHp = activeCharacter.boostHp,
                armor = currentArmor,
                ward = currentWard,
                boostArmor = activeCharacter.boostArmor,
                boostWard = activeCharacter.boostWard
            ),
            amount,
            type
        )
        val newCurrentHp = after.hp
        val newBoostHp = after.boostHp
        val newArmor = after.armor
        val newWard = after.ward
        val newBoostArmor = after.boostArmor
        val newBoostWard = after.boostWard

        val changeArmor = (newArmor + newBoostArmor) - (currentArmor + activeCharacter.boostArmor)
        val changeWard = (newWard + newBoostWard) - (currentWard + activeCharacter.boostWard)
        val changeHp = (newCurrentHp + newBoostHp) - (activeCharacter.currentHp + activeCharacter.boostHp)

        if (changeArmor != 0 || changeWard != 0 || changeHp != 0) {
            recentChangeArmor = if (changeArmor != 0) changeArmor else null
            recentChangeWard = if (changeWard != 0) changeWard else null
            recentChangeHp = if (changeHp != 0) changeHp else null
            recentDamageTrigger = System.currentTimeMillis()
        }

        val updated = activeCharacter.copy(
            currentHp = newCurrentHp,
            boostHp = newBoostHp,
            boostArmor = newBoostArmor,
            boostWard = newBoostWard,
            currentArmor = newArmor,
            currentWard = newWard
        )
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun applyHeal(target: String, amount: Int) {
        if (amount <= 0) return
        var updated = activeCharacter
        var changeArmor = 0
        var changeWard = 0
        var changeHp = 0
        when (target) {
            "HP" -> {
                val maxHp = activeCharacter.getEffectiveMaxHp()
                val newCurrentHp = (activeCharacter.currentHp + amount).coerceAtMost(maxHp)
                changeHp = newCurrentHp - activeCharacter.currentHp
                updated = updated.copy(currentHp = newCurrentHp)
            }
            "Armor" -> {
                val maxArmor = activeCharacter.getTotalArmor()
                val newArmor = (activeCharacter.getEffectiveCurrentArmor() + amount).coerceAtMost(maxArmor)
                changeArmor = newArmor - activeCharacter.getEffectiveCurrentArmor()
                updated = updated.copy(currentArmor = newArmor)
            }
            "Ward Save" -> {
                val maxWard = activeCharacter.getTotalWard()
                val newWard = (activeCharacter.getEffectiveCurrentWard() + amount).coerceAtMost(maxWard)
                changeWard = newWard - activeCharacter.getEffectiveCurrentWard()
                updated = updated.copy(currentWard = newWard)
            }
        }
        if (changeArmor != 0 || changeWard != 0 || changeHp != 0) {
            recentChangeArmor = if (changeArmor != 0) changeArmor else null
            recentChangeWard = if (changeWard != 0) changeWard else null
            recentChangeHp = if (changeHp != 0) changeHp else null
            recentDamageTrigger = System.currentTimeMillis()
        }
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun applyBoost(target: String, amount: Int, turns: Int = 0, resetOnNextRound: Boolean = true) {
        if (amount < 0) return
        var updated = activeCharacter
        when (target) {
            "HP" -> {
                updated = updated.copy(
                    boostHp = amount, maxBoostHp = amount,
                    boostHpTurns = turns, boostHpResetOnNextRound = resetOnNextRound
                )
            }
            "Armor" -> {
                updated = updated.copy(
                    boostArmor = amount, maxBoostArmor = amount,
                    boostArmorTurns = turns, boostArmorResetOnNextRound = resetOnNextRound
                )
            }
            "Ward Save" -> {
                updated = updated.copy(
                    boostWard = amount, maxBoostWard = amount,
                    boostWardTurns = turns, boostWardResetOnNextRound = resetOnNextRound
                )
            }
        }
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun applySpendMp(amount: Int) {
        if (amount <= 0) return
        val newMp = (activeCharacter.currentMp - amount).coerceAtLeast(0)
        val updated = activeCharacter.copy(currentMp = newMp)
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun applyRecoverMp(amount: Int) {
        if (amount <= 0) return
        val maxMp = activeCharacter.getEffectiveMaxMp()
        val newMp = (activeCharacter.currentMp + amount).coerceAtMost(maxMp)
        val updated = activeCharacter.copy(currentMp = newMp)
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun applyCharacterUpdateWithVitalsDelta(getUpdatedChar: (Character) -> Character) {
        val finalUpdated = activeCharacter.withVitalsAdjustedTo(getUpdatedChar(activeCharacter))
        activeCharacter = finalUpdated
        onCharacterUpdated(finalUpdated)
    }

    fun toggleEquipItem(itemId: String) {
        applyCharacterUpdateWithVitalsDelta { char ->
            val updatedList = char.inventory.map { item ->
                if (item.id == itemId) item.copy(isEquipped = !item.isEquipped) else item
            }
            char.withInventory(updatedList)
        }
    }

    fun deleteItem(itemId: String) {
        applyCharacterUpdateWithVitalsDelta { char ->
            val updatedList = char.inventory.filter { it.id != itemId }
            char.withInventory(updatedList)
        }
    }

    fun addItem(newItem: EquipmentItem) {
        applyCharacterUpdateWithVitalsDelta { char ->
            val updatedList = listOf(newItem) + char.inventory
            char.withInventory(updatedList)
        }
    }

    fun updateItem(updatedItem: EquipmentItem) {
        applyCharacterUpdateWithVitalsDelta { char ->
            val updatedList = char.inventory.map { item ->
                if (item.id == updatedItem.id) updatedItem else item
            }
            char.withInventory(updatedList)
        }
    }

    fun addEffect(effect: CharacterEffect) {
        applyCharacterUpdateWithVitalsDelta { char ->
            val updatedList = char.effects + effect
            char.copy(effects = updatedList)
        }
    }

    fun removeEffect(effectId: String) {
        applyCharacterUpdateWithVitalsDelta { char ->
            val updatedList = char.effects.filter { it.id != effectId }
            char.copy(effects = updatedList)
        }
    }

    fun toggleEffectEnabled(effectId: String) {
        applyCharacterUpdateWithVitalsDelta { char ->
            val updatedList = char.effects.map { eff ->
                if (eff.id == effectId) {
                    eff.copy(isEnabled = !eff.enabled)
                } else eff
            }
            char.copy(effects = updatedList)
        }
    }

    fun addAbility(ability: CharacterAbility) {
        val subSpells = SkillsRepository.getSubSpellsForParent(ability.name)
        val toAdd = mutableListOf(ability)
        for (sub in subSpells) {
            val alreadyPresent = activeCharacter.abilities.any { 
                it.name.trim().lowercase() == sub.name.trim().lowercase() 
            }
            if (!alreadyPresent) {
                toAdd.add(CharacterAbility(name = sub.name, description = sub.description))
            }
        }
        val updatedList = activeCharacter.abilities + toAdd
        val updated = activeCharacter.copy(abilities = updatedList)
        activeCharacter = updated
        onCharacterUpdated(updated)
        if (toAdd.size > 1) {
            android.widget.Toast.makeText(
                context,
                "Added '${ability.name}' and ${toAdd.size - 1} sub-spells",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun updateAbility(updatedAbility: CharacterAbility) {
        val updatedList = activeCharacter.abilities.map { a ->
            if (a.id == updatedAbility.id) updatedAbility else a
        }
        val updated = activeCharacter.copy(abilities = updatedList)
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun deleteAbility(abilityId: String) {
        val updatedList = activeCharacter.abilities.filter { it.id != abilityId }
        val updated = activeCharacter.copy(abilities = updatedList)
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun addNote(note: CharacterNote) {
        val updatedList = activeCharacter.notes + note
        val updated = activeCharacter.copy(notes = updatedList)
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun updateNote(updatedNote: CharacterNote) {
        val updatedList = activeCharacter.notes.map { n ->
            if (n.id == updatedNote.id) updatedNote else n
        }
        val updated = activeCharacter.copy(notes = updatedList)
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun deleteNote(noteId: String) {
        val updatedList = activeCharacter.notes.filter { it.id != noteId }
        val updated = activeCharacter.copy(notes = updatedList)
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun toggleArchiveNote(noteId: String) {
        val updatedList = activeCharacter.notes.map { n ->
            if (n.id == noteId) n.copy(isArchived = !n.isArchived) else n
        }
        val updated = activeCharacter.copy(notes = updatedList)
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun moveAbility(abilityId: String, direction: Int) {
        val list = activeCharacter.abilities.toMutableList()
        val index = list.indexOfFirst { it.id == abilityId }
        if (index >= 0) {
            if (direction < 0 && index > 0) {
                val temp = list[index]
                list[index] = list[index - 1]
                list[index - 1] = temp
            } else if (direction > 0 && index < list.size - 1) {
                val temp = list[index]
                list[index] = list[index + 1]
                list[index + 1] = temp
            }
            val updated = activeCharacter.copy(abilities = list)
            activeCharacter = updated
            onCharacterUpdated(updated)
        }
    }

    fun moveNote(noteId: String, direction: Int) {
        val list = activeCharacter.notes.toMutableList()
        val index = list.indexOfFirst { it.id == noteId }
        if (index >= 0) {
            if (direction < 0 && index > 0) {
                val temp = list[index]
                list[index] = list[index - 1]
                list[index - 1] = temp
            } else if (direction > 0 && index < list.size - 1) {
                val temp = list[index]
                list[index] = list[index + 1]
                list[index + 1] = temp
            }
            val updated = activeCharacter.copy(notes = list)
            activeCharacter = updated
            onCharacterUpdated(updated)
        }
    }

    fun moveInventoryItem(itemId: String, direction: Int, isEquippedOnly: Boolean) {
        val list = activeCharacter.inventory.toMutableList()
        val itemCategory = list.find { it.id == itemId }?.category ?: return
        val indices = list.mapIndexedNotNull { i, item -> if (item.isEquipped == isEquippedOnly && item.category == itemCategory) i else null }
        val targetIdxInIndices = indices.indexOfFirst { list[it].id == itemId }

        if (targetIdxInIndices >= 0) {
            if (direction < 0 && targetIdxInIndices > 0) {
                val idx1 = indices[targetIdxInIndices]
                val idx2 = indices[targetIdxInIndices - 1]
                val temp = list[idx1]
                list[idx1] = list[idx2]
                list[idx2] = temp
            } else if (direction > 0 && targetIdxInIndices < indices.size - 1) {
                val idx1 = indices[targetIdxInIndices]
                val idx2 = indices[targetIdxInIndices + 1]
                val temp = list[idx1]
                list[idx1] = list[idx2]
                list[idx2] = temp
            }
            applyCharacterUpdateWithVitalsDelta { char ->
                char.copy(inventory = list)
            }
        }
    }

    fun moveInventoryCategory(category: String, direction: Int, isEquippedOnly: Boolean) {
        val list = activeCharacter.inventory.toMutableList()
        val categories = list.filter { it.isEquipped == isEquippedOnly }.map { it.category }.distinct()
        val catIndex = categories.indexOf(category)
        
        if (catIndex >= 0) {
            val newOrder = categories.toMutableList()
            if (direction < 0 && catIndex > 0) {
                val other = newOrder[catIndex - 1]
                newOrder[catIndex - 1] = category
                newOrder[catIndex] = other
            } else if (direction > 0 && catIndex < categories.size - 1) {
                val other = newOrder[catIndex + 1]
                newOrder[catIndex + 1] = category
                newOrder[catIndex] = other
            } else {
                return
            }
            
            val oldItems = list.filter { it.isEquipped == isEquippedOnly }
            val reorderedItems = mutableListOf<EquipmentItem>()
            for (cat in newOrder) {
                reorderedItems.addAll(oldItems.filter { it.category == cat })
            }
            
            var reorderIdx = 0
            for (i in list.indices) {
                if (list[i].isEquipped == isEquippedOnly) {
                    list[i] = reorderedItems[reorderIdx++]
                }
            }
            applyCharacterUpdateWithVitalsDelta { char ->
                char.copy(inventory = list)
            }
        }
    }

    val equippedItems = activeCharacter.inventory.filter { it.isEquipped }
    val unequippedItems = activeCharacter.inventory.filter { !it.isEquipped }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showEditCharacterDialog = true }
                        ) {
                            if (!activeCharacter.imageUri.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, GoldAccent, CircleShape)
                                ) {
                                    AsyncImage(
                                        model = activeCharacter.imageUri,
                                        contentDescription = activeCharacter.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column {
                                Text(
                                    text = activeCharacter.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = activeCharacter.race,
                                    fontSize = 12.sp,
                                    color = GoldAccent
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    actions = {
                        // Level Button
                        Button(
                            onClick = { showLevelUpDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent.copy(alpha = 0.2f),
                                contentColor = GoldAccent
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("LVL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${activeCharacter.level}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GoldAccent
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Gold Button (between name and edit button)
                        Button(
                            onClick = { showGoldDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent.copy(alpha = 0.2f),
                                contentColor = GoldAccent
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("🪙", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${activeCharacter.gold}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = GoldAccent
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Removed edit button as title is now clickable
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )

                val hasMagicBible = remember(activeCharacter.abilities) {
                    activeCharacter.abilities.any { a ->
                        val n = a.name.trim().lowercase()
                        n.startsWith("magic:") || n.startsWith("bible of") || n.contains("bible")
                    }
                }
                val tabList = remember(hasMagicBible) {
                    if (hasMagicBible) listOf(0, 1, 5, 2, 3, 4) else listOf(0, 1, 2, 3, 4)
                }
                val selectedTabIndex = tabList.indexOf(selectedTab).let { if (it >= 0) it else 0 }

                LaunchedEffect(hasMagicBible) {
                    if (!hasMagicBible && selectedTab == 5) {
                        selectedTab = 1
                    }
                }

                // Dynamic Navigation Tabs (Vitals, Abilities, [Spell Book], Equipment, Inventory, Notes)
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = DarkSurface,
                    contentColor = GoldAccent
                ) {
                    tabList.forEach { tabId ->
                        when (tabId) {
                            0 -> Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Shield, contentDescription = "Vitals", modifier = Modifier.size(22.dp)) }
                            )
                            1 -> Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Abilities", modifier = Modifier.size(22.dp)) }
                            )
                            5 -> Tab(
                                selected = selectedTab == 5,
                                onClick = { selectedTab = 5 },
                                icon = { Icon(Icons.Default.MenuBook, contentDescription = "Spell Book", modifier = Modifier.size(22.dp)) }
                            )
                            2 -> Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Equipment", modifier = Modifier.size(22.dp)) }
                            )
                            3 -> Tab(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Backpack, contentDescription = "Inventory", modifier = Modifier.size(22.dp)) }
                            )
                            4 -> Tab(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                icon = { Icon(Icons.Default.EditNote, contentDescription = "Notes", modifier = Modifier.size(24.dp)) }
                            )
                        }
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isTablet = maxWidth >= 600.dp

            when (selectedTab) {
                0 -> {
                    // TAB 0: VITALS & STATS
                    val vitalsCardContent: @Composable () -> Unit = {
                        // VITAL METRICS CARD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DEFENSES & VITAL METRICS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GoldAccent
                                    )

                                    // NEXT ROUND BUTTON
                                    Button(
                                        onClick = { triggerNextRound() },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent.copy(alpha = 0.2f), contentColor = GoldAccent),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("NEXT ROUND", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                // 1. ARMOR BAR (Rendered ABOVE HP bar if equipped totalArmor > 0 or boosted)
                                val effTotalArmorForUI = totalArmor + activeCharacter.maxBoostArmor
                                if (effTotalArmorForUI > 0) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Shield, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Armor", fontWeight = FontWeight.Bold, color = GoldAccent)
                                            }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "$currentArmor / $totalArmor",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = GoldAccent
                                            )
                                            if (activeCharacter.boostArmor > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = GoldAccent.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                                                    modifier = Modifier.pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onDoubleTap = {
                                                                applyBoost("Armor", 0)
                                                                android.widget.Toast.makeText(context, "Armor Boost Removed", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                ) {
                                                    val turnsTag = if (activeCharacter.boostArmorTurns > 0) " (${activeCharacter.boostArmorTurns}t)" else if (activeCharacter.boostArmorResetOnNextRound) " (R)" else ""
                                                    Text(
                                                        text = "+${activeCharacter.boostArmor} Boost$turnsTag",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GoldAccent,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val armorProgress by animateFloatAsState(targetValue = (currentArmor.toFloat() / totalArmor.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f), animationSpec = tween(500))
                                        Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.fillMaxWidth()) {
                                            LinearProgressIndicator(
                                                progress = { armorProgress },
                                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                                color = GoldAccent,
                                                trackColor = DarkSurfaceVariant
                                            )
                                            key(recentDamageTrigger) {
                                                if (recentChangeArmor != null) {
                                                    val isRecent = (System.currentTimeMillis() - recentDamageTrigger) < 2000L
                                                    var visible by remember { mutableStateOf(isRecent) }
                                                    if (isRecent) {
                                                        LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); visible = false }
                                                    }
                                                    androidx.compose.animation.AnimatedVisibility(
                                                        visible = visible,
                                                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                                                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                                                        modifier = Modifier.offset(y = (-10).dp).padding(end = 8.dp)
                                                    ) {
                                                        val amt = recentChangeArmor!!
                                                        val txt = if (amt > 0) "+$amt" else "$amt"
                                                        val col = if (amt > 0) Color.Green else Color.Red
                                                        Text(txt, color = col, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Divider(color = DarkSurfaceVariant, thickness = 1.dp)
                                }

                                // 2. WARD SAVE BAR (Rendered ABOVE HP bar if equipped totalWard > 0 or boosted)
                                val effTotalWardForUI = totalWard + activeCharacter.maxBoostWard
                                if (effTotalWardForUI > 0) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Ward Save", fontWeight = FontWeight.Bold, color = CyanAccent)
                                            }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "$currentWard / $totalWard",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = CyanAccent
                                            )
                                            if (activeCharacter.boostWard > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = CyanAccent.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                                                    modifier = Modifier.pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onDoubleTap = {
                                                                applyBoost("Ward Save", 0)
                                                                android.widget.Toast.makeText(context, "Ward Save Boost Removed", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                ) {
                                                    val turnsTag = if (activeCharacter.boostWardTurns > 0) " (${activeCharacter.boostWardTurns}t)" else if (activeCharacter.boostWardResetOnNextRound) " (R)" else ""
                                                    Text(
                                                        text = "+${activeCharacter.boostWard} Boost$turnsTag",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CyanAccent,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val wardProgress by animateFloatAsState(targetValue = (currentWard.toFloat() / totalWard.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f), animationSpec = tween(500))
                                        Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.fillMaxWidth()) {
                                            LinearProgressIndicator(
                                                progress = { wardProgress },
                                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                                color = CyanAccent,
                                                trackColor = DarkSurfaceVariant
                                            )
                                            key(recentDamageTrigger) {
                                                if (recentChangeWard != null) {
                                                    val isRecent = (System.currentTimeMillis() - recentDamageTrigger) < 2000L
                                                    var visible by remember { mutableStateOf(isRecent) }
                                                    if (isRecent) {
                                                        LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); visible = false }
                                                    }
                                                    androidx.compose.animation.AnimatedVisibility(
                                                        visible = visible,
                                                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                                                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                                                        modifier = Modifier.offset(y = (-10).dp).padding(end = 8.dp)
                                                    ) {
                                                        val amt = recentChangeWard!!
                                                        val txt = if (amt > 0) "+$amt" else "$amt"
                                                        val col = if (amt > 0) Color.Green else Color.Red
                                                        Text(txt, color = col, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Divider(color = DarkSurfaceVariant, thickness = 1.dp)
                                }

                                 // 3. HEALTH POINTS (HP) TRACKER
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Favorite, contentDescription = null, tint = GreenHp, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Health", fontWeight = FontWeight.Bold, color = GreenHp)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "${activeCharacter.currentHp} / $effectiveMaxHp",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = GreenHp
                                            )
                                            if (activeCharacter.boostHp > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = GreenHp.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenHp),
                                                    modifier = Modifier.pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onDoubleTap = {
                                                                applyBoost("HP", 0)
                                                                android.widget.Toast.makeText(context, "HP Boost Removed", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                ) {
                                                    val turnsTag = if (activeCharacter.boostHpTurns > 0) " (${activeCharacter.boostHpTurns}t)" else if (activeCharacter.boostHpResetOnNextRound) " (R)" else ""
                                                    Text(
                                                        text = "+${activeCharacter.boostHp} Boost$turnsTag",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GreenHp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val isLowHp = (activeCharacter.currentHp.toFloat() / effectiveMaxHp.coerceAtLeast(1).toFloat()) <= 0.25f && activeCharacter.boostHp <= 0
                                    val infiniteTransition = rememberInfiniteTransition(label = "hpBlink")
                                    val blinkColor by infiniteTransition.animateColor(
                                        initialValue = CrimsonPrimary,
                                        targetValue = Color(0xFF550000),
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(750, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "hpBlinkColor"
                                    )
                                    val hpBarColor = if (activeCharacter.boostHp > 0) Color(0xFFFFA500) else if (isLowHp) blinkColor else GreenHp

                                    val hpProgress by animateFloatAsState(targetValue = (activeCharacter.currentHp.toFloat() / effectiveMaxHp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f), animationSpec = tween(500))
                                    Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.fillMaxWidth()) {
                                        LinearProgressIndicator(
                                            progress = { hpProgress },
                                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                            color = hpBarColor,
                                            trackColor = DarkSurfaceVariant
                                        )
                                        key(recentDamageTrigger) {
                                            if (recentChangeHp != null) {
                                                val isRecent = (System.currentTimeMillis() - recentDamageTrigger) < 2000L
                                                var visible by remember { mutableStateOf(isRecent) }
                                                if (isRecent) {
                                                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); visible = false }
                                                }
                                                androidx.compose.animation.AnimatedVisibility(
                                                    visible = visible,
                                                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                                                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                                                    modifier = Modifier.offset(y = (-12).dp).padding(end = 8.dp)
                                                ) {
                                                    val amt = recentChangeHp!!
                                                    val txt = if (amt > 0) "+$amt" else "$amt"
                                                    val col = if (amt > 0) Color.Green else Color.Red
                                                    Text(txt, color = col, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // HP Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { pendingAdjustmentType = AdjustmentType.DAMAGE_PROMPT },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                            contentPadding = PaddingValues(8.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("DAMAGE", color = CrimsonPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = { pendingAdjustmentType = AdjustmentType.HEAL },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                            contentPadding = PaddingValues(8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = GreenHp, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("HEAL", color = GreenHp, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = { pendingAdjustmentType = AdjustmentType.BOOST },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent.copy(alpha = 0.2f)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                                            contentPadding = PaddingValues(8.dp)
                                        ) {
                                            Icon(Icons.Default.Shield, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("BOOST DEFENSES", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }

                                // 4. MP TRACKER (Only shown if effectiveMaxMp > 0)
                                if (effectiveMaxMp > 0) {
                                    Divider(color = DarkSurfaceVariant, thickness = 1.dp)

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Mana", fontWeight = FontWeight.Bold, color = BlueMp)
                                            Text(
                                                "${activeCharacter.currentMp} / $effectiveMaxMp",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = TextPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = (activeCharacter.currentMp.toFloat() / effectiveMaxMp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp)),
                                            color = BlueMp,
                                            trackColor = DarkSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { pendingAdjustmentType = AdjustmentType.SPEND_MP },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                                contentPadding = PaddingValues(8.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("USE MP", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = { pendingAdjustmentType = AdjustmentType.RECOVER_MP },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                                contentPadding = PaddingValues(8.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = BlueMp, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("RECOVER", color = BlueMp, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ON-HAND WEAPON CARD
                    val onHandWeaponContent: @Composable () -> Unit = {
                        val equippedItemsForOnHand = remember(activeCharacter.inventory) {
                            activeCharacter.inventory.filter { it.isEquipped }
                        }
                        val mainHandWeapon = remember(equippedItemsForOnHand, activeCharacter.onHandWeaponId) {
                            equippedItemsForOnHand.find { it.id == activeCharacter.onHandWeaponId }
                        }
                        val offHandWeapon = remember(equippedItemsForOnHand, activeCharacter.offHandWeaponId) {
                            equippedItemsForOnHand.find { it.id == activeCharacter.offHandWeaponId }
                        }
                        val isDualWielding = mainHandWeapon != null && offHandWeapon != null

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "⚔️",
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "ON-HAND WEAPONS",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = GoldAccent
                                        )
                                    }

                                    if (isDualWielding) {
                                        Surface(
                                            color = GoldAccent.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, GoldAccent)
                                        ) {
                                            Text(
                                                text = "⚔️ DUAL WIELDING",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = GoldAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                // MAIN HAND SLOT
                                OnHandWeaponSlotSection(
                                    slotLabel = "MAIN HAND",
                                    slotIcon = "🗡️",
                                    weapon = mainHandWeapon,
                                    onSelectClick = {
                                        selectOnHandTargetIsOffHand = false
                                        showSelectOnHandDialog = true
                                    },
                                    onClearClick = {
                                        val updated = activeCharacter.copy(onHandWeaponId = null)
                                        activeCharacter = updated
                                        onCharacterUpdated(updated)
                                    }
                                )

                                HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)

                                // OFF-HAND SLOT
                                OnHandWeaponSlotSection(
                                    slotLabel = "OFF-HAND",
                                    slotIcon = "🛡️",
                                    weapon = offHandWeapon,
                                    onSelectClick = {
                                        selectOnHandTargetIsOffHand = true
                                        showSelectOnHandDialog = true
                                    },
                                    onClearClick = {
                                        val updated = activeCharacter.copy(offHandWeaponId = null)
                                        activeCharacter = updated
                                        onCharacterUpdated(updated)
                                    }
                                )
                            }
                        }
                    }

                    // ITEM SPECIAL EFFECTS CARD (Between On-Hand Weapon and Character Stats)
                    val itemSpecialEffectsContent: @Composable () -> Unit = {
                        val equippedItemsWithEffects = remember(activeCharacter.inventory) {
                            activeCharacter.inventory.filter { it.isEquipped && it.hasSpecialEffects && it.safeSpecialEffectsText.isNotBlank() }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = "✨", fontSize = 15.sp)
                                        Text(
                                            text = "ITEM SPECIAL EFFECTS",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = CyanAccent
                                        )
                                    }
                                    Text(
                                        text = "${equippedItemsWithEffects.size} Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (equippedItemsWithEffects.isNotEmpty()) CyanAccent else TextSecondary
                                    )
                                }

                                if (equippedItemsWithEffects.isEmpty()) {
                                    Surface(
                                        color = DarkBackground,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No equipped items have active special effects.",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        equippedItemsWithEffects.forEach { item ->
                                            Surface(
                                                color = DarkSurfaceVariant,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.25f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = item.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = GoldAccent
                                                        )
                                                        Text(
                                                            text = item.category.ifBlank { "Equipped" },
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = item.safeSpecialEffectsText,
                                                        fontSize = 12.sp,
                                                        color = TextPrimary,
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // STATS GRID (Interactive tap to view Base stat score)
                    val characterStatsContent: @Composable () -> Unit = {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CHARACTER STATS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Tap stat to view Base",
                                        fontSize = 10.sp,
                                        color = GoldAccent
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        StatDisplayCard(
                                            name = "Strength",
                                            score = activeCharacter.strength,
                                            modifierValue = activeCharacter.getStatModifier("strength"),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatDisplayCard(
                                            name = "Intelligence",
                                            score = activeCharacter.intelligence,
                                            modifierValue = activeCharacter.getStatModifier("intelligence"),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        StatDisplayCard(
                                            name = "Endurance",
                                            score = activeCharacter.endurance,
                                            modifierValue = activeCharacter.getStatModifier("endurance"),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatDisplayCard(
                                            name = "Spirit",
                                            score = activeCharacter.spirit,
                                            modifierValue = activeCharacter.getStatModifier("spirit"),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        StatDisplayCard(
                                            name = "Finesse",
                                            score = activeCharacter.finesse,
                                            modifierValue = activeCharacter.getStatModifier("finesse"),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatDisplayCard(
                                            name = "Charisma",
                                            score = activeCharacter.charisma,
                                            modifierValue = activeCharacter.getStatModifier("charisma"),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ACTIVE STATUS EFFECTS & BUFFS SECTION (UNDER STATS)
                    val activeEffectsContent: @Composable () -> Unit = {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ACTIVE EFFECTS & BUFFS (${activeCharacter.effects.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GoldAccent
                                    )

                                    Button(
                                        onClick = { showAddEffectDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ADD EFFECT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                if (activeCharacter.effects.isEmpty()) {
                                    Text(
                                        text = "No active status effects or buffs applied.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        activeCharacter.effects.forEach { effect ->
                                            EffectItemRow(
                                                effect = effect,
                                                onToggleEnable = { toggleEffectEnabled(effect.id) },
                                                onEdit = { effectToEdit = effect },
                                                onDelete = { removeEffect(effect.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isTablet) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    vitalsCardContent()
                                    onHandWeaponContent()
                                    itemSpecialEffectsContent()
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    characterStatsContent()
                                    activeEffectsContent()
                                }
                            }
                        } else {
                            vitalsCardContent()
                            onHandWeaponContent()
                            itemSpecialEffectsContent()
                            characterStatsContent()
                            activeEffectsContent()
                        }
                    }
                }
                1 -> {
                    // TAB 1: ABILITIES
                    LazyVerticalGrid(
                        columns = if (isTablet) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Button(
                                onClick = { showAddAbilityDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ADD ABILITY", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (activeCharacter.abilities.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No Abilities Added",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap 'ADD ABILITY' to create spells, skills, or special attacks!",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            val topLevelAbilities = activeCharacter.abilities.filter { ability ->
                                val parentName = SkillsRepository.getParentNameForSubSpell(ability.name)
                                parentName == null || !activeCharacter.abilities.any { it.name.trim().equals(parentName, ignoreCase = true) }
                            }
                            items(topLevelAbilities, key = { it.id }) { ability ->
                                val subSpells = activeCharacter.abilities.filter {
                                    SkillsRepository.getParentNameForSubSpell(it.name)?.equals(ability.name, ignoreCase = true) == true
                                }
                                val isCollapsed = collapsedParentAbilityNames.contains(ability.name.trim().lowercase())

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AbilityCard(
                                        ability = ability,
                                        evaluatedDescription = activeCharacter.formatAbilityDescription(ability.description),
                                        onEdit = { abilityToEdit = ability },
                                        onDelete = { deleteAbility(ability.id) },
                                        onMoveUp = { moveAbility(ability.id, -1) },
                                        onMoveDown = { moveAbility(ability.id, 1) },
                                        subSpellsCount = subSpells.size,
                                        isSubSpellsCollapsed = isCollapsed,
                                        onToggleSubSpellsCollapse = {
                                            val key = ability.name.trim().lowercase()
                                            collapsedParentAbilityNames = if (collapsedParentAbilityNames.contains(key)) {
                                                collapsedParentAbilityNames - key
                                            } else {
                                                collapsedParentAbilityNames + key
                                            }
                                        }
                                    )

                                    if (!isCollapsed && subSpells.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            subSpells.forEach { subAbility ->
                                                AbilityCard(
                                                    ability = subAbility,
                                                    evaluatedDescription = activeCharacter.formatAbilityDescription(subAbility.description),
                                                    onEdit = { abilityToEdit = subAbility },
                                                    onDelete = { deleteAbility(subAbility.id) },
                                                    onMoveUp = { moveAbility(subAbility.id, -1) },
                                                    onMoveDown = { moveAbility(subAbility.id, 1) },
                                                    isSubAbility = true
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                5 -> {
                    // TAB 5: SPELL BOOK (Next to Abilities)
                    SpellBookScreen(character = activeCharacter)
                }
                2 -> {
                    // TAB 2: EQUIPMENT (Equipped Items Only)
                    LazyVerticalGrid(
                        columns = if (isTablet) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val allEquipCategories = equippedItems.map { it.category }.toSet()
                            val allEquipCollapsed = allEquipCategories.isNotEmpty() && allEquipCategories.all { collapsedEquippedCategories.contains(it) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAddEquipmentDialog = true },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ADD EQUIPMENT", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        collapsedEquippedCategories = if (allEquipCollapsed) {
                                            emptySet()
                                        } else {
                                            allEquipCategories
                                        }
                                    },
                                    modifier = Modifier.height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkSurfaceVariant,
                                        contentColor = TextPrimary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(if (allEquipCollapsed) "▶ ALL" else "▼ ALL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (equippedItems.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No Equipped Items",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Equip items from your Inventory tab, or tap 'ADD EQUIPMENT'!",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            val groupedEquipped = equippedItems.groupBy { it.category }
                            for (category in groupedEquipped.keys) {
                                val isCollapsed = collapsedEquippedCategories.contains(category)
                                item(key = "header_equip_$category", span = { GridItemSpan(maxLineSpan) }) {
                                    CategoryHeader(
                                        categoryName = category,
                                        isExpanded = !isCollapsed,
                                        onToggleExpand = {
                                            collapsedEquippedCategories = if (isCollapsed) {
                                                collapsedEquippedCategories - category
                                            } else {
                                                collapsedEquippedCategories + category
                                            }
                                        },
                                        onMoveUp = { moveInventoryCategory(category, -1, true) },
                                        onMoveDown = { moveInventoryCategory(category, 1, true) }
                                    )
                                }
                                if (!isCollapsed) {
                                    val itemsInCategory = groupedEquipped[category] ?: emptyList()
                                    items(itemsInCategory, key = { "eq_${category}_${it.id}" }) { item ->
                                        EquipmentItemCard(
                                            item = item,
                                            onEdit = { itemToEdit = item },
                                            onToggleEquip = { toggleEquipItem(item.id) },
                                            onDelete = { deleteItem(item.id) },
                                            onMoveUp = { moveInventoryItem(item.id, -1, true) },
                                            onMoveDown = { moveInventoryItem(item.id, 1, true) },
                                            onUpdateConsumable = { amount -> updateItem(item.copy(consumableAmount = amount)) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // TAB 3: INVENTORY (Unequipped Carried Items Only)
                    LazyVerticalGrid(
                        columns = if (isTablet) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val allInvCategories = unequippedItems.map { it.category }.toSet()
                            val allInvCollapsed = allInvCategories.isNotEmpty() && allInvCategories.all { collapsedUnequippedCategories.contains(it) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAddEquipmentDialog = true },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ADD INVENTORY ITEM", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        collapsedUnequippedCategories = if (allInvCollapsed) {
                                            emptySet()
                                        } else {
                                            allInvCategories
                                        }
                                    },
                                    modifier = Modifier.height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkSurfaceVariant,
                                        contentColor = TextPrimary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(if (allInvCollapsed) "▶ ALL" else "▼ ALL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (unequippedItems.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backpack,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Inventory is Empty",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Unequip items from Equipment, or tap 'ADD INVENTORY ITEM'!",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            val groupedUnequipped = unequippedItems.groupBy { it.category }
                            for (category in groupedUnequipped.keys) {
                                val isCollapsed = collapsedUnequippedCategories.contains(category)
                                item(key = "header_inv_$category", span = { GridItemSpan(maxLineSpan) }) {
                                    CategoryHeader(
                                        categoryName = category,
                                        isExpanded = !isCollapsed,
                                        onToggleExpand = {
                                            collapsedUnequippedCategories = if (isCollapsed) {
                                                collapsedUnequippedCategories - category
                                            } else {
                                                collapsedUnequippedCategories + category
                                            }
                                        },
                                        onMoveUp = { moveInventoryCategory(category, -1, false) },
                                        onMoveDown = { moveInventoryCategory(category, 1, false) }
                                    )
                                }
                                if (!isCollapsed) {
                                    val itemsInCategory = groupedUnequipped[category] ?: emptyList()
                                    items(itemsInCategory, key = { "uneq_${category}_${it.id}" }) { item ->
                                        EquipmentItemCard(
                                            item = item,
                                            onEdit = { itemToEdit = item },
                                            onToggleEquip = { toggleEquipItem(item.id) },
                                            onDelete = { deleteItem(item.id) },
                                            onMoveUp = { moveInventoryItem(item.id, -1, false) },
                                            onMoveDown = { moveInventoryItem(item.id, 1, false) },
                                            onUpdateConsumable = { amount -> updateItem(item.copy(consumableAmount = amount)) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // TAB 4: NOTES
                    val archivedCount = activeCharacter.notes.count { it.isArchived }
                    val visibleNotes = activeCharacter.notes.filter { !it.isArchived || showArchivedNotes }

                    LazyVerticalGrid(
                        columns = if (isTablet) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Button(
                                onClick = { showAddNoteDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ADD NOTE", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        if (archivedCount > 0) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = showArchivedNotes,
                                        onClick = { showArchivedNotes = !showArchivedNotes },
                                        label = { Text(if (showArchivedNotes) "Hide Archived Notes ($archivedCount)" else "Show Archived Notes ($archivedCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(if (showArchivedNotes) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = GoldAccent,
                                            containerColor = DarkSurfaceVariant,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        if (visibleNotes.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (activeCharacter.notes.isEmpty()) "No Notes Created" else "No Active Notes",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (activeCharacter.notes.isEmpty()) "Tap 'ADD NOTE' to record quests, secrets, or NPC info!" else "All notes are currently archived. Tap 'Show Archived Notes' to view them.",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            items(visibleNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onEdit = { noteToEdit = note },
                                    onToggleArchive = { toggleArchiveNote(note.id) },
                                    onDelete = { deleteNote(note.id) },
                                    onMoveUp = { moveNote(note.id, -1) },
                                    onMoveDown = { moveNote(note.id, 1) }
                                )
                            }
                        }
                    }
                }
            }

            // Expiration Notification Popup Banner
            activeExpiredNotification?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeExpiredNotification = null },
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Expired Notification",
                                tint = GoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ROUND UPDATE NOTIFICATION",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = GoldAccent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = msg,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tap to dismiss (Auto-closes in 5s)",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                            IconButton(
                                onClick = { activeExpiredNotification = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // MANAGE GOLD DIALOG
    if (showGoldDialog) {
        var goldAmountInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showGoldDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪙", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MANAGE GOLD", fontWeight = FontWeight.Bold, color = GoldAccent)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Current Gold Balance Card
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("CURRENT BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🪙 ${activeCharacter.gold} Gold",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                    }

                    // Amount Text Field
                    OutlinedTextField(
                        value = goldAmountInput,
                        onValueChange = { goldAmountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Gold Amount") },
                        placeholder = { Text("Enter amount e.g. 50") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Add & Spend Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val change = goldAmountInput.toIntOrNull() ?: 0
                                if (change > 0) {
                                    val newGold = activeCharacter.goldPlus(change)
                                    val updated = activeCharacter.copy(gold = newGold)
                                    activeCharacter = updated
                                    onCharacterUpdated(updated)
                                    goldAmountInput = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenHp, contentColor = DarkBackground)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ADD GOLD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val change = goldAmountInput.toIntOrNull() ?: 0
                                if (change > 0) {
                                    val newGold = (activeCharacter.gold - change).coerceAtLeast(0)
                                    val updated = activeCharacter.copy(gold = newGold)
                                    activeCharacter = updated
                                    onCharacterUpdated(updated)
                                    goldAmountInput = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary, contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SPEND GOLD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Quick Stepper Buttons
                    Text("QUICK ADJUSTMENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1, 5, 10, 50, 100).forEach { amount ->
                            Button(
                                onClick = {
                                    val newGold = activeCharacter.goldPlus(amount)
                                    val updated = activeCharacter.copy(gold = newGold)
                                    activeCharacter = updated
                                    onCharacterUpdated(updated)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent.copy(alpha = 0.15f), contentColor = GoldAccent)
                            ) {
                                Text("+$amount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1, 5, 10, 50, 100).forEach { amount ->
                            Button(
                                onClick = {
                                    val newGold = (activeCharacter.gold - amount).coerceAtLeast(0)
                                    val updated = activeCharacter.copy(gold = newGold)
                                    activeCharacter = updated
                                    onCharacterUpdated(updated)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary.copy(alpha = 0.15f), contentColor = CrimsonPrimary)
                            ) {
                                Text("-$amount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGoldDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface
        )
    }

    // LEVEL UP DIALOG
    if (showLevelUpDialog) {
        AlertDialog(
            onDismissRequest = { showLevelUpDialog = false },
            title = { Text("Level Up?", fontWeight = FontWeight.Bold, color = GoldAccent) },
            text = { Text("Did you level up to level ${activeCharacter.level + 1}?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        previousLevelForAnimation = activeCharacter.level
                        val updatedChar = activeCharacter.copy(level = activeCharacter.level + 1)
                        activeCharacter = updatedChar
                        onCharacterUpdated(updatedChar)
                        showLevelUpDialog = false
                        showLevelUpAnimation = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                ) {
                    Text("YES!", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLevelUpDialog = false }) {
                    Text("NO", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // LEVEL UP ANIMATION
    if (showLevelUpAnimation) {
        Dialog(
            onDismissRequest = { showLevelUpAnimation = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showLevelUpAnimation = false },
                contentAlignment = Alignment.Center
            ) {
                var animationPhase by remember { mutableIntStateOf(0) }
                var glitchX by remember { mutableFloatStateOf(0f) }
                var glitchY by remember { mutableFloatStateOf(0f) }
                var isGlitchFlicker by remember { mutableStateOf(false) }

                LaunchedEffect(animationPhase) {
                    if (animationPhase == 1) {
                        while (true) {
                            glitchX = (-15..15).random().toFloat()
                            glitchY = (-10..10).random().toFloat()
                            isGlitchFlicker = Math.random() > 0.5
                            kotlinx.coroutines.delay((30..100).random().toLong())
                        }
                    } else {
                        glitchX = 0f
                        glitchY = 0f
                        isGlitchFlicker = false
                    }
                }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(500) // Phase 0: Show old level normally
                    animationPhase = 1 // Phase 1: Glitch
                    kotlinx.coroutines.delay(1200) // Glitch duration
                    animationPhase = 2 // Phase 2: Pop in new level
                    kotlinx.coroutines.delay(2000)
                    showLevelUpAnimation = false
                }

                if (animationPhase < 2) {
                    val isGlitching = animationPhase == 1
                    val displayedLevel = if (isGlitchFlicker) activeCharacter.level else previousLevelForAnimation
                    val textStr = if (isGlitchFlicker && Math.random() > 0.7) "L3V#L $displayedLevel" else "LEVEL $displayedLevel"

                    Box(contentAlignment = Alignment.Center) {
                        if (isGlitching) {
                            Text(
                                text = textStr,
                                color = Color.Cyan.copy(alpha = 0.7f),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.offset(x = glitchX.dp, y = glitchY.dp)
                            )
                            Text(
                                text = textStr,
                                color = Color.Magenta.copy(alpha = 0.7f),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.offset(x = (-glitchX).dp, y = (-glitchY).dp)
                            )
                        }
                        Text(
                            text = textStr,
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.offset(x = if (isGlitching) (glitchX / 2).dp else 0.dp)
                        )
                    }
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "LEVEL ${activeCharacter.level}",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }

    // EDIT CHARACTER DIALOG
    if (showEditCharacterDialog) {
        CreateCharacterDialog(
            characterToEdit = activeCharacter,
            onDismiss = { showEditCharacterDialog = false },
            onCharacterCreated = { updatedChar ->
                activeCharacter = updatedChar
                onCharacterUpdated(updatedChar)
                showEditCharacterDialog = false
            }
        )
    }

    // ADD / EDIT EFFECT DIALOG
    if (showAddEffectDialog || effectToEdit != null) {
        AddEffectDialog(
            effectToEdit = effectToEdit,
            onDismiss = {
                showAddEffectDialog = false
                effectToEdit = null
            },
            onEffectSaved = { savedEffect ->
                if (effectToEdit != null) {
                    val updatedList = activeCharacter.effects.map { if (it.id == savedEffect.id) savedEffect else it }
                    val updated = activeCharacter.copy(effects = updatedList)
                    activeCharacter = updated
                    onCharacterUpdated(updated)
                } else {
                    addEffect(savedEffect)
                }
                showAddEffectDialog = false
                effectToEdit = null
            }
        )
    }

    // EDIT ITEM DIALOG
    itemToEdit?.let { targetItem ->
        AddEquipmentDialog(
            initialIsEquipped = targetItem.isEquipped,
            itemToEdit = targetItem,
            onDismiss = { itemToEdit = null },
            onItemSaved = { updatedItem ->
                updateItem(updatedItem)
                itemToEdit = null
            }
        )
    }

    // DAMAGE PROMPT DIALOG WITH TYPE SELECTION
    if (pendingAdjustmentType == AdjustmentType.DAMAGE_PROMPT) {
        var selectedDamageType by remember { mutableStateOf(DamageType.PHYSICAL) }
        var damageAmountInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { pendingAdjustmentType = null },
            title = { Text("Take Damage", fontWeight = FontWeight.Bold, color = CrimsonPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Damage Type:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedDamageType == DamageType.PHYSICAL) GoldAccent.copy(alpha = 0.15f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedDamageType == DamageType.PHYSICAL,
                                onClick = { selectedDamageType = DamageType.PHYSICAL },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🛡️ Physical Damage (Armor First)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedDamageType == DamageType.MAGICAL) CyanAccent.copy(alpha = 0.15f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedDamageType == DamageType.MAGICAL,
                                onClick = { selectedDamageType = DamageType.MAGICAL },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✨ Magical Damage (Ward Save First)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedDamageType == DamageType.UNSAVEABLE) CrimsonPrimary.copy(alpha = 0.15f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedDamageType == DamageType.UNSAVEABLE,
                                onClick = { selectedDamageType = DamageType.UNSAVEABLE },
                                colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("💥 Unsaveable Damage (Direct on HP)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                        }
                    }

                    OutlinedTextField(
                        value = damageAmountInput,
                        onValueChange = { damageAmountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Damage Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = damageAmountInput.toIntOrNull() ?: 0
                        applyDamageWithType(amount, selectedDamageType)
                        pendingAdjustmentType = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary, contentColor = TextPrimary)
                ) {
                    Text("APPLY DAMAGE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAdjustmentType = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Number Input Prompt Dialog for Heal, Boost HP, MP
    if (pendingAdjustmentType != null && pendingAdjustmentType != AdjustmentType.DAMAGE_PROMPT) {
        val type = pendingAdjustmentType!!

        if (type == AdjustmentType.BOOST) {
            var target by remember { mutableStateOf("HP") }
            var amountInput by remember { mutableStateOf("") }
            var turnsInput by remember { mutableStateOf("0") }
            var resetOnNextRound by remember { mutableStateOf(true) }

            AlertDialog(
                onDismissRequest = { pendingAdjustmentType = null },
                title = { Text("Add Boost", fontWeight = FontWeight.Bold, color = GoldAccent) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select what to boost:", fontSize = 13.sp, color = TextPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = target == "HP", onClick = { target = "HP" })
                                Text("HP", fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = target == "Armor", onClick = { target = "Armor" })
                                Text("Armor", fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = target == "Ward Save", onClick = { target = "Ward Save" })
                                Text("Ward", fontSize = 13.sp)
                            }
                        }
                        
                        Text(
                            text = "Enter Boost details. Absorbs damage before base value!",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Boost Amount") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = turnsInput,
                            onValueChange = { turnsInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Duration (Rounds) - 0 = Permanent") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { resetOnNextRound = !resetOnNextRound }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = resetOnNextRound,
                                onCheckedChange = { resetOnNextRound = it },
                                colors = CheckboxDefaults.colors(checkedColor = GoldAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resets to full value each round", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = amountInput.toIntOrNull() ?: 0
                            val turns = turnsInput.toIntOrNull() ?: 0
                            if (amount > 0) {
                                applyBoost(target, amount, turns, resetOnNextRound)
                            }
                            pendingAdjustmentType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                    ) {
                        Text("APPLY BOOST", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingAdjustmentType = null }) {
                        Text("CANCEL", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        } else {
            val title = when (type) {
                AdjustmentType.HEAL -> "Restore Vitals"
                AdjustmentType.SPEND_MP -> "Use MP"
                AdjustmentType.RECOVER_MP -> "Recover MP"
                else -> ""
            }

            val actionColor = when (type) {
                AdjustmentType.HEAL -> GreenHp
                AdjustmentType.SPEND_MP -> TextSecondary
                AdjustmentType.RECOVER_MP -> BlueMp
                else -> GoldAccent
            }

            var amountInput by remember { mutableStateOf("") }
            var healTarget by remember { mutableStateOf("HP") }

            AlertDialog(
                onDismissRequest = { pendingAdjustmentType = null },
                title = { Text(title, fontWeight = FontWeight.Bold, color = actionColor) },
                text = {
                    Column {
                        if (type == AdjustmentType.HEAL) {
                            Text("Select what to restore:", fontSize = 13.sp, color = TextPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = healTarget == "HP", onClick = { healTarget = "HP" }, colors = RadioButtonDefaults.colors(selectedColor = actionColor))
                                    Text("HP", fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = healTarget == "Armor", onClick = { healTarget = "Armor" }, colors = RadioButtonDefaults.colors(selectedColor = actionColor))
                                    Text("Armor", fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = healTarget == "Ward Save", onClick = { healTarget = "Ward Save" }, colors = RadioButtonDefaults.colors(selectedColor = actionColor))
                                    Text("Ward", fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Text(
                            text = when (type) {
                                AdjustmentType.HEAL -> "Enter amount to restore:"
                                AdjustmentType.SPEND_MP -> "Enter MP amount to spend:"
                                AdjustmentType.RECOVER_MP -> "Enter MP amount to recover:"
                                else -> ""
                            },
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Amount") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = actionColor,
                                unfocusedBorderColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = amountInput.toIntOrNull() ?: 0
                            if (amount > 0) {
                                when (type) {
                                    AdjustmentType.HEAL -> applyHeal(healTarget, amount)
                                    AdjustmentType.SPEND_MP -> applySpendMp(amount)
                                    AdjustmentType.RECOVER_MP -> applyRecoverMp(amount)
                                    else -> {}
                                }
                            }
                            pendingAdjustmentType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = actionColor, contentColor = DarkBackground)
                    ) {
                        Text("CONFIRM", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingAdjustmentType = null }) {
                        Text("CANCEL", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }

    // Select On-Hand Weapon Dialog
    if (showSelectOnHandDialog) {
        SelectOnHandWeaponDialog(
            character = activeCharacter,
            initialIsOffHand = selectOnHandTargetIsOffHand,
            onDismissRequest = { showSelectOnHandDialog = false },
            onSelectWeapon = { isOffHand, selectedWeapon ->
                val updated = if (isOffHand) {
                    activeCharacter.copy(offHandWeaponId = selectedWeapon?.id)
                } else {
                    activeCharacter.copy(onHandWeaponId = selectedWeapon?.id)
                }
                activeCharacter = updated
                onCharacterUpdated(updated)
                showSelectOnHandDialog = false
            }
        )
    }

    // Roll Dice Prompt Dialog for Round Effects
    if (showDiceRollPromptDialog) {
        RollDiceRoundDialog(
            diceEffects = activeDiceEffects,
            onDismissRequest = { showDiceRollPromptDialog = false },
            onConfirmRolls = { rollResults ->
                showDiceRollPromptDialog = false
                executeRoundTick(rollResults)
            }
        )
    }

    // Add Equipment Dialog
    if (showAddEquipmentDialog) {
        AddEquipmentDialog(
            initialIsEquipped = (selectedTab == 2),
            itemToEdit = null,
            onDismiss = { showAddEquipmentDialog = false },
            onItemSaved = { newItem ->
                addItem(newItem)
                showAddEquipmentDialog = false
            }
        )
    }

    // Add / Edit Ability Dialog
    if (showAddAbilityDialog || abilityToEdit != null) {
        AddAbilityDialog(
            character = activeCharacter,
            abilityToEdit = abilityToEdit,
            onDismiss = {
                showAddAbilityDialog = false
                abilityToEdit = null
            },
            onAbilitySaved = { savedAbility ->
                if (abilityToEdit != null) {
                    updateAbility(savedAbility)
                } else {
                    addAbility(savedAbility)
                }
                showAddAbilityDialog = false
                abilityToEdit = null
            }
        )
    }

    // Add / Edit Note Dialog
    if (showAddNoteDialog || noteToEdit != null) {
        AddNoteDialog(
            noteToEdit = noteToEdit,
            onDismiss = {
                showAddNoteDialog = false
                noteToEdit = null
            },
            onNoteSaved = { savedNote ->
                if (noteToEdit != null) {
                    updateNote(savedNote)
                } else {
                    addNote(savedNote)
                }
                showAddNoteDialog = false
                noteToEdit = null
            }
        )
    }
}

