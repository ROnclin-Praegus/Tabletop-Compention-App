package com.homebrew.tabletopcompanion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.homebrew.tabletopcompanion.model.CharacterNote
import com.homebrew.tabletopcompanion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteDialog(
    noteToEdit: CharacterNote? = null,
    onDismiss: () -> Unit,
    onNoteSaved: (CharacterNote) -> Unit
) {
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var tag by remember { mutableStateOf(noteToEdit?.tag ?: "") }
    var content by remember { mutableStateOf(noteToEdit?.content ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val suggestedTags = listOf("Quest", "NPC", "Lore", "Loot", "Secret", "Combat")

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
                    text = if (noteToEdit != null) "EDIT NOTE" else "ADD NEW NOTE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = GoldAccent
                )

                Text(
                    text = "Create quick notes with titles and custom tags for quests, lore, or secrets.",
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
                    // Note Title
                    OutlinedTextField(
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        value = title,
                        onValueChange = { title = it; errorMessage = null },
                        label = { Text("Note Title") },
                        placeholder = { Text("e.g. Goblin King's Secret, Ancient Scroll Clue") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextSecondary
                        )
                    )

                    // Note Tag
                    OutlinedTextField(
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        value = tag,
                        onValueChange = { tag = it },
                        label = { Text("Tag / Category") },
                        placeholder = { Text("e.g. Quest, NPC, Lore, Secret") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextSecondary
                        )
                    )

                    // Suggested Tag Chips
                    Text("SUGGESTED TAGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedTags.take(3).forEach { tagChip ->
                            SuggestionChip(
                                onClick = { tag = tagChip },
                                label = { Text(tagChip, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
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
                        suggestedTags.drop(3).forEach { tagChip ->
                            SuggestionChip(
                                onClick = { tag = tagChip },
                                label = { Text(tagChip, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = GoldAccent
                                )
                            )
                        }
                    }

                    // Note Content / Body
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                        label = { Text("Note Details & Content") },
                        placeholder = { Text("Write your notes here...") },
                        minLines = 4,
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
                            if (title.isBlank()) {
                                errorMessage = "Please enter a note title"
                                return@Button
                            }

                            val note = CharacterNote(
                                id = noteToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                                title = title.trim(),
                                tag = tag.trim(),
                                content = content.trim(),
                                isArchived = noteToEdit?.isArchived ?: false
                            )
                            onNoteSaved(note)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground)
                    ) {
                        Text(if (noteToEdit != null) "SAVE CHANGES" else "SAVE NOTE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
