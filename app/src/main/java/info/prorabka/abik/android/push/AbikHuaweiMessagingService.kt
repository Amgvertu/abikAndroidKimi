package info.prorabka.abik.android.push

import android.util.Log
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import info.prorabka.abik.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Приём push-уведомлений Huawei (HMS) и регистрация токена на сервере.
 * Зависимости — через PushEntryPoint (Hilt KSP не принимает базовый класс из AAR).
 */
class AbikHuaweiMessagingService : HmsMessageService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Huawei: новый токен")
        val entryPoint = PushEntryPoint.get(this)
        entryPoint.huaweiTokenProvider().savedToken = token
        if (entryPoint.tokenStore().isLoggedIn) {
            serviceScope.launch { entryPoint.deviceRepository().registerCurrentDevice() }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.dataOfMap["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.dataOfMap["body"] ?: return
        PushNotifier.show(this, title, body)
    }

    private companion object {
        const val TAG = "AbikHuaweiService"
    }
}
