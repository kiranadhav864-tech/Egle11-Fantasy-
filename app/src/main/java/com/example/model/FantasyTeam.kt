package com.example.model

/**
 * User's Fantasy Team Entity
 * Constraints:
 * - 11 Players
 * - Exactly 1 Captain and 1 Vice Captain
 * - Team name can be changed only once (nameEditCount <= 1)
 * - Duplicate team names disallowed across app
 */
data class FantasyTeam(
    val teamId: String = "",
    val userId: String = "",
    val matchId: String = "",
    val teamName: String = "",
    val selectedPlayerIds: List<String> = emptyList(),
    val captainPlayerId: String = "",
    val viceCaptainPlayerId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val nameEditCount: Int = 0,
    val totalPoints: Double = 0.0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "teamId" to teamId,
        "userId" to userId,
        "matchId" to matchId,
        "teamName" to teamName,
        "teamNameLower" to teamName.lowercase().trim(),
        "selectedPlayerIds" to selectedPlayerIds,
        "captainPlayerId" to captainPlayerId,
        "viceCaptainPlayerId" to viceCaptainPlayerId,
        "createdAt" to createdAt,
        "nameEditCount" to nameEditCount,
        "totalPoints" to totalPoints
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): FantasyTeam {
            @Suppress("UNCHECKED_CAST")
            val players = (map["selectedPlayerIds"] as? List<String>) ?: emptyList()
            return FantasyTeam(
                teamId = map["teamId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                matchId = map["matchId"] as? String ?: "",
                teamName = map["teamName"] as? String ?: "",
                selectedPlayerIds = players,
                captainPlayerId = map["captainPlayerId"] as? String ?: "",
                viceCaptainPlayerId = map["viceCaptainPlayerId"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                nameEditCount = (map["nameEditCount"] as? Number)?.toInt() ?: 0,
                totalPoints = (map["totalPoints"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}
