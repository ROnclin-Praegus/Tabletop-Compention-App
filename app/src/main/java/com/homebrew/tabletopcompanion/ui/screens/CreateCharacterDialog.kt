package com.homebrew.tabletopcompanion.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCharacterDialog(
    characterToEdit: Character? = null,
    onDismiss: () -> Unit,
    onCharacterCreated: (Character) -> Unit
) {
    var name by remember { mutableStateOf(characterToEdit?.name ?: "") }
    var race by remember { mutableStateOf(characterToEdit?.race ?: "") }
    var hideMp by remember { mutableStateOf(characterToEdit?.hideMp ?: false) }

    var selectedImageUri by remember {
        mutableStateOf<Uri?>(characterToEdit?.imageUri?.let { Uri.parse(it) })
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // STATS
    var str by remember { mutableIntStateOf(characterToEdit?.strength ?: 0) }
    var intStat by remember { mutableIntStateOf(characterToEdit?.intelligence ?: 0) }
    var end by remember { mutableIntStateOf(characterToEdit?.endurance ?: 0) }
    var spi by remember { mutableIntStateOf(characterToEdit?.spirit ?: 0) }
    var fin by remember { mutableIntStateOf(characterToEdit?.finesse ?: 0) }
    var cha by remember { mutableIntStateOf(characterToEdit?.charisma ?: 0) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (characterToEdit != null) "EDIT HERO" else "CREATE NEW HERO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = GoldAccent
                )

                Text(
                    text = "Configure picture, info and base stats",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Divider(color = DarkSurfaceVariant, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // PHOTO UPLOAD SECTION
                    Text(
                        text = "CHARACTER PICTURE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CrimsonPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .border(2.dp, GoldAccent, CircleShape)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Character Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Upload Picture",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Column {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (selectedImageUri != null) "CHANGE PICTURE" else "UPLOAD PICTURE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (selectedImageUri != null) {
                                TextButton(onClick = { selectedImageUri = null }) {
                                    Text("Remove Picture", color = CrimsonPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // General Info Header
                    Text(
                        text = "GENERAL INFO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CrimsonPrimary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("Character Name") },
                        placeholder = { Text("e.g. Thorin Oakenshield") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextSecondary
                        )
                    )

                    OutlinedTextField(
                        value = race,
                        onValueChange = { race = it; errorMessage = null },
                        label = { Text("Race") },
                        placeholder = { Text("e.g. Dwarf, Elf, Human, Orc") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextSecondary
                        )
                    )

                    // DISABLE MP CHECKBOX (Under Race field)
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hideMp = !hideMp }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = hideMp,
                                onCheckedChange = { hideMp = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CrimsonPrimary,
                                    uncheckedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Disable MP (Hide Mana Points)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Check this if the character is not allowed to use MP",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // DERIVED VITAL METRICS (Endurance = +10 HP, Intelligence = +1 MP, Spirit = MP recharge)
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MAX HP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                Text("${end * 10}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenHp)
                                Text("(${end} End × 10)", fontSize = 9.sp, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MAX MP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (hideMp) TextSecondary else BlueMp)
                                Text(if (hideMp) "OFF" else "${intStat}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (hideMp) TextSecondary else BlueMp)
                                Text("(${intStat} Int × 1)", fontSize = 9.sp, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MP RECHARGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                Text(if (hideMp) "OFF" else "+${spi}/turn", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (hideMp) TextSecondary else GoldAccent)
                                Text("(${spi} Spirit)", fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // STATS Header
                    Text(
                        text = "STATS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CrimsonPrimary
                    )

                    StatStepperItem(label = "Strength", value = str, onValueChange = { str = it })
                    StatStepperItem(label = "Intelligence", value = intStat, onValueChange = { intStat = it })
                    StatStepperItem(label = "Endurance", value = end, onValueChange = { end = it })
                    StatStepperItem(label = "Spirit", value = spi, onValueChange = { spi = it })
                    StatStepperItem(label = "Finesse", value = fin, onValueChange = { fin = it })
                    StatStepperItem(label = "Charisma", value = cha, onValueChange = { cha = it })

                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = CrimsonPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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
                                errorMessage = "Please enter a character name"
                                return@Button
                            }
                            if (race.isBlank()) {
                                errorMessage = "Please enter a race"
                                return@Button
                            }
                            val oldMaxHp = characterToEdit?.getEffectiveMaxHp() ?: 0
                            val oldMaxMp = characterToEdit?.getEffectiveMaxMp() ?: 0

                            val hp = end * 10
                            val mp = intStat * 1

                            val tempChar = Character(
                                id = characterToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                race = race.trim(),
                                maxHp = hp,
                                currentHp = characterToEdit?.currentHp ?: hp,
                                boostHp = characterToEdit?.boostHp ?: 0,
                                currentArmor = characterToEdit?.currentArmor,
                                currentWard = characterToEdit?.currentWard,
                                maxMp = mp,
                                currentMp = characterToEdit?.currentMp ?: mp,
                                hideMp = hideMp,
                                gold = characterToEdit?.gold ?: 0,
                                imageUri = selectedImageUri?.toString(),
                                strength = str,
                                intelligence = intStat,
                                endurance = end,
                                spirit = spi,
                                finesse = fin,
                                charisma = cha,
                                inventory = characterToEdit?.inventory ?: emptyList(),
                                effects = characterToEdit?.effects ?: emptyList()
                            )

                            val newMaxHp = tempChar.getEffectiveMaxHp()
                            val newMaxMp = tempChar.getEffectiveMaxMp()

                            val finalHp = if (characterToEdit != null) {
                                val hpDelta = newMaxHp - oldMaxHp
                                (characterToEdit.currentHp + hpDelta).coerceIn(0, newMaxHp)
                            } else hp

                            val finalMp = if (characterToEdit != null) {
                                val mpDelta = newMaxMp - oldMaxMp
                                (characterToEdit.currentMp + mpDelta).coerceIn(0, newMaxMp)
                            } else mp

                            val updatedChar = tempChar.copy(currentHp = finalHp, currentMp = finalMp)
                            onCharacterCreated(updatedChar)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                    ) {
                        Text(if (characterToEdit != null) "SAVE CHANGES" else "SAVE HERO", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatStepperItem(
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontWeight = FontWeight.SemiBold, color = TextPrimary)

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (value > 0) onValueChange(value - 1) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary)
                }

                Text(
                    text = "$value",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GoldAccent,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                IconButton(
                    onClick = { if (value < 99) onValueChange(value + 1) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextPrimary)
                }
            }
        }
    }
}
