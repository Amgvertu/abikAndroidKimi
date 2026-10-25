package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.InvitationDto
import info.prorabka.abik.android.data.dto.InviteRequestDto
import info.prorabka.abik.android.data.dto.MessageResponse
import info.prorabka.abik.android.data.dto.SetJerseyNumberRequestDto
import info.prorabka.abik.android.data.dto.TeamCreateRequestDto
import info.prorabka.abik.android.data.dto.TeamDetailDto
import info.prorabka.abik.android.data.dto.TeamDto
import info.prorabka.abik.android.data.dto.TeamMemberDto
import info.prorabka.abik.android.data.dto.TransferOwnershipRequestDto
import info.prorabka.abik.android.data.dto.UpdateMemberRoleRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface TeamApi {

    @GET("v1/teams/my")
    suspend fun myTeams(): List<TeamDto>

    @POST("v1/teams")
    suspend fun create(@Body body: TeamCreateRequestDto): TeamDetailDto

    @GET("v1/teams/{teamId}")
    suspend fun detail(@Path("teamId") teamId: Long): TeamDetailDto

    @PUT("v1/teams/{teamId}")
    suspend fun update(
        @Path("teamId") teamId: Long,
        @Body body: TeamCreateRequestDto,
    ): TeamDetailDto

    @POST("v1/teams/{teamId}/archive")
    suspend fun archive(@Path("teamId") teamId: Long): TeamDetailDto

    @POST("v1/teams/{teamId}/transfer-ownership")
    suspend fun transferOwnership(
        @Path("teamId") teamId: Long,
        @Body body: TransferOwnershipRequestDto,
    ): TeamDetailDto

    @GET("v1/teams/{teamId}/members")
    suspend fun members(@Path("teamId") teamId: Long): List<TeamMemberDto>

    @PUT("v1/teams/{teamId}/members/{targetUserId}/role")
    suspend fun changeRole(
        @Path("teamId") teamId: Long,
        @Path("targetUserId") targetUserId: Long,
        @Body body: UpdateMemberRoleRequestDto,
    ): TeamMemberDto

    @PUT("v1/teams/{teamId}/members/{targetUserId}/jersey")
    suspend fun setJersey(
        @Path("teamId") teamId: Long,
        @Path("targetUserId") targetUserId: Long,
        @Body body: SetJerseyNumberRequestDto,
    ): TeamMemberDto

    @DELETE("v1/teams/{teamId}/members/{targetUserId}")
    suspend fun excludeMember(
        @Path("teamId") teamId: Long,
        @Path("targetUserId") targetUserId: Long,
    ): MessageResponse

    @POST("v1/teams/{teamId}/members/{targetUserId}/restore")
    suspend fun restoreMember(
        @Path("teamId") teamId: Long,
        @Path("targetUserId") targetUserId: Long,
    ): TeamMemberDto

    @POST("v1/teams/{teamId}/leave")
    suspend fun leave(@Path("teamId") teamId: Long): MessageResponse

    @POST("v1/teams/{teamId}/invitations")
    suspend fun invite(
        @Path("teamId") teamId: Long,
        @Body body: InviteRequestDto,
    ): InvitationDto

    @GET("v1/teams/{teamId}/invitations")
    suspend fun outgoingInvitations(@Path("teamId") teamId: Long): List<InvitationDto>
}
