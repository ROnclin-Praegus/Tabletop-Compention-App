package com.homebrew.tabletopcompanion.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.homebrew.tabletopcompanion.data.SkillsRepository
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.model.ImportedSkill
import com.homebrew.tabletopcompanion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseSkillDialog(
    character: Character,
    onDismiss: () -> Unit,
    onSkillSelected: (ImportedSkill) -> Unit
) {
    val context = LocalContext.current
    val allSkills = remember { SkillsRepository.getSkills(context) }
    val categories = remember {
        listOf(
            "Weapon & Combat",
            "Professions",
            "Utility",
            "Magic",
            "Faith",
            "Advanced combat moves",
            "Other advanced skills"
        )
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var parentSkillForNotice by remember { mutableStateOf<ImportedSkill?>(null) }

    val filteredSkills = remember(selectedTabIndex, searchQuery, allSkills) {
        val query = searchQuery.trim().lowercase()
        if (query.isNotEmpty()) {
            allSkills.filter {
                it.name.lowercase().contains(query) || it.description.lowercase().contains(query)
            }
        } else {
            val currentCategory = categories.getOrElse(selectedTabIndex) { categories.first() }
            allSkills.filter { it.category == currentCategory }
        }
    }

    if (parentSkillForNotice != null) {
        val pSkill = parentSkillForNotice!!
        val subSpells = remember(pSkill.name) { SkillsRepository.getSubSpellsForParent(pSkill.name) }
        AlertDialog(
            onDismissRequest = { parentSkillForNotice = null },
            title = {
                Text(
                    text = "Parent Skill Notice",
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Selecting '${pSkill.name}' will also automatically add the following ${subSpells.size} sub-spells:",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            subSpells.forEach { sub ->
                                Text(
                                    text = "• ${sub.name}",
                                    color = GoldAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Text(
                        text = "They will be grouped together and can be collapsed on your abilities screen.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = pSkill
                        parentSkillForNotice = null
                        Toast.makeText(
                            context,
                            "Selected '${s.name}' and sub-spells",
                            Toast.LENGTH_SHORT
                        ).show()
                        onSkillSelected(s)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = DarkBackground
                    )
                ) {
                    Text("ADD SKILL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { parentSkillForNotice = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CHOOSE FROM SKILLS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = GoldAccent
                        )
                        Text(
                            text = "Select a skill to copy its name & description",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search all skills & specializations...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = DarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Tabs (Only shown if not searching)
                if (searchQuery.isEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = DarkSurface,
                        contentColor = GoldAccent,
                        edgePadding = 0.dp,
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = GoldAccent
                                )
                            }
                        },
                        divider = {
                            HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
                        }
                    ) {
                        categories.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (selectedTabIndex == index) GoldAccent else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(
                        text = "Search results (${filteredSkills.size}):",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Skills List
                if (filteredSkills.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No skills matching '$searchQuery'" else "No skills found",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSkills, key = { it.name + "_" + it.category }) { skill ->
                            val alreadyAdded = SkillsRepository.isSkillAlreadyAdded(skill, character)
                            val (unlocked, missingReason) = SkillsRepository.isSkillUnlocked(skill, character)
                            val isSelectable = unlocked && !alreadyAdded
                            val subSpells = remember(skill.name) { SkillsRepository.getSubSpellsForParent(skill.name) }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (alreadyAdded) {
                                            Toast
                                                .makeText(
                                                    context,
                                                    "${skill.name} is already added",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        } else if (unlocked) {
                                            if (subSpells.isNotEmpty()) {
                                                parentSkillForNotice = skill
                                            } else {
                                                onSkillSelected(skill)
                                            }
                                        } else {
                                            Toast
                                                .makeText(
                                                    context,
                                                    missingReason ?: "Requirements not met",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        }
                                    }
                                    .alpha(if (isSelectable) 1.0f else if (alreadyAdded) 0.65f else 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelectable) DarkSurfaceVariant else DarkBackground
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = when {
                                        alreadyAdded -> GreenHp.copy(alpha = 0.5f)
                                        unlocked -> GoldAccent.copy(alpha = 0.35f)
                                        else -> Color.Red.copy(alpha = 0.3f)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    // Header: Skill name and compact badges
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = skill.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelectable) GoldAccent else TextSecondary,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        if (alreadyAdded) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = GreenHp.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(1.dp, GreenHp.copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Already Added",
                                                        tint = GreenHp,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "Added",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GreenHp
                                                    )
                                                }
                                            }
                                        } else if (subSpells.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = GoldAccent.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    text = "Parent Skill",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldAccent,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Dedicated lines for requirements / locked status
                                    if (!alreadyAdded) {
                                        if (!unlocked) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Locked",
                                                    tint = CrimsonPrimary,
                                                    modifier = Modifier
                                                        .size(13.dp)
                                                        .padding(top = 1.dp)
                                                )
                                                Text(
                                                    text = missingReason ?: skill.reqText,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = CrimsonPrimary,
                                                    lineHeight = 15.sp,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        } else if (skill.reqText.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Requirement met",
                                                    tint = GreenHp.copy(alpha = 0.8f),
                                                    modifier = Modifier
                                                        .size(13.dp)
                                                        .padding(top = 1.dp)
                                                )
                                                Text(
                                                    text = "Req: ${skill.reqText}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = GreenHp.copy(alpha = 0.9f),
                                                    lineHeight = 15.sp,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        if (subSpells.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "Includes ${subSpells.size} sub-spells: ${subSpells.joinToString { it.name.replace("Reaction Spell: ", "") }}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = GoldAccent.copy(alpha = 0.85f),
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = skill.description,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        lineHeight = 16.sp
                                    )

                                    if (searchQuery.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Category: ${skill.category}",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
