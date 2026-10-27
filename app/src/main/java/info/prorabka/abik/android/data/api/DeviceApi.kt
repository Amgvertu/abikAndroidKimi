package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.DeviceRegisterRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

interface DeviceApi {

    @POST("v1/devices")
    suspend fun register(@Body request: DeviceRegisterRequest): Map<String, String>

    @DELETE("v1/devices/{token}")
    suspend fun unregister(@Path("token") token: String): Map<String, String>
}
