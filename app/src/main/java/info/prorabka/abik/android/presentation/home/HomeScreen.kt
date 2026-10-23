package info.prorabka.abik.android.presentation.home

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.prorabka.abik.android.core.ui.theme.Danger
import info.prorabka.abik.android.core.ui.theme.Success
import info.prorabka.abik.android.core.util.kopecksToRubles
import info.prorabka.abik.android.presentation.shell.ShellViewModel

/**
 * Главная: мой баланс, баланс команды, ближайшее событие, уведомления.
 * Для управляющих — FAB «+» (внести платёж).
 */
@Composable
fun HomeScreen(
    shellViewModel: ShellViewModel,
    onAddPayment: () -> Unit,
) {
    val ui by shellViewModel.ui.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (ui.isManager) {
                ExtendedFloatingActionButton(
                    onClick = onAddPayment,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Платёж") },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BalanceCard(
                    title = "Мой баланс",
                    amount = ui.myBalanceKopecks,
                    modifier = Modifier.weight(1f),
                )
                BalanceCard(
                    title = "Баланс команды",
                    amount = ui.teamBalanceKopecks,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionCard(
                title = "Ближайшее событие",
                text = "Расписание тренировок и игр появится после обновления сервера",
            )

            SectionCard(
                title = "Последние уведомления",
                text = "Лента уведомлений появится после обновления сервера",
            )

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun BalanceCard(title: String, amount: Long?, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amount?.kopecksToRubles() ?: "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    amount == null -> MaterialTheme.colorScheme.onSurface
                    amount < 0 -> Danger
                    else -> Success
                },
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
