package info.prorabka.abik.android.domain.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.MemberSummary
import info.prorabka.abik.android.domain.model.TeamSummary
import java.io.File

interface ReportRepository {
    suspend fun summary(teamId: Long): ApiResult<TeamSummary>
    suspend fun members(teamId: Long): ApiResult<List<MemberSummary>>

    /** Скачать Excel начислений в кэш; вернуть файл. */
    suspend fun exportCharges(
        teamId: Long,
        userId: Long?,
        groupId: Long?,
        status: String?,
        dateFromIso: String?,
        dateToIso: String?,
    ): ApiResult<File>

    /** Скачать Excel сводки по участникам в кэш; вернуть файл. */
    suspend fun exportMembers(teamId: Long): ApiResult<File>
}
