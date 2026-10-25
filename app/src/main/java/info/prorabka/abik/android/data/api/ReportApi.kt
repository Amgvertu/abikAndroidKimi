package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.ChargeSummaryDto
import info.prorabka.abik.android.data.dto.MemberSummaryDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ReportApi {

    @GET("v1/teams/{teamId}/reports/summary")
    suspend fun summary(@Path("teamId") teamId: Long): ChargeSummaryDto

    @GET("v1/teams/{teamId}/reports/members")
    suspend fun members(@Path("teamId") teamId: Long): List<MemberSummaryDto>

    @Streaming
    @GET("v1/teams/{teamId}/reports/charges/export")
    suspend fun exportCharges(
        @Path("teamId") teamId: Long,
        @Query("userId") userId: Long? = null,
        @Query("groupId") groupId: Long? = null,
        @Query("status") status: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
    ): Response<ResponseBody>

    @Streaming
    @GET("v1/teams/{teamId}/reports/members/export")
    suspend fun exportMembers(@Path("teamId") teamId: Long): Response<ResponseBody>
}
