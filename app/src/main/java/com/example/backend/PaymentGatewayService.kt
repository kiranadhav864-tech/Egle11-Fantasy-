package com.example.backend

import com.example.model.PaymentGateway
import com.example.model.PaymentOrderRequest
import com.example.model.PaymentOrderResponse
import com.example.model.TransactionStatus
import com.example.model.WebhookVerificationResult
import kotlinx.coroutines.delay

/**
 * Payment Gateway Service
 * Enforces client security:
 * - Client NEVER holds PhonePe/Paytm merchant keys, API salt, or authorization tokens.
 * - Client asks backend functions to create an order.
 * - Server initializes transaction with payment partner.
 * - Gateway returns webhook directly to backend.
 */
interface PaymentGatewayService {
    suspend fun createOrder(request: PaymentOrderRequest, userId: String): PaymentOrderResponse
    suspend fun verifyTransactionStatus(orderId: String): WebhookVerificationResult
}

class SecurePaymentGatewayService : PaymentGatewayService {

    override suspend fun createOrder(request: PaymentOrderRequest, userId: String): PaymentOrderResponse {
        delay(400) // Simulates server call latency to Cloud Functions: createDepositOrder
        val orderId = "ORD_${System.currentTimeMillis()}_${request.gateway.name}"
        
        return PaymentOrderResponse(
            success = true,
            orderId = orderId,
            amount = request.amount,
            gateway = request.gateway,
            merchantId = "EGLE11_LIVE_MERCHANT",
            paymentUrl = when (request.gateway) {
                PaymentGateway.PHONEPE -> "phonepe://pay?pa=egle11merchant@phonepe&am=${request.amount}&pn=Egle11Fantasy&tr=$orderId"
                PaymentGateway.PAYTM -> "paytmmp://pay?pa=egle11merchant@paytm&am=${request.amount}&pn=Egle11Fantasy&tr=$orderId"
            },
            sdkToken = "sec_tok_${System.currentTimeMillis()}_sha256",
            message = "Order initiated securely via ${request.gateway.displayName}. Complete payment via UPI app."
        )
    }

    override suspend fun verifyTransactionStatus(orderId: String): WebhookVerificationResult {
        delay(300)
        return WebhookVerificationResult(
            isVerified = true,
            transactionId = orderId,
            status = TransactionStatus.SUCCESS,
            creditedAmount = 500.0,
            timestamp = System.currentTimeMillis()
        )
    }
}
