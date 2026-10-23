package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.AdminUserDto
import info.prorabka.abik.android.data.dto.CityDto
import info.prorabka.abik.android.data.dto.CreateCityRequestDto
import info.prorabka.abik.android.data.dto.CreateRegionRequestDto
import info.prorabka.abik.android.data.dto.RegionDto
import info.prorabka.abik.android.data.dto.TeamDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AdminApi {

    @GET("api/abik/v1/admin/users")
    suspend fun listUsers(): List<AdminUserDto>

    @POST("api/abik/v1/admin/users/{userId}/block")
    suspend fun blockUser(@Path("userId") userId: Long): AdminUserDto

    @POST("api/abik/v1/admin/users/{userId}/unblock")
    suspend fun unblockUser(@Path("userId") userId: Long): AdminUserDto

    @GET("api/abik/v1/admin/teams")
    suspend fun listTeams(): List<TeamDto>

    @POST("api/abik/v1/admin/regions")
    suspend fun createRegion(@Body body: CreateRegionRequestDto): RegionDto

    @POST("api/abik/v1/admin/cities")
    suspend fun createCity(@Body body: CreateCityRequestDto): CityDto
}
