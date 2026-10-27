package info.prorabka.abik.android.presentation.subscription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.ui.theme.Danger
import info.prorabka.abik.android.core.ui.theme.Success
import info.prorabka.abik.android.core.util.isoDateTimeToDisplay
import info.prorabka.abik.android.core.util.isoDateToDisplay
import info.prorabka.abik.android.core.util.kopecksToRublesExact
import info.prorabka.abik.android.domain.model.Charge
import info.prorabka.abik.android.domain.model.ChargeStatus
import info.prorabka.abik.android.domain.model.Payment

/** Начисления и платежи конкретного участника (ТЗ 7.1). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberFinanceScreen(
    userId: Long,
    onBack: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onEditCharge: (Long) -> Unit,
    viewModel: MemberFinanceViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var cancelChargeId by remember { mutableStateOf<Long?>(null) }
    var tab by remember { mutableIntStateOf(0) }

    // Первая загрузка + обновление при возврате с форм (платёж/начисление)
    LifecycleResumeEffect(userId) {
        viewModel.load(userId)
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.userName.ifBlank { "Участник" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        if (ui.loading && ui.charges.isEmpty() && ui.payments.isEmpty()) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // --- Сводка «Начислено / Оплачено / Остаток» + баланс ---
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SummaryCell("Начислено", ui.chargedKopecks.kopecksToRublesExact())
                    SummaryCell("Оплачено", ui.chargesPaidKopecks.kopecksToRublesExact())
                    SummaryCell(
                        "Остаток",
                        ui.remainingKopecks.kopecksToRublesExact(),
                        if (ui.remainingKopecks > 0) Danger else Success,
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Баланс",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        ui.balanceKopecks?.kopecksToRublesExact() ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if ((ui.balanceKopecks ?: 0) < 0) Danger else Success,
                    )
                }
            }

            if (ui.error != null) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ErrorText(ui.error)
                    TextButton(
                        onClick = { viewModel.load(userId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Повторить") }
                }
            }

            // --- Вкладки со счётчиками ---
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("Начисления (${ui.charges.size})") },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("Платежи (${ui.payments.size})") },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                if (tab == 0) {
                    if (ui.charges.isEmpty() && ui.error == null) {
                        item { EmptyListHint("Начислений нет") }
                    }
                    items(ui.charges, key = { it.id }) { charge ->
                        val editable = ui.isManager &&
                            (charge.status == ChargeStatus.ACTIVE ||
                                charge.status == ChargeStatus.PARTIALLY_PAID ||
                                charge.status == ChargeStatus.PAID)
                        ChargeRow(
                            charge = charge,
                            canCancel = ui.isManager &&
                                (charge.status == ChargeStatus.ACTIVE ||
                                    charge.status == ChargeStatus.PARTIALLY_PAID),
                            onCancel = { cancelChargeId = charge.id },
                            onClick = if (editable) {
                                { onEditCharge(charge.id) }
                            } else {
                                null
                            },
                        )
                        HorizontalDivider()
                    }
                } else {
                    if (ui.payments.isEmpty() && ui.error == null) {
                        item { EmptyListHint("Платежей нет") }
                    }
                    items(ui.payments, key = { it.id }) { payment ->
                        PaymentRow(
                            payment = payment,
                            onClick = if (ui.isManager) {
                                { onEditPayment(payment.id) }
                            } else {
                                null
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    cancelChargeId?.let { chargeId ->
        CancelChargeDialog(
            onDismiss = { cancelChargeId = null },
            onConfirm = { reason ->
                viewModel.cancelCharge(chargeId, reason)
                cancelChargeId = null
            },
        )
    }
}

@Composable
private fun SummaryCell(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyListHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Composable
private fun ChargeRow(
    charge: Charge,
    canCancel: Boolean,
    onCancel: () -> Unit,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(charge.title.ifBlank { "Начисление" }, style = MaterialTheme.typography.bodyLarge)
            Text(
                buildString {
                    append(charge.status.display)
                    charge.dueDate?.let { append(" • до ${it.isoDateToDisplay()}") }
                    charge.description?.let { append("\n$it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Частичная оплата: «Оплачено X из Y», остаток
            if (charge.status == ChargeStatus.PARTIALLY_PAID || charge.paidKopecks > 0) {
                Text(
                    "Оплачено ${charge.paidKopecks.kopecksToRublesExact()} из ${charge.amountKopecks.kopecksToRublesExact()}" +
                        " • осталось ${charge.remainingKopecks.kopecksToRublesExact()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            charge.amountKopecks.kopecksToRublesExact(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = when (charge.status) {
                ChargeStatus.PAID -> Success
                ChargeStatus.CANCELLED, ChargeStatus.ARCHIVED ->
                    MaterialTheme.colorScheme.onSurfaceVariant
                else -> Danger
            },
        )
        if (canCancel) {
            TextButton(onClick = onCancel) { Text("Отменить") }
        }
    }
}

@Composable
private fun PaymentRow(payment: Payment, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                payment.paidAt?.take(10)?.isoDateToDisplay()
                    ?: payment.createdAt?.isoDateTimeToDisplay()
                    ?: "",
                style = MaterialTheme.typography.bodyLarge,
            )
            payment.description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            "+${payment.amountKopecks.kopecksToRublesExact()}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Success,
        )
    }
}

@Composable
private fun CancelChargeDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Отменить начисление") },
        text = {
            Column {
                Text(
                    "Если начисление частично оплачено, оплата перераспределится на другие долги",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AbikTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Причина отмены",
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank(),
            ) { Text("Отменить начисление") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Назад") }
        },
    )
}
