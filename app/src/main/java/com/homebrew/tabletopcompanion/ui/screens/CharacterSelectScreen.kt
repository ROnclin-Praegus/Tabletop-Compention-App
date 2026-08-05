package com.homebrew.tabletopcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import java.util.UUID
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSelectScreen(
    characters: List<Character>,
    onSelectCharacter: (Character) -> Unit,
    onCreateCharacter: (Character) -> Unit,
    onUpdateCharacter: (Character) -> Unit,
    onDeleteCharacter: (String) -> Unit,
    onCheckForUpdates: () -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var characterToEdit by remember { mutableStateOf<Character?>(null) }
    var characterToDelete by remember { mutableStateOf<Character?>(null) }
    var characterToExport by remember { mutableStateOf<Character?>(null) }

    val context = LocalContext.current
    val gson = remember { Gson() }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { safeUri ->
            characterToExport?.let { char ->
                try {
                    context.contentResolver.openOutputStream(safeUri)?.use { out ->
                        out.write(gson.toJson(char).toByteArray())
                    }
                    Toast.makeText(context, "Character exported successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to export character", Toast.LENGTH_SHORT).show()
                }
            }
        }
        characterToExport = null
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { safeUri ->
            try {
                val json = context.contentResolver.openInputStream(safeUri)?.bufferedReader().use { it?.readText() }
                if (json != null) {
                    val importedChar = gson.fromJson(json, Character::class.java)
                    if (importedChar != null && importedChar.name.isNotBlank()) {
                        val newChar = importedChar.copy(id = UUID.randomUUID().toString())
                        onCreateCharacter(newChar)
                        Toast.makeText(context, "Character imported successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid character file", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import character", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "HOMEBREW RPG",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Character Selection",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { importLauncher.launch("application/json") }) {
                        Text(
                            text = "📥 IMPORT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GoldAccent
                        )
                    }
                    val versionName = try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
                    } catch (e: Exception) {
                        "Unknown"
                    }
                    TextButton(onClick = onCheckForUpdates) {
                        Text(
                            text = "v$versionName 🔄",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GoldAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Create Hero") },
                text = { Text("CREATE HERO", fontWeight = FontWeight.Bold) },
                containerColor = GoldAccent,
                contentColor = DarkBackground
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (characters.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No Heroes Created Yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap '+ CREATE HERO' below to forge your first hero!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(characters, key = { it.id }) { character ->
                        CharacterCard(
                            character = character,
                            onUse = { onSelectCharacter(character) },
                            onEdit = { characterToEdit = character },
                            onDelete = { characterToDelete = character },
                            onDuplicate = {
                                val clone = character.copy(
                                    id = UUID.randomUUID().toString(),
                                    name = character.name + " (Copy)"
                                )
                                onCreateCharacter(clone)
                            },
                            onExport = {
                                characterToExport = character
                                exportLauncher.launch("${character.name}.json")
                            }
                        )
                    }
                }
            }
        }
    }

    // Create Hero Dialog
    if (showCreateDialog) {
        CreateCharacterDialog(
            characterToEdit = null,
            onDismiss = { showCreateDialog = false },
            onCharacterCreated = { newChar ->
                onCreateCharacter(newChar)
                showCreateDialog = false
            }
        )
    }

    // Edit Hero Dialog
    characterToEdit?.let { charToEdit ->
        CreateCharacterDialog(
            characterToEdit = charToEdit,
            onDismiss = { characterToEdit = null },
            onCharacterCreated = { updatedChar ->
                onUpdateCharacter(updatedChar)
                characterToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    characterToDelete?.let { charToDelete ->
        AlertDialog(
            onDismissRequest = { characterToDelete = null },
            title = { Text("Delete Hero", fontWeight = FontWeight.Bold, color = CrimsonPrimary) },
            text = { Text("Are you sure you want to delete ${charToDelete.name}? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCharacter(charToDelete.id)
                        characterToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { characterToDelete = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun CharacterCard(
    character: Character,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Avatar + Name & Race + Edit Pencil Icon + Delete Trashcan
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(CrimsonPrimary.copy(alpha = 0.2f))
                        .border(2.dp, GoldAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!character.imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = character.imageUri,
                            contentDescription = character.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = character.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = character.race,
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•  🪙 ${character.gold}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }

                IconButton(onClick = onExport, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export Character",
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate Character",
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Character",
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Character",
                        tint = CrimsonPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Vitals (HP Bar + Boost HP tag & optional MP Bar)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // HP Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("HP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenHp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${character.currentHp} / ${character.maxHp}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (character.boostHp > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = GoldAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                                ) {
                                    Text(
                                        text = "+${character.boostHp} Boost",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (character.currentHp.toFloat() / character.maxHp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (character.boostHp > 0) GoldAccent else GreenHp,
                        trackColor = DarkSurfaceVariant
                    )
                }

                // MP Bar (Only shown if character uses MP and maxMp > 0)
                if (!character.hideMp && character.maxMp > 0) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("MP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BlueMp)
                            Text(
                                text = "${character.currentMp} / ${character.maxMp}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (character.currentMp.toFloat() / character.maxMp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = BlueMp,
                            trackColor = DarkSurfaceVariant
                        )
                    }
                }
            }

            // Stats Badges Row with equipped stat modifiers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatBadge(label = "STR", value = character.strength, modifierVal = character.getStatModifier("strength"), modifier = Modifier.weight(1f))
                StatBadge(label = "INT", value = character.intelligence, modifierVal = character.getStatModifier("intelligence"), modifier = Modifier.weight(1f))
                StatBadge(label = "END", value = character.endurance, modifierVal = character.getStatModifier("endurance"), modifier = Modifier.weight(1f))
                StatBadge(label = "SPI", value = character.spirit, modifierVal = character.getStatModifier("spirit"), modifier = Modifier.weight(1f))
                StatBadge(label = "FIN", value = character.finesse, modifierVal = character.getStatModifier("finesse"), modifier = Modifier.weight(1f))
                StatBadge(label = "CHA", value = character.charisma, modifierVal = character.getStatModifier("charisma"), modifier = Modifier.weight(1f))
            }

            // Action: USE HERO
            Button(
                onClick = onUse,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GoldAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("USE HERO", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatBadge(
    label: String,
    value: Int,
    modifierVal: Int = 0,
    modifier: Modifier = Modifier
) {
    var showBaseOnly by remember { mutableStateOf(false) }
    val total = value + modifierVal

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { showBaseOnly = !showBaseOnly },
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showBaseOnly) {
                    Text(
                        text = "$value",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                } else {
                    val numColor = when {
                        modifierVal > 0 -> GreenHp
                        modifierVal < 0 -> CrimsonPrimary
                        else -> GoldAccent
                    }
                    Text(
                        text = "$total",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = numColor
                    )
                    if (modifierVal != 0) {
                        val modStr = if (modifierVal > 0) "+$modifierVal" else "$modifierVal"
                        Text(
                            text = " ($modStr)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (modifierVal > 0) GreenHp else CrimsonPrimary
                        )
                    }
                }
            }
        }
    }
}
