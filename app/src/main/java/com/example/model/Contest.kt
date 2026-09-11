package com.example.model

/**
 * Contest Entity
 * Managed by Admin: Create, Edit, Publish/Unpublish, Entry Fee, Spots, Prize Info, Status
 */
data class Contest(
    val contestId: String = "",
    val matchId: String = "",
    val title: String = "",
    val entryFee: Double = 49.0,
    val totalSpots: Int = 100,
    val filledSpots: Int = 0,
    val prizePool: Double = 4000.0,
    val firstPrize: Double = 1500.0,
    val winnerPercentage: Int = 50,
    val prizeBreakdown: List<PrizeTier> = defaultPrizeBreakdown(prizePool, totalSpots),
    val status: ContestStatus = ContestStatus.UPCOMING,
    val isPublished: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "contestId" to contestId,
        "matchId" to matchId,
        "title" to title,
        "entryFee" to entryFee,
        "totalSpots" to totalSpots,
        "filledSpots" to filledSpots,
        "prizePool" to prizePool,
        "firstPrize" to firstPrize,
        "winnerPercentage" to winnerPercentage,
        "prizeBreakdown" to prizeBreakdown.map { it.toMap() },
        "status" to status.name,
        "isPublished" to isPublished,
        "createdAt" to createdAt
    )

    companion object {
        fun defaultPrizeBreakdown(prizePool: Double, spots: Int): List<PrizeTier> = listOf(
            PrizeTier(rankStart = 1, rankEnd = 1, prizeAmount = prizePool * 0.40),
            PrizeTier(rankStart = 2, rankEnd = 3, prizeAmount = prizePool * 0.15),
            PrizeTier(rankStart = 4, rankEnd = 10, prizeAmount = prizePool * 0.03),
            PrizeTier(rankStart = 11, rankEnd = Math.max(12, spots / 2), prizeAmount = prizePool * 0.01)
        )

        fun fromMap(map: Map<String, Any?>): Contest {
            val statusStr = (map["status"] as? String)?.uppercase() ?: "UPCOMING"
            @Suppress("UNCHECKED_CAST")
            val rawBreakdown = map["prizeBreakdown"] as? List<Map<String, Any?>> ?: emptyList()
            val breakdown = rawBreakdown.map { PrizeTier.fromMap(it) }

            return Contest(
                contestId = map["contestId"] as? String ?: "",
                matchId = map["matchId"] as? String ?: "",
                title = map["title"] as? String ?: "",
                entryFee = (map["entryFee"] as? Number)?.toDouble() ?: 0.0,
                totalSpots = (map["totalSpots"] as? Number)?.toInt() ?: 100,
                filledSpots = (map["filledSpots"] as? Number)?.toInt() ?: 0,
                prizePool = (map["prizePool"] as? Number)?.toDouble() ?: 0.0,
                firstPrize = (map["firstPrize"] as? Number)?.toDouble() ?: 0.0,
                winnerPercentage = (map["winnerPercentage"] as? Number)?.toInt() ?: 50,
                prizeBreakdown = if (breakdown.isNotEmpty()) breakdown else defaultPrizeBreakdown(
                    (map["prizePool"] as? Number)?.toDouble() ?: 0.0,
                    (map["totalSpots"] as? Number)?.toInt() ?: 100
                ),
                status = try { ContestStatus.valueOf(statusStr) } catch (_: Exception) { ContestStatus.UPCOMING },
                isPublished = map["isPublished"] as? Boolean ?: true,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class PrizeTier(
    val rankStart: Int = 1,
    val rankEnd: Int = 1,
    val prizeAmount: Double = 0.0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "rankStart" to rankStart,
        "rankEnd" to rankEnd,
        "prizeAmount" to prizeAmount
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): PrizeTier = PrizeTier(
            rankStart = (map["rankStart"] as? Number)?.toInt() ?: 1,
            rankEnd = (map["rankEnd"] as? Number)?.toInt() ?: 1,
            prizeAmount = (map["prizeAmount"] as? Number)?.toDouble() ?: 0.0
        )
    }
}

enum class ContestStatus {
    UPCOMING,
    LIVE,
    COMPLETED,
    CANCELLED
}
