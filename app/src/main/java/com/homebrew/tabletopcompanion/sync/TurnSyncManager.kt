package com.homebrew.tabletopcompanion.sync

import com.google.gson.Gson
import com.homebrew.tabletopcompanion.model.BattleParticipant
import com.homebrew.tabletopcompanion.model.BattleRoomState
import com.homebrew.tabletopcompanion.model.BattleStatus
import com.homebrew.tabletopcompanion.model.TurnSyncEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

object TurnSyncManager {
    private const val BROKER_URL_TCP = "tcp://test.mosquitto.org:1883"
    private const val TOPIC_PREFIX = "tabletop-rpg/rooms/"

    private var mqttClient: MqttAsyncClient? = null
    val myClientId: String = "p-" + UUID.randomUUID().toString().take(6)

    var currentRoomCode: String? = null
        private set
    var isHost: Boolean = false
        private set
    var myCurrentParticipant: BattleParticipant? = null
        private set

    private val _roomState = MutableStateFlow<BattleRoomState?>(null)
    val roomState: StateFlow<BattleRoomState?> = _roomState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _statusMessage = MutableStateFlow("Disconnected")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    fun createRoom(hostName: String, characterId: String? = null, currentHp: Int? = null, maxHp: Int? = null) {
        val code = generateRoomCode()
        val initialParticipant = BattleParticipant(
            id = myClientId,
            name = hostName,
            characterId = characterId,
            slot = 1,
            isEnemy = false,
            currentHp = currentHp,
            maxHp = maxHp
        )
        val initialRoom = BattleRoomState(
            roomCode = code,
            hostId = myClientId,
            round = 1,
            activeSlot = 1,
            status = BattleStatus.LOBBY,
            participants = listOf(initialParticipant)
        )
        isHost = true
        myCurrentParticipant = initialParticipant
        currentRoomCode = code
        _roomState.value = initialRoom

        connectAndJoin(code) {
            broadcastSync()
        }
    }

    fun joinRoom(code: String, playerName: String, characterId: String? = null, currentHp: Int? = null, maxHp: Int? = null) {
        val cleanCode = code.trim().uppercase()
        val participant = BattleParticipant(
            id = myClientId,
            name = playerName,
            characterId = characterId,
            slot = 1,
            isEnemy = false,
            currentHp = currentHp,
            maxHp = maxHp
        )
        isHost = false
        myCurrentParticipant = participant
        currentRoomCode = cleanCode

        connectAndJoin(cleanCode) {
            // Send JOIN message to room so host picks us up
            sendEnvelope("JOIN", gson.toJson(participant))
        }
    }

    fun claimSlot(newSlot: Int) {
        val currentP = myCurrentParticipant ?: return
        val updated = currentP.copy(slot = newSlot)
        myCurrentParticipant = updated

        if (isHost) {
            val state = _roomState.value ?: return
            val updatedList = state.participants.map {
                if (it.id == myClientId) updated else it
            }
            _roomState.value = state.copy(participants = updatedList)
            broadcastSync()
        } else {
            sendEnvelope("CLAIM_SLOT", gson.toJson(mapOf("playerId" to myClientId, "slot" to newSlot)))
        }
    }

    fun startBattle() {
        if (!isHost) return
        val state = _roomState.value ?: return
        _roomState.value = state.copy(
            status = BattleStatus.BATTLE,
            round = 1,
            activeSlot = state.participants.filter { !it.isEnemy }.minOfOrNull { it.slot } ?: 1
        )
        broadcastSync()
    }

    fun endMyTurn() {
        if (isHost) {
            advanceTurnInternal()
        } else {
            sendEnvelope("END_TURN", gson.toJson(mapOf("playerId" to myClientId)))
        }
    }

    fun endEnemiesTurn() {
        if (isHost) {
            advanceEnemiesInternal()
        } else {
            sendEnvelope("END_ENEMIES", gson.toJson(mapOf("playerId" to myClientId)))
        }
    }

    fun forceNextTurn() {
        if (!isHost) return
        val state = _roomState.value ?: return
        if (state.status == BattleStatus.BATTLE) {
            advanceTurnInternal()
        } else if (state.status == BattleStatus.ENEMIES) {
            advanceEnemiesInternal()
        }
    }

    fun forcePrevTurn() {
        if (!isHost) return
        val state = _roomState.value ?: return
        val playerSlots = state.participants.filter { !it.isEnemy }.map { it.slot }.sorted()
        if (playerSlots.isEmpty()) return

        if (state.status == BattleStatus.ENEMIES) {
            _roomState.value = state.copy(
                status = BattleStatus.BATTLE,
                activeSlot = playerSlots.last()
            )
        } else {
            val currentIndex = playerSlots.indexOf(state.activeSlot)
            if (currentIndex > 0) {
                _roomState.value = state.copy(activeSlot = playerSlots[currentIndex - 1])
            }
        }
        broadcastSync()
    }

    fun resetBattle() {
        if (!isHost) return
        val state = _roomState.value ?: return
        _roomState.value = state.copy(
            status = BattleStatus.LOBBY,
            round = 1,
            activeSlot = 1
        )
        broadcastSync()
    }

    fun leaveRoom() {
        sendEnvelope("LEAVE", gson.toJson(mapOf("playerId" to myClientId)))
        try {
            val topic = TOPIC_PREFIX + (currentRoomCode ?: "")
            mqttClient?.unsubscribe(topic)
            mqttClient?.disconnect()
            mqttClient?.close()
        } catch (_: Exception) {}
        mqttClient = null
        currentRoomCode = null
        isHost = false
        myCurrentParticipant = null
        _roomState.value = null
        _isConnected.value = false
        _statusMessage.value = "Disconnected"
    }

    private fun advanceTurnInternal() {
        val state = _roomState.value ?: return
        val playerSlots = state.participants.filter { !it.isEnemy }.map { it.slot }.sorted()
        if (playerSlots.isEmpty()) return

        val currentIndex = playerSlots.indexOf(state.activeSlot)
        if (currentIndex in 0 until playerSlots.size - 1) {
            // Next player
            _roomState.value = state.copy(
                activeSlot = playerSlots[currentIndex + 1],
                status = BattleStatus.BATTLE
            )
        } else {
            // Last player finished -> It is now the Enemies' Turn!
            _roomState.value = state.copy(
                status = BattleStatus.ENEMIES,
                activeSlot = 999
            )
        }
        broadcastSync()
    }

    private fun advanceEnemiesInternal() {
        val state = _roomState.value ?: return
        val firstSlot = state.participants.filter { !it.isEnemy }.minOfOrNull { it.slot } ?: 1
        _roomState.value = state.copy(
            round = state.round + 1,
            activeSlot = firstSlot,
            status = BattleStatus.BATTLE
        )
        broadcastSync()
    }

    private fun broadcastSync() {
        val state = _roomState.value ?: return
        sendEnvelope("SYNC", gson.toJson(state))
    }

    private fun sendEnvelope(type: String, payload: String) {
        val room = currentRoomCode ?: return
        val envelope = TurnSyncEnvelope(
            type = type,
            senderId = myClientId,
            roomCode = room,
            payloadJson = payload
        )
        val msg = MqttMessage(envelope.toJson().toByteArray(Charsets.UTF_8)).apply {
            qos = 1
        }
        val topic = TOPIC_PREFIX + room
        try {
            mqttClient?.publish(topic, msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun connectAndJoin(roomCode: String, onConnected: () -> Unit) {
        scope.launch {
            try {
                _statusMessage.value = "Connecting to room $roomCode..."
                val client = MqttAsyncClient(BROKER_URL_TCP, myClientId, MemoryPersistence())
                mqttClient = client

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 20
                    isAutomaticReconnect = true
                }

                client.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        _isConnected.value = true
                        _statusMessage.value = "Connected to room $roomCode"
                        val topic = TOPIC_PREFIX + roomCode
                        client.subscribe(topic, 1, null, object : IMqttActionListener {
                            override fun onSuccess(asyncActionToken: IMqttToken?) {
                                onConnected()
                            }
                            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                                _statusMessage.value = "Failed to subscribe: ${exception?.message}"
                            }
                        })
                    }

                    override fun connectionLost(cause: Throwable?) {
                        _isConnected.value = false
                        _statusMessage.value = "Connection lost: ${cause?.message}"
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val body = message?.payload?.let { String(it, Charsets.UTF_8) } ?: return
                        handleIncomingEnvelope(body)
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                client.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {}
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        _isConnected.value = false
                        _statusMessage.value = "Connection error: ${exception?.message}"
                    }
                })
            } catch (e: Exception) {
                _isConnected.value = false
                _statusMessage.value = "Connection failed: ${e.message}"
            }
        }
    }

    private fun handleIncomingEnvelope(rawJson: String) {
        val envelope = TurnSyncEnvelope.fromJson(rawJson) ?: return
        if (envelope.roomCode != currentRoomCode) return

        when (envelope.type) {
            "SYNC" -> {
                try {
                    val syncedState = gson.fromJson(envelope.payloadJson, BattleRoomState::class.java)
                    if (syncedState != null) {
                        _roomState.value = syncedState
                        // Update my participant slot if host synced it
                        syncedState.participants.find { it.id == myClientId }?.let {
                            myCurrentParticipant = it
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            "JOIN" -> {
                if (isHost) {
                    try {
                        val newP = gson.fromJson(envelope.payloadJson, BattleParticipant::class.java)
                        val state = _roomState.value ?: return
                        val existing = state.participants.filter { it.id != newP.id }
                        val nextSlot = (existing.maxOfOrNull { it.slot } ?: 0) + 1
                        val assigned = newP.copy(slot = nextSlot)
                        _roomState.value = state.copy(participants = existing + assigned)
                        broadcastSync()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            "CLAIM_SLOT" -> {
                if (isHost) {
                    try {
                        val map = gson.fromJson(envelope.payloadJson, Map::class.java)
                        val pId = map["playerId"] as? String ?: return
                        val slot = (map["slot"] as? Number)?.toInt() ?: return
                        val state = _roomState.value ?: return
                        val updated = state.participants.map {
                            if (it.id == pId) it.copy(slot = slot) else it
                        }
                        _roomState.value = state.copy(participants = updated)
                        broadcastSync()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            "START_BATTLE" -> {
                // Guests wait for host SYNC, but if received from host, update state
            }

            "END_TURN" -> {
                if (isHost) {
                    advanceTurnInternal()
                }
            }

            "END_ENEMIES" -> {
                if (isHost) {
                    advanceEnemiesInternal()
                }
            }

            "LEAVE" -> {
                if (isHost) {
                    try {
                        val map = gson.fromJson(envelope.payloadJson, Map::class.java)
                        val pId = map["playerId"] as? String ?: return
                        val state = _roomState.value ?: return
                        val updated = state.participants.filter { it.id != pId }
                        _roomState.value = state.copy(participants = updated)
                        broadcastSync()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..4).map { chars.random() }.joinToString("")
    }
}
