package com.example.model

/**
 * Winner Record Entity
 * Preserves completed contest ranking and prize payout ledger
 */
data class Winner(
    val winnerId: String = "",
    val contestId: String = "",
    val matchId: String = "",
    val userId: String = "",
    val teamName: String = "",
    val rank: Int = 1,
    val prizeAmount: Double = 0.0,
    val pointsScored: Double = 0.0,
    val settledAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "winnerId" to winnerId,
        "contestId" to contestId,
        "matchId" to matchId,
        "userId" to userId,
        "teamName" to teamName,
        "rank" to rank,
        "prizeAmount" to prizeAmount,
        "pointsScored" to pointsScored,
        "settledAt" to settledAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Winner = Winner(
            winnerId = map["winnerId"] as? String ?: "",
            contestId = map["contestId"] as? String ?: "",
            matchId = map["matchId"] as? String ?: "",
            userId = map["userId"] as? String ?: "",
            teamName = map["teamName"] as? String ?: "",
            rank = (map["rank"] as? Number)?.toInt() ?: 1,
            prizeAmount = (map["prizeAmount"] as? Number)?.toDouble() ?: 0.0,
            pointsScored = (map["pointsScored"] as? Number)?.toDouble() ?: 0.0,
            settledAt = (map["settledAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
