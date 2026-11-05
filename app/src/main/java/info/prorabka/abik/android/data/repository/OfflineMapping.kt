package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.data.local.OfflineOperationEntity
import java.time.Instant
import kotlin.math.abs

internal fun localId(operationId: String): Long {
    val hash = operationId.hashCode().toLong()
    return -(abs(hash).coerceAtLeast(1L))
}

internal val OfflineOperationEntity.localCreatedAt: String
    get() = Instant.ofEpochMilli(createdAt).toString()

internal val OfflineOperationEntity.hasSyncIssue: Boolean
    get() = status == OfflineOperationEntity.STATUS_FAILED ||
        status == OfflineOperationEntity.STATUS_CONFLICT

internal val OfflineOperationEntity.isLocalOnly: Boolean
    get() = status == OfflineOperationEntity.STATUS_PENDING || hasSyncIssue

internal val OfflineOperationEntity.displaySyncError: String?
    get() = when (status) {
        OfflineOperationEntity.STATUS_CONFLICT ->
            lastError ?: "Конфликт: серверная версия изменилась"
        OfflineOperationEntity.STATUS_FAILED -> lastError ?: "Ошибка синхронизации"
        else -> null
    }
