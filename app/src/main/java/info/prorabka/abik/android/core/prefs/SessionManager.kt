package info.prorabka.abik.android.core.prefs

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Управление сессией: сигнал об истечении refresh-токена (автоматический logout).
 */
@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: TokenStore,
) {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired

    fun expireSession() {
        tokenStore.clear()
        _sessionExpired.tryEmit(Unit)
    }

    fun logoutLocal() {
        tokenStore.clear()
    }
}
