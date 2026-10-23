package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.GeoApi
import info.prorabka.abik.android.domain.model.City
import info.prorabka.abik.android.domain.model.Region
import info.prorabka.abik.android.domain.repository.GeoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoRepositoryImpl @Inject constructor(
    private val api: GeoApi,
) : GeoRepository {

    override suspend fun regions(): ApiResult<List<Region>> = apiCall {
        api.regions().map { Region(it.id, it.name) }
    }

    override suspend fun citiesByRegion(regionId: Long): ApiResult<List<City>> = apiCall {
        api.citiesByRegion(regionId).map { City(it.id, it.regionId, it.name) }
    }

    override suspend fun searchCities(query: String): ApiResult<List<City>> = apiCall {
        api.searchCities(query).map { City(it.id, it.regionId, it.name) }
    }
}
