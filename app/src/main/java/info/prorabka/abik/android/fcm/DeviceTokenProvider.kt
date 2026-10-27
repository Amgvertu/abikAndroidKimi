package info.prorabka.abik.android.fcm

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Доступ к FCM-токену устройства.
 * AbikFirebaseMessagingService.onNewToken сохраняет свежий токен сюда,
 * регистрация на сервере выполняется через DeviceRepository.
 */
@Singleton
class DeviceTokenProvider @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Токен, сохранённый сервисом при onNewToken (может быть null до первого запуска FCM). */
    var savedToken: String?
        get() = prefs.getString(KEY_PENDING_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_PENDING_TOKEN, value).apply()

    /** Сохранённый токен либо свежий токен из Firebase (сетевой вызов к FCM). */
    suspend fun currentToken(): String? {
        savedToken?.let { return it }
        return runCatching { FirebaseMessaging.getInstance().token.await() }
            .getOrNull()
            ?.also { savedToken = it }
    }

    companion object {
        const val PREFS_NAME = "fcm"
        const val KEY_PENDING_TOKEN = "pending_token"
    }
}
