package info.prorabka.abik.android.presentation.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikPullToRefresh
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.ui.theme.Danger
import info.prorabka.abik.android.core.ui.theme.Success
import info.prorabka.abik.android.core.util.NameFormatter
import info.prorabka.abik.android.core.util.kopecksToRubles
import info.prorabka.abik.android.domain.model.Payment
import info.prorabka.abik.android.presentation.finance.FinancePeriodCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val transactionDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun TeamPaymentsScreen(viewModel: TeamTransactionsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsState()
    AbikPullToRefresh(loading = ui.loading, onRefresh = viewModel::load) {
        if (ui.loading && ui.payments.isEmpty()) {
            LoadingBox()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FinancePeriodCard(
                        selectedMonth = ui.selectedMonth,
                        periodFrom = ui.periodFrom,
                        periodTo = ui.periodTo,
                        amountKopecks = ui.paymentsTotal,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth,
                        onPeriodSelected = viewModel::setPeriod,
                        onResetPeriod = viewModel::resetPeriod,
                    )
                }
                item { ErrorText(ui.error) }
                if (ui.filteredPayments.isEmpty() && !ui.loading) {
                    item { EmptyState("За выбранный период поступлений нет") }
                }
                items(ui.filteredPayments, key = { it.id }) { payment -> PaymentRow(payment) }
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: Payment) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    NameFormatter.short(payment.userName),
                    style = MaterialTheme.typography.titleSmall,
                )
                paymentDate(payment)?.let {
                    Text(
                        it.format(transactionDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                payment.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                if (payment.pendingSync || payment.syncError != null) {
                    Text(
                        payment.syncError ?: "Ожидает синхронизации",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (payment.syncError == null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                "+${payment.amountKopecks.kopecksToRubles()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Success,
            )
        }
    }
}

@Composable
fun TeamBalanceScreen(viewModel: TeamTransactionsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsState()
    AbikPullToRefresh(loading = ui.loading, onRefresh = viewModel::load) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FinancePeriodCard(
                    selectedMonth = ui.selectedMonth,
                    periodFrom = ui.periodFrom,
                    periodTo = ui.periodTo,
                    amountKopecks = ui.balance,
                    onPreviousMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth,
                    onPeriodSelected = viewModel::setPeriod,
                    onResetPeriod = viewModel::resetPeriod,
                )
            }
            item { ErrorText(ui.error) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Баланс за выбранный период", style = MaterialTheme.typography.titleMedium)
                        BalanceRow("Поступления", ui.paymentsTotal, Success)
                        BalanceRow("Расходы", ui.expensesTotal, Danger)
                        BalanceRow("Итого", ui.balance, if (ui.balance < 0) Danger else Success)
                        Text(
                            "Учитываются только операции выбранного периода, без накопленного остатка.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceRow(label: String, amount: Long, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            amount.kopecksToRubles(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

private fun paymentDate(payment: Payment): LocalDate? =
    (payment.paidAt?.take(10) ?: payment.createdAt?.take(10))
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
