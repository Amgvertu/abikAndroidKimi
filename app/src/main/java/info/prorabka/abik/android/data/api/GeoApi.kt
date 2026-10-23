package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.CityDto
import info.prorabka.abik.android.data.dto.RegionDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GeoApi {

    @GET("api/abik/v1/geo/regions")
    suspend fun regions(): List<RegionDto>

    @GET("api/abik/v1/geo/regions/{regionId}/cities")
    suspend fun citiesByRegion(@Path("regionId") regionId: Long): List<CityDto>

    @GET("api/abik/v1/geo/cities/search")
    suspend fun searchCities(@Query("q") query: String): List<CityDto>
}
