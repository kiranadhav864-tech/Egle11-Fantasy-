package com.example.model

/**
 * User Account Entity
 * Note: Mandated to NOT store Aadhaar or PAN.
 */
data class User(
    val userId: String = "",
    val name: String = "",
    val mobileNumber: String = "",
    val email: String = "",
    val upiId: String = "",
    val selectedState: String = "",
    val isAge18Plus: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val role: UserRole = UserRole.USER,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "name" to name,
        "mobileNumber" to mobileNumber,
        "email" to email,
        "upiId" to upiId,
        "selectedState" to selectedState,
        "isAge18Plus" to isAge18Plus,
        "createdAt" to createdAt,
        "role" to role.name.lowercase(),
        "accountStatus" to accountStatus.name
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): User {
            val roleStr = (map["role"] as? String)?.uppercase() ?: "USER"
            val statusStr = (map["accountStatus"] as? String)?.uppercase() ?: "ACTIVE"
            return User(
                userId = map["userId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                mobileNumber = map["mobileNumber"] as? String ?: "",
                email = map["email"] as? String ?: "",
                upiId = map["upiId"] as? String ?: "",
                selectedState = map["selectedState"] as? String ?: "",
                isAge18Plus = (map["isAge18Plus"] as? Boolean) ?: true,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.USER },
                accountStatus = try { AccountStatus.valueOf(statusStr) } catch (_: Exception) { AccountStatus.ACTIVE }
            )
        }
    }
}

enum class UserRole {
    USER,
    ADMIN
}

enum class AccountStatus {
    ACTIVE,
    SUSPENDED,
    BLOCKED
}
