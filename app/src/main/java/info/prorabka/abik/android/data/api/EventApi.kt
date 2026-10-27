package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.EventCreateRequestDto
import info.prorabka.abik.android.data.dto.EventDetailDto
import info.prorabka.abik.android.data.dto.EventListItemDto
import info.prorabka.abik.android.data.dto.EventUpdateRequestDto
import info.prorabka.abik.android.data.dto.LineupSaveRequestDto
import info.prorabka.abik.android.data.dto.MessageResponse
import info.prorabka.abik.android.data.dto.SweaterColorItemDto
import info.prorabka.abik.android.data.dto.VoteRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** События: тренировки и регулярка (BE-11…BE-13). */
interface EventApi {

    @GET("v1/teams/{teamId}/events")
    suspend fun list(
        @Path("teamId") teamId: Long,
        @Query("from") from: String,
        @Query("to") to: String,
    ): List<EventListItemDto>

    @GET("v1/teams/{teamId}/events/{eventId}")
    suspend fun detail(
        @Path("teamId") teamId: Long,
        @Path("eventId") eventId: Long,
    ): EventDetailDto

    @POST("v1/teams/{teamId}/events")
    suspend fun create(
        @Path("teamId") teamId: Long,
        @Body body: EventCreateRequestDto,
    ): EventDetailDto

    @PUT("v1/teams/{teamId}/events/{eventId}")
    suspend fun update(
        @Path("teamId") teamId: Long,
        @Path("eventId") eventId: Long,
        @Query("scope") scope: String,
        @Body body: EventUpdateRequestDto,
    ): EventDetailDto

    @DELETE("v1/teams/{teamId}/events/{eventId}")
    suspend fun delete(
        @Path("teamId") teamId: Long,
        @Path("eventId") eventId: Long,
        @Query("scope") scope: String,
    ): MessageResponse

    @PUT("v1/teams/{teamId}/events/{eventId}/vote")
    suspend fun vote(
        @Path("teamId") teamId: Long,
        @Path("eventId") eventId: Long,
        @Body body: VoteRequestDto,
    ): MessageResponse

    @PUT("v1/teams/{teamId}/events/{eventId}/sweater-colors")
    suspend fun setSweaters(
        @Path("teamId") teamId: Long,
        @Path("eventId") eventId: Long,
        @Body items: List<SweaterColorItemDto>,
    ): MessageResponse

    @PUT("v1/teams/{teamId}/events/{eventId}/lineup")
    suspend fun setLineup(
        @Path("teamId") teamId: Long,
        @Path("eventId") eventId: Long,
        @Body body: LineupSaveRequestDto,
    ): MessageResponse
}
