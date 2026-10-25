package info.prorabka.abik.android.core.network

import info.prorabka.abik.android.BuildConfig
import info.prorabka.abik.android.core.prefs.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Текущий base URL. Следит за переопределением из SettingsStore,
 * читается синхронно из интерсептора.
 */
@Singleton
class BaseUrlHolder @Inject constructor(settingsStore: SettingsStore) {
    @Volatile
    var baseUrl: String = BuildConfig.DEFAULT_BASE_URL
        private set

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settingsStore.baseUrlOverride.collect { override ->
                baseUrl = override ?: BuildConfig.DEFAULT_BASE_URL
            }
        }
    }
}

/**
 * Подставляет актуальный base URL в каждый запрос — позволяет переключать
 * сервер без пересоздания Retrofit. Переносит не только scheme/host/port,
 * но и префикс пути: прод ходит через nginx на /api/, а локальный сервер —
 * напрямую в context-path /api/abik.
 */
@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val holder: BaseUrlHolder,
) : Interceptor {

    private val defaultBase: HttpUrl = BuildConfig.DEFAULT_BASE_URL.toHttpUrl()

    override fun intercept(chain: Interceptor.Chain): Response {
        val target = runCatching { holder.baseUrl.toHttpUrl() }.getOrNull()
            ?: return chain.proceed(chain.request())
        val url = chain.request().url

        val targetPrefix = target.encodedPath.removeSuffix("/")
        val defaultPrefix = defaultBase.encodedPath.removeSuffix("/")

        val sameOrigin = url.scheme == target.scheme && url.host == target.host && url.port == target.port
        val prefixOk = targetPrefix.isEmpty() ||
            url.encodedPath == targetPrefix || url.encodedPath.startsWith("$targetPrefix/")
        if (sameOrigin && prefixOk) return chain.proceed(chain.request())

        // Отрезаем префикс базы по умолчанию (/api) и добавляем префикс цели (/api/abik)
        val relative = url.encodedPath
            .removePrefix(defaultPrefix)
            .removePrefix("/")
        val newPath = if (targetPrefix.isEmpty()) "/$relative" else "$targetPrefix/$relative"

        val rewritten = url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .encodedPath(newPath)
            .build()
        return chain.proceed(chain.request().newBuilder().url(rewritten).build())
    }
}
