package info.prorabka.abik.android.data.api

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GuideApi {
    @GET("v1/guide/progress")
    suspend fun completed(): List<String>

    @POST("v1/guide/progress/{screenKey}/{roleKey}")
    suspend fun complete(@Path("screenKey") screenKey: String, @Path("roleKey") roleKey: String)
}
