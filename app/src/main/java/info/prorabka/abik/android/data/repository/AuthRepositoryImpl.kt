package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.core.prefs.SessionManager
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.data.api.AuthApi
import info.prorabka.abik.android.data.dto.LoginRequest
import info.prorabka.abik.android.data.dto.LogoutRequest
import info.prorabka.abik.android.data.dto.PasswordResetConfirmRequest
import info.prorabka.abik.android.data.dto.PhoneRequest
import info.prorabka.abik.android.data.dto.RegisterConfirmRequest
import info.prorabka.abik.android.data.dto.RegisterStartRequest
import info.prorabka.abik.android.domain.repository.AuthRepository
import info.prorabka.abik.android.domain.repository.DeviceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val sessionManager: SessionManager,
    private val deviceRepository: DeviceRepository,
) : AuthRepository {

    companion object {
        /** Версия политики ПД, зафиксированная в приложении. */
        const val POLICY_VERSION = "1.0"
    }

    override suspend fun registerStart(
        phone: String,
        password: String,
        acceptedPd: Boolean,
    ): ApiResult<Unit> = apiCall {
        api.registerStart(RegisterStartRequest(phone, password, acceptedPd, POLICY_VERSION))
        Unit
    }

    override suspend fun registerConfirm(phone: String, code: String): ApiResult<Unit> =
        apiCall {
            val response = api.registerConfirm(RegisterConfirmRequest(phone, code))
            tokenStore.save(
                response.tokens.accessToken,
                response.tokens.refreshToken,
                response.user.id,
            )
            deviceRepository.registerCurrentDevice()
        }

    override suspend fun login(phone: String, password: String): ApiResult<Unit> = apiCall {
        val response = api.login(LoginRequest(phone, password))
        tokenStore.save(
            response.tokens.accessToken,
            response.tokens.refreshToken,
            response.user.id,
        )
        deviceRepository.registerCurrentDevice()
    }

    override suspend fun logout(): ApiResult<Unit> {
        // Сначала отвязываем устройство, пока access-токен ещё действителен
        deviceRepository.unregisterCurrentDevice()
        val refresh = tokenStore.refreshToken
        val result = if (refresh != null) {
            apiCall { api.logout(LogoutRequest(refresh)); Unit }
        } else {
            ApiResult.Success(Unit)
        }
        sessionManager.logoutLocal()
        return result
    }

    override suspend fun passwordResetStart(phone: String): ApiResult<Unit> = apiCall {
        api.passwordResetStart(PhoneRequest(phone))
        Unit
    }

    override suspend fun passwordResetConfirm(
        phone: String,
        code: String,
        newPassword: String,
    ): ApiResult<Unit> = apiCall {
        api.passwordResetConfirm(PasswordResetConfirmRequest(phone, code, newPassword))
        Unit
    }
}
