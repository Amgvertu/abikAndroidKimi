package info.prorabka.abik.android.presentation.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.util.filterMoneyInput
import info.prorabka.abik.android.core.util.kopecksToInput
import info.prorabka.abik.android.core.util.parseMoneyToKopecks
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
        /** Плательщик из загруженного платежа (id + имя), даже если он исключён из команды. */
        val payerUserId: Long? = null,
        val payerNameFallback: String? = null,
        val amountRubles: String = "",
        val paidAt: LocalDate = LocalDate.now(),
        val note: String = "",
        val editPaymentId: Long? = null,
    ) {
        /** Плательщик известен, но его нет среди активных участников (исключён). */
        val payerExcluded: Boolean
            get() = payerUserId != null && members.none { it.userId == payerUserId }

        val payerDisplayName: String
            get() = selectedMember?.displayName
                ?: payerNameFallback?.let { "$it (исключён из команды)" }
                ?: ""
    }

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
                        val preselected = active.firstOrNull { it.userId == preselectUserId }
                        state.copy(
                            members = active,
                            selectedMember = preselected ?: state.selectedMember,
                            // предвыбор должен засчитываться как выбранный плательщик
                            payerUserId = preselected?.userId ?: state.payerUserId,
                        )
                    }
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = m.message) }
                is ApiResult.NetworkError -> _ui.update { it.copy(error = "Нет соединения с сервером") }
            }
            if (paymentId > 0) {
                // AND-02: платёж грузим по ID отдельным запросом, а не поиском в общем списке
                when (val p = financeRepository.paymentById(teamId, paymentId)) {
                    is ApiResult.Success -> {
                        val payment = p.data
                        _ui.update { state ->
                            state.copy(
                                amountRubles = payment.amountKopecks.kopecksToInput(),
                                note = payment.description.orEmpty(),
                                paidAt = payment.paidAt
                                    ?.take(10)
                                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                                    ?: LocalDate.now(),
                                payerUserId = payment.userId,
                                payerNameFallback = payment.userName.ifBlank { null },
                                selectedMember = state.members.firstOrNull {
                                    it.userId == payment.userId
                                } ?: state.selectedMember,
                            )
                        }
                    }
                    is ApiResult.HttpError -> _ui.update { it.copy(error = p.message) }
                    is ApiResult.NetworkError -> _ui.update {
                        it.copy(error = "Нет соединения с сервером")
                    }
                }
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    fun selectMember(member: TeamMember) = _ui.update {
        it.copy(selectedMember = member, payerUserId = member.userId, payerNameFallback = null)
    }
    fun onAmount(v: String) = _ui.update { it.copy(amountRubles = v.filterMoneyInput()) }
    fun onPaidAt(v: LocalDate) = _ui.update { it.copy(paidAt = v) }
    fun onNote(v: String) = _ui.update { it.copy(note = v.take(500)) }

    fun save(onDone: () -> Unit) {
        val s = _ui.value
        val teamId = teamManager.selected.value?.id ?: return
        val kopecks = s.amountRubles.parseMoneyToKopecks()
        val payerId = s.payerUserId
        when {
            payerId == null -> _ui.update { it.copy(error = "Выберите, кто внёс платёж") }
            kopecks == null || kopecks <= 0 ->
                _ui.update { it.copy(error = "Укажите сумму в рублях") }
            else -> viewModelScope.launch {
                _ui.update { it.copy(saving = true, error = null) }
                val paidAtIso = s.paidAt.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val result = if (s.editPaymentId != null) {
                    financeRepository.updatePayment(
                        teamId = teamId,
                        paymentId = s.editPaymentId,
                        userId = payerId,
                        amountKopecks = kopecks,
                        paidAtIso = paidAtIso,
                        description = s.note.ifBlank { null },
                    )
                } else {
                    financeRepository.createPayment(
                        teamId = teamId,
                        userId = payerId,
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
