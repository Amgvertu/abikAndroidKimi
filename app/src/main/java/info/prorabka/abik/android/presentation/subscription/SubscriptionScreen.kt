package info.prorabka.abik.android.presentation.subscription

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.ui.theme.Danger
import info.prorabka.abik.android.core.ui.theme.Success
import info.prorabka.abik.android.core.util.kopecksToRubles

/**
 * Экран «Абонемент»: таблица участников с балансами.
 * Короткий тап — детали; длинный тап (управляющие) — платёж/начисление.
 */
@Composable
fun SubscriptionScreen(
    onMemberClick: (Long) -> Unit,
    onAddPayment: (Long) -> Unit,
    onAddCharge: (Long) -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (ui.isManager) {
                FloatingActionButton(onClick = { onAddCharge(-1L) }) {
                    Icon(Icons.Default.Add, contentDescription = "Создать начисление")
                }
            }
        },
    ) { padding ->
        if (ui.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(ui.rows, key = { it.userId }) { row ->
                MemberBalanceRow(
                    row = row,
                    isManager = ui.isManager,
                    onClick = { onMemberClick(row.userId) },
                    onPayment = { onAddPayment(row.userId) },
                    onCharge = { onAddCharge(row.userId) },
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemberBalanceRow(
    row: SubscriptionViewModel.MemberBalance,
    isManager: Boolean,
    onClick: () -> Unit,
    onPayment: () -> Unit,
    onCharge: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { if (isManager) menuExpanded = true },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.jerseyNumber?.toString() ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(row.name, style = MaterialTheme.typography.bodyLarge)
                if (row.status != info.prorabka.abik.android.domain.model.MemberStatus.ACTIVE) {
                    Text(
                        row.status.display,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = row.balanceKopecks.kopecksToRubles(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (row.balanceKopecks < 0) Danger else Success,
            )
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Внести платёж") },
                onClick = {
                    menuExpanded = false
                    onPayment()
                },
            )
            DropdownMenuItem(
                text = { Text("Сделать начисление") },
                onClick = {
                    menuExpanded = false
                    onCharge()
                },
            )
        }
    }
}
