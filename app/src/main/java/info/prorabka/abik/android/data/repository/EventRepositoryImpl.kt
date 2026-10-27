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
) : EventRepository {

    override suspend fun events(
        teamId: Long,
        fromIso: String,
        toIso: String,
    ): ApiResult<List<EventListItem>> = apiCall {
        api.list(teamId, fromIso, toIso).map { dto ->
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
            )
        }
    }

    override suspend fun detail(teamId: Long, eventId: Long): ApiResult<EventDetail> = apiCall {
        api.detail(teamId, eventId).toDomain()
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
    ): ApiResult<Unit> = apiCall {
        api.setSweaters(
            teamId, eventId,
            items.map { (userId, color) -> SweaterColorItemDto(userId, color?.name) },
        )
        Unit
    }

    override suspend fun setLineup(
        teamId: Long,
        eventId: Long,
        slots: List<LineupSlot>,
    ): ApiResult<Unit> = apiCall {
        api.setLineup(
            teamId, eventId,
            LineupSaveRequestDto(slots.map {
                LineupSlotDto(
                    lineNumber = it.lineNumber,
                    slotNumber = it.slotNumber,
                    role = it.role.name,
                    userId = it.userId,
                )
            }),
        )
        Unit
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
    )

    private fun sectionOf(raw: String): EventSection =
        runCatching { EventSection.valueOf(raw) }.getOrDefault(EventSection.TRAINING)

    private fun eventTypeOf(raw: String): EventType =
        runCatching { EventType.valueOf(raw) }.getOrDefault(EventType.TRAINING)

    private fun voteOf(raw: String?): VoteAnswer? =
        raw?.let { runCatching { VoteAnswer.valueOf(it) }.getOrNull() }
}
