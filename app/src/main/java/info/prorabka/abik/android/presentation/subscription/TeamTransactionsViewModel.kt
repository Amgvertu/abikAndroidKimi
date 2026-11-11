package info.prorabka.abik.android.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.Payment
import info.prorabka.abik.android.domain.model.TeamExpense
import info.prorabka.abik.android.domain.repository.ExpenseRepository
import info.prorabka.abik.android.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class TeamTransactionsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val expenseRepository: ExpenseRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {
    data class UiState(
        val loading: Boolean = true,
        val payments: List<Payment> = emptyList(),
        val expenses: List<TeamExpense> = emptyList(),
        val selectedMonth: YearMonth = YearMonth.now(),
        val periodFrom: LocalDate? = null,
        val periodTo: LocalDate? = null,
        val error: String? = null,
    ) {
        val filteredPayments: List<Payment>
            get() = payments.filter { inSelectedPeriod(it.paidAt?.take(10) ?: it.createdAt?.take(10)) }
        val filteredExpenses: List<TeamExpense>
            get() = expenses.filter { inSelectedPeriod(it.expenseDate) }
        val paymentsTotal: Long get() = filteredPayments.sumOf { it.amountKopecks }
        val expensesTotal: Long get() = filteredExpenses.sumOf { it.amountKopecks }
        val balance: Long get() = paymentsTotal - expensesTotal

        private fun inSelectedPeriod(dateIso: String?): Boolean {
            val date = dateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
            return if (periodFrom != null && periodTo != null) {
                !date.isBefore(periodFrom) && !date.isAfter(periodTo)
            } else {
                YearMonth.from(date) == selectedMonth
            }
        }
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init { load() }

    fun previousMonth() = _ui.update {
        it.copy(selectedMonth = it.selectedMonth.minusMonths(1), periodFrom = null, periodTo = null)
    }

    fun nextMonth() = _ui.update {
        it.copy(selectedMonth = it.selectedMonth.plusMonths(1), periodFrom = null, periodTo = null)
    }

    fun setPeriod(from: LocalDate, to: LocalDate) = _ui.update {
        it.copy(periodFrom = from, periodTo = to)
    }

    fun resetPeriod() = _ui.update { it.copy(periodFrom = null, periodTo = null) }

    fun load() {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            var error: String? = null
            val payments = when (val result = financeRepository.payments(teamId)) {
                is ApiResult.Success -> result.data
                is ApiResult.HttpError -> {
                    error = result.message
                    emptyList()
                }
                is ApiResult.NetworkError -> {
                    error = "Нет соединения с сервером"
                    emptyList()
                }
            }
            val expenses = when (val result = expenseRepository.list(teamId)) {
                is ApiResult.Success -> result.data
                is ApiResult.HttpError -> {
                    if (error == null) error = result.message
                    emptyList()
                }
                is ApiResult.NetworkError -> {
                    if (error == null) error = "Нет соединения с сервером"
                    emptyList()
                }
            }
            _ui.update {
                it.copy(loading = false, payments = payments, expenses = expenses, error = error)
            }
        }
    }
}
