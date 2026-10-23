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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.ui.theme.Danger
import info.prorabka.abik.android.core.ui.theme.Success
import info.prorabka.abik.android.core.util.isoDateTimeToDisplay
import info.prorabka.abik.android.core.util.isoDateToDisplay
import info.prorabka.abik.android.core.util.kopecksToRubles
import info.prorabka.abik.android.domain.model.Charge
import info.prorabka.abik.android.domain.model.ChargeStatus
import info.prorabka.abik.android.domain.model.Payment

/** Начисления и платежи конкретного участника. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberFinanceScreen(
    userId: Long,
    onBack: () -> Unit,
    onEditPayment: (Long) -> Unit,
    viewModel: MemberFinanceViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var cancelChargeId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(userId) { viewModel.load(userId) }

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
        if (ui.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Баланс",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        ui.balanceKopecks?.kopecksToRubles() ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            (ui.balanceKopecks ?: 0) < 0 -> Danger
                            else -> Success
                        },
                    )
                }
            }

            Text("Начисления", style = MaterialTheme.typography.titleMedium)
            if (ui.charges.isEmpty()) {
                Text(
                    "Начислений нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ui.charges.forEach { charge ->
                ChargeRow(
                    charge = charge,
                    canCancel = ui.isManager &&
                        (charge.status == ChargeStatus.ACTIVE ||
                            charge.status == ChargeStatus.PARTIALLY_PAID),
                    onCancel = { cancelChargeId = charge.id },
                )
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Платежи", style = MaterialTheme.typography.titleMedium)
            if (ui.payments.isEmpty()) {
                Text(
                    "Платежей нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ui.payments.forEach { payment ->
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
private fun ChargeRow(charge: Charge, canCancel: Boolean, onCancel: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(charge.title.ifBlank { "Начисление" }, style = MaterialTheme.typography.bodyLarge)
            Text(
                buildString {
                    append(charge.status.display)
                    charge.dueDate?.let { append(" • ${it.isoDateToDisplay()}") }
                    if (charge.status == ChargeStatus.PARTIALLY_PAID) {
                        append(" • осталось ${charge.remainingKopecks.kopecksToRubles()}")
                    }
                    charge.description?.let { append("\n$it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            charge.amountKopecks.kopecksToRubles(),
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
            "+${payment.amountKopecks.kopecksToRubles()}",
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
