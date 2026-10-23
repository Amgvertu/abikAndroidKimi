package info.prorabka.abik.android.presentation.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.FinanceRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Единая форма платежа (создание и редактирование) по ТЗ v3:
 * сумма, кто внёс, дата внесения (по умолчанию сегодня), примечание.
 */
@HiltViewModel
class PaymentFormViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val teamRepository: TeamRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val saving: Boolean = false,
        val error: String? = null,
        val members: List<TeamMember> = emptyList(),
        val selectedMember: TeamMember? = null,
        val amountRubles: String = "",
        val paidAt: LocalDate = LocalDate.now(),
        val note: String = "",
        val editPaymentId: Long? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    /** paymentId > 0 — режим редактирования; preselectUserId > 0 — предвыбор плательщика. */
    fun load(paymentId: Long, preselectUserId: Long) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, editPaymentId = paymentId.takeIf { id -> id > 0 }) }
            when (val m = teamRepository.members(teamId)) {
                is ApiResult.Success -> {
                    val active = m.data.filter { it.status == MemberStatus.ACTIVE }
                    _ui.update { state ->
                        state.copy(
                            members = active,
                            selectedMember = active.firstOrNull { it.userId == preselectUserId }
                                ?: state.selectedMember,
                        )
                    }
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = m.message) }
                is ApiResult.NetworkError -> _ui.update { it.copy(error = "Нет соединения с сервером") }
            }
            if (paymentId > 0) {
                when (val p = financeRepository.payments(teamId)) {
                    is ApiResult.Success -> {
                        val payment = p.data.firstOrNull { it.id == paymentId }
                        if (payment != null) {
                            _ui.update { state ->
                                state.copy(
                                    amountRubles = (payment.amountKopecks / 100).toString(),
                                    note = payment.description.orEmpty(),
                                    paidAt = payment.paidAt
                                        ?.take(10)
                                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                                        ?: LocalDate.now(),
                                    selectedMember = state.members.firstOrNull {
                                        it.userId == payment.userId
                                    } ?: state.selectedMember,
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    fun selectMember(member: TeamMember) = _ui.update { it.copy(selectedMember = member) }
    fun onAmount(v: String) = _ui.update { it.copy(amountRubles = v.filter(Char::isDigit).take(9)) }
    fun onPaidAt(v: LocalDate) = _ui.update { it.copy(paidAt = v) }
    fun onNote(v: String) = _ui.update { it.copy(note = v.take(500)) }

    fun save(onDone: () -> Unit) {
        val s = _ui.value
        val teamId = teamManager.selected.value?.id ?: return
        val amountRubles = s.amountRubles.toLongOrNull()
        when {
            s.selectedMember == null -> _ui.update { it.copy(error = "Выберите, кто внёс платёж") }
            amountRubles == null || amountRubles <= 0 ->
                _ui.update { it.copy(error = "Укажите сумму в рублях") }
            else -> viewModelScope.launch {
                _ui.update { it.copy(saving = true, error = null) }
                val kopecks = amountRubles * 100
                val paidAtIso = s.paidAt.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val result = if (s.editPaymentId != null) {
                    financeRepository.updatePayment(
                        teamId = teamId,
                        paymentId = s.editPaymentId!!,
                        userId = s.selectedMember!!.userId,
                        amountKopecks = kopecks,
                        paidAtIso = paidAtIso,
                        description = s.note.ifBlank { null },
                    )
                } else {
                    financeRepository.createPayment(
                        teamId = teamId,
                        userId = s.selectedMember!!.userId,
                        amountKopecks = kopecks,
                        paidAtIso = paidAtIso,
                        description = s.note.ifBlank { null },
                    )
                }
                when (result) {
                    is ApiResult.Success -> onDone()
                    is ApiResult.HttpError -> _ui.update {
                        it.copy(saving = false, error = result.message)
                    }
                    is ApiResult.NetworkError -> _ui.update {
                        it.copy(saving = false, error = "Нет соединения с сервером")
                    }
                }
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val s = _ui.value
        val teamId = teamManager.selected.value?.id ?: return
        val paymentId = s.editPaymentId ?: return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            when (val r = financeRepository.deletePayment(teamId, paymentId)) {
                is ApiResult.Success -> onDone()
                is ApiResult.HttpError -> _ui.update { it.copy(saving = false, error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(saving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }
}
