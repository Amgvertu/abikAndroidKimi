package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.AuthResponse
import info.prorabka.abik.android.data.dto.LoginRequest
import info.prorabka.abik.android.data.dto.LogoutRequest
import info.prorabka.abik.android.data.dto.MessageResponse
import info.prorabka.abik.android.data.dto.PasswordResetConfirmRequest
import info.prorabka.abik.android.data.dto.PhoneRequest
import info.prorabka.abik.android.data.dto.RefreshRequest
import info.prorabka.abik.android.data.dto.RegisterConfirmRequest
import info.prorabka.abik.android.data.dto.RegisterStartRequest
import info.prorabka.abik.android.data.dto.RegisterStartResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/abik/v1/auth/register/start")
    suspend fun registerStart(@Body body: RegisterStartRequest): RegisterStartResponse

    @POST("api/abik/v1/auth/register/confirm")
    suspend fun registerConfirm(@Body body: RegisterConfirmRequest): AuthResponse

    @POST("api/abik/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/abik/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequest): MessageResponse

    /** Синхронный вариант для TokenAuthenticator. */
    @POST("api/abik/v1/auth/refresh")
    fun refreshSync(@Body body: RefreshRequest): Call<AuthResponse>

    // --- Восстановление пароля (backend-api-extension.md, раздел 2а) ---

    @POST("api/abik/v1/auth/password-reset/start")
    suspend fun passwordResetStart(@Body body: PhoneRequest): MessageResponse

    @POST("api/abik/v1/auth/password-reset/confirm")
    suspend fun passwordResetConfirm(@Body body: PasswordResetConfirmRequest): MessageResponse
}
