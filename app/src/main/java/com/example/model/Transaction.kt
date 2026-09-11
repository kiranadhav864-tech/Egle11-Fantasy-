package com.example.model

/**
 * Immutable Transaction Ledger Record
 */
data class Transaction(
    val transactionId: String = "",
    val userId: String = "",
    val type: TransactionType = TransactionType.DEPOSIT,
    val amount: Double = 0.0,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val gateway: String = "PHONEPE", // PHONEPE, PAYTM, SYSTEM, UPI
    val referenceId: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "transactionId" to transactionId,
        "userId" to userId,
        "type" to type.name,
        "amount" to amount,
        "status" to status.name,
        "gateway" to gateway,
        "referenceId" to referenceId,
        "notes" to notes,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Transaction {
            val typeStr = (map["type"] as? String)?.uppercase() ?: "DEPOSIT"
            val statusStr = (map["status"] as? String)?.uppercase() ?: "PENDING"
            return Transaction(
                transactionId = map["transactionId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                type = try { TransactionType.valueOf(typeStr) } catch (_: Exception) { TransactionType.DEPOSIT },
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                status = try { TransactionStatus.valueOf(statusStr) } catch (_: Exception) { TransactionStatus.PENDING },
                gateway = map["gateway"] as? String ?: "PHONEPE",
                referenceId = map["referenceId"] as? String ?: "",
                notes = map["notes"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

enum class TransactionType(val displayName: String, val isCredit: Boolean) {
    DEPOSIT("Add Cash", true),
    CONTEST_ENTRY("Contest Entry Fee", false),
    WINNINGS("Contest Winnings", true),
    WITHDRAWAL("Withdrawal Request", false),
    REFUND("Contest Cancelled Refund", true)
}

enum class TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED
}
