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
fun AbilityCard(
    ability: CharacterAbility,
    evaluatedDescription: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isSubAbility: Boolean = false,
    subSpellsCount: Int = 0,
    isSubSpellsCollapsed: Boolean = false,
    onToggleSubSpellsCollapse: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeletePrompt by remember { mutableStateOf(false) }
    val maxCharLimit = 130
    val isLong = evaluatedDescription.length > maxCharLimit

    val displayText = if (isLong && !isExpanded) {
        evaluatedDescription.take(maxCharLimit) + "..."
    } else {
        evaluatedDescription
    }

    if (showDeletePrompt) {
        AlertDialog(
            onDismissRequest = { showDeletePrompt = false },
            title = { Text("Delete Ability?", fontWeight = FontWeight.Bold, color = CrimsonPrimary) },
            text = { Text("Are you sure you want to delete ${ability.name}?") },
            confirmButton = {
                Button(
                    onClick = { showDeletePrompt = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary, contentColor = TextPrimary)
                ) { Text("DELETE", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePrompt = false }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSubAbility) DarkSurfaceVariant.copy(alpha = 0.85f) else DarkSurface
        ),
        border = if (isSubAbility) androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.25f)) else null
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSubAbility) {
                        Text(
                            text = "↳",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GoldAccent.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = ability.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isSubAbility) 15.sp else 16.sp,
                        color = if (isSubAbility) GoldAccent.copy(alpha = 0.9f) else GoldAccent
                    )
                    if (ability.tier != null) {
                        Surface(
                            color = GoldAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Tier ${ability.tier}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
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
                        Icon(Icons.Default.Edit, contentDescription = "Edit Ability", tint = GoldAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showDeletePrompt = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Ability", tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (evaluatedDescription.isNotBlank()) {
                Surface(
                    color = if (isSubAbility) DarkSurface else DarkSurfaceVariant,
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
                    }
                }
            }

            if (subSpellsCount > 0) {
                Surface(
                    color = DarkSurfaceVariant.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleSubSpellsCollapse?.invoke() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSubSpellsCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isSubSpellsCollapsed) "Expand Sub-spells" else "Collapse Sub-spells",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isSubSpellsCollapsed) "$subSpellsCount Sub-spells (Tap to expand)" else "$subSpellsCount Sub-spells (Tap to collapse)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                        Text(
                            text = if (isSubSpellsCollapsed) "COLLAPSED" else "EXPANDED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSubSpellsCollapsed) TextSecondary else GreenHp
                        )
                    }
                }
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
    var showDeletePrompt by remember { mutableStateOf(false) }
    val maxCharLimit = 130
    val isLong = note.content.length > maxCharLimit

    val displayText = if (isLong && !isExpanded) {
        note.content.take(maxCharLimit) + "..."
    } else {
        note.content
    }

    if (showDeletePrompt) {
        AlertDialog(
            onDismissRequest = { showDeletePrompt = false },
            title = { Text("Delete Note?", fontWeight = FontWeight.Bold, color = CrimsonPrimary) },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                Button(
                    onClick = { showDeletePrompt = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary, contentColor = TextPrimary)
                ) { Text("DELETE", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePrompt = false }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
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
                    IconButton(onClick = { showDeletePrompt = true }, modifier = Modifier.size(32.dp)) {
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

