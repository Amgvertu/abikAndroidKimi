package info.prorabka.abik.android.push

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.aaid.HmsInstanceId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Токен Huawei Push Kit (HMS).
 * Работает только на устройствах Huawei с HMS Core и при наличии
 * app/agconnect-services.json из AppGallery Connect. Иначе молча неактивен.
 */
@Singleton
class HuaweiTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var savedToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    /** Сохранённый токен либо свежий из HMS (null, если HMS/agconnect не настроены). */
    suspend fun currentToken(): String? {
        savedToken?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val appId = AGConnectServicesConfig.fromContext(context)
                    .getString("client/app_id")
                if (appId.isNullOrBlank()) return@runCatching null
                HmsInstanceId.getInstance(context).getToken(appId, TOKEN_SCOPE)
            }.onFailure { Log.w(TAG, "Huawei: токен недоступен", it) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.also { savedToken = it }
        }
    }

    companion object {
        const val PREFS_NAME = "huawei_push"
        const val KEY_TOKEN = "token"
        const val TOKEN_SCOPE = "HCM"
        const val TAG = "HuaweiTokenProvider"
    }
}
