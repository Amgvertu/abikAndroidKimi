package info.prorabka.abik.android.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.util.isoDateTimeToDisplay
import info.prorabka.abik.android.domain.model.AdminMessageDraft
import java.net.URI

@Composable
fun AdminMessagesContent(ui: AdminViewModel.UiState, viewModel: AdminViewModel) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var region by remember { mutableStateOf<Long?>(null) }
    var city by remember { mutableStateOf<Long?>(null) }
    var team by remember { mutableStateOf<Long?>(null) }
    var position by remember { mutableStateOf<String?>(null) }
    var validation by remember { mutableStateOf<String?>(null) }
    val draft = AdminMessageDraft(title, body, link, region, city, team, position)
    val positions = listOf("GOALKEEPER" to "Вратари", "DEFENDER" to "Защитники", "FORWARD" to "Нападающие", "TRAINER" to "Тренеры")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Новая рассылка", style = MaterialTheme.typography.titleMedium)
            AbikTextField(title, { title = it.take(200) }, "Заголовок")
            AbikTextField(body, { body = it.take(1000) }, "Текст сообщения", singleLine = false)
            AbikTextField(link, { link = it.take(2000) }, "Ссылка (необязательно)")
            AdminDropdown("Регион", ui.regions, ui.regions.firstOrNull { it.id == region }, {
                region = it?.id; city = null; viewModel.loadMessageCities(region)
            }, { it.name })
            AdminDropdown("Город", ui.cities, ui.cities.firstOrNull { it.id == city }, { city = it?.id }, { it.name })
            AdminDropdown("Команда", ui.targetTeams, ui.targetTeams.firstOrNull { it.id == team }, { team = it?.id }, { it.name })
            AdminDropdown("Амплуа", positions, positions.firstOrNull { it.first == position }, { position = it?.first }, { it.second })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.recipientCount(draft) }, enabled = title.isNotBlank() && body.isNotBlank()) { Text("Проверить аудиторию") }
                ui.recipientCount?.let { Text("Получателей: $it", modifier = Modifier.padding(top = 12.dp)) }
            }
            validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            AbikButton("Отправить", enabled = title.isNotBlank() && body.isNotBlank(), onClick = {
                val validLink = link.isBlank() || runCatching { URI(link).scheme in listOf("http", "https") }.getOrDefault(false)
                if (!validLink) validation = "Ссылка должна начинаться с http:// или https://"
                else {
                    validation = null; viewModel.sendMessage(draft)
                    title = ""; body = ""; link = ""
                }
            })
        } } }
        item { Text("История и прочтения", style = MaterialTheme.typography.titleMedium) }
        items(ui.messages, key = { it.id }) { message ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(message.title, fontWeight = FontWeight.Bold)
                Text(message.body)
                Text(message.audience, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Получателей: ${message.recipients} • Прочитали: ${message.read}")
                Text(message.createdAt?.isoDateTimeToDisplay().orEmpty(), style = MaterialTheme.typography.labelSmall)
            } }
        }
    }
}

@Composable
fun AdminRegistrationContent(ui: AdminViewModel.UiState, viewModel: AdminViewModel) {
    var status by remember { mutableStateOf<String?>(null) }
    val statuses = listOf("ERROR" to "Ошибки", "DELIVERY_FAILED" to "Не доставлено", "OPERATOR_ACCEPTED" to "У оператора", "DELIVERED" to "Доставлено", "WARNING" to "Предупреждения")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Регистрации и SMS", style = MaterialTheme.typography.titleMedium)
            Text("Телефоны маскированы. Пароли и коды подтверждения не записываются.", style = MaterialTheme.typography.bodySmall)
        }
        item { AdminDropdown("Статус", statuses, statuses.firstOrNull { it.first == status }, {
            status = it?.first; viewModel.loadRegistrationEvents(status)
        }, { it.second }) }
        items(ui.registrationEvents, key = { it.id }) { event ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${event.stage} • ${event.status}", fontWeight = FontWeight.Bold,
                    color = if (event.status.contains("ERROR") || event.status.contains("FAILED")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(listOfNotNull(event.phoneMasked, event.code, event.client).joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                event.details?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(event.occurredAt?.isoDateTimeToDisplay().orEmpty(), style = MaterialTheme.typography.labelSmall)
            } }
        }
    }
}
