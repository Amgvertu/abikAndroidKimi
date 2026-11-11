package info.prorabka.abik.android.presentation.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.prorabka.abik.android.core.ui.components.DateField
import info.prorabka.abik.android.core.util.kopecksToRubles
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** Общий фильтр месяца/произвольного периода для финансовых вкладок. */
@Composable
fun FinancePeriodCard(
    selectedMonth: YearMonth,
    periodFrom: LocalDate?,
    periodTo: LocalDate?,
    amountKopecks: Long,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPeriodSelected: (LocalDate, LocalDate) -> Unit,
    onResetPeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPeriodDialog by remember { mutableStateOf(false) }
    val customPeriod = periodFrom != null && periodTo != null
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth, enabled = !customPeriod) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Предыдущий месяц")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (customPeriod) {
                            "${periodFrom!!.format(displayDate)} — ${periodTo!!.format(displayDate)}"
                        } else {
                            selectedMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")))
                                .replaceFirstChar { it.uppercase() }
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        amountKopecks.kopecksToRubles(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = onNextMonth, enabled = !customPeriod) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Следующий месяц")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { showPeriodDialog = true }) { Text("Выбрать период") }
                if (customPeriod) {
                    TextButton(onClick = onResetPeriod) { Text("Вернуться к месяцу") }
                }
            }
        }
    }

    if (showPeriodDialog) {
        var from by remember { mutableStateOf(periodFrom ?: selectedMonth.atDay(1)) }
        var to by remember { mutableStateOf(periodTo ?: selectedMonth.atEndOfMonth()) }
        var validation by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPeriodDialog = false },
            title = { Text("Период") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField(
                        displayValue = from.format(displayDate),
                        label = "С",
                        onSelect = { from = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DateField(
                        displayValue = to.format(displayDate),
                        label = "По",
                        onSelect = { to = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (from.isAfter(to)) {
                        validation = "Дата начала не может быть позже даты окончания"
                    } else {
                        onPeriodSelected(from, to)
                        showPeriodDialog = false
                    }
                }) { Text("Применить") }
            },
            dismissButton = {
                TextButton(onClick = { showPeriodDialog = false }) { Text("Отмена") }
            },
        )
    }
}
