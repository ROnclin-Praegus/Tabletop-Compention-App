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

@Composable
fun CategoryHeader(
    categoryName: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = GoldAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = categoryName.uppercase(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = GoldAccent
            )
        }
        Row {
            IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Category Up", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Category Down", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
    androidx.compose.material3.HorizontalDivider(color = GoldAccent.copy(alpha = 0.3f), thickness = 1.dp)
}

@OptIn(ExperimentalLayoutApi::class)

@Composable
fun EquipmentItemCard(
    item: EquipmentItem,
    onEdit: () -> Unit,
    onToggleEquip: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUpdateConsumable: (Int) -> Unit = {}
) {
    var showDeletePrompt by remember { mutableStateOf<String?>(null) }

    val promptText = showDeletePrompt
    if (promptText != null) {
        AlertDialog(
            onDismissRequest = { showDeletePrompt = null },
            title = { Text(if (promptText.contains("Amount reached 0")) "Delete Consumable?" else "Delete Item?", fontWeight = FontWeight.Bold, color = CrimsonPrimary) },
            text = { Text(promptText) },
            confirmButton = {
                Button(
                    onClick = { showDeletePrompt = null; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary, contentColor = TextPrimary)
                ) { Text("DELETE", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePrompt = null }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }
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
                    IconButton(onClick = { showDeletePrompt = "Are you sure you want to delete ${item.name}?" }, modifier = Modifier.size(32.dp)) {
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

                if (item.hasStatModifiers && item.statModifiers?.isNotEmpty() == true) {
                    item.statModifiers?.forEach { (stat, valMod) ->
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
                            text = "⚔️ Attacks: ${item.attackCount} × (${if (item.safeAttackDamage.isNotBlank()) item.safeAttackDamage else "Base"} Dmg)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }

            // SPECIAL EFFECTS BADGES
            if (item.hasSpecialEffects && item.safeSpecialEffectsText.isNotBlank()) {
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
                            text = "🛡️ Equipped Effect: ${item.safeSpecialEffectsText}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CyanAccent
                        )
                    }
                }
            }

            if (item.hasSpecialEffects && item.safeOnHandSpecialEffectsText.isNotBlank()) {
                Surface(
                    color = GoldAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚔️ On-Hand Effect: ${item.safeOnHandSpecialEffectsText}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GoldAccent
                        )
                    }
                }
            }

            if (item.hasMagic && (item.spellDamageModifier != 0 || item.spellHealingModifier != 0)) {
                Surface(
                    color = Color(0xFFB388FF).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔮 Spell Dmg: ${if (item.spellDamageModifier > 0) "+${item.spellDamageModifier}" else "${item.spellDamageModifier}"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB388FF)
                        )
                        Text(
                            text = "💖 Spell Heal: ${if (item.spellHealingModifier > 0) "+${item.spellHealingModifier}" else "${item.spellHealingModifier}"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenHp
                        )
                    }
                }
            }

            // Equip / Unequip / Consumable Action Button
            if (!item.isEquipped && item.isConsumable) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Amount: ${item.consumableAmount}", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row {
                        Button(
                            onClick = { 
                                val newAmount = item.consumableAmount - 1
                                if (newAmount <= 0) {
                                    onUpdateConsumable(0)
                                    showDeletePrompt = "Amount reached 0. Do you want to delete ${item.name} from your inventory?"
                                } else {
                                    onUpdateConsumable(newAmount)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary.copy(alpha = 0.2f), contentColor = CrimsonPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onUpdateConsumable(item.consumableAmount + 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenHp.copy(alpha = 0.2f), contentColor = GreenHp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GreenHp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
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


@Composable
fun OnHandWeaponSlotSection(
    slotLabel: String,
    slotIcon: String,
    weapon: EquipmentItem?,
    onSelectClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                Text(text = slotIcon, fontSize = 13.sp)
                Text(
                    text = slotLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onSelectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (weapon != null) DarkSurfaceVariant else GoldAccent,
                        contentColor = if (weapon != null) GoldAccent else DarkBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp),
                    border = if (weapon != null) BorderStroke(1.dp, GoldAccent) else null
                ) {
                    Text(
                        text = if (weapon != null) "Switch" else "Select Weapon",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (weapon != null) {
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear $slotLabel",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (weapon != null) {
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = weapon.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = weapon.category.ifBlank { "Weapon" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldAccent
                            )
                        }

                        if (weapon.hasAttacks) {
                            Surface(
                                color = GoldAccent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, GoldAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "ATTACK",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${weapon.attackCount} × ${if (weapon.attackDamage.isNotBlank()) weapon.attackDamage else "Base"} Dmg",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldAccent
                                    )
                                }
                            }
                        }
                    }

                    // Badges for armor, ward, stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (weapon.hasArmor && weapon.armorSave > 0) {
                            Surface(
                                color = GoldAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Armor: ${weapon.armorSave}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (weapon.hasWard && weapon.wardSave > 0) {
                            Surface(
                                color = CyanAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Ward: ${weapon.wardSave}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        weapon.statModifiers?.forEach { (stat, mod) ->
                            if (mod != 0) {
                                Surface(
                                    color = if (mod > 0) GreenHp.copy(alpha = 0.15f) else CrimsonPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${stat.take(3).uppercase()}: ${if (mod > 0) "+$mod" else "$mod"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mod > 0) GreenHp else CrimsonPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (weapon.hasSpecialEffects && weapon.safeOnHandSpecialEffectsText.isNotBlank()) {
                        Surface(
                            color = GoldAccent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "⚔️ ON-HAND SPECIAL EFFECT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = weapon.safeOnHandSpecialEffectsText,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    if (weapon.hasSpecialEffects && weapon.safeSpecialEffectsText.isNotBlank()) {
                        Surface(
                            color = CyanAccent.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "🛡️ EQUIPPED SPECIAL EFFECT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = weapon.safeSpecialEffectsText,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                color = DarkBackground,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectClick() }
            ) {
                Text(
                    text = "No ${slotLabel.lowercase()} weapon selected. Tap 'Select Weapon' to choose from your equipped items.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}


@Composable
fun SelectOnHandWeaponDialog(
    character: Character,
    initialIsOffHand: Boolean = false,
    onDismissRequest: () -> Unit,
    onSelectWeapon: (isOffHand: Boolean, EquipmentItem?) -> Unit
) {
    var isOffHand by remember(initialIsOffHand) { mutableStateOf(initialIsOffHand) }
    val equippedItems = remember(character.inventory) {
        character.inventory.filter { it.isEquipped }
    }
    val currentSlotWeaponId = if (isOffHand) character.offHandWeaponId else character.onHandWeaponId

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
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
                        text = if (isOffHand) "Select Off-Hand Weapon" else "Select Main Hand Weapon",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GoldAccent
                    )
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Hand slot selector tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isOffHand,
                        onClick = { isOffHand = false },
                        label = {
                            Text(
                                text = "🗡️ Main Hand",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                            selectedLabelColor = GoldAccent,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = !isOffHand,
                            selectedBorderColor = GoldAccent,
                            borderColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = isOffHand,
                        onClick = { isOffHand = true },
                        label = {
                            Text(
                                text = "🛡️ Off-Hand",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                            selectedLabelColor = GoldAccent,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isOffHand,
                            selectedBorderColor = GoldAccent,
                            borderColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Choose an equipped item for your ${if (isOffHand) "off-hand" else "main hand"}:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                if (equippedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No equipped items available.\nEquip an item from the Equipment tab first.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(equippedItems) { item ->
                            val isSelected = item.id == currentSlotWeaponId
                            val isInMainHand = item.id == character.onHandWeaponId
                            val isInOffHand = item.id == character.offHandWeaponId
                            Surface(
                                onClick = { onSelectWeapon(isOffHand, item) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) GoldAccent.copy(alpha = 0.15f) else DarkSurfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) GoldAccent else Color.Transparent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isSelected) GoldAccent else TextPrimary
                                            )
                                            if (isInMainHand) {
                                                Surface(
                                                    color = GoldAccent.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "MAIN",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = GoldAccent,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            if (isInOffHand) {
                                                Surface(
                                                    color = CyanAccent.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "OFF",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = CyanAccent,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = item.category.ifBlank { "Item" },
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                            if (item.hasAttacks) {
                                                Text(
                                                    text = "• ${item.attackCount}×(${if (item.attackDamage.isNotBlank()) item.attackDamage else "Base"})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldAccent
                                                )
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { onSelectWeapon(isOffHand, null) }) {
                        Text("Clear ${if (isOffHand) "Off-Hand" else "Main Hand"}", color = CrimsonPrimary, fontSize = 12.sp)
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text("Close", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

