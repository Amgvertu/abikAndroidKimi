package info.prorabka.abik.android.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.ChargeGroup
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.model.MemberSummary
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.model.TeamSummary
import info.prorabka.abik.android.domain.repository.ChargeRepository
import info.prorabka.abik.android.domain.repository.ReportRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val chargeRepository: ChargeRepository,
    private val teamRepository: TeamRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val summary: TeamSummary? = null,
        val members: List<MemberSummary> = emptyList(),
        val teamMembers: List<TeamMember> = emptyList(),
        val groups: List<ChargeGroup> = emptyList(),
        // Фильтры экспорта
        val filterUserId: Long? = null,
        val filterGroupId: Long? = null,
        val filterStatus: String? = null,
        val filterDateFrom: LocalDate? = null,
        val filterDateTo: LocalDate? = null,
        val exporting: Boolean = false,
        val exportedFile: File? = null,
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load() {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val s = reportRepository.summary(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(summary = s.data) }
                is ApiResult.HttpError -> _ui.update { it.copy(error = s.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
            when (val m = reportRepository.members(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(members = m.data) }
                else -> Unit
            }
            when (val t = teamRepository.members(teamId)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(teamMembers = t.data.filter { m -> m.status == MemberStatus.ACTIVE })
                }
                else -> Unit
            }
            when (val g = chargeRepository.groups(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(groups = g.data) }
                else -> Unit
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    fun setFilterUser(id: Long?) = _ui.update { it.copy(filterUserId = id) }
    fun setFilterGroup(id: Long?) = _ui.update { it.copy(filterGroupId = id) }
    fun setFilterStatus(status: String?) = _ui.update { it.copy(filterStatus = status) }
    fun setFilterDateFrom(d: LocalDate?) = _ui.update { it.copy(filterDateFrom = d) }
    fun setFilterDateTo(d: LocalDate?) = _ui.update { it.copy(filterDateTo = d) }

    fun exportCharges() {
        val teamId = teamManager.selected.value?.id ?: return
        val s = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(exporting = true, error = null, exportedFile = null) }
            val result = reportRepository.exportCharges(
                teamId = teamId,
                userId = s.filterUserId,
                groupId = s.filterGroupId,
                status = s.filterStatus,
                dateFromIso = s.filterDateFrom?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                dateToIso = s.filterDateTo?.format(DateTimeFormatter.ISO_LOCAL_DATE),
            )
            when (result) {
                is ApiResult.Success -> _ui.update {
                    it.copy(exporting = false, exportedFile = result.data)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(exporting = false, error = result.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(exporting = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun exportMembers() {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(exporting = true, error = null, exportedFile = null) }
            when (val r = reportRepository.exportMembers(teamId)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(exporting = false, exportedFile = r.data)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(exporting = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(exporting = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun consumeExportedFile() = _ui.update { it.copy(exportedFile = null) }
}
