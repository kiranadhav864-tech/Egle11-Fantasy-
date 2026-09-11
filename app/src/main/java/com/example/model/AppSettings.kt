package com.example.model

/**
 * Global App Settings Entity stored in /app_settings/global
 */
data class AppSettings(
    val settingId: String = "global",
    val minWithdrawalAmount: Double = 100.0,
    val maxWithdrawalAmount: Double = 50000.0,
    val isPhonePeEnabled: Boolean = true,
    val isPaytmEnabled: Boolean = true,
    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Egle11 is temporarily undergoing scheduled maintenance.",
    val supportEmail: String = "support@egle11.com",
    val supportPhone: String = "+91 8000 111 222",
    val restrictedStates: List<String> = listOf("Assam", "Andhra Pradesh", "Nagaland", "Odisha", "Sikkim", "Telangana")
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "settingId" to settingId,
        "minWithdrawalAmount" to minWithdrawalAmount,
        "maxWithdrawalAmount" to maxWithdrawalAmount,
        "isPhonePeEnabled" to isPhonePeEnabled,
        "isPaytmEnabled" to isPaytmEnabled,
        "isMaintenanceMode" to isMaintenanceMode,
        "maintenanceMessage" to maintenanceMessage,
        "supportEmail" to supportEmail,
        "supportPhone" to supportPhone,
        "restrictedStates" to restrictedStates
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): AppSettings {
            @Suppress("UNCHECKED_CAST")
            val states = map["restrictedStates"] as? List<String> ?: listOf("Assam", "Andhra Pradesh", "Nagaland", "Odisha", "Sikkim", "Telangana")
            return AppSettings(
                settingId = map["settingId"] as? String ?: "global",
                minWithdrawalAmount = (map["minWithdrawalAmount"] as? Number)?.toDouble() ?: 100.0,
                maxWithdrawalAmount = (map["maxWithdrawalAmount"] as? Number)?.toDouble() ?: 50000.0,
                isPhonePeEnabled = map["isPhonePeEnabled"] as? Boolean ?: true,
                isPaytmEnabled = map["isPaytmEnabled"] as? Boolean ?: true,
                isMaintenanceMode = map["isMaintenanceMode"] as? Boolean ?: false,
                maintenanceMessage = map["maintenanceMessage"] as? String ?: "Egle11 is temporarily undergoing scheduled maintenance.",
                supportEmail = map["supportEmail"] as? String ?: "support@egle11.com",
                supportPhone = map["supportPhone"] as? String ?: "+91 8000 111 222",
                restrictedStates = states
            )
        }
    }
}
