package info.prorabka.abik.android.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import dagger.hilt.android.qualifiers.ApplicationContext
import info.prorabka.abik.android.BuildConfig
import info.prorabka.abik.android.core.prefs.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-only признак доступности локального abikServer.
 *
 * Не полагаемся на Wi-Fi/IP: проверяем health-endpoint и подпись сервиса.
 * Release никогда не выполняет локальный probe и не принимает сохранённый override.
 */
@Singleton
class LocalServerAvailability @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsStore: SettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val probeGeneration = AtomicLong(0)
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available

    private val client = OkHttpClient.Builder()
        .connectTimeout(1500, TimeUnit.MILLISECONDS)
        .readTimeout(1500, TimeUnit.MILLISECONDS)
        .writeTimeout(1500, TimeUnit.MILLISECONDS)
        .callTimeout(2, TimeUnit.SECONDS)
        .build()

    init {
        if (BuildConfig.DEBUG) {
            connectivityManager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = refresh()

                    override fun onLost(network: Network) {
                        // BaseUrlHolder мгновенно переключится на remote, не ожидая probe.
                        _available.value = false
                        refresh()
                    }
                },
            )
            refresh()
        } else {
            scope.launch { settingsStore.setBaseUrlOverride(null) }
        }
    }

    /** Перепроверить локальный сервер при старте и смене сети. */
    fun refresh() {
        if (!BuildConfig.DEBUG) return
        val generation = probeGeneration.incrementAndGet()
        scope.launch {
            // onAvailable может сработать до завершения настройки маршрута Wi-Fi.
            delay(250)
            val reachable = probe()
            // Более позднее событие сети отменяет устаревший результат.
            if (generation != probeGeneration.get()) return@launch
            _available.value = reachable
            if (!reachable) settingsStore.setBaseUrlOverride(null)
        }
    }

    private fun probe(): Boolean = runCatching {
        val request = Request.Builder()
            .url("${settingsStore.localBaseUrl}v1/health")
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@runCatching false
            val body = response.body?.string() ?: return@runCatching false
            val json = Json.parseToJsonElement(body).jsonObject
            json["status"]?.jsonPrimitive?.content == "ok" &&
                json["service"]?.jsonPrimitive?.content == "abik-server"
        }
    }.getOrDefault(false)
}
