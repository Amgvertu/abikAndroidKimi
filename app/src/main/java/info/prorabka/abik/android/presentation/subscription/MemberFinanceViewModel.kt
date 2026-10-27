package info.prorabka.abik.android.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.Charge
import info.prorabka.abik.android.domain.model.ChargeStatus
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.Payment
import info.prorabka.abik.android.domain.repository.ChargeRepository
import info.prorabka.abik.android.domain.repository.FinanceRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Детальная информация по начислениям и платежам участника. */
@HiltViewModel
class MemberFinanceViewModel @Inject constructor(
    private val chargeRepository: ChargeRepository,
    private val financeRepository: FinanceRepository,
    private val teamRepository: TeamRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val userName: String = "",
        val balanceKopecks: Long? = null,
        val charges: List<Charge> = emptyList(),
        val payments: List<Payment> = emptyList(),
        val isManager: Boolean = false,
        val error: String? = null,
    ) {
        /** Начисления, участвующие в долге (не отменённые и не архивные). */
        private val effectiveCharges: List<Charge>
            get() = charges.filter {
                it.status != ChargeStatus.CANCELLED && it.status != ChargeStatus.ARCHIVED
            }

        /** Сводка: сколько начислено / сколько из этого оплачено / остаток к оплате. */
        val chargedKopecks: Long get() = effectiveCharges.sumOf { it.amountKopecks }
        val chargesPaidKopecks: Long get() = effectiveCharges.sumOf { it.paidKopecks }
        val remainingKopecks: Long get() = effectiveCharges.sumOf { it.remainingKopecks }
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private var userId: Long = 0L

    fun load(userId: Long) {
        this.userId = userId
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }

            when (val d = teamRepository.detail(teamId)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(
                        isManager = d.data.myRole == MemberRole.OWNER ||
                            d.data.myRole == MemberRole.MODERATOR ||
                            d.data.myRole == MemberRole.ADMIN,
                    )
                }
                else -> Unit
            }
            when (val b = financeRepository.balanceOf(teamId, userId)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(
                        balanceKopecks = b.data.amountKopecks,
                        userName = b.data.userName,
                    )
                }
                else -> Unit
            }
            when (val c = chargeRepository.charges(teamId, userId = userId)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(charges = c.data.sortedByDescending { ch -> ch.createdAt })
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = c.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
            when (val p = financeRepository.payments(teamId, userId = userId)) {
                is ApiResult.Success -> _ui.update {
                    // AND-01: сортировка по дате внесения платежа (paidAt), новые сверху
                    it.copy(
                        payments = p.data.sortedByDescending { pm -> pm.paidAt ?: pm.createdAt },
                    )
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = p.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
            // Ошибка загрузки не должна выглядеть как пустой список:
            // показываем баннер с ошибкой и кнопкой «Повторить»
            _ui.update { it.copy(loading = false) }
        }
    }

    fun cancelCharge(chargeId: Long, reason: String) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            when (val r = chargeRepository.cancel(teamId, chargeId, reason)) {
                is ApiResult.Success -> load(userId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }
}
