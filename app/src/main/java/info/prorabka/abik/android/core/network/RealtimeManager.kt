package info.prorabka.abik.android.core.network

import info.prorabka.abik.android.core.prefs.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/** Подключение к серверному сигналу изменений. Сообщения не содержат данных. */
@Singleton
class RealtimeManager @Inject constructor(
    private val client: OkHttpClient,
    private val baseUrlHolder: BaseUrlHolder,
    private val tokenStore: TokenStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision
    private var socket: WebSocket? = null
    private var running = false
    private var attempt = 0

    @Synchronized
    fun start() {
        if (running) return
        running = true
        connect()
    }

    @Synchronized
    fun stop() {
        running = false
        socket?.close(1000, "Приложение свёрнуто")
        socket = null
    }

    @Synchronized
    private fun connect() {
        if (!running || socket != null) return
        val token = tokenStore.accessToken
        if (token.isNullOrBlank()) { running = false; return }
        val base = runCatching { baseUrlHolder.baseUrl.toHttpUrl() }.getOrNull() ?: return
        val url = base.newBuilder()
            .scheme(if (base.isHttps) "https" else "http")
            .encodedPath(base.encodedPath.trimEnd('/') + "/ws")
            .setQueryParameter("token", token)
            .build()
        socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { attempt = 0 }
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("\"changed\"")) _revision.value = _revision.value + 1
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                disconnected(webSocket)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                disconnected(webSocket)
            }
        })
    }

    private fun disconnected(closed: WebSocket) {
        synchronized(this) {
            if (socket !== closed) return
            socket = null
            if (!running) return
            attempt += 1
        }
        val wait = minOf(30_000L, 1_000L * (1L shl attempt.coerceAtMost(5)))
        scope.launch { delay(wait); connect() }
    }
}
