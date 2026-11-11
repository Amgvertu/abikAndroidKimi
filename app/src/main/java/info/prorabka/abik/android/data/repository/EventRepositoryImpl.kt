package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.EventApi
import info.prorabka.abik.android.data.dto.EventCreateRequestDto
import info.prorabka.abik.android.data.dto.EventUpdateRequestDto
import info.prorabka.abik.android.data.dto.EventVoterDto
import info.prorabka.abik.android.data.dto.LineupSaveRequestDto
import info.prorabka.abik.android.data.dto.LineupSlotDto
import info.prorabka.abik.android.data.dto.RecurrenceDto
import info.prorabka.abik.android.data.dto.SweaterColorItemDto
import info.prorabka.abik.android.data.dto.VoteRequestDto
import info.prorabka.abik.android.domain.model.EventChargeDistribution
import info.prorabka.abik.android.domain.model.EventChargeMode
import info.prorabka.abik.android.domain.model.EventDetail
import info.prorabka.abik.android.domain.model.EventFormData
import info.prorabka.abik.android.domain.model.EventListItem
import info.prorabka.abik.android.domain.model.EventSection
import info.prorabka.abik.android.domain.model.EventType
import info.prorabka.abik.android.domain.model.EventVoter
import info.prorabka.abik.android.domain.model.LineupRole
import info.prorabka.abik.android.domain.model.LineupSlot
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.SweaterColor
import info.prorabka.abik.android.domain.model.VoteAnswer
import info.prorabka.abik.android.domain.repository.EventRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val api: EventApi,
    private val queue: info.prorabka.abik.android.data.local.OfflineQueue,
    private val cache: info.prorabka.abik.android.data.local.CacheStore,
    private val json: kotlinx.serialization.json.Json,
    private val tokenStore: info.prorabka.abik.android.core.prefs.TokenStore,
) : EventRepository {

    override suspend fun events(
        teamId: Long,
        fromIso: String,
        toIso: String,
    ): ApiResult<List<EventListItem>> {
        val key = "${tokenStore.userId}:team:$teamId:events:$fromIso:$toIso"
        val serializer = kotlinx.serialization.builtins.ListSerializer(
            info.prorabka.abik.android.data.dto.EventListItemDto.serializer())
        fun map(items: List<info.prorabka.abik.android.data.dto.EventListItemDto>) = items.map { dto ->
            EventListItem(
                id = dto.id,
                title = dto.title.ifBlank { eventTypeOf(dto.type).display },
                section = sectionOf(dto.section),
                type = eventTypeOf(dto.type),
                dateIso = dto.date,
                time = dto.time,
                place = dto.place,
                myVote = voteOf(dto.myVote),
                votesYes = dto.votesYes,
                votesNo = dto.votesNo,
                votesMaybe = dto.votesMaybe,
                requiredPlayers = dto.requiredPlayers,
                votesYesGoalies = dto.votesYesGoalies,
                votesYesDefenders = dto.votesYesDefenders,
                votesYesForwards = dto.votesYesForwards,
                votesYesTrainers = dto.votesYesTrainers,
                votesYesReserve = dto.votesYesReserve,
                myInReserve = dto.myInReserve,
                votesUnanswered = dto.votesUnanswered,
                lineup = dto.lineup.map { slot ->
                    LineupSlot(
                        lineNumber = slot.lineNumber,
                        slotNumber = slot.slotNumber,
                        role = runCatching { LineupRole.valueOf(slot.role) }
                            .getOrDefault(LineupRole.FORWARD),
                        userId = slot.userId,
                    )
                },
                lineupNames = dto.lineup.mapNotNull { slot ->
                    slot.fullName?.let { slot.userId to it }
                }.toMap(),
            )
        }
        return when (val result = apiCall { api.list(teamId, fromIso, toIso) }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                ApiResult.Success(map(result.data))
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer)
                if (cached != null) ApiResult.Success(map(cached)) else result
            }
            is ApiResult.HttpError -> result
        }
    }

    override suspend fun detail(teamId: Long, eventId: Long): ApiResult<EventDetail> {
        val key = eventCacheKey(teamId, eventId)
        val operations = queue.forTypes(teamId, listOf(
            info.prorabka.abik.android.data.local.OfflineOperationTypes.EVENT_LINEUP_SAVE,
            info.prorabka.abik.android.data.local.OfflineOperationTypes.EVENT_SWEATERS_SAVE,
        )).filter { it.eventId == eventId }
        when (val result = apiCall { api.detail(teamId, eventId) }) {
            is ApiResult.Success -> {
                cache.write(key, info.prorabka.abik.android.data.dto.EventDetailDto.serializer(), result.data)
                queue.clearSyncedEvent(teamId,
                    info.prorabka.abik.android.data.local.OfflineOperationTypes.EVENT_LINEUP_SAVE, eventId)
                queue.clearSyncedEvent(teamId,
                    info.prorabka.abik.android.data.local.OfflineOperationTypes.EVENT_SWEATERS_SAVE, eventId)
                return ApiResult.Success(applyLocalOperations(result.data,
                    operations.filter { it.isLocalOnly }).toDomain())
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, info.prorabka.abik.android.data.dto.EventDetailDto.serializer())
                    ?: return result
                return ApiResult.Success(applyLocalOperations(cached, operations).toDomain())
            }
            is ApiResult.HttpError -> return result
        }
    }

    override suspend fun create(teamId: Long, data: EventFormData): ApiResult<EventDetail> =
        apiCall {
            api.create(teamId, data.toCreateDto()).toDomain()
        }

    override suspend fun update(
        teamId: Long,
        eventId: Long,
        data: EventFormData,
        scope: String,
    ): ApiResult<EventDetail> = apiCall {
        api.update(
            teamId, eventId, scope,
            EventUpdateRequestDto(
                title = data.title,
                section = data.section.name,
                type = data.type.name,
                date = data.dateIso,
                time = data.time,
                place = data.place,
                note = data.note,
                requiredPlayers = data.requiredPlayers,
                chargeMode = data.chargeMode.name,
                chargeAmountKopecks = data.chargeAmountKopecks,
                chargeDistribution = data.chargeDistribution?.name,
                chargeTemplateId = data.chargeTemplateId,
                chargeMemberIds = data.chargeMemberIds,
        voterListId = data.voterListId,
            ),
        ).toDomain()
    }

    override suspend fun delete(teamId: Long, eventId: Long, scope: String): ApiResult<Unit> =
        apiCall {
            api.delete(teamId, eventId, scope)
            Unit
        }

    override suspend fun vote(teamId: Long, eventId: Long, answer: VoteAnswer): ApiResult<Unit> =
        apiCall {
            api.vote(teamId, eventId, VoteRequestDto(answer.name))
            Unit
        }

    override suspend fun setSweaters(
        teamId: Long,
        eventId: Long,
        items: Map<Long, SweaterColor?>,
    ): ApiResult<info.prorabka.abik.android.domain.model.SyncOutcome> {
        val type = info.prorabka.abik.android.data.local.OfflineOperationTypes.EVENT_SWEATERS_SAVE
        val body = info.prorabka.abik.android.data.dto.OfflineSweatersRequestDto(
            eventId, items.map { (userId, color) -> SweaterColorItemDto(userId, color?.name) })
        return when (val result = apiCall { api.setSweaters(teamId, eventId, body.items) }) {
            is ApiResult.Success -> {
                queue.clearEventOperations(teamId, type, eventId)
                ApiResult.Success(info.prorabka.abik.android.domain.model.SyncOutcome.SYNCED)
            }
            is ApiResult.NetworkError -> {
                val revision = cache.read(eventCacheKey(teamId, eventId),
                    info.prorabka.abik.android.data.dto.EventDetailDto.serializer())?.sweaterRevision
                    ?: return ApiResult.HttpError(409, "OFFLINE_CONFLICT",
                        "Нет сохранённой версии события. Откройте его онлайн перед изменением цветов.")
                queue.enqueue(teamId, type, body,
                    info.prorabka.abik.android.data.dto.OfflineSweatersRequestDto.serializer(),
                    expectedEventRevision = revision, eventId = eventId, replacePendingEvent = true)
                ApiResult.Success(info.prorabka.abik.android.domain.model.SyncOutcome.PENDING)
            }
            is ApiResult.HttpError -> result
        }
    }

    override suspend fun setLineup(
        teamId: Long,
        eventId: Long,
        slots: List<LineupSlot>,
    ): ApiResult<info.prorabka.abik.android.domain.model.SyncOutcome> {
        val type = info.prorabka.abik.android.data.local.OfflineOperationTypes.EVENT_LINEUP_SAVE
        val body = info.prorabka.abik.android.data.dto.OfflineLineupRequestDto(
            eventId, slots.map {
                LineupSlotDto(it.lineNumber, it.slotNumber, it.role.name, it.userId)
            })
        return when (val result = apiCall { api.setLineup(teamId, eventId, LineupSaveRequestDto(body.slots)) }) {
            is ApiResult.Success -> {
                queue.clearEventOperations(teamId, type, eventId)
                ApiResult.Success(info.prorabka.abik.android.domain.model.SyncOutcome.SYNCED)
            }
            is ApiResult.NetworkError -> {
                val revision = cache.read(eventCacheKey(teamId, eventId),
                    info.prorabka.abik.android.data.dto.EventDetailDto.serializer())?.lineupRevision
                    ?: return ApiResult.HttpError(409, "OFFLINE_CONFLICT",
                        "Нет сохранённой версии события. Откройте его онлайн перед изменением звеньев.")
                queue.enqueue(teamId, type, body,
                    info.prorabka.abik.android.data.dto.OfflineLineupRequestDto.serializer(),
                    expectedEventRevision = revision, eventId = eventId, replacePendingEvent = true)
                ApiResult.Success(info.prorabka.abik.android.domain.model.SyncOutcome.PENDING)
            }
            is ApiResult.HttpError -> result
        }
    }

    private fun eventCacheKey(teamId: Long, eventId: Long) =
        "${tokenStore.userId}:team:$teamId:event:$eventId"

    private fun applyLocalOperations(
        source: info.prorabka.abik.android.data.dto.EventDetailDto,
        operations: List<info.prorabka.abik.android.data.local.OfflineOperationEntity>,
    ): info.prorabka.abik.android.data.dto.EventDetailDto {
        var result = source
        operations.forEach { operation ->
            when (operation.type) {
                info.prorabka.abik.android.data.local.OfflineOperationTypes.EVENT_LINEUP_SAVE -> {
                    val body = runCatching {
                        json.decodeFromString(
                            info.prorabka.abik.android.data.dto.OfflineLineupRequestDto.serializer(),
                            operation.payloadJson)
                    }.getOrNull()
                    if (body != null) result = result.copy(
                        lineup = info.prorabka.abik.android.data.dto.LineupDto(body.slots))
                }
                info.prorabka.abik.android.data.local.OfflineOperationTypes.EVENT_SWEATERS_SAVE -> {
                    val body = runCatching {
                        json.decodeFromString(
                            info.prorabka.abik.android.data.dto.OfflineSweatersRequestDto.serializer(),
                            operation.payloadJson)
                    }.getOrNull()
                    if (body != null) {
                        val colors = body.items.associate { it.userId to it.color }
                        result = result.copy(voters = result.voters.map {
                            it.copy(sweaterColor = colors[it.userId])
                        })
                    }
                }
            }
        }
        return result.copy(
            pendingSync = operations.any { it.isLocalOnly },
            syncError = operations.mapNotNull { it.displaySyncError }.distinct().joinToString("\n").ifBlank { null },
        )
    }

    // ====== Маппинг ======

    private fun info.prorabka.abik.android.data.dto.EventDetailDto.toDomain() = EventDetail(
        id = id,
        title = title.ifBlank { eventTypeOf(type).display },
        section = sectionOf(section),
        type = eventTypeOf(type),
        dateIso = date,
        time = time,
        place = place,
        note = note,
        status = status,
        seriesId = seriesId,
        myVote = voteOf(myVote),
        voters = voters.map { it.toDomain() },
        lineup = lineup?.slots?.map { dto ->
            LineupSlot(
                lineNumber = dto.lineNumber,
                slotNumber = dto.slotNumber,
                role = runCatching { LineupRole.valueOf(dto.role) }
                    .getOrDefault(LineupRole.FORWARD),
                userId = dto.userId,
            )
        },
        requiredPlayers = requiredPlayers,
        chargeMode = chargeMode?.let {
            runCatching { EventChargeMode.valueOf(it) }.getOrNull()
        } ?: EventChargeMode.NONE,
        chargeAmountKopecks = chargeAmountKopecks,
        chargeDistribution = chargeDistribution?.let {
            runCatching { EventChargeDistribution.valueOf(it) }.getOrNull()
        },
        chargeTemplateId = chargeTemplateId,
        chargeMemberIds = chargeMemberIds.orEmpty(),
        voterListId = voterListId,
        charged = charged,
        lineupRevision = lineupRevision,
        sweaterRevision = sweaterRevision,
        pendingSync = pendingSync,
        syncError = syncError,
    )

    private fun EventVoterDto.toDomain() = EventVoter(
        userId = userId,
        fullName = fullName ?: "user#$userId",
        answer = voteOf(answer),
        positions = positions.mapNotNull { runCatching { Position.valueOf(it) }.getOrNull() }
            .toSet(),
        isTrainer = isTrainer,
        sweaterColor = sweaterColor?.let {
            runCatching { SweaterColor.valueOf(it) }.getOrNull()
        },
        inReserve = inReserve,
    )

    private fun EventFormData.toCreateDto() = EventCreateRequestDto(
        title = title,
        section = section.name,
        type = type.name,
        date = dateIso,
        time = time,
        place = place,
        note = note,
        recurrence = if (weekdays.isNullOrEmpty() || untilDateIso == null) {
            null
        } else {
            RecurrenceDto(weekdays = weekdays, untilDate = untilDateIso)
        },
        requiredPlayers = requiredPlayers,
        chargeMode = chargeMode.name,
        chargeAmountKopecks = chargeAmountKopecks,
        chargeDistribution = chargeDistribution?.name,
        chargeTemplateId = chargeTemplateId,
        chargeMemberIds = chargeMemberIds,
        voterListId = voterListId,
    )

    private fun sectionOf(raw: String): EventSection =
        runCatching { EventSection.valueOf(raw) }.getOrDefault(EventSection.TRAINING)

    private fun eventTypeOf(raw: String): EventType =
        runCatching { EventType.valueOf(raw) }.getOrDefault(EventType.TRAINING)

    private fun voteOf(raw: String?): VoteAnswer? =
        raw?.let { runCatching { VoteAnswer.valueOf(it) }.getOrNull() }
}
