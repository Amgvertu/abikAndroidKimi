package info.prorabka.abik.android.presentation.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.util.isoDateTimeToDisplay
import info.prorabka.abik.android.domain.model.*
import java.net.URI
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AdminAdsContent(ui: AdminViewModel.UiState, viewModel: AdminViewModel) {
    var editing by remember { mutableStateOf<AdvertisementDraft?>(null) }
    var deleting by remember { mutableStateOf<Advertisement?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Реклама на главной", style = MaterialTheme.typography.titleMedium)
            Text("Показывается одна публикация. Приоритет: команда → город → регион → все пользователи.", style = MaterialTheme.typography.bodySmall)
        }
        item { AbikButton("Создать публикацию", onClick = { editing = AdvertisementDraft() }) }
        items(ui.ads, key = { it.id }) { ad ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ad.imageUrl?.let { AsyncImage(viewModel.resolveUrl(it), null, Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Crop) }
                Text(ad.title ?: ad.body ?: "Публикация с изображением", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ad.body?.let { Text(it, maxLines = 3) }
                Text("Аудитория: ${ad.targetName ?: ad.targetType}", style = MaterialTheme.typography.bodySmall)
                Text("Период: ${ad.startsAt.isoDateTimeToDisplay()} — ${ad.endsAt.isoDateTimeToDisplay()}", style = MaterialTheme.typography.bodySmall)
                Text("Автор: ${ad.creatorName.orEmpty()}", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(if (ad.active) "Включена" else "Выключена", Modifier.weight(1f))
                    Switch(ad.active, { viewModel.setAdActive(ad.id, it) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { editing = ad.toDraft() }) { Text("Изменить") }
                    TextButton(onClick = { viewModel.adStats(ad.id) }) { Text("Статистика") }
                    TextButton(onClick = { deleting = ad }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                }
            } }
        }
    }
    editing?.let { draft -> AdminAdEditor(draft, ui, viewModel, onDismiss = { editing = null }) { viewModel.saveAd(it); editing = null } }
    deleting?.let { ad -> ConfirmAdminDialog("Удалить публикацию?", "Публикация и её изображение будут удалены. Статистика показов также удалится.", onDismiss = { deleting = null }) { viewModel.deleteAd(ad.id); deleting = null } }
}

@Composable
private fun AdminAdEditor(initial: AdvertisementDraft, ui: AdminViewModel.UiState, viewModel: AdminViewModel, onDismiss: () -> Unit, onSave: (AdvertisementDraft) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    var startText by remember { mutableStateOf(initial.startsAt.ifBlank {
        LocalDateTime.now().plusHours(1).format(adInputFormatter)
    }.displayToInput()) }
    var endText by remember { mutableStateOf(initial.endsAt.ifBlank {
        LocalDateTime.now().plusDays(7).format(adInputFormatter)
    }.displayToInput()) }
    var validation by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.uploadAdImage(uri) { url -> draft = draft.copy(imageUrl = url) }
    }
    val targetTypes = listOf("GLOBAL" to "Все пользователи", "REGION" to "Регион", "CITY" to "Город", "TEAM" to "Команда")
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (draft.id == null) "Новая публикация" else "Редактирование") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { AbikTextField(draft.title, { draft = draft.copy(title = it.take(100)) }, "Заголовок") }
            item { AbikTextField(draft.body, { draft = draft.copy(body = it.take(1000)) }, "Текст", singleLine = false) }
            item {
                draft.imageUrl?.let { AsyncImage(viewModel.resolveUrl(it), null, Modifier.fillMaxWidth().height(130.dp), contentScale = ContentScale.Crop) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !ui.uploadingImage) { Text(if (ui.uploadingImage) "Загрузка…" else "Выбрать изображение") }
                    if (draft.imageUrl != null) TextButton(onClick = { draft = draft.copy(imageUrl = null) }) { Text("Убрать") }
                }
            }
            item { AbikTextField(draft.linkUrl, { draft = draft.copy(linkUrl = it.take(2000)) }, "Ссылка https://") }
            item { AbikTextField(draft.buttonText, { draft = draft.copy(buttonText = it.take(100)) }, "Текст кнопки перехода") }
            item { AdminDropdown("Аудитория", targetTypes, targetTypes.firstOrNull { it.first == draft.targetType }, {
                draft = draft.copy(targetType = it?.first ?: "GLOBAL", targetId = null)
            }, { it.second }) }
            if (draft.targetType == "REGION") item { AdminDropdown("Регион", ui.regions, ui.regions.firstOrNull { it.id == draft.targetId }, {
                draft = draft.copy(targetId = it?.id)
            }, { it.name }) }
            if (draft.targetType == "CITY") {
                item { AdminDropdown("Регион для выбора города", ui.regions, ui.regions.firstOrNull { it.id == ui.filterRegionId }, {
                    draft = draft.copy(targetId = null); viewModel.loadMessageCities(it?.id)
                }, { it.name }) }
                item { AdminDropdown("Город", ui.cities, ui.cities.firstOrNull { it.id == draft.targetId }, {
                    draft = draft.copy(targetId = it?.id)
                }, { it.name }) }
            }
            if (draft.targetType == "TEAM") item { AdminDropdown("Команда", ui.targetTeams, ui.targetTeams.firstOrNull { it.id == draft.targetId }, {
                draft = draft.copy(targetId = it?.id)
            }, { it.name }) }
            item { Text("Даты в формате ДД.ММ.ГГГГ ЧЧ:ММ", style = MaterialTheme.typography.bodySmall) }
            item { AbikTextField(startText, { startText = it }, "Начало") }
            item { AbikTextField(endText, { endText = it }, "Окончание") }
            item { Row { Text("Активна", Modifier.weight(1f)); Switch(draft.active, { draft = draft.copy(active = it) }) } }
            validation?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }, confirmButton = { TextButton(onClick = {
        val start = startText.parseInput(); val end = endText.parseInput()
        val linkValid = draft.linkUrl.isBlank() || runCatching { URI(draft.linkUrl).scheme in listOf("http", "https") }.getOrDefault(false)
        when {
            draft.title.isBlank() && draft.body.isBlank() && draft.imageUrl == null -> validation = "Заполните заголовок, текст или добавьте изображение"
            draft.linkUrl.isNotBlank() && draft.buttonText.isBlank() -> validation = "Для ссылки укажите текст кнопки"
            !linkValid -> validation = "Некорректная ссылка"
            draft.targetType != "GLOBAL" && draft.targetId == null -> validation = "Выберите значение аудитории"
            start == null || end == null -> validation = "Проверьте даты и время"
            !OffsetDateTime.parse(end).isAfter(OffsetDateTime.parse(start)) -> validation = "Окончание должно быть позже начала"
            else -> onSave(draft.copy(startsAt = start, endsAt = end))
        }
    }) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@Composable
fun AdminAdStatsDialog(stats: AdStats, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Статистика публикации") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Показы: ${stats.totals.views} • Уникальные зрители: ${stats.totals.viewers}") }
            item { Text("Клики: ${stats.totals.clicks} • Уникальные: ${stats.totals.uniqueClicks}") }
            item { Text("География фактических зрителей", style = MaterialTheme.typography.titleMedium) }
            items(stats.geography) { row -> Card { Column(Modifier.padding(10.dp)) {
                Text(listOfNotNull(row.regionName, row.cityName, row.teamName).ifEmpty { listOf("Без географии") }.joinToString(" • "))
                Text("Показы: ${row.views} / ${row.viewers} • Клики: ${row.clicks} / ${row.uniqueClicks}", style = MaterialTheme.typography.bodySmall)
            } } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } })
}

private fun Advertisement.toDraft() = AdvertisementDraft(id, title.orEmpty(), body.orEmpty(), imageUrl, linkUrl.orEmpty(),
    buttonText.orEmpty(), targetType, targetId, startsAt, endsAt, active)
private fun AdvertisementDraft.withDisplayDates() = this
private val adInputFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
private fun String.displayToInput(): String {
    if (isBlank()) return LocalDateTime.now().plusHours(1).format(adInputFormatter)
    return runCatching { OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.systemDefault()).format(adInputFormatter) }.getOrDefault(this)
}
private fun String.parseInput(): String? = runCatching {
    LocalDateTime.parse(trim(), adInputFormatter).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()
}.getOrNull()
