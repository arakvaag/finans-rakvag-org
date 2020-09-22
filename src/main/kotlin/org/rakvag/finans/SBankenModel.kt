package org.rakvag.finans

import java.time.ZonedDateTime

data class GetAccountInfoResponse(
        val availableItems: Int,
        val items: List<AccountInfo>,
        val errorType: String? = null,
        val isError: Boolean = false,
        val errorCode: String? = null,
        val errorMessage: String? = null,
        val traceId: String? = null
)

data class AccountInfo(
        val accountId: String,
        val accountNumber: String,
        val ownerCustomerId: String,
        val name: String,
        val accountType: String,
        val available: Double,
        val balance: Double,
        val creditLimit: Double
)

data class GetPaymentsResponse(
        val availableItems: Int,
        val items: List<Payment>,
        val errorType: String? = null,
        val isError: Boolean = false,
        val errorCode: String? = null,
        val errorMessage: String? = null,
        val traceId: String? = null
)

data class Payment(
        val paymentId: String,
        val recipientAccountNumber: String,
        val amount: Double,
        val dueDate: ZonedDateTime,
        val kid: String? = null,
        val text: String? = null,
        val isActive: Boolean,
        val status: String? = null,
        val statusDetails: String? = null,
        val productType: String? = null,
        val paymentType: String? = null,
        val paymentNumber: Int,
        val beneficiaryName: String? = null
)
