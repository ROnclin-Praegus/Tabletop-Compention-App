package com.homebrew.tabletopcompanion.server

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import java.util.UUID
import com.google.gson.Gson
import com.homebrew.tabletopcompanion.model.BattleParticipant
import com.homebrew.tabletopcompanion.model.BattleRoomState
import com.homebrew.tabletopcompanion.model.BattleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object GameServerManager {
    var server: LocalGameServer? = null
    var isRunning = false
    var apiKey: String = ""
    var uploads = mutableListOf<UploadRecord>()
    var onSyncReceived: ((String) -> Unit)? = null
    
    private const val PREFS_NAME = "GameServerPrefs"
    private const val KEY_API_KEY = "apiKey"
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        apiKey = prefs.getString(KEY_API_KEY, null) ?: generateNewKey(context)
        
        val uploadsJson = prefs.getString("uploads_list", null)
        if (uploadsJson != null) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<UploadRecord>>() {}.type
                val list: List<UploadRecord> = com.google.gson.Gson().fromJson(uploadsJson, type)
                uploads = list.toMutableList()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun saveUploads() {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putString("uploads_list", com.google.gson.Gson().toJson(uploads))?.apply()
    }
    
    fun removeUpload(upload: UploadRecord) {
        uploads.remove(upload)
        saveUploads()
    }

    fun generateNewKey(context: Context): String {
        val newKey = UUID.randomUUID().toString().substring(0, 8).uppercase()
        apiKey = newKey
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_API_KEY, newKey).apply()
        return newKey
    }

    fun startServer(context: Context) {
        if (isRunning) return
        val intent = android.content.Intent(context, ApiServerService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }

    fun stopServer() {
        if (!isRunning) return
        appContext?.let {
            val intent = android.content.Intent(it, ApiServerService::class.java)
            it.stopService(intent)
        }
    }

    fun startServerInternal() {
        try {
            server = LocalGameServer(8080) { jsonData ->
                addUpload(jsonData)
            }
            server?.start()
            isRunning = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopServerInternal() {
        try {
            server?.stop()
            server = null
            isRunning = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

        private fun addUpload(jsonData: String) {
        val record = UploadRecord(System.currentTimeMillis(), jsonData)
        val gson = com.google.gson.Gson()
        val characterId = try {
            gson.fromJson(jsonData, com.homebrew.tabletopcompanion.model.Character::class.java)?.id
        } catch(e: Exception) { null }
        val charName = try {
            gson.fromJson(jsonData, com.homebrew.tabletopcompanion.model.Character::class.java)?.name
        } catch(e: Exception) { "Hero" }
        
        uploads.add(0, record)
        
        if (characterId != null) {
            val charUploads = uploads.filter { 
                try { gson.fromJson(it.jsonData, com.homebrew.tabletopcompanion.model.Character::class.java)?.id == characterId } catch(e:Exception) { false }
            }
            if (charUploads.size > 5) {
                val toRemove = charUploads.drop(5)
                uploads.removeAll(toRemove)
            }
        } else {
             if (uploads.size > 50) uploads = uploads.take(50).toMutableList()
        }
        
        appContext?.let { ctx ->
            try {
                val notificationIntent = android.content.Intent(ctx, com.homebrew.tabletopcompanion.MainActivity::class.java)
                val pendingIntent = android.app.PendingIntent.getActivity(
                    ctx, 0, notificationIntent, android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val notification = androidx.core.app.NotificationCompat.Builder(ctx, "ApiServerChannel")
                    .setContentTitle("Character Sync Received!")
                    .setContentText("Successfully synced $charName from the Web")
                    .setSmallIcon(com.homebrew.tabletopcompanion.R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
                    
                val manager = ctx.getSystemService(android.app.NotificationManager::class.java)
                manager.notify(System.currentTimeMillis().toInt(), notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        saveUploads()
        onSyncReceived?.invoke(jsonData)
    }

    fun getLocalIpAddress(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } catch (e: Exception) {
            e.printStackTrace()
            "127.0.0.1"
        }
    }

    // --- Turn Order Battle Management ---
    private val _battleState = MutableStateFlow<BattleRoomState?>(null)
    val battleState: StateFlow<BattleRoomState?> = _battleState.asStateFlow()

    fun createBattle(hostName: String, charId: String?, hp: Int?, maxHp: Int?, context: Context): BattleRoomState {
        startServer(context)
        val code = generateRoomCode()
        val hostParticipant = BattleParticipant(
            id = "host-${UUID.randomUUID().toString().substring(0, 6)}",
            name = hostName.ifBlank { "Host" },
            characterId = charId,
            slot = 1,
            isEnemy = false,
            currentHp = hp,
            maxHp = maxHp
        )
        val state = BattleRoomState(
            roomCode = code,
            hostId = hostParticipant.id,
            round = 1,
            activeSlot = 1,
            status = BattleStatus.LOBBY,
            participants = listOf(hostParticipant)
        )
        _battleState.value = state
        return state
    }

    fun handleBattleAction(action: String, payloadJson: String): BattleRoomState? {
        val currentState = _battleState.value ?: return null
        val gson = Gson()
        val newState = when (action.uppercase()) {
            "JOIN" -> {
                val newP = try { gson.fromJson(payloadJson, BattleParticipant::class.java) } catch(e: Exception) { null }
                if (newP == null) currentState
                else {
                    val existing = currentState.participants.filter { it.id != newP.id }
                    val playerSlots = existing.filter { !it.isEnemy }.map { it.slot }
                    val nextSlot = (playerSlots.maxOrNull() ?: 0) + 1
                    val assignedP = if (newP.slot <= 0 || existing.any { it.slot == newP.slot }) {
                        newP.copy(slot = nextSlot)
                    } else newP
                    currentState.copy(participants = existing + assignedP)
                }
            }
            "CLAIM_SLOT" -> {
                val obj = try { gson.fromJson(payloadJson, Map::class.java) } catch(e: Exception) { null }
                val pId = obj?.get("playerId") as? String ?: ""
                val targetSlot = (obj?.get("slot") as? Number)?.toInt() ?: 1
                val updated = currentState.participants.map { p ->
                    if (p.id == pId) p.copy(slot = targetSlot) else p
                }
                currentState.copy(participants = updated)
            }
            "START" -> {
                val firstSlot = currentState.playerParticipants.firstOrNull()?.slot ?: 1
                currentState.copy(status = BattleStatus.BATTLE, round = 1, activeSlot = firstSlot)
            }
            "END_TURN" -> {
                val playerSlots = currentState.playerParticipants.map { it.slot }
                if (playerSlots.isEmpty()) currentState
                else {
                    val currIdx = playerSlots.indexOf(currentState.activeSlot)
                    if (currIdx in 0 until playerSlots.size - 1) {
                        currentState.copy(status = BattleStatus.BATTLE, activeSlot = playerSlots[currIdx + 1])
                    } else {
                        // After last player, it is always ENEMIES turn!
                        currentState.copy(status = BattleStatus.ENEMIES, activeSlot = 999)
                    }
                }
            }
            "END_ENEMIES" -> {
                val firstSlot = currentState.playerParticipants.firstOrNull()?.slot ?: 1
                currentState.copy(
                    status = BattleStatus.BATTLE,
                    round = currentState.round + 1,
                    activeSlot = firstSlot
                )
            }
            "NEXT" -> {
                if (currentState.status == BattleStatus.BATTLE) {
                    val playerSlots = currentState.playerParticipants.map { it.slot }
                    val currIdx = playerSlots.indexOf(currentState.activeSlot)
                    if (currIdx in 0 until playerSlots.size - 1) {
                        currentState.copy(activeSlot = playerSlots[currIdx + 1])
                    } else {
                        currentState.copy(status = BattleStatus.ENEMIES, activeSlot = 999)
                    }
                } else if (currentState.status == BattleStatus.ENEMIES) {
                    val firstSlot = currentState.playerParticipants.firstOrNull()?.slot ?: 1
                    currentState.copy(status = BattleStatus.BATTLE, round = currentState.round + 1, activeSlot = firstSlot)
                } else currentState
            }
            "PREV" -> {
                val playerSlots = currentState.playerParticipants.map { it.slot }
                if (currentState.status == BattleStatus.ENEMIES) {
                    val lastSlot = playerSlots.lastOrNull() ?: 1
                    currentState.copy(status = BattleStatus.BATTLE, activeSlot = lastSlot)
                } else {
                    val currIdx = playerSlots.indexOf(currentState.activeSlot)
                    if (currIdx > 0) {
                        currentState.copy(activeSlot = playerSlots[currIdx - 1])
                    } else currentState
                }
            }
            "RESET" -> {
                currentState.copy(status = BattleStatus.LOBBY, round = 1, activeSlot = 1)
            }
            "LEAVE" -> {
                val obj = try { gson.fromJson(payloadJson, Map::class.java) } catch(e: Exception) { null }
                val pId = obj?.get("playerId") as? String ?: ""
                currentState.copy(participants = currentState.participants.filter { it.id != pId })
            }
            else -> currentState
        }
        _battleState.value = newState
        return newState
    }

    fun leaveOrCloseBattle() {
        _battleState.value = null
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..4).map { chars.random() }.joinToString("")
    }
}

