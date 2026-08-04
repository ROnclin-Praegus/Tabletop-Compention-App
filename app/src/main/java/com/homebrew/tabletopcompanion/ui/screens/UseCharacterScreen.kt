package com.homebrew.tabletopcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.model.CharacterAbility
import com.homebrew.tabletopcompanion.model.CharacterEffect
import com.homebrew.tabletopcompanion.model.CharacterNote
import com.homebrew.tabletopcompanion.model.DamageType
import com.homebrew.tabletopcompanion.model.EffectType
import com.homebrew.tabletopcompanion.model.EquipmentItem
import com.homebrew.tabletopcompanion.ui.theme.*

enum class AdjustmentType {
    DAMAGE_PROMPT,
    HEAL_HP,
    BOOST_HP,
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

    // Active Tab state: 0 = Vitals, 1 = Abilities, 2 = Equipment, 3 = Inventory, 4 = Notes
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog states
    var pendingAdjustmentType by remember { mutableStateOf<AdjustmentType?>(null) }
    var showAddEquipmentDialog by remember { mutableStateOf(false) }
    var showEditCharacterDialog by remember { mutableStateOf(false) }
    var showAddEffectDialog by remember { mutableStateOf(false) }
    var showAddAbilityDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showArchivedNotes by remember { mutableStateOf(false) }
    var showGoldDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<EquipmentItem?>(null) }
    var abilityToEdit by remember { mutableStateOf<CharacterAbility?>(null) }
    var noteToEdit by remember { mutableStateOf<CharacterNote?>(null) }

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

    fun resetDefensesAndTickRound() {
        var newHp = activeCharacter.currentHp
        var newMp = activeCharacter.currentMp
        var newArmor = totalArmor
        var newWard = totalWard
        var newBoostHp = activeCharacter.boostHp
        var newMaxBoostHp = activeCharacter.maxBoostHp
        var newBoostHpTurns = activeCharacter.boostHpTurns

        val expiredList = mutableListOf<String>()

        val effectiveMaxHp = activeCharacter.getEffectiveMaxHp()
        val effectiveMaxMp = activeCharacter.getEffectiveMaxMp()
        val effectiveSpirit = activeCharacter.getEffectiveSpirit()

        // 1. Reset Boost HP to max amount if reset flag is true and Boost HP is active
        if (newMaxBoostHp > 0 && activeCharacter.boostHpResetOnNextRound && (newBoostHpTurns > 0 || (newBoostHpTurns == 0 && newBoostHp > 0))) {
            newBoostHp = newMaxBoostHp
        }

        // 2. Spirit MP recharge on Next Round (1 point in Spirit = +1 MP recharge)
        if (effectiveMaxMp > 0 && effectiveSpirit > 0) {
            newMp = (newMp + effectiveSpirit).coerceIn(0, effectiveMaxMp)
        }

        // 3. Process per-round HP/MP effects and decrement duration
        val updatedEffects = activeCharacter.effects.mapNotNull { effect ->
            // Apply per-round HP change
            if (effect.effectType == EffectType.HP_CHANGE_PER_ROUND) {
                if (effect.value > 0) {
                    newHp = (newHp + effect.value).coerceIn(0, effectiveMaxHp)
                } else if (effect.value < 0) {
                    var remainingDamage = kotlin.math.abs(effect.value)
                    when (effect.damageType) {
                        DamageType.PHYSICAL -> {
                            if (newArmor > 0) {
                                if (remainingDamage <= newArmor) {
                                    newArmor -= remainingDamage
                                    remainingDamage = 0
                                } else {
                                    remainingDamage -= newArmor
                                    newArmor = 0
                                }
                            }
                        }
                        DamageType.MAGICAL -> {
                            if (newWard > 0) {
                                if (remainingDamage <= newWard) {
                                    newWard -= remainingDamage
                                    remainingDamage = 0
                                } else {
                                    remainingDamage -= newWard
                                    newWard = 0
                                }
                            }
                        }
                        DamageType.UNSAVEABLE -> {
                            // Direct HP / Boost HP damage
                        }
                    }

                    // Boost HP is affected FIRST before Current HP!
                    if (remainingDamage > 0 && newBoostHp > 0) {
                        if (remainingDamage <= newBoostHp) {
                            newBoostHp -= remainingDamage
                            remainingDamage = 0
                        } else {
                            remainingDamage -= newBoostHp
                            newBoostHp = 0
                        }
                    }

                    if (remainingDamage > 0) {
                        newHp = (newHp - remainingDamage).coerceAtLeast(0)
                    }
                }
            }
            // Apply per-round MP change
            if (effect.effectType == EffectType.MP_CHANGE_PER_ROUND && effectiveMaxMp > 0) {
                newMp = (newMp + effect.value).coerceIn(0, effectiveMaxMp)
            }

            // Decrement duration (0 = unlimited)
            if (effect.roundsRemaining > 1) {
                effect.copy(roundsRemaining = effect.roundsRemaining - 1)
            } else if (effect.roundsRemaining == 1) {
                expiredList.add("Effect expired: ${effect.name}")
                null // Expired!
            } else {
                effect // Permanent / Unlimited (roundsRemaining == 0)
            }
        }

        // 3. Decrement Boost HP turns
        if (newBoostHpTurns > 1) {
            newBoostHpTurns -= 1
        } else if (newBoostHpTurns == 1) {
            newBoostHpTurns = 0
            newBoostHp = 0
            newMaxBoostHp = 0
            expiredList.add("Boost HP duration has ended!")
        }

        if (expiredList.isNotEmpty()) {
            activeExpiredNotification = expiredList.joinToString("\n")
        }

        val updated = activeCharacter.copy(
            currentHp = newHp,
            currentMp = newMp,
            boostHp = newBoostHp,
            maxBoostHp = newMaxBoostHp,
            boostHpTurns = newBoostHpTurns,
            currentArmor = newArmor,
            currentWard = newWard,
            effects = updatedEffects
        )
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun applyDamageWithType(amount: Int, type: DamageType) {
        if (amount <= 0) return
        var remainingDamage = amount
        var newArmor = currentArmor
        var newWard = currentWard
        var newBoostHp = activeCharacter.boostHp
        var newCurrentHp = activeCharacter.currentHp

        when (type) {
            DamageType.PHYSICAL -> {
                if (newArmor > 0) {
                    if (remainingDamage <= newArmor) {
                        newArmor -= remainingDamage
                        remainingDamage = 0
                    } else {
                        remainingDamage -= newArmor
                        newArmor = 0
                    }
                }
            }
            DamageType.MAGICAL -> {
                if (newWard > 0) {
                    if (remainingDamage <= newWard) {
                        newWard -= remainingDamage
                        remainingDamage = 0
                    } else {
                        remainingDamage -= newWard
                        newWard = 0
                    }
                }
            }
            DamageType.UNSAVEABLE -> {
                // Direct HP damage, bypasses Armor and Ward!
            }
        }

        if (remainingDamage > 0) {
            if (newBoostHp > 0) {
                if (remainingDamage <= newBoostHp) {
                    newBoostHp -= remainingDamage
                    remainingDamage = 0
                } else {
                    remainingDamage -= newBoostHp
                    newBoostHp = 0
                }
            }
        }

        if (remainingDamage > 0) {
            newCurrentHp = (newCurrentHp - remainingDamage).coerceAtLeast(0)
        }

        val updated = activeCharacter.copy(
            currentHp = newCurrentHp,
            boostHp = newBoostHp,
            currentArmor = newArmor,
            currentWard = newWard
        )
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun applyHeal(amount: Int) {
        if (amount <= 0) return
        val maxHp = activeCharacter.getEffectiveMaxHp()
        val newCurrentHp = (activeCharacter.currentHp + amount).coerceAtMost(maxHp)
        val updated = activeCharacter.copy(currentHp = newCurrentHp)
        activeCharacter = updated
        onCharacterUpdated(updated)
    }

    fun applyBoostHp(amount: Int, turns: Int = 0, resetOnNextRound: Boolean = true) {
        if (amount <= 0) return
        val updated = activeCharacter.copy(
            boostHp = amount,
            maxBoostHp = amount,
            boostHpTurns = turns,
            boostHpResetOnNextRound = resetOnNextRound
        )
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
        val oldMaxHp = activeCharacter.getEffectiveMaxHp()
        val oldMaxMp = activeCharacter.getEffectiveMaxMp()

        val updatedBase = getUpdatedChar(activeCharacter)

        val newMaxHp = updatedBase.getEffectiveMaxHp()
        val newMaxMp = updatedBase.getEffectiveMaxMp()

        val hpDelta = newMaxHp - oldMaxHp
        val mpDelta = newMaxMp - oldMaxMp

        val adjustedHp = (activeCharacter.currentHp + hpDelta).coerceIn(0, newMaxHp)
        val adjustedMp = (activeCharacter.currentMp + mpDelta).coerceIn(0, newMaxMp)

        val finalUpdated = updatedBase.copy(
            currentHp = adjustedHp,
            currentMp = adjustedMp
        )
        activeCharacter = finalUpdated
        onCharacterUpdated(finalUpdated)
    }

    fun toggleEquipItem(itemId: String) {
        applyCharacterUpdateWithVitalsDelta { char ->
            val updatedList = char.inventory.map { item ->
                if (item.id == itemId) item.copy(isEquipped = !item.isEquipped) else item
            }
            char.copy(
                inventory = updatedList,
                currentArmor = null,
                currentWard = null
            )
        }
    }

    fun deleteItem(itemId: String) {
        applyCharacterUpdateWithVitalsDelta { char ->
            val updatedList = char.inventory.filter { it.id != itemId }
            char.copy(
                inventory = updatedList,
                currentArmor = null,
                currentWard = null
            )
        }
    }

    fun addItem(newItem: EquipmentItem) {
        applyCharacterUpdateWithVitalsDelta { char ->
            val updatedList = listOf(newItem) + char.inventory
            char.copy(
                inventory = updatedList,
                currentArmor = null,
                currentWard = null
            )
        }
    }

    fun updateItem(updatedItem: EquipmentItem) {
        applyCharacterUpdateWithVitalsDelta { char ->
            val updatedList = char.inventory.map { item ->
                if (item.id == updatedItem.id) updatedItem else item
            }
            char.copy(
                inventory = updatedList,
                currentArmor = null,
                currentWard = null
            )
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

    fun addAbility(ability: CharacterAbility) {
        val updatedList = activeCharacter.abilities + ability
        val updated = activeCharacter.copy(abilities = updatedList)
        activeCharacter = updated
        onCharacterUpdated(updated)
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
        val indices = list.mapIndexedNotNull { i, item -> if (item.isEquipped == isEquippedOnly) i else null }
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

    val equippedItems = activeCharacter.inventory.filter { it.isEquipped }
    val unequippedItems = activeCharacter.inventory.filter { !it.isEquipped }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    text = "${activeCharacter.race} • Active Session",
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

                        IconButton(onClick = { showEditCharacterDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Hero", tint = GoldAccent)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )

                // 5 Icon-Only Navigation Tabs (Vitals, Abilities, Equipment, Inventory, Notes)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = GoldAccent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Shield, contentDescription = "Vitals", modifier = Modifier.size(22.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Abilities", modifier = Modifier.size(22.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Equipment", modifier = Modifier.size(22.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Backpack, contentDescription = "Inventory", modifier = Modifier.size(22.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.EditNote, contentDescription = "Notes", modifier = Modifier.size(24.dp)) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab in 1..4) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            1 -> showAddAbilityDialog = true
                            2, 3 -> showAddEquipmentDialog = true
                            4 -> showAddNoteDialog = true
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = {
                        Text(
                            text = when (selectedTab) {
                                1 -> "ADD ABILITY"
                                2 -> "ADD EQUIPMENT"
                                3 -> "ADD INVENTORY ITEM"
                                else -> "ADD NOTE"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = GoldAccent,
                    contentColor = DarkBackground
                )
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: VITALS & STATS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                                        onClick = { resetDefensesAndTickRound() },
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

                                // 1. ARMOR BAR (Rendered ABOVE HP bar if equipped totalArmor > 0)
                                if (totalArmor > 0) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Shield, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Armor Points (Physical)", fontWeight = FontWeight.Bold, color = GoldAccent)
                                            }

                                            Text(
                                                "$currentArmor / $totalArmor",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = GoldAccent
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = (currentArmor.toFloat() / totalArmor.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = GoldAccent,
                                            trackColor = DarkSurfaceVariant
                                        )
                                    }

                                    Divider(color = DarkSurfaceVariant, thickness = 1.dp)
                                }

                                // 2. WARD SAVE BAR (Rendered ABOVE HP bar if equipped totalWard > 0)
                                if (totalWard > 0) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Ward Save (Magical)", fontWeight = FontWeight.Bold, color = CyanAccent)
                                            }

                                            Text(
                                                "$currentWard / $totalWard",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = CyanAccent
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = (currentWard.toFloat() / totalWard.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = CyanAccent,
                                            trackColor = DarkSurfaceVariant
                                        )
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
                                            Text("Health Points (HP)", fontWeight = FontWeight.Bold, color = GreenHp)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "${activeCharacter.currentHp} / $effectiveMaxHp",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = TextPrimary
                                            )
                                            if (activeCharacter.boostHp > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = GoldAccent.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                                                ) {
                                                    val turnsTag = if (activeCharacter.boostHpTurns > 0) " (${activeCharacter.boostHpTurns}t)" else if (activeCharacter.boostHpResetOnNextRound) " (Reset)" else ""
                                                    Text(
                                                        text = "+${activeCharacter.boostHp} Boost$turnsTag",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GoldAccent,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = (activeCharacter.currentHp.toFloat() / effectiveMaxHp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp)),
                                        color = if (activeCharacter.boostHp > 0) GoldAccent else GreenHp,
                                        trackColor = DarkSurfaceVariant
                                    )
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
                                            onClick = { pendingAdjustmentType = AdjustmentType.HEAL_HP },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                            contentPadding = PaddingValues(8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = GreenHp, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("HEAL", color = GreenHp, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = { pendingAdjustmentType = AdjustmentType.BOOST_HP },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent.copy(alpha = 0.2f)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                                            contentPadding = PaddingValues(8.dp)
                                        ) {
                                            Icon(Icons.Default.Shield, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("BOOST HP", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                            Text("Mana Points (MP)", fontWeight = FontWeight.Bold, color = BlueMp)
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

                        // STATS GRID (Interactive tap to view Base stat score)
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

                        // ACTIVE STATUS EFFECTS & BUFFS SECTION (UNDER STATS)
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
                                                onDelete = { removeEffect(effect.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: ABILITIES
                    if (activeCharacter.abilities.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
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
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(activeCharacter.abilities, key = { it.id }) { ability ->
                                AbilityCard(
                                    ability = ability,
                                    evaluatedDescription = activeCharacter.formatAbilityDescription(ability.description),
                                    onEdit = { abilityToEdit = ability },
                                    onDelete = { deleteAbility(ability.id) },
                                    onMoveUp = { moveAbility(ability.id, -1) },
                                    onMoveDown = { moveAbility(ability.id, 1) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: EQUIPMENT (Equipped Items Only)
                    if (equippedItems.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
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
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(equippedItems, key = { it.id }) { item ->
                                EquipmentItemCard(
                                    item = item,
                                    onEdit = { itemToEdit = item },
                                    onToggleEquip = { toggleEquipItem(item.id) },
                                    onDelete = { deleteItem(item.id) },
                                    onMoveUp = { moveInventoryItem(item.id, -1, true) },
                                    onMoveDown = { moveInventoryItem(item.id, 1, true) }
                                )
                            }
                        }
                    }
                }
                3 -> {
                    // TAB 3: INVENTORY (Unequipped Carried Items Only)
                    if (unequippedItems.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
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
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(unequippedItems, key = { it.id }) { item ->
                                EquipmentItemCard(
                                    item = item,
                                    onEdit = { itemToEdit = item },
                                    onToggleEquip = { toggleEquipItem(item.id) },
                                    onDelete = { deleteItem(item.id) },
                                    onMoveUp = { moveInventoryItem(item.id, -1, false) },
                                    onMoveDown = { moveInventoryItem(item.id, 1, false) }
                                )
                            }
                        }
                    }
                }
                4 -> {
                    // TAB 4: NOTES
                    val archivedCount = activeCharacter.notes.count { it.isArchived }
                    val visibleNotes = activeCharacter.notes.filter { !it.isArchived || showArchivedNotes }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (archivedCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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

                        if (visibleNotes.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
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
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                    val newGold = activeCharacter.gold + change
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
                                    val newGold = activeCharacter.gold + amount
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

    // ADD EFFECT DIALOG
    if (showAddEffectDialog) {
        AddEffectDialog(
            onDismiss = { showAddEffectDialog = false },
            onEffectAdded = { newEffect ->
                addEffect(newEffect)
                showAddEffectDialog = false
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

        if (type == AdjustmentType.BOOST_HP) {
            var amountInput by remember { mutableStateOf("") }
            var turnsInput by remember { mutableStateOf("0") }
            var resetOnNextRound by remember { mutableStateOf(true) }

            AlertDialog(
                onDismissRequest = { pendingAdjustmentType = null },
                title = { Text("Add Boost HP", fontWeight = FontWeight.Bold, color = GoldAccent) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enter Boost HP details. Boost HP absorbs damage first before HP!",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Boost HP Amount") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = turnsInput,
                            onValueChange = { turnsInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Duration in Turns (0 = Unlimited)") },
                            placeholder = { Text("0 for unlimited, or 1, 2, 3...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reset to Max on Next Round", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("Resets Boost HP to full value each round", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = resetOnNextRound,
                                onCheckedChange = { resetOnNextRound = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = GoldAccent)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = amountInput.toIntOrNull() ?: 0
                            val turns = turnsInput.toIntOrNull() ?: 0
                            applyBoostHp(amount, turns, resetOnNextRound)
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
                AdjustmentType.HEAL_HP -> "Heal HP"
                AdjustmentType.SPEND_MP -> "Use MP"
                AdjustmentType.RECOVER_MP -> "Recover MP"
                else -> ""
            }

            val actionColor = when (type) {
                AdjustmentType.HEAL_HP -> GreenHp
                AdjustmentType.SPEND_MP -> TextSecondary
                AdjustmentType.RECOVER_MP -> BlueMp
                else -> GoldAccent
            }

            var amountInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { pendingAdjustmentType = null },
                title = { Text(title, fontWeight = FontWeight.Bold, color = actionColor) },
                text = {
                    Column {
                        Text(
                            text = when (type) {
                                AdjustmentType.HEAL_HP -> "Enter heal amount to restore HP (up to max HP):"
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            when (type) {
                                AdjustmentType.HEAL_HP -> applyHeal(amount)
                                AdjustmentType.SPEND_MP -> applySpendMp(amount)
                                AdjustmentType.RECOVER_MP -> applyRecoverMp(amount)
                                else -> {}
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

@Composable
fun AbilityCard(
    ability: CharacterAbility,
    evaluatedDescription: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val maxCharLimit = 130
    val isLong = evaluatedDescription.length > maxCharLimit

    val displayText = if (isLong && !isExpanded) {
        evaluatedDescription.take(maxCharLimit) + "..."
    } else {
        evaluatedDescription
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ability.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GoldAccent,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Ability", tint = GoldAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Ability", tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (evaluatedDescription.isNotBlank()) {
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLong) Modifier.clickable { isExpanded = !isExpanded } else Modifier)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = displayText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        if (isLong) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isExpanded) "▲ Tap to collapse" else "▼ Tap to expand",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                        if (ability.description.isNotBlank() && ability.description != evaluatedDescription) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Formula: ${ability.description}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EffectItemRow(
    effect: CharacterEffect,
    onDelete: () -> Unit
) {
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = effect.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )

                val valStr = if (effect.value > 0) "+${effect.value}" else "${effect.value}"
                val desc = when (effect.effectType) {
                    EffectType.STAT_MODIFIER -> "${effect.targetStat?.uppercase()}: $valStr"
                    EffectType.HP_CHANGE_PER_ROUND -> {
                        if (effect.value < 0) {
                            val typeLabel = when (effect.damageType) {
                                DamageType.PHYSICAL -> "Physical 🛡️"
                                DamageType.MAGICAL -> "Magical ✨"
                                DamageType.UNSAVEABLE -> "Unsaveable 💥"
                            }
                            "$valStr $typeLabel HP / round"
                        } else {
                            "$valStr HP / round"
                        }
                    }
                    EffectType.MP_CHANGE_PER_ROUND -> "$valStr MP / round"
                }

                Text(
                    text = desc,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (effect.value >= 0) GreenHp else CrimsonPrimary
                )

                val durationText = if (effect.roundsRemaining == 0) "Duration: Unlimited" else "Duration: ${effect.roundsRemaining} Rounds left"
                Text(
                    text = durationText,
                    fontSize = 10.sp,
                    color = GoldAccent
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Effect",
                    tint = CrimsonPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EquipmentItemCard(
    item: EquipmentItem,
    onEdit: () -> Unit,
    onToggleEquip: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isEquipped) DarkSurfaceVariant else DarkSurface
        ),
        border = if (item.isEquipped) androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)) else null
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Item", tint = GoldAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Badges & Details FlowRow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.hasArmor && item.armorSave > 0) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "🛡️ Armor: +${item.armorSave}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            maxLines = 1
                        )
                    }
                }

                if (item.hasWard && item.wardSave > 0) {
                    Surface(
                        color = CyanAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "✨ Ward: +${item.wardSave}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            maxLines = 1
                        )
                    }
                }

                if (item.hasStatModifiers && item.statModifiers.isNotEmpty()) {
                    item.statModifiers.forEach { (stat, valMod) ->
                        if (valMod != 0) {
                            val color = if (valMod > 0) GreenHp else CrimsonPrimary
                            val valStr = if (valMod > 0) "+$valMod" else "$valMod"
                            Surface(
                                color = color.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${stat.take(3).uppercase()}: $valStr",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // HAS ATTACKS BADGE
            if (item.hasAttacks) {
                Surface(
                    color = GoldAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚔️ Attacks: ${item.attackCount} × (${if (item.attackDamage.isNotBlank()) item.attackDamage else "Base"} Dmg)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }

            // SPECIAL EFFECTS BADGE
            if (item.hasSpecialEffects && item.specialEffectsText.isNotBlank()) {
                Surface(
                    color = CyanAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ Special Effect: ${item.specialEffectsText}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CyanAccent
                        )
                    }
                }
            }

            // Equip / Unequip Action Button
            Button(
                onClick = onToggleEquip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (item.isEquipped) CrimsonPrimary.copy(alpha = 0.2f) else GreenHp.copy(alpha = 0.2f),
                    contentColor = if (item.isEquipped) CrimsonPrimary else GreenHp
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (item.isEquipped) CrimsonPrimary else GreenHp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (item.isEquipped) Icons.Default.RemoveCircleOutline else Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (item.isEquipped) "UNEQUIP (MOVE TO INVENTORY)" else "EQUIP (MOVE TO EQUIPMENT)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun NoteCard(
    note: CharacterNote,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val maxCharLimit = 130
    val isLong = note.content.length > maxCharLimit

    val displayText = if (isLong && !isExpanded) {
        note.content.take(maxCharLimit) + "..."
    } else {
        note.content
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isArchived) DarkSurfaceVariant.copy(alpha = 0.6f) else DarkSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = note.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (note.isArchived) TextSecondary else GoldAccent
                    )
                    if (note.tag.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = DarkBackground,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = note.tag.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (note.isArchived) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = CrimsonPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ARCHIVED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    if (isLong) {
                        IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse Note" else "Expand Note",
                                tint = GoldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleArchive, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (note.isArchived) "Unarchive" else "Archive",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Note", tint = GoldAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (note.content.isNotBlank()) {
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLong) Modifier.clickable { isExpanded = !isExpanded } else Modifier)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = displayText,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatDisplayCard(
    name: String,
    score: Int,
    modifierValue: Int,
    modifier: Modifier = Modifier
) {
    var showBaseOnly by remember { mutableStateOf(false) }
    val total = score + modifierValue
    val modStr = if (modifierValue > 0) "+$modifierValue" else "$modifierValue"
    val numberColor = when {
        showBaseOnly -> GoldAccent
        modifierValue > 0 -> GreenHp
        modifierValue < 0 -> CrimsonPrimary
        else -> GoldAccent
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { showBaseOnly = !showBaseOnly },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = name.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (showBaseOnly) {
                Text(
                    text = "$score",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Base Score",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$total",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = numberColor,
                        textAlign = TextAlign.Center
                    )
                    if (modifierValue != 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "($modStr)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (modifierValue > 0) GreenHp else CrimsonPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Total",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
