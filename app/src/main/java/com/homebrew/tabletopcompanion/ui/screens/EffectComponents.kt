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
fun EffectItemRow(
    effect: CharacterEffect,
    onToggleEnable: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeletePrompt by remember { mutableStateOf(false) }
    val isEffectActive = effect.enabled

    if (showDeletePrompt) {
        AlertDialog(
            onDismissRequest = { showDeletePrompt = false },
            title = { Text("Remove Effect?", fontWeight = FontWeight.Bold, color = CrimsonPrimary) },
            text = { Text("Are you sure you want to remove ${effect.safeName}?") },
            confirmButton = {
                Button(
                    onClick = { showDeletePrompt = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary, contentColor = TextPrimary)
                ) { Text("REMOVE", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePrompt = false }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }

    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isEffectActive) 1f else 0.55f }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = effect.safeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isEffectActive) TextPrimary else TextSecondary
                    )
                    if (!isEffectActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = CrimsonPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "DISABLED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CrimsonPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                val annotatedDesc = buildAnnotatedString {
                    when (effect.effectType) {
                        EffectType.STAT_MODIFIER -> {
                            val allMods = mutableMapOf<String, Int>()
                            if (effect.targetStat != null && effect.value != 0) {
                                allMods[effect.targetStat] = (allMods[effect.targetStat] ?: 0) + effect.value
                            }
                            effect.statModifiers?.forEach { (k, v) ->
                                allMods[k] = (allMods[k] ?: 0) + v
                            }
                            val entries = allMods.entries.toList()
                            entries.forEachIndexed { index, entry ->
                                val valStr = if (entry.value > 0) "+${entry.value}" else "${entry.value}"
                                val color = if (entry.value >= 0) GreenHp else CrimsonPrimary
                                withStyle(style = SpanStyle(color = color)) {
                                    append("${entry.key.uppercase()}: $valStr")
                                }
                                if (index < entries.size - 1) {
                                    withStyle(style = SpanStyle(color = TextSecondary)) {
                                        append(", ")
                                    }
                                }
                            }
                        }
                        EffectType.HP_CHANGE_PER_ROUND -> {
                            if (effect.isDiceRoll) {
                                val mode = effect.safeChangeMode
                                val color = when (mode) {
                                    "HEAL" -> GreenHp
                                    "DMG" -> CrimsonPrimary
                                    else -> CyanAccent
                                }
                                withStyle(style = SpanStyle(color = color)) {
                                    val typeLabel = if (mode != "HEAL") {
                                        when (effect.damageType) {
                                            DamageType.PHYSICAL -> " (Physical 🛡️)"
                                            DamageType.MAGICAL -> " (Magical ✨)"
                                            DamageType.UNSAVEABLE -> " (Unsaveable 💥)"
                                        }
                                    } else ""
                                    append("🎲 ${effect.safeDiceFormula} ($mode)$typeLabel HP / round")
                                }
                            } else {
                                val color = if (effect.value >= 0) GreenHp else CrimsonPrimary
                                withStyle(style = SpanStyle(color = color)) {
                                    val valStr = if (effect.value > 0) "+${effect.value}" else "${effect.value}"
                                    if (effect.value < 0) {
                                        val typeLabel = when (effect.damageType) {
                                            DamageType.PHYSICAL -> "Physical 🛡️"
                                            DamageType.MAGICAL -> "Magical ✨"
                                            DamageType.UNSAVEABLE -> "Unsaveable 💥"
                                        }
                                        append("$valStr $typeLabel HP / round")
                                    } else {
                                        append("$valStr HP / round")
                                    }
                                }
                            }
                        }
                        EffectType.MP_CHANGE_PER_ROUND -> {
                            if (effect.isDiceRoll) {
                                val mode = effect.safeChangeMode
                                val color = when (mode) {
                                    "HEAL" -> GreenHp
                                    "DMG" -> CrimsonPrimary
                                    else -> CyanAccent
                                }
                                withStyle(style = SpanStyle(color = color)) {
                                    append("🎲 ${effect.safeDiceFormula} ($mode) MP / round")
                                }
                            } else {
                                val color = if (effect.value >= 0) GreenHp else CrimsonPrimary
                                withStyle(style = SpanStyle(color = color)) {
                                    val valStr = if (effect.value > 0) "+${effect.value}" else "${effect.value}"
                                    append("$valStr MP / round")
                                }
                            }
                        }
                    }
                }

                Text(
                    text = annotatedDesc,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val durationText = if (effect.roundsRemaining == 0) "Duration: Unlimited" else "Duration: ${effect.roundsRemaining} Rounds left"
                Text(
                    text = durationText,
                    fontSize = 10.sp,
                    color = GoldAccent
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (effect.effectType == EffectType.STAT_MODIFIER && onToggleEnable != null) {
                    Switch(
                        checked = isEffectActive,
                        onCheckedChange = { onToggleEnable() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldAccent,
                            checkedTrackColor = GoldAccent.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurface
                        ),
                        modifier = Modifier
                            .graphicsLayer { scaleX = 0.8f; scaleY = 0.8f }
                            .padding(end = 4.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Effect",
                        tint = GoldAccent.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = { showDeletePrompt = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Effect",
                        tint = CrimsonPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}


@Composable
fun RollDiceRoundDialog(
    diceEffects: List<CharacterEffect>,
    onDismissRequest: () -> Unit,
    onConfirmRolls: (Map<String, Int>) -> Unit
) {
    val rollInputs = remember(diceEffects) {
        mutableStateMapOf<String, String>().apply {
            diceEffects.forEach { this[it.id] = "" }
        }
    }
    val modeInputs = remember(diceEffects) {
        mutableStateMapOf<String, String>().apply {
            diceEffects.forEach { this[it.id] = if (it.changeMode == "MIXED") "DMG" else it.changeMode }
        }
    }
    val allRollsFilled = diceEffects.all {
        val input = rollInputs[it.id]?.trim() ?: ""
        input.isNotEmpty() && input.toIntOrNull() != null
    }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🎲", fontSize = 18.sp)
                        Text(
                            text = "Roll Dice for Round Effects",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GoldAccent
                        )
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    text = "The following active effects require dice rolls this round. Roll your dice and input the results below:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(diceEffects) { effect ->
                        val targetTypeLabel = if (effect.effectType == EffectType.HP_CHANGE_PER_ROUND) "HP" else "MP"
                        val currentMode = modeInputs[effect.id] ?: "DMG"
                        val currentRoll = rollInputs[effect.id] ?: ""

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = effect.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        color = GoldAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "Roll ${effect.diceFormula}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (effect.changeMode == "MIXED") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilterChip(
                                            selected = currentMode == "DMG",
                                            onClick = { modeInputs[effect.id] = "DMG" },
                                            label = { Text("💥 Damage $targetTypeLabel", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CrimsonPrimary,
                                                selectedLabelColor = TextPrimary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = currentMode == "HEAL",
                                            onClick = { modeInputs[effect.id] = "HEAL" },
                                            label = { Text("💚 Heal $targetTypeLabel", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GreenHp,
                                                selectedLabelColor = DarkBackground
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (effect.changeMode == "HEAL") "💚 Heals / Restores $targetTypeLabel" else "💥 Deals Damage / Drains $targetTypeLabel",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (effect.changeMode == "HEAL") GreenHp else CrimsonPrimary
                                    )
                                }

                                OutlinedTextField(
                                    value = currentRoll,
                                    onValueChange = { input ->
                                        rollInputs[effect.id] = input.filter { it.isDigit() }
                                    },
                                    label = { Text("Rolled Result for ${effect.diceFormula}") },
                                    placeholder = { Text("e.g. 4") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldAccent,
                                        unfocusedBorderColor = TextSecondary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!allRollsFilled) {
                        Text(
                            text = "Please enter rolled results for all effects above to proceed.",
                            color = CrimsonPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val resultMap = mutableMapOf<String, Int>()
                                diceEffects.forEach { effect ->
                                    val roll = (rollInputs[effect.id]?.toIntOrNull() ?: 0)
                                    val mode = modeInputs[effect.id] ?: effect.changeMode
                                    val signed = if (mode == "DMG") -kotlin.math.abs(roll) else kotlin.math.abs(roll)
                                    resultMap[effect.id] = signed
                                }
                                onConfirmRolls(resultMap)
                            },
                            enabled = allRollsFilled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = DarkBackground,
                                disabledContainerColor = GoldAccent.copy(alpha = 0.3f),
                                disabledContentColor = DarkBackground.copy(alpha = 0.6f)
                            )
                        ) {
                            Text("Apply & Next Round", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

