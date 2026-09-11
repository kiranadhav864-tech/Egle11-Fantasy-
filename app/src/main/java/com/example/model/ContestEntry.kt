package com.example.model

/**
 * Contest Entry Entity
 * Connects user's fantasy team to a contest
 */
data class ContestEntry(
    val entryId: String = "",
    val contestId: String = "",
    val matchId: String = "",
    val userId: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val entryFeePaid: Double = 0.0,
    val points: Double = 0.0,
    val rank: Int = 0,
    val wonAmount: Double = 0.0,
    val joinedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "entryId" to entryId,
        "contestId" to contestId,
        "matchId" to matchId,
        "userId" to userId,
        "teamId" to teamId,
        "teamName" to teamName,
        "entryFeePaid" to entryFeePaid,
        "points" to points,
        "rank" to rank,
        "wonAmount" to wonAmount,
        "joinedAt" to joinedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): ContestEntry = ContestEntry(
            entryId = map["entryId"] as? String ?: "",
            contestId = map["contestId"] as? String ?: "",
            matchId = map["matchId"] as? String ?: "",
            userId = map["userId"] as? String ?: "",
            teamId = map["teamId"] as? String ?: "",
            teamName = map["teamName"] as? String ?: "",
            entryFeePaid = (map["entryFeePaid"] as? Number)?.toDouble() ?: 0.0,
            points = (map["points"] as? Number)?.toDouble() ?: 0.0,
            rank = (map["rank"] as? Number)?.toInt() ?: 0,
            wonAmount = (map["wonAmount"] as? Number)?.toDouble() ?: 0.0,
            joinedAt = (map["joinedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
