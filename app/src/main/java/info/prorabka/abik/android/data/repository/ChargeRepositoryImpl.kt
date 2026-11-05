package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.ChargeApi
import info.prorabka.abik.android.data.dto.ChargeCancelRequestDto
import info.prorabka.abik.android.data.dto.ChargeCreateManualRequestDto
import info.prorabka.abik.android.data.dto.ChargeGroupDto
import info.prorabka.abik.android.data.dto.ChargeSubgroupDto
import info.prorabka.abik.android.data.dto.ChargeGroupRequestDto
import info.prorabka.abik.android.data.dto.ChargeListRequestDto
import info.prorabka.abik.android.data.dto.ChargeSubgroupRequestDto
import info.prorabka.abik.android.data.dto.ChargeTemplateRequestDto
import info.prorabka.abik.android.data.dto.ChargeUpdateRequestDto
import info.prorabka.abik.android.domain.model.Charge
import info.prorabka.abik.android.domain.model.ChargeDistribution
import info.prorabka.abik.android.domain.model.ChargeGroup
import info.prorabka.abik.android.domain.model.ChargeList
import info.prorabka.abik.android.domain.model.ChargeStatus
import info.prorabka.abik.android.domain.model.ChargeSubgroup
import info.prorabka.abik.android.domain.model.ChargeTemplate
import info.prorabka.abik.android.domain.repository.ChargeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargeRepositoryImpl @Inject constructor(
    private val api: ChargeApi,
    private val queue: info.prorabka.abik.android.data.local.OfflineQueue,
    private val cache: info.prorabka.abik.android.data.local.CacheStore,
    private val json: kotlinx.serialization.json.Json,
    private val tokenStore: info.prorabka.abik.android.core.prefs.TokenStore,
) : ChargeRepository {

    override suspend fun charges(
        teamId: Long,
        userId: Long?,
        groupId: Long?,
        status: String?,
        dateFromIso: String?,
        dateToIso: String?,
    ): ApiResult<List<Charge>> {
        val key = "${tokenStore.userId}:team:$teamId:charges:$userId:$groupId:$status:$dateFromIso:$dateToIso"
        val serializer = kotlinx.serialization.builtins.ListSerializer(
            info.prorabka.abik.android.data.dto.ChargeDto.serializer())
        val local = queue.forType(teamId, info.prorabka.abik.android.data.local.OfflineOperationTypes.CHARGE_CREATE)
        when (val result = apiCall { api.list(teamId, userId, groupId, null, status, dateFromIso, dateToIso) }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                queue.clearSynced(teamId, info.prorabka.abik.android.data.local.OfflineOperationTypes.CHARGE_CREATE)
                return ApiResult.Success(result.data.map { it.toDomain() } + pendingCharges(teamId,
                    local.filter { it.isLocalOnly }, userId, groupId, status, dateFromIso, dateToIso))
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer).orEmpty()
                if (cached.isEmpty() && local.isEmpty()) return result
                return ApiResult.Success(cached.map { it.toDomain() } + pendingCharges(teamId,
                    local, userId, groupId, status, dateFromIso, dateToIso))
            }
            is ApiResult.HttpError -> return result
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
    ): ApiResult<info.prorabka.abik.android.domain.model.SyncOutcome> {
        val body = ChargeCreateManualRequestDto(groupId, subgroupId, userIds, amountKopecks, description, dueDateIso)
        return when (val result = apiCall { api.createManual(teamId, body) }) {
            is ApiResult.Success -> ApiResult.Success(info.prorabka.abik.android.domain.model.SyncOutcome.SYNCED)
            is ApiResult.NetworkError -> createManualOffline(teamId, groupId, "", subgroupId, null,
                userIds, amountKopecks, description, dueDateIso)
            is ApiResult.HttpError -> result
        }
    }

    override suspend fun createManualOffline(
        teamId: Long,
        groupId: Long?,
        groupName: String,
        subgroupId: Long?,
        subgroupName: String?,
        userIds: Set<Long>,
        amountKopecks: Long,
        description: String?,
        dueDateIso: String,
    ): ApiResult<info.prorabka.abik.android.domain.model.SyncOutcome> {
        val body = info.prorabka.abik.android.data.dto.OfflineChargeCreateRequestDto(
            groupId = groupId, groupName = groupName.ifBlank { null },
            subgroupId = subgroupId, subgroupName = subgroupName?.ifBlank { null },
            userIds = userIds, amountKopecks = amountKopecks,
            description = description, dueDate = dueDateIso,
        )
        queue.enqueue(teamId, info.prorabka.abik.android.data.local.OfflineOperationTypes.CHARGE_CREATE,
            body, info.prorabka.abik.android.data.dto.OfflineChargeCreateRequestDto.serializer())
        return ApiResult.Success(info.prorabka.abik.android.domain.model.SyncOutcome.PENDING)
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

    override suspend fun groups(teamId: Long): ApiResult<List<ChargeGroup>> {
        val key = "${tokenStore.userId}:team:$teamId:charge-groups"
        val serializer = kotlinx.serialization.builtins.ListSerializer(ChargeGroupDto.serializer())
        when (val result = apiCall { api.listGroups(teamId) }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                return ApiResult.Success(result.data.map { ChargeGroup(it.id, it.name) })
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer)
                return if (cached != null) ApiResult.Success(cached.map { ChargeGroup(it.id, it.name) }) else result
            }
            is ApiResult.HttpError -> return result
        }
    }

    override suspend fun createGroup(teamId: Long, name: String): ApiResult<ChargeGroup> = apiCall {
        val dto = api.createGroup(teamId, ChargeGroupRequestDto(name))
        ChargeGroup(dto.id, dto.name)
    }

    override suspend fun subgroups(teamId: Long, groupId: Long): ApiResult<List<ChargeSubgroup>> {
        val key = "${tokenStore.userId}:team:$teamId:charge-group:$groupId:subgroups"
        val serializer = kotlinx.serialization.builtins.ListSerializer(ChargeSubgroupDto.serializer())
        when (val result = apiCall { api.listSubgroups(teamId, groupId) }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                return ApiResult.Success(result.data.map { ChargeSubgroup(it.id, it.groupId, it.name) })
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer)
                return if (cached != null) {
                    ApiResult.Success(cached.map { ChargeSubgroup(it.id, it.groupId, it.name) })
                } else result
            }
            is ApiResult.HttpError -> return result
        }
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

    // --- Списки получателей ---

    override suspend fun chargeLists(teamId: Long): ApiResult<List<ChargeList>> {
        val key = "${tokenStore.userId}:team:$teamId:charge-lists"
        val serializer = kotlinx.serialization.builtins.ListSerializer(
            info.prorabka.abik.android.data.dto.ChargeListDto.serializer())
        when (val result = apiCall { api.listChargeLists(teamId) }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                return ApiResult.Success(result.data.map { ChargeList(it.id, it.name, it.memberIds) })
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer)
                return if (cached != null) {
                    ApiResult.Success(cached.map { ChargeList(it.id, it.name, it.memberIds) })
                } else result
            }
            is ApiResult.HttpError -> return result
        }
    }

    override suspend fun createChargeList(
        teamId: Long,
        name: String,
        memberIds: Set<Long>,
    ): ApiResult<ChargeList> = apiCall {
        val dto = api.createChargeList(
            teamId,
            ChargeListRequestDto(name = name, memberIds = memberIds),
        )
        ChargeList(id = dto.id, name = dto.name, memberIds = dto.memberIds)
    }

    override suspend fun updateChargeList(
        teamId: Long,
        listId: Long,
        name: String,
        memberIds: Set<Long>,
    ): ApiResult<Unit> = apiCall {
        api.updateChargeList(teamId, listId, ChargeListRequestDto(name = name, memberIds = memberIds))
        Unit
    }

    override suspend fun deleteChargeList(teamId: Long, listId: Long): ApiResult<Unit> = apiCall {
        api.deleteChargeList(teamId, listId)
        Unit
    }

    private suspend fun pendingCharges(
        teamId: Long,
        operations: List<info.prorabka.abik.android.data.local.OfflineOperationEntity>,
        userId: Long?,
        groupId: Long?,
        status: String?,
        dateFrom: String?,
        dateTo: String?,
    ): List<Charge> {
        val groups = cache.read("${tokenStore.userId}:team:$teamId:charge-groups",
            kotlinx.serialization.builtins.ListSerializer(ChargeGroupDto.serializer())).orEmpty()
        val members = cache.read("${tokenStore.userId}:team:$teamId:members",
            kotlinx.serialization.builtins.ListSerializer(
                info.prorabka.abik.android.data.dto.TeamMemberDto.serializer())).orEmpty()
        return operations.flatMap { operation ->
            val body = runCatching {
                json.decodeFromString(
                    info.prorabka.abik.android.data.dto.OfflineChargeCreateRequestDto.serializer(),
                    operation.payloadJson)
            }.getOrNull() ?: return@flatMap emptyList()
            if (groupId != null && body.groupId != groupId) return@flatMap emptyList()
            if (status != null && status != ChargeStatus.ACTIVE.name) return@flatMap emptyList()
            if (dateFrom != null && body.dueDate < dateFrom) return@flatMap emptyList()
            if (dateTo != null && body.dueDate > dateTo) return@flatMap emptyList()
            val groupName = body.groupName ?: groups.firstOrNull { it.id == body.groupId }?.name.orEmpty()
            body.userIds.filter { userId == null || it == userId }.map { recipientId ->
                val member = members.firstOrNull { it.userId == recipientId }
                Charge(
                    id = localId("${operation.operationId}:$recipientId"),
                    groupName = groupName, subgroupName = body.subgroupName,
                    userId = recipientId,
                    userName = listOfNotNull(member?.lastName, member?.firstName).joinToString(" "),
                    amountKopecks = body.amountKopecks, paidKopecks = 0,
                    remainingKopecks = body.amountKopecks, status = ChargeStatus.ACTIVE,
                    description = body.description, dueDate = body.dueDate,
                    createdAt = operation.localCreatedAt,
                    pendingSync = operation.isLocalOnly, syncError = operation.displaySyncError,
                )
            }
        }
    }
}

private fun info.prorabka.abik.android.data.dto.ChargeDto.toDomain() = Charge(
    id = id, groupName = groupName ?: "", subgroupName = subgroupName, userId = userId,
    userName = listOfNotNull(userLastName, userFirstName).joinToString(" "),
    amountKopecks = amountKopecks, paidKopecks = paidKopecks, remainingKopecks = remainingKopecks,
    status = runCatching { ChargeStatus.valueOf(status) }.getOrDefault(ChargeStatus.ACTIVE),
    description = description, dueDate = dueDate, createdAt = createdAt,
)
