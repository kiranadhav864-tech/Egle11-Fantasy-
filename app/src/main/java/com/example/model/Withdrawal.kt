package com.example.model

/**
 * Withdrawal Request Entity
 * Processed via User's registered UPI ID
 */
data class Withdrawal(
    val withdrawalId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userMobile: String = "",
    val upiId: String = "",
    val amount: Double = 0.0,
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val remarks: String = "Awaiting review"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "withdrawalId" to withdrawalId,
        "userId" to userId,
        "userName" to userName,
        "userMobile" to userMobile,
        "upiId" to upiId,
        "amount" to amount,
        "status" to status.name,
        "requestedAt" to requestedAt,
        "reviewedAt" to reviewedAt,
        "reviewedBy" to reviewedBy,
        "remarks" to remarks
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Withdrawal {
            val statusStr = (map["status"] as? String)?.uppercase() ?: "PENDING"
            return Withdrawal(
                withdrawalId = map["withdrawalId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                userMobile = map["userMobile"] as? String ?: "",
                upiId = map["upiId"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                status = try { WithdrawalStatus.valueOf(statusStr) } catch (_: Exception) { WithdrawalStatus.PENDING },
                requestedAt = (map["requestedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                reviewedAt = (map["reviewedAt"] as? Number)?.toLong(),
                reviewedBy = map["reviewedBy"] as? String,
                remarks = map["remarks"] as? String ?: ""
            )
        }
    }
}

enum class WithdrawalStatus(val displayName: String) {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    COMPLETED("Completed (Paid via UPI)")
}
