package info.prorabka.abik.android.domain.repository

import info.prorabka.abik.android.core.network.ApiResult

interface GuideRepository {
    suspend fun completed(): ApiResult<List<String>>
    suspend fun complete(screenKey: String, roleKey: String): ApiResult<Unit>
}
