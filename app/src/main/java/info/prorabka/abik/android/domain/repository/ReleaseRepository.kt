package info.prorabka.abik.android.domain.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.AppRelease

interface ReleaseRepository {
    suspend fun latest(): ApiResult<AppRelease>
}
