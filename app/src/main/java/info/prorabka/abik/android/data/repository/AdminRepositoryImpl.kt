package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.AdminApi
import info.prorabka.abik.android.data.dto.CreateCityRequestDto
import info.prorabka.abik.android.data.dto.CreateRegionRequestDto
import info.prorabka.abik.android.domain.model.Team
import info.prorabka.abik.android.domain.model.User
import info.prorabka.abik.android.domain.repository.AdminRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val api: AdminApi,
) : AdminRepository {

    override suspend fun listUsers(): ApiResult<List<User>> = apiCall {
        api.listUsers().map { User(it.id, it.phone, it.status, it.isSuperAdmin) }
    }

    override suspend fun blockUser(userId: Long): ApiResult<Unit> = apiCall {
        api.blockUser(userId)
        Unit
    }

    override suspend fun unblockUser(userId: Long): ApiResult<Unit> = apiCall {
        api.unblockUser(userId)
        Unit
    }

    override suspend fun listTeams(): ApiResult<List<Team>> = apiCall {
        api.listTeams().map {
            Team(
                id = it.id,
                name = it.name,
                cityName = it.cityName,
                regionName = it.homeRegionName,
                logoUrl = it.logoUrl,
                status = it.status,
            )
        }
    }

    override suspend fun createRegion(name: String, code: String, timezone: String): ApiResult<Unit> =
        apiCall {
            api.createRegion(CreateRegionRequestDto(name, code, timezone))
            Unit
        }

    override suspend fun createCity(regionId: Long, name: String): ApiResult<Unit> = apiCall {
        api.createCity(CreateCityRequestDto(regionId, name))
        Unit
    }

    override suspend fun stats() = apiCall {
        api.stats().let {
            info.prorabka.abik.android.domain.model.AdminStats(
                it.totalUsers, it.totalTeams, it.usersToday, it.usersWeek, it.usersMonth,
                it.teamsToday, it.teamsWeek, it.teamsMonth,
                it.last30Days.map { day -> info.prorabka.abik.android.domain.model.DailyStat(day.date, day.users, day.teams) },
            )
        }
    }

    override suspend fun publishRelease(release: info.prorabka.abik.android.domain.model.AppRelease) = apiCall {
        api.publishRelease(info.prorabka.abik.android.data.dto.AppReleaseRequestDto(
            release.versionCode, release.versionName, release.releaseNotes, release.downloadUrl, release.mandatory,
        ))
        Unit
    }
}
