package info.prorabka.abik.android.push

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.rustore.sdk.pushclient.RuStorePushClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Токен RuStore Push.
 * Токен выдаётся только после RuStorePushClient.init(...) с project ID
 * из консоли RuStore (BuildConfig.RUSTORE_PROJECT_ID). Пока ID не задан —
 * провайдер молча неактивен.
 */
@Singleton
class RuStoreTokenProvider @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var savedToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    /** Сохранённый токен либо свежий из RuStore SDK (null, если SDK не инициализирован). */
    suspend fun currentToken(): String? {
        savedToken?.let { return it }
        if (!RuStorePushInitializer.initialized) return null
        return suspendCoroutine { cont ->
            runCatching {
                RuStorePushClient.getToken()
                    .addOnSuccessListener { token -> cont.resume(token) }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "RuStore: не удалось получить токен", e)
                        cont.resume(null)
                    }
            }.onFailure { e ->
                Log.w(TAG, "RuStore: SDK недоступен", e)
                cont.resume(null)
            }
        }
    }

    companion object {
        const val PREFS_NAME = "rustore_push"
        const val KEY_TOKEN = "token"
        const val TAG = "RuStoreTokenProvider"
    }
}

/** Признак, что RuStorePushClient инициализирован (см. AbikApp). */
object RuStorePushInitializer {
    @Volatile
    var initialized: Boolean = false
}
