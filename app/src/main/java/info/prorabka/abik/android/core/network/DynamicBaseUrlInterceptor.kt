package info.prorabka.abik.android.core.network

import android.util.Log
import info.prorabka.abik.android.BuildConfig
import info.prorabka.abik.android.core.prefs.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
        if (BuildConfig.DEBUG) {
            // В debug — всегда локальный сервер, override из DataStore игнорируем
            baseUrl = BuildConfig.DEFAULT_BASE_URL
            Log.d("BaseUrlHolder", "debug mode, force baseUrl=$baseUrl")
        } else {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                settingsStore.baseUrlOverride.collect { override ->
                    baseUrl = override ?: BuildConfig.DEFAULT_BASE_URL
                    Log.d("BaseUrlHolder", "override=$override -> effective=$baseUrl")
                }
            }
        }
    }
}

/**
 * Подставляет актуальный base URL (scheme/host/port) в каждый запрос —
 * позволяет переключать сервер без пересоздания Retrofit.
 */
@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val holder: BaseUrlHolder,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val base = runCatching { holder.baseUrl.toHttpUrl() }.getOrNull()
            ?: return chain.proceed(chain.request())
        val url = chain.request().url
        if (url.scheme == base.scheme && url.host == base.host && url.port == base.port) {
            return chain.proceed(chain.request())
        }
        val rewritten = url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
        return chain.proceed(chain.request().newBuilder().url(rewritten).build())
    }
}
