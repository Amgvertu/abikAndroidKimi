package info.prorabka.abik.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.ReportApi
import info.prorabka.abik.android.domain.model.MemberSummary
import info.prorabka.abik.android.domain.model.TeamSummary
import info.prorabka.abik.android.domain.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import okhttp3.ResponseBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val api: ReportApi,
    @ApplicationContext private val context: Context,
) : ReportRepository {

    override suspend fun summary(teamId: Long): ApiResult<TeamSummary> = apiCall {
        val dto = api.summary(teamId)
        TeamSummary(
            totalChargesKopecks = dto.totalChargesKopecks,
            totalPaidKopecks = dto.totalPaidKopecks,
            totalRemainingKopecks = dto.totalRemainingKopecks,
            totalBalanceKopecks = dto.totalBalanceKopecks,
            activeChargesCount = dto.activeChargesCount,
            paidChargesCount = dto.paidChargesCount,
        )
    }

    override suspend fun members(teamId: Long): ApiResult<List<MemberSummary>> = apiCall {
        api.members(teamId).map { dto ->
            MemberSummary(
                userId = dto.userId,
                name = listOfNotNull(dto.lastName, dto.firstName).joinToString(" ")
                    .ifBlank { dto.phone ?: "ID ${dto.userId}" },
                totalChargesKopecks = dto.totalChargesKopecks,
                totalPaidKopecks = dto.totalPaidKopecks,
                totalRemainingKopecks = dto.totalRemainingKopecks,
                balanceKopecks = dto.balanceKopecks,
            )
        }
    }

    override suspend fun exportCharges(
        teamId: Long,
        userId: Long?,
        groupId: Long?,
        status: String?,
        dateFromIso: String?,
        dateToIso: String?,
    ): ApiResult<File> = download(
        "abik_charges_${teamId}.xlsx",
    ) {
        api.exportCharges(teamId, userId, groupId, status, dateFromIso, dateToIso)
    }

    override suspend fun exportMembers(teamId: Long): ApiResult<File> = download(
        "abik_members_${teamId}.xlsx",
    ) {
        api.exportMembers(teamId)
    }

    private suspend fun download(
        fileName: String,
        call: suspend () -> Response<ResponseBody>,
    ): ApiResult<File> = withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (!response.isSuccessful) {
                return@withContext ApiResult.HttpError(
                    httpCode = response.code(),
                    code = null,
                    message = "Ошибка экспорта (${response.code()})",
                )
            }
            val body = response.body()
                ?: return@withContext ApiResult.HttpError(200, null, "Пустой ответ сервера")
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(dir, fileName)
            body.byteStream().use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            ApiResult.Success(file)
        } catch (e: IOException) {
            ApiResult.NetworkError(e.message)
        }
    }
}
