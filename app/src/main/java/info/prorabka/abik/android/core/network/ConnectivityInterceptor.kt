package info.prorabka.abik.android.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Не заставляет пользователя ждать таймаут каждого запроса без сети.
 * VALIDATED специально не требуется: локальный сервер доступен и без интернета.
 */
@Singleton
class ConnectivityInterceptor @Inject constructor(
    @ApplicationContext context: Context,
) : Interceptor {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Volatile
    private var retryAfterMs = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        val now = SystemClock.elapsedRealtime()
        if (!hasNetwork() || now < retryAfterMs) {
            throw IOException("Сеть недоступна")
        }
        return try {
            chain.proceed(chain.request()).also { retryAfterMs = 0L }
        } catch (e: IOException) {
            // После первого сбоя остальные экраны сразу используют локальный кэш.
            retryAfterMs = now + FAILURE_COOLDOWN_MS
            throw e
        }
    }

    private fun hasNetwork(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private companion object {
        const val FAILURE_COOLDOWN_MS = 10_000L
    }
}
