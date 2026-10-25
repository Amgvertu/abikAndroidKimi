package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.MessageResponse
import info.prorabka.abik.android.data.dto.PaymentCreateRequestDto
import info.prorabka.abik.android.data.dto.PaymentDto
import info.prorabka.abik.android.data.dto.PaymentUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PaymentApi {

    @POST("v1/teams/{teamId}/payments")
    suspend fun create(
        @Path("teamId") teamId: Long,
        @Body body: PaymentCreateRequestDto,
    ): PaymentDto

    @GET("v1/teams/{teamId}/payments")
    suspend fun list(
        @Path("teamId") teamId: Long,
        @Query("userId") userId: Long? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("amountMin") amountMinKopecks: Long? = null,
        @Query("amountMax") amountMaxKopecks: Long? = null,
    ): List<PaymentDto>

    @GET("v1/teams/{teamId}/payments/{paymentId}")
    suspend fun get(
        @Path("teamId") teamId: Long,
        @Path("paymentId") paymentId: Long,
    ): PaymentDto

    @PUT("v1/teams/{teamId}/payments/{paymentId}")
    suspend fun update(
        @Path("teamId") teamId: Long,
        @Path("paymentId") paymentId: Long,
        @Body body: PaymentUpdateRequestDto,
    ): PaymentDto

    @DELETE("v1/teams/{teamId}/payments/{paymentId}")
    suspend fun delete(
        @Path("teamId") teamId: Long,
        @Path("paymentId") paymentId: Long,
    ): MessageResponse
}
