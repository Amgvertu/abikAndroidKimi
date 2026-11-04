package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.ReleaseApi
import info.prorabka.abik.android.domain.model.AppRelease
import info.prorabka.abik.android.domain.repository.ReleaseRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReleaseRepositoryImpl @Inject constructor(private val api: ReleaseApi) : ReleaseRepository {
    override suspend fun latest(): ApiResult<AppRelease> = apiCall {
        api.latest().let { AppRelease(it.versionCode, it.versionName, it.releaseNotes, it.downloadUrl, it.mandatory) }
    }
}
