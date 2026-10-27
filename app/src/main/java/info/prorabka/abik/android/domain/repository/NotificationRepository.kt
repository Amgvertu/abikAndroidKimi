package info.prorabka.abik.android.domain.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.AppNotification

interface NotificationRepository {
    suspend fun list(onlyUnread: Boolean = false): ApiResult<List<AppNotification>>
    suspend fun unreadCount(): ApiResult<Int>
    suspend fun markRead(id: Long): ApiResult<Unit>
    suspend fun markAllRead(): ApiResult<Unit>
}

/**
 * Регистрация FCM-токена устройства на сервере.
 * Ошибки не пробрасываем наружу — push не критичен для основной работы.
 */
interface DeviceRepository {
    /** Зарегистрировать текущий FCM-токен (если он уже известен/доступен). */
    suspend fun registerCurrentDevice()

    /** Отозвать текущий FCM-токен (перед logout). */
    suspend fun unregisterCurrentDevice()
}
