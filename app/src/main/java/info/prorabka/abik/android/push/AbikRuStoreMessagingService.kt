package info.prorabka.abik.android.push

import android.util.Log
import info.prorabka.abik.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.rustore.sdk.pushclient.messaging.model.RemoteMessage
import ru.rustore.sdk.pushclient.messaging.service.RuStoreMessagingService

/**
 * Приём push-уведомлений RuStore и регистрация токена на сервере.
 * Зависимости — через PushEntryPoint (Hilt KSP не принимает базовый класс из AAR).
 */
class AbikRuStoreMessagingService : RuStoreMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "RuStore: новый токен")
        val entryPoint = PushEntryPoint.get(this)
        entryPoint.ruStoreTokenProvider().savedToken = token
        if (entryPoint.tokenStore().isLoggedIn) {
            serviceScope.launch { entryPoint.deviceRepository().registerCurrentDevice() }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        PushNotifier.show(this, title, body)
    }

    private companion object {
        const val TAG = "AbikRuStoreService"
    }
}
