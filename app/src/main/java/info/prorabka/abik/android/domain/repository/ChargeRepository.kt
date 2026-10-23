package info.prorabka.abik.android.domain.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.Charge
import info.prorabka.abik.android.domain.model.ChargeDistribution
import info.prorabka.abik.android.domain.model.ChargeGroup
import info.prorabka.abik.android.domain.model.ChargeSubgroup
import info.prorabka.abik.android.domain.model.ChargeTemplate

interface ChargeRepository {
    suspend fun charges(
        teamId: Long,
        userId: Long? = null,
        groupId: Long? = null,
        status: String? = null,
        dateFromIso: String? = null,
        dateToIso: String? = null,
    ): ApiResult<List<Charge>>

    suspend fun createManual(
        teamId: Long,
        groupId: Long,
        subgroupId: Long?,
        userIds: Set<Long>,
        amountKopecks: Long,
        description: String?,
        dueDateIso: String,
    ): ApiResult<Unit>

    suspend fun cancel(teamId: Long, chargeId: Long, reason: String): ApiResult<Unit>

    suspend fun groups(teamId: Long): ApiResult<List<ChargeGroup>>
    suspend fun createGroup(teamId: Long, name: String): ApiResult<ChargeGroup>
    suspend fun subgroups(teamId: Long, groupId: Long): ApiResult<List<ChargeSubgroup>>
    suspend fun createSubgroup(teamId: Long, groupId: Long, name: String): ApiResult<ChargeSubgroup>

    suspend fun templates(teamId: Long, onlyActive: Boolean = false): ApiResult<List<ChargeTemplate>>

    suspend fun saveTemplate(
        teamId: Long,
        templateId: Long?,
        groupId: Long,
        subgroupId: Long?,
        name: String,
        amountKopecks: Long,
        dayOfMonth: Int,
        distribution: ChargeDistribution,
        memberIds: Set<Long>,
    ): ApiResult<Unit>

    suspend fun setTemplateActive(teamId: Long, templateId: Long, active: Boolean): ApiResult<Unit>
}
