package com.example.model

/**
 * Player Entity
 * Managed by Admin: Add, Edit, Assign to matches, Player Role (WK, BAT, AR, BOWL), Availability
 */
data class Player(
    val playerId: String = "",
    val name: String = "",
    val teamCode: String = "", // e.g. "IND", "AUS"
    val role: PlayerRole = PlayerRole.BAT,
    val credits: Double = 8.5,
    val isAvailable: Boolean = true,
    val matchId: String = "", // assigned match
    val pointsEarned: Double = 0.0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "playerId" to playerId,
        "name" to name,
        "teamCode" to teamCode,
        "role" to role.name,
        "credits" to credits,
        "isAvailable" to isAvailable,
        "matchId" to matchId,
        "pointsEarned" to pointsEarned
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Player {
            val roleStr = (map["role"] as? String)?.uppercase() ?: "BAT"
            return Player(
                playerId = map["playerId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                teamCode = map["teamCode"] as? String ?: "",
                role = try { PlayerRole.valueOf(roleStr) } catch (_: Exception) { PlayerRole.BAT },
                credits = (map["credits"] as? Number)?.toDouble() ?: 8.5,
                isAvailable = map["isAvailable"] as? Boolean ?: true,
                matchId = map["matchId"] as? String ?: "",
                pointsEarned = (map["pointsEarned"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

enum class PlayerRole(val displayName: String, val minCount: Int, val maxCount: Int) {
    WK("Wicket Keeper", 1, 4),
    BAT("Batsman", 3, 6),
    AR("All Rounder", 1, 4),
    BOWL("Bowler", 3, 6)
}
