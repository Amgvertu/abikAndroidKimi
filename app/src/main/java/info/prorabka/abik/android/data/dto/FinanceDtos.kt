package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

// --- Балансы ---

@Serializable
data class BalanceDto(
    val teamId: Long,
    val userId: Long,
    val userLastName: String? = null,
    val userFirstName: String? = null,
    val amountKopecks: Long,
    val updatedAt: String? = null,
)

@Serializable
data class BalanceAdjustmentRequestDto(
    val amountKopecks: Long,
    val reason: String,
)

@Serializable
data class BalanceAdjustmentDto(
    val id: Long,
    val teamId: Long,
    val userId: Long,
    val amountKopecks: Long,
    val reason: String? = null,
    val createdByUserId: Long? = null,
    val createdAt: String? = null,
)

// --- Платежи ---

@Serializable
data class PaymentDto(
    val id: Long,
    val teamId: Long,
    val userId: Long,
    val userLastName: String? = null,
    val userFirstName: String? = null,
    val amountKopecks: Long,
    val description: String? = null,
    val paidAt: String? = null,
    val createdByUserId: Long? = null,
    val createdByLastName: String? = null,
    val createdByFirstName: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * paidAt — из расширения API (backend-api-extension.md, раздел 2).
 * Пока сервер поле не поддерживает, оно игнорируется (Spring Boot
 * не падает на неизвестных полях), после доработки заработает автоматически.
 */
@Serializable
data class PaymentCreateRequestDto(
    val userId: Long,
    val amountKopecks: Long,
    val paidAt: String? = null,
    val description: String? = null,
)

@Serializable
data class PaymentUpdateRequestDto(
    val userId: Long? = null,
    val amountKopecks: Long? = null,
    val paidAt: String? = null,
    val description: String? = null,
)
