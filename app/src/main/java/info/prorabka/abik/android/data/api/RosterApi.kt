package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.MessageResponse
import info.prorabka.abik.android.data.dto.RosterAddRequestDto
import info.prorabka.abik.android.data.dto.RosterItemDto
import info.prorabka.abik.android.data.dto.RosterReplaceRequestDto
import info.prorabka.abik.android.data.dto.SeasonDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Состав регулярки. Сезон передаётся query-параметром (BE-09b). */
interface RosterApi {

    @GET("v1/teams/{teamId}/seasons")
    suspend fun seasons(@Path("teamId") teamId: Long): List<SeasonDto>

    @GET("v1/teams/{teamId}/seasons/roster")
    suspend fun roster(
        @Path("teamId") teamId: Long,
        @Query("season") season: String,
    ): List<RosterItemDto>

    @PUT("v1/teams/{teamId}/seasons/roster")
    suspend fun replace(
        @Path("teamId") teamId: Long,
        @Query("season") season: String,
        @Body body: RosterReplaceRequestDto,
    ): MessageResponse

    @POST("v1/teams/{teamId}/seasons/roster/members")
    suspend fun add(
        @Path("teamId") teamId: Long,
        @Query("season") season: String,
        @Body body: RosterAddRequestDto,
    ): RosterItemDto

    @DELETE("v1/teams/{teamId}/seasons/roster/members/{userId}")
    suspend fun remove(
        @Path("teamId") teamId: Long,
        @Path("userId") userId: Long,
        @Query("season") season: String,
    ): MessageResponse
}
