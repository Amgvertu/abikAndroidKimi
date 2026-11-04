package info.prorabka.abik.android.presentation.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.UrlResolver
import info.prorabka.abik.android.core.util.PhoneUtils
import info.prorabka.abik.android.domain.model.ChargeList
import info.prorabka.abik.android.domain.model.Invitation
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.TeamCreateData
import info.prorabka.abik.android.domain.model.TeamDetail
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.ChargeRepository
import info.prorabka.abik.android.domain.repository.FileRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val chargeRepository: ChargeRepository,
    private val teamManager: CurrentTeamManager,
    private val fileRepository: FileRepository,
    private val urlResolver: UrlResolver,
) : ViewModel() {

    fun resolveUrl(url: String?): String? = urlResolver.resolve(url)

    data class UiState(
        val loading: Boolean = true,
        val noTeam: Boolean = false,
        val detail: TeamDetail? = null,
        val members: List<TeamMember> = emptyList(),
        val outgoingInvitations: List<Invitation> = emptyList(),
        /** Общие именованные списки команды и получателей начислений. */
        val chargeLists: List<ChargeList> = emptyList(),
        val listSaving: Boolean = false,
        val error: String? = null,
        val info: String? = null,
    ) {
        val isManager: Boolean
            get() = detail?.myRole == MemberRole.OWNER ||
                detail?.myRole == MemberRole.MODERATOR ||
                detail?.myRole == MemberRole.ADMIN
        val isOwner: Boolean get() = detail?.myRole == MemberRole.OWNER
        val canManageMembership: Boolean
            get() = isOwner || detail?.myRole == MemberRole.ADMIN
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch {
            teamManager.selected.collect { team ->
                if (team != null) {
                    load(team.id)
                } else {
                    _ui.update {
                        it.copy(loading = false, noTeam = true, detail = null, members = emptyList())
                    }
                }
            }
        }
    }

    fun load(teamId: Long) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, noTeam = false, error = null) }
            when (val d = teamRepository.detail(teamId)) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(detail = d.data) }
                    val membershipManager = d.data.myRole == MemberRole.OWNER ||
                        d.data.myRole == MemberRole.ADMIN
                    if (membershipManager) loadOutgoingInvitations(teamId)
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = d.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
            when (val m = teamRepository.members(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(members = m.data) }
                else -> Unit
            }
            loadChargeLists(teamId)
            _ui.update { it.copy(loading = false) }
        }
    }

    /** Обновить вкладки после возврата из экрана «Списки получателей». */
    fun refreshChargeLists() {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch { loadChargeLists(teamId) }
    }

    /** Списки на вкладках команды — те же списки, что используются в начислениях. */
    private suspend fun loadChargeLists(teamId: Long) {
        when (val r = chargeRepository.chargeLists(teamId)) {
            is ApiResult.Success -> _ui.update { it.copy(chargeLists = r.data) }
            is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
            is ApiResult.NetworkError -> _ui.update {
                it.copy(error = "Нет соединения с сервером")
            }
        }
    }

    /** Создать общий список команды и сразу вернуть его для открытия вкладки. */
    fun createChargeList(name: String, memberIds: Set<Long>, onCreated: (ChargeList) -> Unit) {
        val teamId = currentTeamId() ?: return
        if (name.isBlank()) {
            _ui.update { it.copy(error = "Укажите название списка") }
            return
        }
        if (memberIds.isEmpty()) {
            _ui.update { it.copy(error = "Выберите хотя бы одного участника") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(listSaving = true, error = null) }
            when (val r = chargeRepository.createChargeList(teamId, name.trim(), memberIds)) {
                is ApiResult.Success -> {
                    _ui.update {
                        it.copy(listSaving = false, info = "Список «${r.data.name}» создан")
                    }
                    loadChargeLists(teamId)
                    onCreated(r.data)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(listSaving = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(listSaving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun addToChargeList(listId: Long, userId: Long) {
        val list = _ui.value.chargeLists.find { it.id == listId } ?: return
        if (userId in list.memberIds) return
        updateChargeListMembers(list, list.memberIds + userId, "Игрок добавлен в список")
    }

    fun removeFromChargeList(listId: Long, userId: Long) {
        val list = _ui.value.chargeLists.find { it.id == listId } ?: return
        updateChargeListMembers(list, list.memberIds - userId, "Игрок исключён из списка")
    }

    fun deleteChargeList(list: ChargeList, onDeleted: () -> Unit) {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch {
            _ui.update { it.copy(listSaving = true, error = null) }
            when (val r = chargeRepository.deleteChargeList(teamId, list.id)) {
                is ApiResult.Success -> {
                    _ui.update {
                        it.copy(
                            chargeLists = it.chargeLists.filter { item -> item.id != list.id },
                            listSaving = false,
                            info = "Список «${list.name}» удалён",
                        )
                    }
                    onDeleted()
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(listSaving = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(listSaving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    private fun updateChargeListMembers(
        list: ChargeList,
        memberIds: Set<Long>,
        successMessage: String,
    ) {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch {
            _ui.update { it.copy(listSaving = true, error = null) }
            when (
                val r = chargeRepository.updateChargeList(
                    teamId = teamId,
                    listId = list.id,
                    name = list.name,
                    memberIds = memberIds,
                )
            ) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(listSaving = false, info = successMessage) }
                    loadChargeLists(teamId)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(listSaving = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(listSaving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    private fun currentTeamId(): Long? = teamManager.selected.value?.id

    /** Долгий тап на логотип: загрузить файл и обновить логотип команды. */
    fun changeLogo(uri: android.net.Uri) {
        val teamId = currentTeamId() ?: return
        val detail = _ui.value.detail ?: return
        viewModelScope.launch {
            when (val uploaded = fileRepository.upload(uri)) {
                is ApiResult.Success -> {
                    val result = teamRepository.update(
                        teamId,
                        TeamCreateData(
                            name = detail.name,
                            cityId = detail.cityId ?: 0L,
                            homeRegionId = detail.homeRegionId ?: 0L,
                            logoUrl = uploaded.data,
                            description = detail.description,
                            season = detail.season,
                        ),
                    )
                    when (result) {
                        is ApiResult.Success -> {
                            _ui.update { it.copy(detail = result.data, info = "Логотип обновлён") }
                            teamManager.refresh()
                        }
                        is ApiResult.HttpError -> _ui.update {
                            it.copy(error = result.message)
                        }
                        is ApiResult.NetworkError -> _ui.update {
                            it.copy(error = "Нет соединения с сервером")
                        }
                    }
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = uploaded.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun invite(phoneRaw: String) {
        val teamId = currentTeamId() ?: return
        val phone = PhoneUtils.normalize(phoneRaw)
        if (phone == null) {
            _ui.update { it.copy(error = "Введите корректный номер телефона") }
            return
        }
        viewModelScope.launch {
            when (val r = teamRepository.invite(teamId, phone)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(info = "Приглашение отправлено на ${PhoneUtils.formatInput(phone)}")
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    /** Редактирование названия/описания/сезона (город, регион, логотип сохраняются). */
    fun updateTeam(name: String, description: String, season: String) {
        val teamId = currentTeamId() ?: return
        val detail = _ui.value.detail ?: return
        if (name.isBlank()) {
            _ui.update { it.copy(error = "Название команды не может быть пустым") }
            return
        }
        viewModelScope.launch {
            val result = teamRepository.update(
                teamId,
                TeamCreateData(
                    name = name.trim(),
                    cityId = detail.cityId ?: 0L,
                    homeRegionId = detail.homeRegionId ?: 0L,
                    logoUrl = detail.logoUrl ?: "",
                    description = description.trim().ifBlank { null },
                    season = season.trim().ifBlank { null },
                ),
            )
            when (result) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(detail = result.data, info = "Команда обновлена") }
                    teamManager.refresh()
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = result.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun setJerseyNumber(member: TeamMember, number: Int?) {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch {
            when (val r = teamRepository.setJerseyNumber(teamId, member.userId, number)) {
                is ApiResult.Success -> load(teamId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun toggleModerator(member: TeamMember) {
        val teamId = currentTeamId() ?: return
        val newRole = if (member.role == MemberRole.MODERATOR) MemberRole.MEMBER
        else MemberRole.MODERATOR
        viewModelScope.launch {
            when (val r = teamRepository.changeRole(teamId, member.userId, newRole)) {
                is ApiResult.Success -> load(teamId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun toggleAdmin(member: TeamMember) {
        val teamId = currentTeamId() ?: return
        val newRole = if (member.role == MemberRole.ADMIN) MemberRole.MEMBER else MemberRole.ADMIN
        viewModelScope.launch {
            when (val r = teamRepository.changeRole(teamId, member.userId, newRole)) {
                is ApiResult.Success -> load(teamId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    /** Назначить/снять тренера команды (только владелец). */
    fun toggleTrainer(member: TeamMember) {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch {
            when (
                val r = teamRepository.setTrainer(teamId, member.userId, !member.isTrainer)
            ) {
                is ApiResult.Success -> load(teamId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun exclude(member: TeamMember) {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch {
            when (val r = teamRepository.excludeMember(teamId, member.userId)) {
                is ApiResult.Success -> load(teamId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun restore(member: TeamMember) {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch {
            when (val r = teamRepository.restoreMember(teamId, member.userId)) {
                is ApiResult.Success -> load(teamId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    private suspend fun loadOutgoingInvitations(teamId: Long) {
        when (val r = teamRepository.outgoingInvitations(teamId)) {
            is ApiResult.Success -> _ui.update { it.copy(outgoingInvitations = r.data) }
            else -> Unit
        }
    }

    /** Отменить исходящее приглашение (управляющие). */
    fun cancelInvitation(invitation: Invitation) {
        viewModelScope.launch {
            when (val r = teamRepository.cancelInvitation(invitation.id)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(
                        outgoingInvitations = it.outgoingInvitations
                            .filter { i -> i.id != invitation.id },
                        info = "Приглашение отменено",
                    )
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    /** Покинуть команду (все, кроме владельца). */
    fun leaveTeam() {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch {
            when (val r = teamRepository.leave(teamId)) {
                // refresh переключит на другую команду или покажет заглушку
                is ApiResult.Success -> teamManager.refresh()
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    /** Удалить команду (владелец): сервер переводит её в архив. */
    fun archiveTeam() {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch {
            when (val r = teamRepository.archive(teamId)) {
                is ApiResult.Success -> teamManager.refresh()
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun clearMessages() = _ui.update { it.copy(error = null, info = null) }
}
