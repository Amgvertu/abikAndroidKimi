package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.AppReleaseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ReleaseApi {
    @GET("v1/app/releases/latest")
    suspend fun latest(@Query("platform") platform: String = "android"): AppReleaseDto
}
