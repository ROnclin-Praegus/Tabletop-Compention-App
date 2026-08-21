package com.homebrew.tabletopcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.homebrew.tabletopcompanion.model.CharacterEffect
import com.homebrew.tabletopcompanion.model.EffectType
import com.homebrew.tabletopcompanion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEffectDialog(
    onDismiss: () -> Unit,
    onEffectAdded: (CharacterEffect) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(EffectType.STAT_MODIFIER) }
    var selectedStat by remember { mutableStateOf("strength") }
    var selectedDamageType by remember { mutableStateOf(com.homebrew.tabletopcompanion.model.DamageType.UNSAVEABLE) }
    var valueAmount by remember { mutableIntStateOf(1) }
    var roundsText by remember { mutableStateOf("0") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val stats = listOf("strength", "intelligence", "endurance", "spirit", "finesse", "charisma")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "APPLY EFFECT / BUFF",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = GoldAccent
                )

                Text(
                    text = "Add stat modifiers or per-round HP/MP effects",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Divider(color = DarkSurfaceVariant, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Effect Name
                    OutlinedTextField(
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("Effect Name") },
                        placeholder = { Text("e.g. Poison, Berserk, Mage Shield") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextSecondary
                        )
                    )

                    // Effect Type Selection
                    Text("EFFECT TARGET TYPE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedType == EffectType.STAT_MODIFIER) GoldAccent.copy(alpha = 0.15f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedType == EffectType.STAT_MODIFIER,
                                onClick = { selectedType = EffectType.STAT_MODIFIER },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📊 Modifies Stat Score", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedType == EffectType.HP_CHANGE_PER_ROUND) GreenHp.copy(alpha = 0.15f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedType == EffectType.HP_CHANGE_PER_ROUND,
                                onClick = { selectedType = EffectType.HP_CHANGE_PER_ROUND },
                                colors = RadioButtonDefaults.colors(selectedColor = GreenHp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("❤️ HP per Round (Damage or Heal)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedType == EffectType.MP_CHANGE_PER_ROUND) BlueMp.copy(alpha = 0.15f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedType == EffectType.MP_CHANGE_PER_ROUND,
                                onClick = { selectedType = EffectType.MP_CHANGE_PER_ROUND },
                                colors = RadioButtonDefaults.colors(selectedColor = BlueMp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔷 MP per Round (Use or Recover)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    // If Stat Modifier: Pick Stat
                    if (selectedType == EffectType.STAT_MODIFIER) {
                        Text("TARGET STAT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            stats.take(3).forEach { st ->
                                FilterChip(
                                    selected = selectedStat == st,
                                    onClick = { selectedStat = st },
                                    label = { Text(st.take(3).uppercase(), fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldAccent,
                                        selectedLabelColor = DarkBackground
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            stats.drop(3).forEach { st ->
                                FilterChip(
                                    selected = selectedStat == st,
                                    onClick = { selectedStat = st },
                                    label = { Text(st.take(3).uppercase(), fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldAccent,
                                        selectedLabelColor = DarkBackground
                                    )
                                )
                            }
                        }
                    }

                    // VALUE AMOUNT (+ or -)
                    Text("EFFECT VALUE (+ OR -)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Amount", fontWeight = FontWeight.SemiBold, color = TextPrimary)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { valueAmount -= 1 },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary)
                                }

                                val formattedVal = if (valueAmount > 0) "+$valueAmount" else "$valueAmount"
                                Text(
                                    text = formattedVal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (valueAmount > 0) GreenHp else if (valueAmount < 0) CrimsonPrimary else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                IconButton(
                                    onClick = { valueAmount += 1 },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextPrimary)
                                }
                            }
                        }
                    }

                    // IF HP CHANGE PER ROUND & NEGATIVE: PICK DAMAGE TYPE
                    if (selectedType == EffectType.HP_CHANGE_PER_ROUND && valueAmount < 0) {
                        Text("DAMAGE TYPE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedDamageType == com.homebrew.tabletopcompanion.model.DamageType.PHYSICAL,
                                onClick = { selectedDamageType = com.homebrew.tabletopcompanion.model.DamageType.PHYSICAL },
                                label = { Text("🛡️ Physical", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccent,
                                    selectedLabelColor = DarkBackground
                                )
                            )
                            FilterChip(
                                selected = selectedDamageType == com.homebrew.tabletopcompanion.model.DamageType.MAGICAL,
                                onClick = { selectedDamageType = com.homebrew.tabletopcompanion.model.DamageType.MAGICAL },
                                label = { Text("✨ Magical", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanAccent,
                                    selectedLabelColor = DarkBackground
                                )
                            )
                            FilterChip(
                                selected = selectedDamageType == com.homebrew.tabletopcompanion.model.DamageType.UNSAVEABLE,
                                onClick = { selectedDamageType = com.homebrew.tabletopcompanion.model.DamageType.UNSAVEABLE },
                                label = { Text("💥 Unsaveable", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary,
                                    selectedLabelColor = TextPrimary
                                )
                            )
                        }
                    }

                    // DURATION IN ROUNDS
                    Text("DURATION IN ROUNDS (0 = UNLIMITED)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent)

                    OutlinedTextField(
                        value = roundsText,
                        onValueChange = { roundsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Rounds (0 = Permanent / Unlimited)") },
                        placeholder = { Text("0 for unlimited, or 1, 2, 3...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextSecondary
                        )
                    )

                    errorMessage?.let { msg ->
                        Text(text = msg, color = CrimsonPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Please enter an effect name"
                                return@Button
                            }

                            val rounds = roundsText.toIntOrNull() ?: 0

                            val effect = CharacterEffect(
                                name = name.trim(),
                                effectType = selectedType,
                                targetStat = if (selectedType == EffectType.STAT_MODIFIER) selectedStat else null,
                                value = valueAmount,
                                roundsRemaining = rounds,
                                damageType = if (selectedType == EffectType.HP_CHANGE_PER_ROUND && valueAmount < 0) selectedDamageType else com.homebrew.tabletopcompanion.model.DamageType.UNSAVEABLE
                            )
                            onEffectAdded(effect)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                    ) {
                        Text("APPLY EFFECT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
