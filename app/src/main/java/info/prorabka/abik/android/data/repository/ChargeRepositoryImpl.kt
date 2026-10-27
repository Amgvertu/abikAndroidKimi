package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.ChargeApi
import info.prorabka.abik.android.data.dto.ChargeCancelRequestDto
import info.prorabka.abik.android.data.dto.ChargeCreateManualRequestDto
import info.prorabka.abik.android.data.dto.ChargeGroupRequestDto
import info.prorabka.abik.android.data.dto.ChargeSubgroupRequestDto
import info.prorabka.abik.android.data.dto.ChargeTemplateRequestDto
import info.prorabka.abik.android.data.dto.ChargeUpdateRequestDto
import info.prorabka.abik.android.domain.model.Charge
import info.prorabka.abik.android.domain.model.ChargeDistribution
import info.prorabka.abik.android.domain.model.ChargeGroup
import info.prorabka.abik.android.domain.model.ChargeStatus
import info.prorabka.abik.android.domain.model.ChargeSubgroup
import info.prorabka.abik.android.domain.model.ChargeTemplate
import info.prorabka.abik.android.domain.repository.ChargeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargeRepositoryImpl @Inject constructor(
    private val api: ChargeApi,
) : ChargeRepository {

    override suspend fun charges(
        teamId: Long,
        userId: Long?,
        groupId: Long?,
        status: String?,
        dateFromIso: String?,
        dateToIso: String?,
    ): ApiResult<List<Charge>> = apiCall {
        api.list(teamId, userId, groupId, null, status, dateFromIso, dateToIso).map { dto ->
            Charge(
                id = dto.id,
                groupName = dto.groupName ?: "",
                subgroupName = dto.subgroupName,
                userId = dto.userId,
                userName = listOfNotNull(dto.userLastName, dto.userFirstName).joinToString(" "),
                amountKopecks = dto.amountKopecks,
                paidKopecks = dto.paidKopecks,
                remainingKopecks = dto.remainingKopecks,
                status = runCatching { ChargeStatus.valueOf(dto.status) }
                    .getOrDefault(ChargeStatus.ACTIVE),
                description = dto.description,
                dueDate = dto.dueDate,
                createdAt = dto.createdAt,
            )
        }
    }

    override suspend fun createManual(
        teamId: Long,
        groupId: Long,
        subgroupId: Long?,
        userIds: Set<Long>,
        amountKopecks: Long,
        description: String?,
        dueDateIso: String,
    ): ApiResult<Unit> = apiCall {
        api.createManual(
            teamId,
            ChargeCreateManualRequestDto(
                groupId = groupId,
                subgroupId = subgroupId,
                userIds = userIds,
                amountKopecks = amountKopecks,
                description = description,
                dueDate = dueDateIso,
            ),
        )
        Unit
    }

    override suspend fun cancel(teamId: Long, chargeId: Long, reason: String): ApiResult<Unit> =
        apiCall {
            api.cancel(teamId, chargeId, ChargeCancelRequestDto(reason))
            Unit
        }

    override suspend fun chargeById(teamId: Long, chargeId: Long): ApiResult<Charge> = apiCall {
        api.get(teamId, chargeId).let { dto ->
            Charge(
                id = dto.id,
                groupName = dto.groupName ?: "",
                subgroupName = dto.subgroupName,
                userId = dto.userId,
                userName = listOfNotNull(dto.userLastName, dto.userFirstName).joinToString(" "),
                amountKopecks = dto.amountKopecks,
                paidKopecks = dto.paidKopecks,
                remainingKopecks = dto.remainingKopecks,
                status = runCatching { ChargeStatus.valueOf(dto.status) }
                    .getOrDefault(ChargeStatus.ACTIVE),
                description = dto.description,
                dueDate = dto.dueDate,
                createdAt = dto.createdAt,
            )
        }
    }

    override suspend fun updateCharge(
        teamId: Long,
        chargeId: Long,
        groupId: Long,
        subgroupId: Long?,
        amountKopecks: Long,
        description: String?,
        dueDateIso: String,
    ): ApiResult<Unit> = apiCall {
        api.update(
            teamId,
            chargeId,
            ChargeUpdateRequestDto(
                groupId = groupId,
                subgroupId = subgroupId,
                amountKopecks = amountKopecks,
                description = description,
                dueDate = dueDateIso,
            ),
        )
        Unit
    }

    override suspend fun deleteCharge(teamId: Long, chargeId: Long): ApiResult<Unit> = apiCall {
        api.delete(teamId, chargeId)
        Unit
    }

    override suspend fun groups(teamId: Long): ApiResult<List<ChargeGroup>> = apiCall {
        api.listGroups(teamId).map { ChargeGroup(it.id, it.name) }
    }

    override suspend fun createGroup(teamId: Long, name: String): ApiResult<ChargeGroup> = apiCall {
        val dto = api.createGroup(teamId, ChargeGroupRequestDto(name))
        ChargeGroup(dto.id, dto.name)
    }

    override suspend fun subgroups(teamId: Long, groupId: Long): ApiResult<List<ChargeSubgroup>> =
        apiCall {
            api.listSubgroups(teamId, groupId).map { ChargeSubgroup(it.id, it.groupId, it.name) }
        }

    override suspend fun createSubgroup(
        teamId: Long,
        groupId: Long,
        name: String,
    ): ApiResult<ChargeSubgroup> = apiCall {
        val dto = api.createSubgroup(teamId, groupId, ChargeSubgroupRequestDto(name))
        ChargeSubgroup(dto.id, dto.groupId, dto.name)
    }

    override suspend fun templates(
        teamId: Long,
        onlyActive: Boolean,
    ): ApiResult<List<ChargeTemplate>> = apiCall {
        api.listTemplates(teamId, onlyActive).map { dto ->
            ChargeTemplate(
                id = dto.id,
                groupId = dto.groupId,
                subgroupId = dto.subgroupId,
                name = dto.name,
                amountKopecks = dto.amountKopecks,
                type = dto.type,
                periodicity = dto.periodicity,
                dayOfMonth = dto.dayOfMonth,
                distribution = runCatching { ChargeDistribution.valueOf(dto.distribution) }
                    .getOrDefault(ChargeDistribution.FIXED),
                active = dto.active,
                memberIds = dto.memberIds,
            )
        }
    }

    override suspend fun saveTemplate(
        teamId: Long,
        templateId: Long?,
        groupId: Long,
        subgroupId: Long?,
        name: String,
        amountKopecks: Long,
        dayOfMonth: Int,
        distribution: ChargeDistribution,
        memberIds: Set<Long>,
    ): ApiResult<Unit> = apiCall {
        val body = ChargeTemplateRequestDto(
            groupId = groupId,
            subgroupId = subgroupId,
            name = name,
            amountKopecks = amountKopecks,
            type = "SYSTEMATIC",
            periodicity = "MONTHLY",
            dayOfMonth = dayOfMonth,
            distribution = distribution.name,
            memberIds = memberIds,
        )
        if (templateId != null && templateId > 0) {
            api.updateTemplate(teamId, templateId, body)
        } else {
            api.createTemplate(teamId, body)
        }
        Unit
    }

    override suspend fun setTemplateActive(
        teamId: Long,
        templateId: Long,
        active: Boolean,
    ): ApiResult<Unit> = apiCall {
        if (active) {
            api.activateTemplate(teamId, templateId)
        } else {
            api.deactivateTemplate(teamId, templateId)
        }
        Unit
    }
}
