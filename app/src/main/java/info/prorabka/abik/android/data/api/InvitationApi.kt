package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.InvitationDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface InvitationApi {

    @GET("v1/invitations/my")
    suspend fun myIncoming(): List<InvitationDto>

    @POST("v1/invitations/{invitationId}/accept")
    suspend fun accept(@Path("invitationId") invitationId: Long): InvitationDto

    @POST("v1/invitations/{invitationId}/decline")
    suspend fun decline(@Path("invitationId") invitationId: Long): InvitationDto

    @POST("v1/invitations/{invitationId}/cancel")
    suspend fun cancel(@Path("invitationId") invitationId: Long): InvitationDto
}
