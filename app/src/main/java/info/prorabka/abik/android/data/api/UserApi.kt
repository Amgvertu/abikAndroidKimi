package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.MessageResponse
import info.prorabka.abik.android.data.dto.PhoneChangeConfirmRequest
import info.prorabka.abik.android.data.dto.PhoneChangeSendNewRequest
import info.prorabka.abik.android.data.dto.PhoneChangeTokenResponse
import info.prorabka.abik.android.data.dto.SmsCodeRequest
import info.prorabka.abik.android.data.dto.UserDto
import info.prorabka.abik.android.data.dto.UserProfileRequest
import info.prorabka.abik.android.data.dto.UserProfileResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {

    @GET("v1/users/me")
    suspend fun me(): UserDto

    @GET("v1/users/me/profile")
    suspend fun myProfile(): UserProfileResponse

    @PUT("v1/users/me/profile")
    suspend fun updateMyProfile(@Body body: UserProfileRequest): UserProfileResponse

    @GET("v1/users/{userId}/profile")
    suspend fun profileOfUser(@Path("userId") userId: Long): UserProfileResponse

    // --- Смена номера (backend-api-extension.md, раздел 2а.3) ---

    @POST("v1/users/me/phone-change/start")
    suspend fun phoneChangeStart(): MessageResponse

    @POST("v1/users/me/phone-change/verify-old")
    suspend fun phoneChangeVerifyOld(@Body body: SmsCodeRequest): PhoneChangeTokenResponse

    @POST("v1/users/me/phone-change/send-new")
    suspend fun phoneChangeSendNew(@Body body: PhoneChangeSendNewRequest): MessageResponse

    @POST("v1/users/me/phone-change/confirm")
    suspend fun phoneChangeConfirm(@Body body: PhoneChangeConfirmRequest): MessageResponse
}
