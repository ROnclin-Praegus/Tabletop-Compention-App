package com.homebrew.tabletopcompanion.sync

import android.content.Context
import com.google.gson.Gson
import com.homebrew.tabletopcompanion.model.BattleParticipant
import com.homebrew.tabletopcompanion.model.BattleRoomState
import com.homebrew.tabletopcompanion.model.BattleStatus
import com.homebrew.tabletopcompanion.server.GameServerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

object TurnSyncManager {
    val myClientId: String = "p-" + UUID.randomUUID().toString().take(6)

    var currentRoomCode: String? = null
        private set
    var isHost: Boolean = false
        private set
    var myCurrentParticipant: BattleParticipant? = null
        private set
    var hostAddress: String = ""
        private set

    private val _roomState = MutableStateFlow<BattleRoomState?>(null)
    val roomState: StateFlow<BattleRoomState?> = _roomState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _statusMessage = MutableStateFlow("Disconnected")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var clientPollJob: Job? = null
    private var hostCollectJob: Job? = null

    private val okHttpClient = OkHttpClient.Builder().build()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun createRoom(
        context: Context,
        hostName: String,
        characterId: String? = null,
        currentHp: Int? = null,
        maxHp: Int? = null
    ) {
        leaveRoom()
        isHost = true
        val localIp = GameServerManager.getLocalIpAddress(context)
        hostAddress = "$localIp:8080"

        val initialRoom = GameServerManager.createBattle(
            hostName = hostName,
            charId = characterId,
            hp = currentHp,
            maxHp = maxHp,
            context = context
        )

        val hostP = initialRoom.participants.firstOrNull()
        myCurrentParticipant = hostP
        currentRoomCode = initialRoom.roomCode
        _roomState.value = initialRoom
        _isConnected.value = true
        _statusMessage.value = "Hosting at $hostAddress"

        // Collect changes from GameServerManager.battleState
        hostCollectJob?.cancel()
        hostCollectJob = scope.launch {
            GameServerManager.battleState.collect { updated ->
                _roomState.value = updated
                val found = updated?.participants?.find { it.id == hostP?.id }
                if (found != null) {
                    myCurrentParticipant = found
                }
            }
        }
    }

    fun joinRoom(
        targetHostAddress: String,
        playerName: String,
        characterId: String? = null,
        currentHp: Int? = null,
        maxHp: Int? = null
    ) {
        leaveRoom()
        isHost = false
        var cleaned = targetHostAddress.trim()
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            cleaned = "http://$cleaned"
        }
        if (!cleaned.removePrefix("http://").removePrefix("https://").contains(":")) {
            cleaned = "$cleaned:8080"
        }
        hostAddress = cleaned

        val participant = BattleParticipant(
            id = myClientId,
            name = playerName.ifBlank { "Player" },
            characterId = characterId,
            slot = 1,
            isEnemy = false,
            currentHp = currentHp,
            maxHp = maxHp
        )
        myCurrentParticipant = participant
        _statusMessage.value = "Connecting to $hostAddress..."

        scope.launch {
            try {
                val actionPayload = mapOf(
                    "action" to "JOIN",
                    "payload" to participant
                )
                val body = gson.toJson(actionPayload).toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$hostAddress/api/battle/action")
                    .post(body)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val respJson = response.body?.string() ?: ""
                    val state = try { gson.fromJson(respJson, BattleRoomState::class.java) } catch(e: Exception) { null }
                    if (state != null) {
                        currentRoomCode = state.roomCode
                        _roomState.value = state
                        val me = state.participants.find { it.id == myClientId }
                        if (me != null) myCurrentParticipant = me
                        _isConnected.value = true
                        _statusMessage.value = "Connected to room ${state.roomCode}"
                        startClientPolling()
                    }
                } else {
                    _statusMessage.value = "Failed to join: HTTP ${response.code}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Connection error: ${e.message}"
            }
        }
    }

    private fun startClientPolling() {
        clientPollJob?.cancel()
        clientPollJob = scope.launch {
            while (isActive && !isHost && _isConnected.value) {
                delay(800)
                try {
                    val request = Request.Builder()
                        .url("$hostAddress/api/battle/state")
                        .get()
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val json = response.body?.string() ?: ""
                        if (json.isNotBlank() && json != "{}") {
                            val state = gson.fromJson(json, BattleRoomState::class.java)
                            if (state != null) {
                                _roomState.value = state
                                val me = state.participants.find { it.id == myClientId }
                                if (me != null) myCurrentParticipant = me
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Silently ignore momentary network hiccups
                }
            }
        }
    }

    fun claimSlot(newSlot: Int) {
        val currentP = myCurrentParticipant ?: return
        val updated = currentP.copy(slot = newSlot)
        myCurrentParticipant = updated

        if (isHost) {
            val payload = mapOf("playerId" to currentP.id, "slot" to newSlot)
            GameServerManager.handleBattleAction("CLAIM_SLOT", gson.toJson(payload))
        } else {
            sendClientAction("CLAIM_SLOT", mapOf("playerId" to currentP.id, "slot" to newSlot))
        }
    }

    fun startBattle() {
        if (isHost) {
            GameServerManager.handleBattleAction("START", "{}")
        } else {
            sendClientAction("START", emptyMap<String, Any>())
        }
    }

    fun endMyTurn() {
        val currentP = myCurrentParticipant ?: return
        if (isHost) {
            GameServerManager.handleBattleAction("END_TURN", gson.toJson(mapOf("playerId" to currentP.id)))
        } else {
            sendClientAction("END_TURN", mapOf("playerId" to currentP.id))
        }
    }

    fun endEnemiesTurn() {
        val currentP = myCurrentParticipant ?: return
        if (isHost) {
            GameServerManager.handleBattleAction("END_ENEMIES", gson.toJson(mapOf("playerId" to currentP.id)))
        } else {
            sendClientAction("END_ENEMIES", mapOf("playerId" to currentP.id))
        }
    }

    fun forceNextTurn() {
        if (isHost) {
            GameServerManager.handleBattleAction("NEXT", "{}")
        } else {
            sendClientAction("NEXT", emptyMap<String, Any>())
        }
    }

    fun forcePrevTurn() {
        if (isHost) {
            GameServerManager.handleBattleAction("PREV", "{}")
        } else {
            sendClientAction("PREV", emptyMap<String, Any>())
        }
    }

    fun resetBattle() {
        if (isHost) {
            GameServerManager.handleBattleAction("RESET", "{}")
        } else {
            sendClientAction("RESET", emptyMap<String, Any>())
        }
    }

    fun leaveRoom() {
        clientPollJob?.cancel()
        clientPollJob = null
        hostCollectJob?.cancel()
        hostCollectJob = null

        val pId = myCurrentParticipant?.id ?: myClientId
        if (isHost) {
            GameServerManager.leaveOrCloseBattle()
        } else if (_isConnected.value) {
            scope.launch {
                try {
                    val actionPayload = mapOf("action" to "LEAVE", "payload" to mapOf("playerId" to pId))
                    val body = gson.toJson(actionPayload).toRequestBody(jsonMediaType)
                    val request = Request.Builder().url("$hostAddress/api/battle/action").post(body).build()
                    okHttpClient.newCall(request).execute()
                } catch (e: Exception) {}
            }
        }

        currentRoomCode = null
        isHost = false
        myCurrentParticipant = null
        _roomState.value = null
        _isConnected.value = false
        _statusMessage.value = "Disconnected"
    }

    private fun sendClientAction(action: String, payload: Any) {
        scope.launch {
            try {
                val actionPayload = mapOf(
                    "action" to action,
                    "payload" to payload
                )
                val body = gson.toJson(actionPayload).toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$hostAddress/api/battle/action")
                    .post(body)
                    .build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val respJson = response.body?.string() ?: ""
                    val state = try { gson.fromJson(respJson, BattleRoomState::class.java) } catch(e: Exception) { null }
                    if (state != null) {
                        _roomState.value = state
                        val me = state.participants.find { it.id == myClientId }
                        if (me != null) myCurrentParticipant = me
                    }
                }
            } catch (e: Exception) {
                // Ignore network glitch
            }
        }
    }
}
