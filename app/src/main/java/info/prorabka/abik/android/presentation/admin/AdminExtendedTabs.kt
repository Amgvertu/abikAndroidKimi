package info.prorabka.abik.android.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.domain.model.AdminStats
import info.prorabka.abik.android.domain.model.AppRelease
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatsTab(stats: AdminStats?) {
    if (stats == null) {
        Text("Статистика недоступна", Modifier.padding(16.dp))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Всего", style = MaterialTheme.typography.titleMedium)
                    Text("Пользователи: ${stats.totalUsers}")
                    Text("Команды: ${stats.totalTeams}")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Новые пользователи", style = MaterialTheme.typography.titleMedium)
                    Text("Сегодня: ${stats.usersToday}  •  За 7 дней: ${stats.usersWeek}  •  За 30 дней: ${stats.usersMonth}")
                    Text("Новые команды", style = MaterialTheme.typography.titleMedium)
                    Text("Сегодня: ${stats.teamsToday}  •  За 7 дней: ${stats.teamsWeek}  •  За 30 дней: ${stats.teamsMonth}")
                }
            }
        }
        item { Text("Последние 30 дней", style = MaterialTheme.typography.titleMedium) }
        items(stats.last30Days) { day ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    runCatching { LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }
                        .getOrDefault(day.date),
                    Modifier.weight(1f),
                )
                Text("Пользователи: ${day.users}", Modifier.weight(1f))
                Text("Команды: ${day.teams}", Modifier.weight(1f), fontWeight = FontWeight.Medium)
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun ReleaseTab(onPublish: (AppRelease) -> Unit) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var mandatory by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Публикация версии Android", style = MaterialTheme.typography.titleMedium)
        Text(
            "APK должен быть доступен по прямой HTTPS-ссылке. После публикации приложение предложит обновление пользователям.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AbikTextField(code, { code = it.filter(Char::isDigit).take(9) }, "Код версии", keyboardType = KeyboardType.Number)
        AbikTextField(name, { name = it.take(40) }, "Название версии, например 1.1")
        AbikTextField(notes, { notes = it.take(10000) }, "Что нового", singleLine = false)
        AbikTextField(url, { url = it.take(2000) }, "HTTPS-ссылка на APK", singleLine = false)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = mandatory, onCheckedChange = { mandatory = it })
            Text("Обязательное обновление")
        }
        validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        AbikButton(
            text = "Опубликовать",
            enabled = code.isNotBlank() && name.isNotBlank() && notes.isNotBlank() && url.isNotBlank(),
            onClick = {
                val versionCode = code.toIntOrNull()
                if (versionCode == null || versionCode <= 0 || !url.startsWith("https://")) {
                    validation = "Проверьте код версии и прямую HTTPS-ссылку"
                } else {
                    validation = null
                    onPublish(AppRelease(versionCode, name.trim(), notes.trim(), url.trim(), mandatory))
                }
            },
        )
    }
}
