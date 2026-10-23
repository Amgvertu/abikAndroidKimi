package info.prorabka.abik.android.core.network

import info.prorabka.abik.android.core.prefs.SessionManager
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.data.api.AuthApi
import info.prorabka.abik.android.data.dto.RefreshRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * При 401 пробует обновить пару токенов через refresh.
 * При неудаче — сбрасывает сессию (приложение уходит на экран входа).
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val sessionManager: SessionManager,
    private val authApi: Provider<AuthApi>,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Не зацикливаемся: после неудачной попытки сдаёмся
        if (responseCount(response) >= 2) return null
        val path = response.request.url.encodedPath
        if (path.contains("/v1/auth/")) return null

        val refresh = tokenStore.refreshToken ?: run {
            sessionManager.expireSession()
            return null
        }

        synchronized(this) {
            // Возможно, другой поток уже обновил токен
            val currentAccess = tokenStore.accessToken
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
            if (!currentAccess.isNullOrBlank() && currentAccess != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccess")
                    .build()
            }

            val refreshResponse = runCatching {
                authApi.get().refreshSync(RefreshRequest(refresh)).execute()
            }.getOrNull()

            if (refreshResponse == null || !refreshResponse.isSuccessful) {
                sessionManager.expireSession()
                return null
            }
            val body = refreshResponse.body() ?: run {
                sessionManager.expireSession()
                return null
            }
            tokenStore.save(
                access = body.tokens.accessToken,
                refresh = body.tokens.refreshToken,
                userId = body.user.id,
            )
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${body.tokens.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
