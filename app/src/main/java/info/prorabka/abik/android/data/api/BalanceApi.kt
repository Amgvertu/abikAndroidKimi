package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.BalanceAdjustmentDto
import info.prorabka.abik.android.data.dto.BalanceAdjustmentRequestDto
import info.prorabka.abik.android.data.dto.BalanceDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BalanceApi {

    @GET("v1/teams/{teamId}/balances")
    suspend fun list(@Path("teamId") teamId: Long): List<BalanceDto>

    @GET("v1/teams/{teamId}/balances/{userId}")
    suspend fun get(
        @Path("teamId") teamId: Long,
        @Path("userId") userId: Long,
    ): BalanceDto

    @POST("v1/teams/{teamId}/balances/{userId}/adjust")
    suspend fun adjust(
        @Path("teamId") teamId: Long,
        @Path("userId") userId: Long,
        @Body body: BalanceAdjustmentRequestDto,
    ): BalanceAdjustmentDto

    @GET("v1/teams/{teamId}/balances/{userId}/adjustments")
    suspend fun adjustments(
        @Path("teamId") teamId: Long,
        @Path("userId") userId: Long,
    ): List<BalanceAdjustmentDto>
}
