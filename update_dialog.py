import re

with open('app/src/main/java/com/homebrew/tabletopcompanion/ui/screens/ApiConfigDialog.kt', 'r') as f:
    content = f.read()

content = content.replace('fun ApiConfigDialog(', 'fun ApiConfigDialog(\\n    onSave: (com.homebrew.tabletopcompanion.data.Character) -> Unit,\\n')
content = content.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\\nimport com.google.gson.Gson\\nimport com.homebrew.tabletopcompanion.data.Character\\nimport android.widget.Toast\\nimport androidx.compose.material.icons.Icons\\nimport androidx.compose.material.icons.filled.ExpandMore\\nimport androidx.compose.material.icons.filled.ExpandLess\\nimport androidx.compose.foundation.clickable')

card_replacement = '''
                        LazyColumn(modifier = Modifier.height(250.dp)) {
                            items(uploads) { upload ->
                                val gson = Gson()
                                val character = try {
                                    gson.fromJson(upload.jsonData, Character::class.java)
                                } catch (e: Exception) { null }
                                
                                var expanded by remember { mutableStateOf(false) }
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { expanded = !expanded },
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(character?.name ?: "Unknown Hero", fontWeight = FontWeight.Bold, color = TextPrimary)
                                                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                                Text("Received: ", color = GoldAccent, fontSize = 12.sp)
                                            }
                                            Icon(
                                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = TextSecondary
                                            )
                                        }
                                        
                                        if (expanded) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        if (character != null) {
                                                            onSave(character)
                                                            Toast.makeText(context, " saved!", Toast.LENGTH_SHORT).show()
                                                            GameServerManager.uploads.remove(upload)
                                                            uploads = GameServerManager.uploads.toList()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = GreenHp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("SAVE TO APP", fontSize = 12.sp)
                                                }
                                                Button(
                                                    onClick = {
                                                        GameServerManager.uploads.remove(upload)
                                                        uploads = GameServerManager.uploads.toList()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("DELETE", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
'''

content = re.sub(r'LazyColumn\(modifier = Modifier\.height\(250\.dp\)\) \{[\s\S]*?\}\n                        \}', card_replacement.strip() + '\\n                        }', content)

with open('app/src/main/java/com/homebrew/tabletopcompanion/ui/screens/ApiConfigDialog.kt', 'w') as f:
    f.write(content)
print("done")
