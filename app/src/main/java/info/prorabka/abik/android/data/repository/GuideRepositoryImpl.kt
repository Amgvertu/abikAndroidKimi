package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.GuideApi
import info.prorabka.abik.android.domain.repository.GuideRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuideRepositoryImpl @Inject constructor(private val api: GuideApi) : GuideRepository {
    override suspend fun completed(): ApiResult<List<String>> = apiCall { api.completed() }
    override suspend fun complete(screenKey: String, roleKey: String): ApiResult<Unit> = apiCall {
        api.complete(screenKey, roleKey)
    }
}
