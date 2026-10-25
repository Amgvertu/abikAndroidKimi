package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.ChargeCancelRequestDto
import info.prorabka.abik.android.data.dto.ChargeCreateManualRequestDto
import info.prorabka.abik.android.data.dto.ChargeDto
import info.prorabka.abik.android.data.dto.ChargeGroupDto
import info.prorabka.abik.android.data.dto.ChargeGroupRequestDto
import info.prorabka.abik.android.data.dto.ChargeSubgroupDto
import info.prorabka.abik.android.data.dto.ChargeSubgroupRequestDto
import info.prorabka.abik.android.data.dto.ChargeTemplateDto
import info.prorabka.abik.android.data.dto.ChargeTemplateRequestDto
import info.prorabka.abik.android.data.dto.MessageResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ChargeApi {

    @GET("v1/teams/{teamId}/charges")
    suspend fun list(
        @Path("teamId") teamId: Long,
        @Query("userId") userId: Long? = null,
        @Query("groupId") groupId: Long? = null,
        @Query("subgroupId") subgroupId: Long? = null,
        @Query("status") status: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
    ): List<ChargeDto>

    @GET("v1/teams/{teamId}/charges/{chargeId}")
    suspend fun get(
        @Path("teamId") teamId: Long,
        @Path("chargeId") chargeId: Long,
    ): ChargeDto

    @POST("v1/teams/{teamId}/charges")
    suspend fun createManual(
        @Path("teamId") teamId: Long,
        @Body body: ChargeCreateManualRequestDto,
    ): List<ChargeDto>

    @POST("v1/teams/{teamId}/charges/{chargeId}/cancel")
    suspend fun cancel(
        @Path("teamId") teamId: Long,
        @Path("chargeId") chargeId: Long,
        @Body body: ChargeCancelRequestDto,
    ): ChargeDto

    // --- Группы и подгруппы (автодополнение «Назначение») ---

    @GET("v1/teams/{teamId}/charge-groups")
    suspend fun listGroups(@Path("teamId") teamId: Long): List<ChargeGroupDto>

    @POST("v1/teams/{teamId}/charge-groups")
    suspend fun createGroup(
        @Path("teamId") teamId: Long,
        @Body body: ChargeGroupRequestDto,
    ): ChargeGroupDto

    @GET("v1/teams/{teamId}/charge-groups/{groupId}/subgroups")
    suspend fun listSubgroups(
        @Path("teamId") teamId: Long,
        @Path("groupId") groupId: Long,
    ): List<ChargeSubgroupDto>

    @POST("v1/teams/{teamId}/charge-groups/{groupId}/subgroups")
    suspend fun createSubgroup(
        @Path("teamId") teamId: Long,
        @Path("groupId") groupId: Long,
        @Body body: ChargeSubgroupRequestDto,
    ): ChargeSubgroupDto

    @DELETE("v1/teams/{teamId}/charge-groups/{groupId}")
    suspend fun deleteGroup(
        @Path("teamId") teamId: Long,
        @Path("groupId") groupId: Long,
    ): MessageResponse

    // --- Шаблоны начислений ---

    @GET("v1/teams/{teamId}/charge-templates")
    suspend fun listTemplates(
        @Path("teamId") teamId: Long,
        @Query("onlyActive") onlyActive: Boolean = false,
    ): List<ChargeTemplateDto>

    @POST("v1/teams/{teamId}/charge-templates")
    suspend fun createTemplate(
        @Path("teamId") teamId: Long,
        @Body body: ChargeTemplateRequestDto,
    ): ChargeTemplateDto

    @PUT("v1/teams/{teamId}/charge-templates/{templateId}")
    suspend fun updateTemplate(
        @Path("teamId") teamId: Long,
        @Path("templateId") templateId: Long,
        @Body body: ChargeTemplateRequestDto,
    ): ChargeTemplateDto

    @POST("v1/teams/{teamId}/charge-templates/{templateId}/activate")
    suspend fun activateTemplate(
        @Path("teamId") teamId: Long,
        @Path("templateId") templateId: Long,
    ): ChargeTemplateDto

    @POST("v1/teams/{teamId}/charge-templates/{templateId}/deactivate")
    suspend fun deactivateTemplate(
        @Path("teamId") teamId: Long,
        @Path("templateId") templateId: Long,
    ): ChargeTemplateDto
}
