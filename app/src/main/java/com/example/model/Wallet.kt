package com.example.model

/**
 * User Wallet Entity
 * Balances separated cleanly:
 * - Deposit Balance (from PhonePe / Paytm)
 * - Winnings Balance (from won contests, eligible for withdrawal)
 * - Bonus Balance (promotional discounts on contest entry)
 */
data class Wallet(
    val userId: String = "",
    val depositBalance: Double = 0.0,
    val winningsBalance: Double = 0.0,
    val bonusBalance: Double = 0.0,
    val totalBalance: Double = depositBalance + winningsBalance + bonusBalance,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "depositBalance" to depositBalance,
        "winningsBalance" to winningsBalance,
        "bonusBalance" to bonusBalance,
        "totalBalance" to (depositBalance + winningsBalance + bonusBalance),
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Wallet {
            val dep = (map["depositBalance"] as? Number)?.toDouble() ?: 0.0
            val win = (map["winningsBalance"] as? Number)?.toDouble() ?: 0.0
            val bon = (map["bonusBalance"] as? Number)?.toDouble() ?: 0.0
            val tot = (map["totalBalance"] as? Number)?.toDouble() ?: (dep + win + bon)
            return Wallet(
                userId = map["userId"] as? String ?: "",
                depositBalance = dep,
                winningsBalance = win,
                bonusBalance = bon,
                totalBalance = tot,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
