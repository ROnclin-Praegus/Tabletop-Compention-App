package com.homebrew.tabletopcompanion.model

import com.google.gson.Gson

data class BattleParticipant(
    val id: String,
    val name: String,
    val characterId: String? = null,
    val slot: Int = 1, // 1, 2, 3...
    val isEnemy: Boolean = false,
    val currentHp: Int? = null,
    val maxHp: Int? = null
)

enum class BattleStatus {
    LOBBY,
    BATTLE,
    ENEMIES
}

data class BattleRoomState(
    val roomCode: String,
    val hostId: String,
    val round: Int = 1,
    val activeSlot: Int = 1, // slot currently taking action (1..N, or 999 for enemies)
    val status: BattleStatus = BattleStatus.LOBBY,
    val participants: List<BattleParticipant> = emptyList()
) {
    fun getActiveParticipant(): BattleParticipant? {
        if (status == BattleStatus.ENEMIES) return null
        return participants.filter { !it.isEnemy }.find { it.slot == activeSlot }
    }

    val maxPlayerSlot: Int
        get() = participants.filter { !it.isEnemy }.maxOfOrNull { it.slot } ?: 0

    val playerParticipants: List<BattleParticipant>
        get() = participants.filter { !it.isEnemy }.sortedBy { it.slot }
}

data class TurnSyncEnvelope(
    val type: String, // "SYNC", "JOIN", "CLAIM_SLOT", "START_BATTLE", "END_TURN", "END_ENEMIES", "RESTART_BATTLE", "LEAVE"
    val senderId: String,
    val roomCode: String,
    val payloadJson: String = ""
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): TurnSyncEnvelope? = try {
            Gson().fromJson(json, TurnSyncEnvelope::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
