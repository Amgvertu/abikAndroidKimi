package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AdminOperationsApi {
    @GET("v1/admin/overview")
    suspend fun overview(@Query("from") from: String?, @Query("to") to: String?, @Query("regionId") regionId: Long?, @Query("cityId") cityId: Long?, @Query("teamId") teamId: Long?): AdminOverviewDto
    @GET("v1/admin/manage/users") suspend fun users(): List<AdminUserRowDto>
    @GET("v1/admin/manage/users/{id}") suspend fun userDetails(@Path("id") id: Long): AdminUserDetailsDto
    @POST("v1/admin/manage/users/{id}/block") suspend fun blockUser(@Path("id") id: Long, @Body body: AdminBlockRequestDto): AdminUserRowDto
    @POST("v1/admin/manage/users/{id}/unblock") suspend fun unblockUser(@Path("id") id: Long): AdminUserRowDto
    @POST("v1/admin/manage/users/{id}/ad-admin") suspend fun setAdAdmin(@Path("id") id: Long, @Body body: AdminBooleanRequestDto): AdminUserRowDto
    @GET("v1/admin/manage/teams") suspend fun teams(): List<AdminTeamRowDto>
    @GET("v1/admin/manage/teams/{id}") suspend fun teamDetails(@Path("id") id: Long): AdminTeamDetailsDto
    @POST("v1/admin/manage/teams/{id}/block") suspend fun blockTeam(@Path("id") id: Long, @Body body: AdminBlockRequestDto): AdminTeamRowDto
    @POST("v1/admin/manage/teams/{id}/unblock") suspend fun unblockTeam(@Path("id") id: Long): AdminTeamRowDto
    @POST("v1/admin/messages") suspend fun sendMessage(@Body body: AdminMessageRequestDto): AdminMessageRowDto
    @POST("v1/admin/messages/recipients") suspend fun recipientCount(@Body body: AdminMessageRequestDto): AdminRecipientCountDto
    @GET("v1/admin/messages") suspend fun messages(): List<AdminMessageRowDto>
    @GET("v1/admin/registration-events") suspend fun registrationEvents(@Query("status") status: String?): List<AdminRegistrationEventDto>
    @GET("v1/admin/ads") suspend fun ads(): List<AdvertisementDto>
    @GET("v1/admin/ad-targets/teams") suspend fun targetTeams(): List<AdminTargetDto>
    @POST("v1/admin/ads") suspend fun createAd(@Body body: AdvertisementRequestDto): AdvertisementDto
    @PUT("v1/admin/ads/{id}") suspend fun updateAd(@Path("id") id: Long, @Body body: AdvertisementRequestDto): AdvertisementDto
    @POST("v1/admin/ads/{id}/active") suspend fun setAdActive(@Path("id") id: Long, @Body body: AdminBooleanRequestDto): AdvertisementDto
    @DELETE("v1/admin/ads/{id}") suspend fun deleteAd(@Path("id") id: Long): Response<Unit>
    @GET("v1/admin/ads/{id}/stats") suspend fun adStats(@Path("id") id: Long): AdStatsDto
    @GET("v1/ads/active") suspend fun activeAd(@Query("teamId") teamId: Long?): Response<AdvertisementDto>
    @POST("v1/ads/{id}/events") suspend fun recordAdEvent(@Path("id") id: Long, @Body body: AdvertisementEventRequestDto): Response<Unit>
}
