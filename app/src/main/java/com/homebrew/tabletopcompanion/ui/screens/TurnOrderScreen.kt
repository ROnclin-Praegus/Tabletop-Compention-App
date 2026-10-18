package com.homebrew.tabletopcompanion.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homebrew.tabletopcompanion.model.BattleParticipant
import com.homebrew.tabletopcompanion.model.BattleStatus
import com.homebrew.tabletopcompanion.model.Character
import com.homebrew.tabletopcompanion.sync.TurnSyncManager
import com.homebrew.tabletopcompanion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnOrderDialog(
    activeCharacter: Character?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .padding(vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface
        ) {
            TurnOrderScreenContent(
                activeCharacter = activeCharacter,
                onClose = onDismiss
            )
        }
    }
}

@Composable
fun TurnOrderScreenContent(
    activeCharacter: Character?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val roomState by TurnSyncManager.roomState.collectAsState()
    val isConnected by TurnSyncManager.isConnected.collectAsState()
    val statusMessage by TurnSyncManager.statusMessage.collectAsState()

    var joinCodeInput by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf(activeCharacter?.name ?: "Hero") }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                color = DarkSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚔️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "TURN ORDER",
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (roomState != null) "Room: ${roomState?.roomCode}" else "Real-time Party Combat",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (roomState != null) {
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Room Code", roomState?.roomCode ?: ""))
                                    Toast.makeText(context, "Room Code copied: ${roomState?.roomCode}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = GoldAccent, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { TurnSyncManager.leaveRoom() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "Leave", tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                            }
                        }

                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Body Content
            if (roomState == null) {
                // Not in room: Show Host / Join
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚔️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Turn Order System",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sync turn order directly across players on Android and iOS (Web) with zero server setup.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Player Name Field
                    OutlinedTextField(
                        value = customNameInput,
                        onValueChange = { customNameInput = it },
                        label = { Text("Your Player / Hero Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(if (isTablet) 0.6f else 1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            focusedLabelColor = GoldAccent
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Host Button
                    Button(
                        onClick = {
                            TurnSyncManager.createRoom(
                                hostName = customNameInput.ifBlank { "Host" },
                                characterId = activeCharacter?.id,
                                currentHp = activeCharacter?.currentHp,
                                maxHp = activeCharacter?.maxHp
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkBackground),
                        modifier = Modifier
                            .fillMaxWidth(if (isTablet) 0.6f else 1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("HOST NEW BATTLE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(if (isTablet) 0.6f else 1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkSurfaceVariant)
                        Text("  OR JOIN BATTLE  ", fontSize = 11.sp, color = TextSecondary)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Join by Code
                    Row(
                        modifier = Modifier.fillMaxWidth(if (isTablet) 0.6f else 1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = joinCodeInput,
                            onValueChange = { if (it.length <= 4) joinCodeInput = it.uppercase() },
                            label = { Text("Room Code (e.g. ABCD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                focusedLabelColor = CyanAccent
                            )
                        )

                        Button(
                            onClick = {
                                if (joinCodeInput.isNotBlank()) {
                                    TurnSyncManager.joinRoom(
                                        code = joinCodeInput,
                                        playerName = customNameInput.ifBlank { "Player" },
                                        characterId = activeCharacter?.id,
                                        currentHp = activeCharacter?.currentHp,
                                        maxHp = activeCharacter?.maxHp
                                    )
                                }
                            },
                            enabled = joinCodeInput.length >= 3,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkBackground),
                            modifier = Modifier
                                .height(56.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Text("JOIN", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // In Room: Show Lobby or Battle
                val state = roomState!!

                if (state.status == BattleStatus.LOBBY) {
                    LobbyView(
                        state = state,
                        isTablet = isTablet,
                        onClaimSlot = { slot -> TurnSyncManager.claimSlot(slot) },
                        onStartBattle = { TurnSyncManager.startBattle() },
                        isHost = TurnSyncManager.isHost,
                        myClientId = TurnSyncManager.myClientId
                    )
                } else {
                    BattleActiveView(
                        state = state,
                        isTablet = isTablet,
                        isHost = TurnSyncManager.isHost,
                        myClientId = TurnSyncManager.myClientId,
                        onEndMyTurn = { TurnSyncManager.endMyTurn() },
                        onEndEnemiesTurn = { TurnSyncManager.endEnemiesTurn() },
                        onForceNext = { TurnSyncManager.forceNextTurn() },
                        onForcePrev = { TurnSyncManager.forcePrevTurn() },
                        onResetBattle = { TurnSyncManager.resetBattle() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LobbyView(
    state: com.homebrew.tabletopcompanion.model.BattleRoomState,
    isTablet: Boolean,
    onClaimSlot: (Int) -> Unit,
    onStartBattle: () -> Unit,
    isHost: Boolean,
    myClientId: String
) {
    val players = state.playerParticipants
    val maxSlot = maxOf(3, state.maxPlayerSlot + 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Room Code Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ROOM CODE", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(
                    text = state.roomCode,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldAccent,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "Share this 4-letter code with other players to join.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "DECLARE YOUR TURN ORDER",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = GoldAccent
        )
        Text(
            text = "No initiative rolls! Claim when you want to act (1st, 2nd, 3rd...). After the last player, it is always the Enemies' Turn.",
            fontSize = 11.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items((1..maxSlot).toList()) { slotNum ->
                val participant = players.find { it.slot == slotNum }
                val isMe = participant?.id == myClientId

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMe) GoldAccent.copy(alpha = 0.15f) else DarkSurfaceVariant
                    ),
                    border = if (isMe) androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (participant != null) GoldAccent else TextSecondary.copy(alpha = 0.2f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$slotNum",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (participant != null) DarkBackground else TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = getSlotLabel(slotNum),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = participant?.name ?: "[Vacant Slot]",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (participant != null) TextPrimary else TextSecondary
                                )
                            }
                        }

                        if (participant == null || !isMe) {
                            Button(
                                onClick = { onClaimSlot(slotNum) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (participant == null) GoldAccent else DarkSurface,
                                    contentColor = if (participant == null) DarkBackground else GoldAccent
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(if (participant == null) "Claim Slot" else "Swap", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GoldAccent.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "YOU",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Always pinned: Enemies' Turn
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonPrimary.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👹", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("FINAL SLOT (AUTOMATIC)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                            Text("Enemies' Turn", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Takes action after the last player acts each round.", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isHost) {
            Button(
                onClick = onStartBattle,
                colors = ButtonDefaults.buttonColors(containerColor = GreenHp, contentColor = DarkBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("START BATTLE (ROUND 1)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Waiting for Host to start combat...",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun BattleActiveView(
    state: com.homebrew.tabletopcompanion.model.BattleRoomState,
    isTablet: Boolean,
    isHost: Boolean,
    myClientId: String,
    onEndMyTurn: () -> Unit,
    onEndEnemiesTurn: () -> Unit,
    onForceNext: () -> Unit,
    onForcePrev: () -> Unit,
    onResetBattle: () -> Unit
) {
    val players = state.playerParticipants
    val activeParticipant = state.getActiveParticipant()
    val isMyTurn = state.status == BattleStatus.BATTLE && activeParticipant?.id == myClientId
    val isEnemiesTurn = state.status == BattleStatus.ENEMIES

    // Pulsing animation for active turn
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Round & Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = GoldAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                ) {
                    Text(
                        text = "ROUND ${state.round}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEnemiesTurn) "Enemies Phase" else "Player Phase",
                    fontSize = 12.sp,
                    color = if (isEnemiesTurn) CrimsonPrimary else GreenHp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isHost) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onForcePrev, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onForceNext, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Active Turn Action Banner
        if (isMyTurn) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pulseScale),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(2.dp, GoldAccent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔥 IT'S YOUR TURN! 🔥", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Take your actions, roll dice or cast spells, then tap End Turn.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onEndMyTurn,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenHp, contentColor = DarkBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("END MY TURN", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
            }
        } else if (isEnemiesTurn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CrimsonPrimary.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(2.dp, CrimsonPrimary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👹 ENEMIES' TURN!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CrimsonPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All players have taken their turns. The enemies take their actions now.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onEndEnemiesTurn,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.NavigateNext, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("END ENEMIES' TURN & START ROUND ${state.round + 1}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        } else {
            // Waiting for someone else's turn
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = GoldAccent
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Currently Acting: ${activeParticipant?.name ?: "Unknown"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GoldAccent
                        )
                        Text(
                            text = "Slot ${state.activeSlot} • Please wait for your turn.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Sequential Turn Queue
        Text(
            text = "TURN SEQUENCE",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = GoldAccent
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(players) { p ->
                val isSlotActive = state.status == BattleStatus.BATTLE && state.activeSlot == p.slot
                val hasAlreadyActed = state.status == BattleStatus.ENEMIES || (state.status == BattleStatus.BATTLE && state.activeSlot > p.slot)
                val isMe = p.id == myClientId

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isSlotActive -> GoldAccent.copy(alpha = 0.2f)
                            hasAlreadyActed -> DarkSurfaceVariant.copy(alpha = 0.5f)
                            else -> DarkSurfaceVariant
                        }
                    ),
                    border = if (isSlotActive) androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    isSlotActive -> GoldAccent
                                    hasAlreadyActed -> GreenHp.copy(alpha = 0.6f)
                                    else -> TextSecondary.copy(alpha = 0.25f)
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (hasAlreadyActed) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(14.dp))
                                    } else {
                                        Text(
                                            text = "${p.slot}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isSlotActive) DarkBackground else TextPrimary
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = p.name + if (isMe) " (You)" else "",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSlotActive) GoldAccent else TextPrimary
                                )
                                Text(
                                    text = getSlotLabel(p.slot),
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (isSlotActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GoldAccent
                            ) {
                                Text(
                                    text = "ACTING NOW",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkBackground,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Enemies' Turn Row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEnemiesTurn) CrimsonPrimary.copy(alpha = 0.25f) else DarkSurfaceVariant
                    ),
                    border = if (isEnemiesTurn) androidx.compose.foundation.BorderStroke(1.5.dp, CrimsonPrimary) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👹", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Enemies' Turn", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isEnemiesTurn) CrimsonPrimary else TextPrimary)
                                Text("Acts after all players", fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        if (isEnemiesTurn) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CrimsonPrimary
                            ) {
                                Text(
                                    text = "ACTING NOW",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Host controls
        if (isHost) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetBattle,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
                ) {
                    Text("Reorder Slots", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onForceNext,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("Skip / Next", fontSize = 11.sp)
                }
            }
        }
    }
}

private fun getSlotLabel(slot: Int): String = when (slot) {
    1 -> "1st to Act"
    2 -> "2nd to Act"
    3 -> "3rd to Act"
    else -> "${slot}th to Act"
}
