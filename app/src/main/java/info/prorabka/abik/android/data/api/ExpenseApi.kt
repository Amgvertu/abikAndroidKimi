package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.ExpenseDto
import info.prorabka.abik.android.data.dto.ExpenseRequestDto
import retrofit2.http.*

interface ExpenseApi {
    @GET("v1/teams/{teamId}/expenses")
    suspend fun list(@Path("teamId") teamId: Long): List<ExpenseDto>
    @POST("v1/teams/{teamId}/expenses")
    suspend fun create(@Path("teamId") teamId: Long, @Body body: ExpenseRequestDto): ExpenseDto
    @PUT("v1/teams/{teamId}/expenses/{id}")
    suspend fun update(@Path("teamId") teamId: Long, @Path("id") id: Long, @Body body: ExpenseRequestDto): ExpenseDto
    @DELETE("v1/teams/{teamId}/expenses/{id}")
    suspend fun delete(@Path("teamId") teamId: Long, @Path("id") id: Long)
}
