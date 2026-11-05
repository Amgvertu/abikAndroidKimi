package info.prorabka.abik.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_operations")
data class OfflineOperationEntity(
    @PrimaryKey val operationId: String,
    val userId: Long,
    val teamId: Long,
    val type: String,
    val eventId: Long? = null,
    val payloadJson: String,
    val expectedEventRevision: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = STATUS_PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SYNCED = "SYNCED"
        const val STATUS_CONFLICT = "CONFLICT"
        const val STATUS_FAILED = "FAILED"
    }
}

object OfflineOperationTypes {
    const val EXPENSE_CREATE = "EXPENSE_CREATE"
    const val PAYMENT_CREATE = "PAYMENT_CREATE"
    const val CHARGE_CREATE = "CHARGE_CREATE"
    const val EVENT_LINEUP_SAVE = "EVENT_LINEUP_SAVE"
    const val EVENT_SWEATERS_SAVE = "EVENT_SWEATERS_SAVE"
}
