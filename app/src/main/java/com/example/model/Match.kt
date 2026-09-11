package com.example.model

/**
 * Match Entity in Firestore
 * Managed by Admin: Add, Edit, Publish/Unpublish, Match Status
 */
data class Match(
    val matchId: String = "",
    val title: String = "",
    val team1Short: String = "",
    val team1Name: String = "",
    val team2Short: String = "",
    val team2Name: String = "",
    val matchType: String = "T20", // T20, ODI, TEST
    val venue: String = "",
    val startTimeEpoch: Long = System.currentTimeMillis() + 86400000L,
    val status: MatchStatus = MatchStatus.UPCOMING,
    val isPublished: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "matchId" to matchId,
        "title" to title,
        "team1Short" to team1Short,
        "team1Name" to team1Name,
        "team2Short" to team2Short,
        "team2Name" to team2Name,
        "matchType" to matchType,
        "venue" to venue,
        "startTimeEpoch" to startTimeEpoch,
        "status" to status.name,
        "isPublished" to isPublished,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Match {
            val statusStr = (map["status"] as? String)?.uppercase() ?: "UPCOMING"
            return Match(
                matchId = map["matchId"] as? String ?: "",
                title = map["title"] as? String ?: "",
                team1Short = map["team1Short"] as? String ?: "",
                team1Name = map["team1Name"] as? String ?: "",
                team2Short = map["team2Short"] as? String ?: "",
                team2Name = map["team2Name"] as? String ?: "",
                matchType = map["matchType"] as? String ?: "T20",
                venue = map["venue"] as? String ?: "",
                startTimeEpoch = (map["startTimeEpoch"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                status = try { MatchStatus.valueOf(statusStr) } catch (_: Exception) { MatchStatus.UPCOMING },
                isPublished = map["isPublished"] as? Boolean ?: true,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

enum class MatchStatus {
    UPCOMING,
    LIVE,
    COMPLETED,
    ABANDONED
}
