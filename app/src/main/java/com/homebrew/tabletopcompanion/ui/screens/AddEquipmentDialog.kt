package com.homebrew.tabletopcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.homebrew.tabletopcompanion.model.EquipmentItem
import com.homebrew.tabletopcompanion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEquipmentDialog(
    initialIsEquipped: Boolean = true,
    itemToEdit: EquipmentItem? = null,
    onDismiss: () -> Unit,
    onItemSaved: (EquipmentItem) -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "Weapon") }
    var isEquipped by remember { mutableStateOf(itemToEdit?.isEquipped ?: initialIsEquipped) }
    var isConsumable by remember { mutableStateOf(itemToEdit?.isConsumable ?: false) }
    var consumableAmount by remember { mutableIntStateOf(itemToEdit?.consumableAmount ?: 1) }

    // Checkboxes
    var hasArmor by remember { mutableStateOf(itemToEdit?.let { it.hasArmor || it.armorSave > 0 } ?: false) }
    var armorSaveText by remember { mutableStateOf(itemToEdit?.armorSave?.toString() ?: "0") }

    var hasWard by remember { mutableStateOf(itemToEdit?.let { it.hasWard || it.wardSave > 0 } ?: false) }
    var wardSaveText by remember { mutableStateOf(itemToEdit?.wardSave?.toString() ?: "0") }

    var hasStatModifiers by remember { mutableStateOf(itemToEdit?.hasStatModifiers ?: (itemToEdit?.statModifiers?.isNotEmpty() == true)) }

    // Stat Modifiers (-5 to +5 or any integer)
    var modStr by remember { mutableIntStateOf(itemToEdit?.statModifiers?.get("strength") ?: 0) }
    var modInt by remember { mutableIntStateOf(itemToEdit?.statModifiers?.get("intelligence") ?: 0) }
    var modEnd by remember { mutableIntStateOf(itemToEdit?.statModifiers?.get("endurance") ?: 0) }
    var modSpi by remember { mutableIntStateOf(itemToEdit?.statModifiers?.get("spirit") ?: 0) }
    var modFin by remember { mutableIntStateOf(itemToEdit?.statModifiers?.get("finesse") ?: 0) }
    var modCha by remember { mutableIntStateOf(itemToEdit?.statModifiers?.get("charisma") ?: 0) }

    // Has Attacks?
    var hasAttacks by remember { mutableStateOf(itemToEdit?.hasAttacks ?: false) }
    var attackCountText by remember { mutableStateOf(itemToEdit?.attackCount?.toString() ?: "1") }
    var attackDamageText by remember { mutableStateOf(itemToEdit?.attackDamage ?: "") }

    // Special Effects?
    var hasSpecialEffects by remember { mutableStateOf(itemToEdit?.hasSpecialEffects ?: false) }
    var specialEffectsText by remember { mutableStateOf(itemToEdit?.specialEffectsText ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (itemToEdit != null) "EDIT ITEM" else if (initialIsEquipped) "ADD TO EQUIPMENT" else "ADD TO INVENTORY",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = GoldAccent
                )

                Text(
                    text = "Configure item saves, attacks, special effects, and properties",
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
                    // Item Name
                    OutlinedTextField(
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("Item Name") },
                        placeholder = { Text("e.g. Aegis Shield, Flame Sword") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextSecondary
                        )
                    )

                    // Category Selection (Freeform text field)
                    OutlinedTextField(
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category / Type (Freeform)") },
                        placeholder = { Text("e.g. Weapon, Armor, Shield, Potion, Scroll, Relic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextSecondary
                        )
                    )

                    // Equipped Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isEquipped) "Status: Equipped" else "Status: In Inventory", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Switch(
                            checked = isEquipped,
                            onCheckedChange = { isEquipped = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = GoldAccent.copy(alpha = 0.4f))
                        )
                    }
                    
                    if (!isEquipped) {
                        Divider(color = DarkSurfaceVariant, thickness = 1.dp)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isConsumable,
                                onCheckedChange = { isConsumable = it },
                                colors = CheckboxDefaults.colors(checkedColor = GoldAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Is Consumable?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldAccent)
                        }
                        
                        if (isConsumable) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(start = 12.dp)
                            ) {
                                Text("Amount:", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { if (consumableAmount > 1) consumableAmount-- }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary)
                                }
                                Text("$consumableAmount", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { consumableAmount++ }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextPrimary)
                                }
                            }
                        }
                    }

                    Divider(color = DarkSurfaceVariant, thickness = 1.dp)

                    // CHECKBOX 1: HAS ARMOR?
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasArmor,
                            onCheckedChange = { hasArmor = it },
                            colors = CheckboxDefaults.colors(checkedColor = GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Has Armor?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldAccent)
                    }

                    if (hasArmor) {
                        OutlinedTextField(
                            value = armorSaveText,
                            onValueChange = { armorSaveText = it.filter { c -> c.isDigit() } },
                            label = { Text("Armor Points") },
                            placeholder = { Text("e.g. 5, 10") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = TextSecondary
                            )
                        )
                    }

                    // CHECKBOX 2: HAS WARD?
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasWard,
                            onCheckedChange = { hasWard = it },
                            colors = CheckboxDefaults.colors(checkedColor = CyanAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Has Ward?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CyanAccent)
                    }

                    if (hasWard) {
                        OutlinedTextField(
                            value = wardSaveText,
                            onValueChange = { wardSaveText = it.filter { c -> c.isDigit() } },
                            label = { Text("Ward Points") },
                            placeholder = { Text("e.g. 3, 5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = TextSecondary
                            )
                        )
                    }

                    Divider(color = DarkSurfaceVariant, thickness = 1.dp)

                    // CHECKBOX 3: MODIFIES STATS?
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasStatModifiers,
                            onCheckedChange = { hasStatModifiers = it },
                            colors = CheckboxDefaults.colors(checkedColor = CrimsonPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Modifies Stats?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CrimsonPrimary)
                    }

                    if (hasStatModifiers) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("STAT MODIFIERS (+ or -)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            ModifierStepperItem(label = "Strength Modifier", value = modStr, onValueChange = { modStr = it })
                            ModifierStepperItem(label = "Intelligence Modifier", value = modInt, onValueChange = { modInt = it })
                            ModifierStepperItem(label = "Endurance Modifier", value = modEnd, onValueChange = { modEnd = it })
                            ModifierStepperItem(label = "Spirit Modifier", value = modSpi, onValueChange = { modSpi = it })
                            ModifierStepperItem(label = "Finesse Modifier", value = modFin, onValueChange = { modFin = it })
                            ModifierStepperItem(label = "Charisma Modifier", value = modCha, onValueChange = { modCha = it })
                        }
                    }

                    Divider(color = DarkSurfaceVariant, thickness = 1.dp)

                    // CHECKBOX 4: HAS ATTACKS?
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasAttacks,
                            onCheckedChange = { hasAttacks = it },
                            colors = CheckboxDefaults.colors(checkedColor = GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Has Attacks?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldAccent)
                    }

                    if (hasAttacks) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = attackCountText,
                                onValueChange = { attackCountText = it.filter { c -> c.isDigit() } },
                                label = { Text("Attacks Count") },
                                placeholder = { Text("e.g. 1, 2") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = TextSecondary
                                )
                            )
                            OutlinedTextField(
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                value = attackDamageText,
                                onValueChange = { attackDamageText = it },
                                label = { Text("Damage per Attack") },
                                placeholder = { Text("e.g. 1d8+2, 10") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = TextSecondary
                                )
                            )
                        }
                    }

                    // CHECKBOX 5: SPECIAL EFFECTS?
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasSpecialEffects,
                            onCheckedChange = { hasSpecialEffects = it },
                            colors = CheckboxDefaults.colors(checkedColor = CyanAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Special Effects?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CyanAccent)
                    }

                    if (hasSpecialEffects) {
                        OutlinedTextField(
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            value = specialEffectsText,
                            onValueChange = { specialEffectsText = it },
                            label = { Text("Special Effects Description (Freeform)") },
                            placeholder = { Text("e.g. Ignores resistance, grants +2 stealth...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = TextSecondary
                            )
                        )
                    }

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
                                errorMessage = "Please enter an item name"
                                return@Button
                            }

                            val rawArmorPts = armorSaveText.toIntOrNull() ?: 0
                            val finalHasArmor = hasArmor || rawArmorPts > 0
                            val armorPts = if (finalHasArmor) rawArmorPts else 0

                            val rawWardPts = wardSaveText.toIntOrNull() ?: 0
                            val finalHasWard = hasWard || rawWardPts > 0
                            val wardPts = if (finalHasWard) rawWardPts else 0

                            val statModMap = mutableMapOf<String, Int>()
                            if (hasStatModifiers) {
                                if (modStr != 0) statModMap["strength"] = modStr
                                if (modInt != 0) statModMap["intelligence"] = modInt
                                if (modEnd != 0) statModMap["endurance"] = modEnd
                                if (modSpi != 0) statModMap["spirit"] = modSpi
                                if (modFin != 0) statModMap["finesse"] = modFin
                                if (modCha != 0) statModMap["charisma"] = modCha
                            }

                            val atkCount = if (hasAttacks) (attackCountText.toIntOrNull() ?: 1) else 1
                            val atkDmg = if (hasAttacks) attackDamageText.trim() else ""
                            val specEffects = if (hasSpecialEffects) specialEffectsText.trim() else ""

                            val item = EquipmentItem(
                                id = itemToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                category = if (category.isBlank()) "Misc" else category.trim(),
                                isEquipped = isEquipped,
                                hasArmor = finalHasArmor,
                                armorSave = armorPts,
                                hasWard = finalHasWard,
                                wardSave = wardPts,
                                hasStatModifiers = hasStatModifiers,
                                statModifiers = statModMap,
                                hasAttacks = hasAttacks,
                                attackCount = atkCount,
                                attackDamage = atkDmg,
                                hasSpecialEffects = hasSpecialEffects,
                                specialEffectsText = specEffects,
                                isConsumable = if (!isEquipped) isConsumable else false,
                                consumableAmount = if (!isEquipped && isConsumable) consumableAmount else 1
                            )
                            onItemSaved(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                    ) {
                        Text(if (itemToEdit != null) "SAVE CHANGES" else "SAVE ITEM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ModifierStepperItem(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onValueChange(value - 1) },
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary, modifier = Modifier.size(16.dp))
                }

                val formattedVal = if (value > 0) "+$value" else "$value"
                Text(
                    text = formattedVal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (value > 0) GreenHp else if (value < 0) CrimsonPrimary else TextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                IconButton(
                    onClick = { onValueChange(value + 1) },
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
