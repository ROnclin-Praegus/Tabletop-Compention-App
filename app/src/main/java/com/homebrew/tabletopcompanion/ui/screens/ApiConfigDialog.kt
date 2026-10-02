package com.homebrew.tabletopcompanion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homebrew.tabletopcompanion.server.GameServerManager
import com.homebrew.tabletopcompanion.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isRunning by remember { mutableStateOf(GameServerManager.isRunning) }
    var apiKey by remember { mutableStateOf(GameServerManager.apiKey) }
    var selectedTab by remember { mutableStateOf(0) }
    val ipAddress = remember { GameServerManager.getLocalIpAddress(context) }
    
    // Auto-refresh uploads list when tab changes
    var uploads by remember(selectedTab) { mutableStateOf(GameServerManager.uploads.toList()) }

    LaunchedEffect(Unit) {
        GameServerManager.init(context)
        apiKey = GameServerManager.apiKey
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Local API Server", fontWeight = FontWeight.Bold, color = GoldAccent) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = GoldAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoldAccent
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Settings") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Uploads (${GameServerManager.uploads.size})") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    Text("Host a local API server to allow web users on your Wi-Fi network to sync characters.", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Status: ${if (isRunning) "RUNNING" else "STOPPED"}", fontWeight = FontWeight.Bold, color = if (isRunning) GreenHp else CrimsonPrimary)
                    if (isRunning) {
                        Text("Address: http://$ipAddress:8080/api/sync", color = GoldAccent, fontSize = 14.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("API Key:", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(apiKey, color = CyanAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                apiKey = GameServerManager.generateNewKey(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                        ) {
                            Text("Generate New Key", color = GoldAccent, fontSize = 12.sp)
                        }
                    }
                } else {
                    if (uploads.isEmpty()) {
                        Text("No characters uploaded yet.", color = TextSecondary, modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(modifier = Modifier.height(250.dp)) {
                            items(uploads) { upload ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        Text("Time: ${sdf.format(Date(upload.timestamp))}", color = GoldAccent, fontSize = 12.sp)
                                        Text("ID: ${upload.id}", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                if (isRunning) {
                    Button(
                        onClick = {
                            GameServerManager.stopServer()
                            isRunning = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Text("STOP SERVER")
                    }
                } else {
                    Button(
                        onClick = {
                            GameServerManager.startServer(context)
                            isRunning = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenHp)
                    ) {
                        Text("START SERVER")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = TextSecondary)
            }
        }
    )
}
