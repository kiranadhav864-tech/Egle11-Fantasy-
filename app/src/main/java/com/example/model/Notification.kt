package com.example.model

/**
 * Notification System Entity
 * Categories: New matches, Contest announcements, Contest results, Wallet updates, Withdrawal status, App announcements
 */
data class Notification(
    val notificationId: String = "",
    val userId: String = "all", // "all" for broadcast, or specific userId
    val type: NotificationType = NotificationType.SYSTEM_ANNOUNCEMENT,
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val referenceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "notificationId" to notificationId,
        "userId" to userId,
        "type" to type.name,
        "title" to title,
        "message" to message,
        "isRead" to isRead,
        "referenceId" to referenceId,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Notification {
            val typeStr = (map["type"] as? String)?.uppercase() ?: "SYSTEM_ANNOUNCEMENT"
            return Notification(
                notificationId = map["notificationId"] as? String ?: "",
                userId = map["userId"] as? String ?: "all",
                type = try { NotificationType.valueOf(typeStr) } catch (_: Exception) { NotificationType.SYSTEM_ANNOUNCEMENT },
                title = map["title"] as? String ?: "",
                message = map["message"] as? String ?: "",
                isRead = map["isRead"] as? Boolean ?: false,
                referenceId = map["referenceId"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

enum class NotificationType(val category: String) {
    NEW_MATCH("Matches"),
    CONTEST_ANNOUNCEMENT("Contests"),
    CONTEST_RESULTS("Contests"),
    WALLET_UPDATE("Wallet"),
    WITHDRAWAL_STATUS("Withdrawal"),
    SYSTEM_ANNOUNCEMENT("Announcements")
}
