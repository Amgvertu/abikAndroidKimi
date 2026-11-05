package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.OfflineOperationRequestDto
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface OfflineApi {
    @POST("v1/teams/{teamId}/offline-operations")
    suspend fun execute(
        @Path("teamId") teamId: Long,
        @Body body: OfflineOperationRequestDto,
    ): JsonElement
}
