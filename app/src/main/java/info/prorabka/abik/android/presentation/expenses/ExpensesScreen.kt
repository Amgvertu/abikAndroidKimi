package info.prorabka.abik.android.presentation.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.*
import info.prorabka.abik.android.core.util.kopecksToRubles
import info.prorabka.abik.android.core.util.parseMoneyToKopecks
import info.prorabka.abik.android.domain.model.ExpenseFormData
import info.prorabka.abik.android.domain.model.TeamExpense
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val expenseCategories = linkedMapOf(
    "ICE" to "Лёд",
    "COACH" to "Тренер",
    "GAME" to "Игра",
    "GOALKEEPER" to "Вратарь",
    "OTHER" to "Прочее",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onBack: () -> Unit = {},
    embedded: Boolean = false,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    info.prorabka.abik.android.presentation.guide.ScreenGuide("expenses")
    info.prorabka.abik.android.core.ui.RealtimeRefresh { viewModel.load() }
    var editor by remember { mutableStateOf<TeamExpense?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TeamExpense?>(null) }

    LaunchedEffect(ui.exportedFile) {
        ui.exportedFile?.let { file ->
            info.prorabka.abik.android.presentation.reports.shareXlsx(context, file)
            viewModel.consumeExportedFile()
        }
    }

    Scaffold(
        contentWindowInsets = if (embedded) {
            androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        topBar = {
            if (!embedded) {
                TopAppBar(
                    title = { Text("Расходы") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        if (ui.isOwner) {
                            TextButton(onClick = viewModel::export, enabled = !ui.exporting) {
                                Text(if (ui.exporting) "Экспорт…" else "Excel")
                            }
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (ui.canEdit) {
                FloatingActionButton(onClick = { creating = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить расход")
                }
            }
        },
    ) { padding ->
        AbikPullToRefresh(
            loading = ui.loading,
            onRefresh = viewModel::load,
            modifier = Modifier.padding(padding),
        ) {
            when {
                ui.loading && ui.expenses.isEmpty() -> LoadingBox()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        info.prorabka.abik.android.presentation.finance.FinancePeriodCard(
                            selectedMonth = ui.selectedMonth,
                            periodFrom = ui.periodFrom,
                            periodTo = ui.periodTo,
                            amountKopecks = ui.total,
                            onPreviousMonth = viewModel::previousMonth,
                            onNextMonth = viewModel::nextMonth,
                            onPeriodSelected = viewModel::setPeriod,
                            onResetPeriod = viewModel::resetPeriod,
                        )
                    }
                    if (embedded && ui.isOwner) {
                        item {
                            TextButton(
                                onClick = viewModel::export,
                                enabled = !ui.exporting,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (ui.exporting) "Формирование Excel…" else "Выгрузить расходы в Excel")
                            }
                        }
                    }
                    item { ErrorText(ui.error) }
                    if (ui.filteredExpenses.isEmpty() && !ui.loading) {
                        item { EmptyState("За выбранный период расходов нет") }
                    }
                    items(ui.filteredExpenses, key = { it.id }) { expense ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                                    Text(
                                        expenseCategories[expense.category] ?: expense.category,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        LocalDate.parse(expense.expenseDate)
                                            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    expense.comment?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (expense.pendingSync || expense.syncError != null) {
                                        Text(
                                            expense.syncError ?: "Ожидает синхронизации",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (expense.syncError == null) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            },
                                        )
                                    }
                                    Text(
                                        expense.amountKopecks.kopecksToRubles(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                if (ui.canEdit && !expense.pendingSync && expense.id > 0) {
                                    IconButton(onClick = { editor = expense }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Изменить")
                                    }
                                    IconButton(onClick = { deleting = expense }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Удалить",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating || editor != null) {
        ExpenseDialog(
            initial = editor,
            saving = ui.saving,
            onDismiss = { creating = false; editor = null },
            onSave = { data ->
                viewModel.save(editor?.id, data) { creating = false; editor = null }
            },
        )
    }
    deleting?.let { expense ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Удалить расход?") },
            text = { Text("Запись на ${expense.amountKopecks.kopecksToRubles()} будет удалена.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(expense.id) { deleting = null } }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun ExpenseDialog(
    initial: TeamExpense?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ExpenseFormData) -> Unit,
) {
    var category by remember { mutableStateOf(initial?.category ?: "ICE") }
    var amount by remember {
        mutableStateOf(initial?.amountKopecks?.let { String.format(Locale.US, "%.2f", it / 100.0) } ?: "")
    }
    var date by remember { mutableStateOf(initial?.expenseDate?.let(LocalDate::parse) ?: LocalDate.now()) }
    var comment by remember { mutableStateOf(initial?.comment.orEmpty()) }
    var validation by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (initial == null) "Новый расход" else "Изменить расход") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Категория", style = MaterialTheme.typography.labelMedium)
                expenseCategories.forEach { (key, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = category == key, onClick = { category = key })
                        Text(label)
                    }
                }
                Spacer(Modifier.height(8.dp))
                AbikTextField(
                    value = amount,
                    onValueChange = { amount = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' }.take(12) },
                    label = "Сумма, ₽",
                    keyboardType = KeyboardType.Decimal,
                )
                Spacer(Modifier.height(8.dp))
                DateField(
                    displayValue = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    label = "Дата",
                    onSelect = { date = it },
                )
                Spacer(Modifier.height(8.dp))
                AbikTextField(
                    value = comment,
                    onValueChange = { comment = it.take(1000) },
                    label = "Комментарий",
                    singleLine = false,
                )
                ErrorText(validation)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    val kopecks = amount.parseMoneyToKopecks()
                    if (kopecks == null || kopecks <= 0) {
                        validation = "Введите сумму больше нуля"
                    } else {
                        onSave(ExpenseFormData(category, kopecks, date.toString(), comment.trim().ifBlank { null }))
                    }
                },
            ) { Text(if (saving) "Сохранение…" else "Сохранить") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Отмена") } },
    )
}
