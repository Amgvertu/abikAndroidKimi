package info.prorabka.abik.android.presentation.reports

import android.content.Intent
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
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.BuildConfig
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.DateField
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.ui.theme.Danger
import info.prorabka.abik.android.core.ui.theme.Success
import info.prorabka.abik.android.core.util.kopecksToRubles
import info.prorabka.abik.android.domain.model.ChargeStatus
import java.io.File
import java.time.format.DateTimeFormatter

/** Отчёты: сводка по команде, сводка по участникам, экспорт в Excel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.load() }

    // Как только файл выгружен — открываем системный диалог «Поделиться»
    LaunchedEffect(ui.exportedFile) {
        ui.exportedFile?.let { file ->
            shareXlsx(context, file)
            viewModel.consumeExportedFile()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отчёты") },
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
            // --- Сводка ---
            ui.summary?.let { summary ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Сводка по команде", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        SummaryRow("Всего начислено", summary.totalChargesKopecks.kopecksToRubles())
                        SummaryRow("Оплачено", summary.totalPaidKopecks.kopecksToRubles())
                        SummaryRow(
                            "Остаток к оплате",
                            summary.totalRemainingKopecks.kopecksToRubles(),
                            Danger,
                        )
                        SummaryRow(
                            "Общий баланс",
                            summary.totalBalanceKopecks.kopecksToRubles(),
                            if (summary.totalBalanceKopecks < 0) Danger else Success,
                        )
                        SummaryRow(
                            "Активных начислений",
                            summary.activeChargesCount.toString(),
                        )
                    }
                }
            }

            // --- Сводка по участникам ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("По участникам", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (ui.members.isEmpty()) {
                        Text(
                            "Нет данных",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ui.members.forEach { member ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "начислено ${member.totalChargesKopecks.kopecksToRubles()} • " +
                                        "оплачено ${member.totalPaidKopecks.kopecksToRubles()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                member.balanceKopecks.kopecksToRubles(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (member.balanceKopecks < 0) Danger else Success,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }

            // --- Экспорт ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Экспорт в Excel", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    FilterDropdown(
                        label = "Участник",
                        value = ui.teamMembers.firstOrNull { it.userId == ui.filterUserId }
                            ?.displayName ?: "Все",
                        items = listOf(null to "Все") +
                            ui.teamMembers.map { it.userId as Long? to it.displayName },
                        onSelect = viewModel::setFilterUser,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FilterDropdown(
                        label = "Группа",
                        value = ui.groups.firstOrNull { it.id == ui.filterGroupId }?.name ?: "Все",
                        items = listOf(null to "Все") +
                            ui.groups.map { it.id as Long? to it.name },
                        onSelect = viewModel::setFilterGroup,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Статус", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        listOf(
                            null to "Все",
                            ChargeStatus.ACTIVE.name to "Активные",
                            ChargeStatus.PAID.name to "Оплач.",
                            ChargeStatus.PARTIALLY_PAID.name to "Частично",
                            ChargeStatus.CANCELLED.name to "Отмен.",
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = ui.filterStatus == value,
                                onClick = { viewModel.setFilterStatus(value) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateField(
                            displayValue = ui.filterDateFrom
                                ?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "—",
                            label = "С",
                            onSelect = viewModel::setFilterDateFrom,
                            modifier = Modifier.weight(1f),
                        )
                        DateField(
                            displayValue = ui.filterDateTo
                                ?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "—",
                            label = "По",
                            onSelect = viewModel::setFilterDateTo,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    ErrorText(ui.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    AbikButton(
                        text = "Скачать начисления (.xlsx)",
                        onClick = viewModel::exportCharges,
                        loading = ui.exporting,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AbikButton(
                        text = "Скачать сводку по участникам (.xlsx)",
                        onClick = viewModel::exportMembers,
                        loading = ui.exporting,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    value: String,
    items: List<Pair<Long?, String>>,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun shareXlsx(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(intent, "Экспорт Excel").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
