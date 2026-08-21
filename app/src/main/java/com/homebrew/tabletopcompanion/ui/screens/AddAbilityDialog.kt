package com.homebrew.tabletopcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.model.CharacterAbility
import com.homebrew.tabletopcompanion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAbilityDialog(
    character: Character,
    abilityToEdit: CharacterAbility? = null,
    onDismiss: () -> Unit,
    onAbilitySaved: (CharacterAbility) -> Unit
) {
    var name by remember { mutableStateOf(abilityToEdit?.name ?: "") }
    var description by remember { mutableStateOf(abilityToEdit?.description ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val statTags = listOf("{STR}", "{INT}", "{END}", "{SPI}", "{FIN}", "{CHA}")

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
                    text = if (abilityToEdit != null) "EDIT ABILITY" else "ADD NEW ABILITY",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = GoldAccent
                )

                Text(
                    text = "Add stat tags like {STR}, {INT}, {END} to automatically calculate total values!",
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
                    // Ability Name
                    OutlinedTextField(
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("Ability Name") },
                        placeholder = { Text("e.g. Fireball, Whirlwind Strike, Holy Heal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextSecondary
                        )
                    )

                    // Quick Stat Tag Insertion Chips
                    Text("QUICK STAT FORMULA TAGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statTags.take(3).forEach { tag ->
                            SuggestionChip(
                                onClick = { description += tag },
                                label = { Text("+ $tag", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = GoldAccent
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statTags.drop(3).forEach { tag ->
                            SuggestionChip(
                                onClick = { description += tag },
                                label = { Text("+ $tag", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = GoldAccent
                                )
                            )
                        }
                    }

                    // Ability Description
                    OutlinedTextField(
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Formula") },
                        placeholder = { Text("e.g. Deal {STR}+10+1d10 damage to target. Restores {SPI} HP.") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextSecondary
                        )
                    )

                    // Live Evaluated Preview Box
                    Text("EVALUATED PREVIEW (OVERVIEW)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val evaluated = character.formatAbilityDescription(description)
                            Text(
                                text = if (evaluated.isBlank()) "Preview will appear here as you type..." else evaluated,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (evaluated.isBlank()) TextSecondary else TextPrimary
                            )
                        }
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
                                errorMessage = "Please enter an ability name"
                                return@Button
                            }

                            val ability = CharacterAbility(
                                id = abilityToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                description = description.trim()
                            )
                            onAbilitySaved(ability)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                    ) {
                        Text(if (abilityToEdit != null) "SAVE CHANGES" else "SAVE ABILITY", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
