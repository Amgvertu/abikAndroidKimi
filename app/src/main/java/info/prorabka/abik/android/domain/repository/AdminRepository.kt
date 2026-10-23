package info.prorabka.abik.android.domain.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.Team
import info.prorabka.abik.android.domain.model.User

interface AdminRepository {
    suspend fun listUsers(): ApiResult<List<User>>
    suspend fun blockUser(userId: Long): ApiResult<Unit>
    suspend fun unblockUser(userId: Long): ApiResult<Unit>
    suspend fun listTeams(): ApiResult<List<Team>>
    suspend fun createRegion(name: String, code: String, timezone: String): ApiResult<Unit>
    suspend fun createCity(regionId: Long, name: String): ApiResult<Unit>
}
