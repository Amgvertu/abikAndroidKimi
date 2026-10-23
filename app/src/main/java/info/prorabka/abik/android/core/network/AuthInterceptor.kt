package info.prorabka.abik.android.core.network

import info.prorabka.abik.android.core.prefs.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Добавляет Authorization: Bearer <access> ко всем запросам,
 * кроме публичных /auth и /geo.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (path.contains("/v1/auth/") || path.contains("/v1/geo/")) {
            return chain.proceed(request)
        }
        val token = tokenStore.accessToken
        if (token.isNullOrBlank()) {
            return chain.proceed(request)
        }
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build(),
        )
    }
}
