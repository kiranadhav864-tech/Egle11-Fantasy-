package com.example.model

/**
 * Payment Gateway Contracts for PhonePe and Paytm
 * Architecture Note:
 * - Client initiates order request by sending desired amount and user auth token to Cloud Functions.
 * - Server holds Merchant ID, Salt Key, Salt Index, and Private Certificate.
 * - Server signs the payload and returns an order token.
 * - Gateway invokes the Cloud Function Webhook directly for real balance credit.
 */

enum class PaymentGateway(val displayName: String, val minAmount: Double, val maxAmount: Double) {
    PHONEPE("PhonePe UPI & Gateway", 10.0, 50000.0),
    PAYTM("Paytm UPI & Wallet", 10.0, 50000.0)
}

data class PaymentOrderRequest(
    val amount: Double,
    val gateway: PaymentGateway
)

data class PaymentOrderResponse(
    val success: Boolean,
    val orderId: String,
    val amount: Double,
    val gateway: PaymentGateway,
    val merchantId: String,
    val paymentUrl: String?,
    val sdkToken: String?,
    val message: String
)

data class WebhookVerificationResult(
    val isVerified: Boolean,
    val transactionId: String,
    val status: TransactionStatus,
    val creditedAmount: Double,
    val timestamp: Long
)
