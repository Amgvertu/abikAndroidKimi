package info.prorabka.abik.android.presentation.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikPullToRefresh
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.domain.model.EventDetail
import info.prorabka.abik.android.domain.model.EventSection
import info.prorabka.abik.android.domain.model.EventType
import info.prorabka.abik.android.domain.model.EventVoter
import info.prorabka.abik.android.domain.model.LineupRole
import info.prorabka.abik.android.domain.model.LineupSlot
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.SweaterColor
import info.prorabka.abik.android.domain.model.VoteAnswer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Событие уже прошло: момент окончания = указанные дата и время. */
private fun isEventPast(dateIso: String, time: String): Boolean {
    val date = runCatching { LocalDate.parse(dateIso) }.getOrNull() ?: return false
    val localTime = runCatching { LocalTime.parse(time) }.getOrNull()
        ?: return date.isBefore(LocalDate.now())
    return !LocalDateTime.of(date, localTime).isAfter(LocalDateTime.now())
}

/** Детали события: информация, голосование, списки, заявка (просмотр). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: EventDetailsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    info.prorabka.abik.android.core.ui.RealtimeRefresh { viewModel.load() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSweatersDialog by remember { mutableStateOf(false) }
    var showLineupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(ui.deleted) {
        if (ui.deleted) onBack()
    }
    LaunchedEffect(ui.sweatersSaved) {
        if (ui.sweatersSaved) {
            showSweatersDialog = false
            viewModel.consumeActionFlags()
        }
    }
    LaunchedEffect(ui.lineupSaved) {
        if (ui.lineupSaved) {
            showLineupDialog = false
            viewModel.consumeActionFlags()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(ui.event?.title ?: "Событие", maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (ui.isManager && ui.event != null) {
                        IconButton(onClick = { onEdit(viewModel.eventId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        AbikPullToRefresh(
            loading = ui.loading,
            onRefresh = viewModel::load,
            modifier = Modifier.padding(padding),
        ) {
            Box(Modifier.fillMaxSize()) {
                when {
                    ui.loading -> LoadingBox()
                    ui.event == null -> EmptyState(ui.error ?: "Событие не найдено")
                    else -> EventContent(
                        event = ui.event!!,
                        acting = ui.acting,
                        error = ui.error,
                        info = ui.info,
                        canEditSweaters = ui.canEditSweaters,
                        canEditLineup = ui.canEditLineup,
                        onVote = viewModel::vote,
                        onEditSweaters = { showSweatersDialog = true },
                        onEditLineup = { showLineupDialog = true },
                    )
                }
            }
        }
    }

    if (showSweatersDialog && ui.event != null) {
        SweatersDialog(
            voters = ui.event!!.voters.filter {
                it.answer == VoteAnswer.YES && !it.inReserve && !it.isTrainer
            },
            saving = ui.sweatersSaving,
            error = ui.error,
            onSave = viewModel::setSweaters,
            onDismiss = {
                showSweatersDialog = false
                viewModel.clearMessages()
            },
        )
    }

    if (showLineupDialog && ui.event != null) {
        LineupDialog(
            event = ui.event!!,
            saving = ui.lineupSaving,
            error = ui.error,
            onSave = { slots, sweaters ->
                if (ui.event!!.section == EventSection.REGULAR) {
                    viewModel.setLineup(slots)
                } else {
                    viewModel.setLineupWithSweaters(slots, sweaters)
                }
            },
            onDismiss = {
                showLineupDialog = false
                viewModel.clearMessages()
            },
        )
    }

    if (showDeleteDialog) {
        val inSeries = ui.event?.seriesId != null
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить событие?") },
            text = {
                Text(
                    if (inSeries) {
                        "Событие входит в серию. Удалить только его или всю серию целиком?"
                    } else {
                        "Действие нельзя отменить."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete("single")
                    },
                ) { Text(if (inSeries) "Только это" else "Удалить") }
            },
            dismissButton = {
                Row {
                    if (inSeries) {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                viewModel.delete("series")
                            },
                        ) { Text("Всю серию", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
                }
            },
        )
    }
}

@Composable
private fun EventContent(
    event: EventDetail,
    acting: Boolean,
    error: String?,
    info: String?,
    canEditSweaters: Boolean,
    canEditLineup: Boolean,
    onVote: (VoteAnswer) -> Unit,
    onEditSweaters: () -> Unit,
    onEditLineup: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Тип + дата + место
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                event.type.display,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
            if (event.status != "ACTIVE") {
                Spacer(Modifier.size(8.dp))
                Text(
                    "Отменено",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            formatDateTime(event.dateIso, event.time),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            event.place,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        event.note?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(Modifier.height(4.dp))
            Text(note, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))

        // Голосование
        Text(
            "Мой ответ",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        // Опрос закрывается в указанное время события (момент окончания)
        val pollClosed = isEventPast(event.dateIso, event.time)
        if (pollClosed) {
            Text(
                "Опрос завершён",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoteAnswer.entries.forEach { answer ->
                // «Иду» — только основной состав; резерв считается отдельно
                val count = if (answer == VoteAnswer.YES && event.requiredPlayers != null) {
                    event.voters.count { it.answer == answer && !it.inReserve }
                } else {
                    event.voters.count { it.answer == answer }
                }
                FilterChip(
                    selected = event.myVote == answer,
                    onClick = { if (!acting) onVote(answer) },
                    enabled = !acting && !pollClosed,
                    label = { Text("${answer.display} ($count)") },
                )
            }
        }

        if (canEditSweaters) {
            TextButton(onClick = onEditSweaters) { Text("Назначить свитеры") }
        }

        if (event.pendingSync || event.syncError != null) {
            Text(
                event.syncError ?: "Есть изменения, ожидающие синхронизации с сервером",
                color = if (event.syncError != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        info?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        ErrorText(error)

        // Списки проголосовавших
        VoteAnswer.entries.forEach { answer ->
            val group = event.voters.filter { it.answer == answer }
            if (group.isNotEmpty()) {
                val main = if (answer == VoteAnswer.YES) group.filter { !it.inReserve } else group
                val reserve = if (answer == VoteAnswer.YES) group.filter { it.inReserve } else emptyList()
                Spacer(Modifier.height(12.dp))
                val header = if (answer == VoteAnswer.YES && event.requiredPlayers != null) {
                    "${answer.display} (${main.size} из ${event.requiredPlayers})"
                } else {
                    "${answer.display} (${group.size})"
                }
                Text(
                    header,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (answer == VoteAnswer.YES) MainVotersView(event, main) else VoterGroup(main)
                if (reserve.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Резерв (${reserve.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    VoterGroup(reserve)
                }
            }
        }

        if (canEditLineup) {
            TextButton(onClick = onEditLineup) {
                Text(
                    if (event.lineup.isNullOrEmpty()) "Создать звенья"
                    else "Изменить звенья",
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** Основной состав: цвета на тренировке, звенья в игре, тренеры отдельно. */
@Composable
private fun MainVotersView(event: EventDetail, voters: List<EventVoter>) {
    val trainers = voters.filter { it.isTrainer }
    val players = voters.filter { !it.isTrainer }
    when {
        event.type == EventType.TRAINING && players.any { it.sweaterColor != null } -> {
            TrainingColorColumns(players)
        }
        event.type == EventType.TRAINING -> VoterGroup(players)
        !event.lineup.isNullOrEmpty() -> {
            LineupView(event)
            val assigned = event.lineup.orEmpty().mapTo(mutableSetOf()) { it.userId }
            val unassigned = players.filter { it.userId !in assigned }
            if (unassigned.isNotEmpty()) {
                Text(
                    "Не распределены (${unassigned.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                VoterGroup(unassigned)
            }
        }
        else -> VoterGroup(players)
    }
    if (trainers.isNotEmpty()) {
        Text(
            "Тренеры (${trainers.size})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        VoterGroup(trainers)
    }
}

/** Список проголосовавших с группировкой по амплуа. Тренеры — отдельная группа. */
@Composable
private fun VoterGroup(voters: List<EventVoter>) {
    val order = listOf(
        Position.GOALKEEPER to "Вратари",
        Position.DEFENDER to "Защитники",
        Position.FORWARD to "Нападающие",
    )
    val trainers = voters.filter { it.isTrainer }
    val players = voters.filter { !it.isTrainer }
    Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            var first = true
            order.forEach { (position, label) ->
                val inPosition = players.filter { it.positions.contains(position) }
                if (inPosition.isNotEmpty()) {
                    if (!first) Spacer(Modifier.height(6.dp))
                    first = false
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    inPosition.forEach { VoterRow(it) }
                }
            }
            val withoutPosition = players.filter { it.positions.isEmpty() }
            if (withoutPosition.isNotEmpty()) {
                if (!first) Spacer(Modifier.height(6.dp))
                first = false
                Text(
                    "Без амплуа",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                withoutPosition.forEach { VoterRow(it) }
            }
            if (trainers.isNotEmpty()) {
                if (!first) Spacer(Modifier.height(6.dp))
                Text(
                    "Тренер",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                trainers.forEach { VoterRow(it) }
            }
        }
    }
}

@Composable
private fun VoterRow(voter: EventVoter) {
    Row(
        // Отступ списка под заголовком группы
        Modifier.fillMaxWidth().padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        voter.sweaterColor?.let { color ->
            SweaterDot(color)
            Spacer(Modifier.size(6.dp))
        }
        Text(shortPlayerName(voter.fullName), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SweaterDot(color: SweaterColor) {
    val fill = when (color) {
        SweaterColor.WHITE -> Color.White
        SweaterColor.BLACK -> Color.Black
        SweaterColor.RED -> Color(0xFFD32F2F)
    }
    Box(
        Modifier
            .size(12.dp)
            .background(fill, CircleShape)
            .border(1.dp, Color.Gray, CircleShape),
    )
}

/** Звенья (просмотр): вратарь и звенья. */
@Composable
private fun LineupView(event: EventDetail) {
    val names = event.voters.associate { it.userId to shortPlayerName(it.fullName) }
    val slots = event.lineup ?: return
    Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            slots.filter { it.role == LineupRole.GOALKEEPER }.forEach { slot ->
                Text(
                    "Вратарь: ${names[slot.userId] ?: "user#${slot.userId}"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val lines = slots.filter { it.role != LineupRole.GOALKEEPER }
                .groupBy { it.lineNumber ?: 0 }
                .toSortedMap()
            lines.forEach { (lineNumber, lineSlots) ->
                Spacer(Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))
                val lineColor = if (event.section == EventSection.SCRIMMAGE) {
                    lineSlots.firstNotNullOfOrNull { slot ->
                        event.voters.firstOrNull { it.userId == slot.userId }
                            ?.sweaterColor.supportedColor()
                    }
                } else {
                    null
                }
                Text(
                    "Звено $lineNumber" + lineColor?.let { " (${it.display})" }.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val defenders = lineSlots.filter { it.role == LineupRole.DEFENDER }
                    .sortedBy { it.slotNumber }
                val forwards = lineSlots.filter { it.role == LineupRole.FORWARD }
                    .sortedBy { it.slotNumber }
                if (forwards.isNotEmpty()) {
                    Text(
                        "Н: " + forwards.joinToString(", ") {
                            names[it.userId] ?: "user#${it.userId}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (defenders.isNotEmpty()) {
                    Text(
                        "З: " + defenders.joinToString(", ") {
                            names[it.userId] ?: "user#${it.userId}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}


/** Фамилия и инициал имени для всех представлений события. */
private fun shortPlayerName(fullName: String): String =
    info.prorabka.abik.android.core.util.NameFormatter.short(fullName)

// ====== Редактор свитеров (тренировка; владелец/админ/тренер) ======

@Composable
private fun SweatersDialog(
    voters: List<EventVoter>,
    saving: Boolean,
    error: String?,
    onSave: (Map<Long, SweaterColor?>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = remember {
        mutableStateMapOf<Long, SweaterColor?>().apply {
            voters.forEach { put(it.userId, it.sweaterColor.supportedColor()) }
        }
    }
    val options = remember {
        listOf<Pair<SweaterColor?, String>>(
            SweaterColor.BLACK to "Чёрные",
            null to "Не распределены",
            SweaterColor.WHITE to "Белые",
        )
    }
    fun colorKey(color: SweaterColor?) = color?.name ?: "none"

    var drag by remember { mutableStateOf<LineupDrag?>(null) }
    val columnBounds = remember { mutableStateMapOf<String, Rect>() }
    var containerTopLeft by remember { mutableStateOf(Offset.Zero) }

    fun finishDrag(d: LineupDrag) {
        // Для горизонтального свайпа важна только колонка по X. Палец может
        // закончить жест на любом игроке или на пустом месте под заголовком.
        val target = columnBounds.entries.firstOrNull { (_, bounds) ->
            d.pos.x >= bounds.left && d.pos.x <= bounds.right
        }?.key ?: return
        colors[d.userId] = if (target == "none") null else SweaterColor.valueOf(target)
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!saving && drag == null) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Box(Modifier.onGloballyPositioned { containerTopLeft = it.boundsInRoot().topLeft }) {
                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    Text(
                        "Цвета джерси",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Проведите фамилию влево или вправо к нужному цвету.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (voters.isEmpty()) {
                        Text("В основном составе пока нет игроков")
                    } else {
                        Row(
                            Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            options.forEach { (color, title) ->
                                val key = colorKey(color)
                                val columnVoters = voters.filter { colors[it.userId] == color }
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .onGloballyPositioned {
                                            columnBounds[key] = it.boundsInRoot()
                                        }
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .verticalScroll(
                                            rememberScrollState(),
                                            enabled = drag == null,
                                        )
                                        .padding(4.dp),
                                ) {
                                    ColorColumnTitle(title, color)
                                    groupedVoters(columnVoters).forEach { (group, members) ->
                                        Text(
                                            group,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                                        )
                                        members.forEach { voter ->
                                            SweaterChip(
                                                userId = voter.userId,
                                                name = shortPlayerName(voter.fullName),
                                                slotId = key,
                                                drag = drag,
                                                onDragStart = { drag = it },
                                                onDragMove = { drag?.pos = it },
                                                onDragEnd = {
                                                    drag?.let { finishDrag(it) }
                                                    drag = null
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ErrorText(error)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss, enabled = !saving && drag == null) {
                            Text("Отмена")
                        }
                        Spacer(Modifier.size(8.dp))
                        TextButton(
                            onClick = { onSave(colors.toMap()) },
                            enabled = !saving && drag == null && voters.isNotEmpty(),
                        ) { Text(if (saving) "Сохранение…" else "Сохранить") }
                    }
                }
                drag?.let { d ->
                    Box(
                        Modifier
                            .offset {
                                IntOffset(
                                    (d.pos.x - containerTopLeft.x - d.chipSize.width / 2).roundToInt(),
                                    (d.pos.y - containerTopLeft.y - d.chipSize.height / 2).roundToInt(),
                                )
                            }
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    ) {
                        Text(
                            voters.firstOrNull { it.userId == d.userId }
                                ?.let { shortPlayerName(it.fullName) }.orEmpty(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

/** Старые значения цвета считаются нераспределёнными, чтобы не терять игроков. */
private fun SweaterColor?.supportedColor(): SweaterColor? = when (this) {
    SweaterColor.BLACK, SweaterColor.WHITE -> this
    else -> null
}

/** Три группы тренировки: чёрные, нераспределённые и белые. */
@Composable
private fun TrainingColorColumns(voters: List<EventVoter>) {
    val options = listOf<Pair<SweaterColor?, String>>(
        SweaterColor.BLACK to "Чёрные",
        null to "Не распределены",
        SweaterColor.WHITE to "Белые",
    )
    Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (color, title) ->
                Column(Modifier.weight(1f)) {
                    ColorColumnTitle(title, color)
                    groupedVoters(voters.filter { it.sweaterColor.supportedColor() == color })
                        .forEach { (group, members) ->
                            Text(
                                group,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 5.dp),
                            )
                            members.forEach {
                                Text(
                                    shortPlayerName(it.fullName),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    modifier = Modifier.padding(start = 3.dp, top = 2.dp),
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun ColorColumnTitle(title: String, color: SweaterColor?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (color != null) {
            SweaterDot(color)
            Spacer(Modifier.size(3.dp))
        }
        Text(title, style = MaterialTheme.typography.labelSmall, maxLines = 2)
    }
}

/** Основное амплуа: каждый игрок попадает только в одну группу. */
private fun groupedVoters(voters: List<EventVoter>): List<Pair<String, List<EventVoter>>> {
    val grouped = voters.groupBy { voter ->
        when {
            voter.isTrainer -> 4
            Position.GOALKEEPER in voter.positions -> 0
            Position.DEFENDER in voter.positions -> 1
            Position.FORWARD in voter.positions -> 2
            else -> 3
        }
    }
    return listOf(
        0 to "Вратари",
        1 to "Защитники",
        2 to "Нападающие",
        3 to "Без амплуа",
        4 to "Тренеры",
    ).mapNotNull { (index, title) ->
        grouped[index]?.sortedBy { it.fullName }?.let { title to it }
    }
}

// ====== Редактор заявки на игру (игра/матч; владелец/админ) ======
// Игроки распределяются по слотам звеньев перетаскиванием (long-press + drag).

/** Полевые игроки могут занимать слоты и защитников, и нападающих. */
private fun slotAccepts(slotId: String, positions: Set<Position>): Boolean = when {
    positions.isEmpty() -> true // без амплуа — универсал
    slotId == "gk" -> Position.GOALKEEPER in positions
    slotId.startsWith("d") || slotId.startsWith("f") ->
        Position.DEFENDER in positions || Position.FORWARD in positions
    else -> false
}

private class LineupDrag(
    val userId: Long,
    val from: String,
    val chipSize: androidx.compose.ui.geometry.Size,
    var pos: Offset, // позиция пальца в root-координатах
)

private class LineSlots(
    val defenders: MutableList<Long?> = androidx.compose.runtime.mutableStateListOf(null, null),
    val forwards: MutableList<Long?> = androidx.compose.runtime.mutableStateListOf(null, null, null),
)

/** Цвета маек по игрокам: всем игрокам звена — цвет этого звена. */
private fun buildSweaterMap(
    lines: List<LineSlots>,
    lineSweaters: Map<Int, SweaterColor?>,
): Map<Long, SweaterColor?> {
    val result = mutableMapOf<Long, SweaterColor?>()
    lines.forEachIndexed { index, line ->
        val color = lineSweaters[index]
        (line.defenders + line.forwards).filterNotNull().forEach { result[it] = color }
    }
    return result
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineupDialog(
    event: EventDetail,
    saving: Boolean,
    error: String?,
    onSave: (List<LineupSlot>, Map<Long, SweaterColor?>) -> Unit,
    onDismiss: () -> Unit,
) {
    // В заявку включаем только основной состав, тренеры остаются отдельно.
    val yesVoters = event.voters.filter {
        it.answer == VoteAnswer.YES && !it.inReserve && !it.isTrainer
    }
    val names = event.voters.associate { it.userId to shortPlayerName(it.fullName) }
    val positions = event.voters.associate { it.userId to it.positions }

    // Исходное состояние из текущей заявки; резерв и тренеров исключаем.
    var goalkeeper by remember {
        mutableStateOf(
            event.lineup?.firstOrNull { it.role == LineupRole.GOALKEEPER }?.userId
                ?.takeIf { id -> yesVoters.any { it.userId == id } },
        )
    }
    val lines = remember {
        androidx.compose.runtime.mutableStateListOf<LineSlots>().apply {
            val existing = event.lineup.orEmpty()
                .filter { it.role != LineupRole.GOALKEEPER }
                .filter { slot -> yesVoters.any { it.userId == slot.userId } }
                .groupBy { it.lineNumber ?: 0 }
                .toSortedMap()
            if (existing.isEmpty()) {
                add(LineSlots())
            } else {
                existing.forEach { (_, slots) ->
                    val line = LineSlots()
                    slots.filter { it.role == LineupRole.DEFENDER }
                        .sortedBy { it.slotNumber }
                        .forEachIndexed { i, s -> if (i < 2) line.defenders[i] = s.userId }
                    slots.filter { it.role == LineupRole.FORWARD }
                        .sortedBy { it.slotNumber }
                        .forEachIndexed { i, s -> if (i < 3) line.forwards[i] = s.userId }
                    add(line)
                }
            }
        }
    }

    // В регулярке цветов нет; в двухсторонке доступны только чёрные и белые.
    val showSweaters = event.section == EventSection.SCRIMMAGE
    val voterColors = event.voters.associate { it.userId to it.sweaterColor.supportedColor() }
    val lineSweaters = remember {
        mutableStateMapOf<Int, SweaterColor?>().apply {
            lines.forEachIndexed { index, line ->
                val ids = (line.defenders + line.forwards).filterNotNull()
                this[index] = ids.firstNotNullOfOrNull { voterColors[it] }
            }
        }
    }

    fun getSlot(slotId: String): Long? = when {
        slotId == "gk" -> goalkeeper
        slotId.startsWith("d") -> {
            val (l, i) = slotId.drop(1).split("_").map { it.toInt() }
            lines.getOrNull(l)?.defenders?.getOrNull(i)
        }
        slotId.startsWith("f") -> {
            val (l, i) = slotId.drop(1).split("_").map { it.toInt() }
            lines.getOrNull(l)?.forwards?.getOrNull(i)
        }
        else -> null
    }

    fun setSlot(slotId: String, userId: Long?) {
        when {
            slotId == "gk" -> goalkeeper = userId
            slotId.startsWith("d") -> {
                val (l, i) = slotId.drop(1).split("_").map { it.toInt() }
                lines.getOrNull(l)?.defenders?.set(i, userId)
            }
            slotId.startsWith("f") -> {
                val (l, i) = slotId.drop(1).split("_").map { it.toInt() }
                lines.getOrNull(l)?.forwards?.set(i, userId)
            }
        }
    }

    // Перетаскивание
    var drag by remember { mutableStateOf<LineupDrag?>(null) }
    val slotBounds = remember { mutableStateMapOf<String, Rect>() }
    var poolBounds by remember { mutableStateOf<Rect?>(null) }
    var containerTopLeft by remember { mutableStateOf(Offset.Zero) }

    fun removeUserFromSlots(userId: Long) {
        if (goalkeeper == userId) goalkeeper = null
        lines.forEach { line ->
            line.defenders.indices.forEach { index ->
                if (line.defenders[index] == userId) line.defenders[index] = null
            }
            line.forwards.indices.forEach { index ->
                if (line.forwards[index] == userId) line.forwards[index] = null
            }
        }
    }

    fun finishDrag(d: LineupDrag) {
        val target = slotBounds.entries.firstOrNull { it.value.contains(d.pos) }?.key
        when {
            target != null && slotAccepts(target, positions[d.userId].orEmpty()) -> {
                if (target == d.from) return
                val displaced = getSlot(target)

                // Сначала убираем игрока из всех слотов: дубли невозможны даже
                // при быстром повторном переносе до обновления списка свободных.
                removeUserFromSlots(d.userId)
                if (d.from != "pool" && displaced != null && displaced != d.userId &&
                    slotAccepts(d.from, positions[displaced].orEmpty())
                ) {
                    setSlot(d.from, displaced)
                }
                setSlot(target, d.userId)
            }
            poolBounds?.contains(d.pos) == true && d.from != "pool" -> {
                removeUserFromSlots(d.userId)
            }
        }
    }

    val assigned: Set<Long> = buildSet {
        goalkeeper?.let { add(it) }
        lines.forEach { l -> (l.defenders + l.forwards).filterNotNull().forEach { add(it) } }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!saving && drag == null) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Card(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(
                Modifier.onGloballyPositioned { containerTopLeft = it.boundsInRoot().topLeft },
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState(), enabled = drag == null)
                        .padding(16.dp),
                ) {
                    Text(
                        "Звенья",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Перетаскивайте игроков на позиции. Чтобы убрать — перетащите обратно в список.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))

                    if (yesVoters.isEmpty()) {
                        Text("Пока никто не ответил «Иду» — звенья можно будет составить после голосования")
                    } else {
                        // Свободные игроки
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { poolBounds = it.boundsInRoot() }
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(8.dp),
                        ) {
                            Column {
                                Text(
                                    "Свободные игроки",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                val free = yesVoters.filter { it.userId !in assigned }
                                if (free.isEmpty()) {
                                    Text(
                                        "Все распределены",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                }
                                groupedVoters(free).forEach { (group, members) ->
                                    Text(
                                        group,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 5.dp),
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        members.forEach { voter ->
                                            DragChip(
                                                userId = voter.userId,
                                                name = names[voter.userId]
                                                    ?: "user#${voter.userId}",
                                                slotId = "pool",
                                                drag = drag,
                                                slotBounds = slotBounds,
                                                onDragStart = { drag = it },
                                                onDragMove = { drag?.pos = it },
                                                onDragEnd = {
                                                    drag?.let { finishDrag(it) }
                                                    drag = null
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Вратарь
                        LineupSlotRow(
                            label = "Вратарь",
                            slotId = "gk",
                            occupant = goalkeeper,
                            names = names,
                            drag = drag,
                            slotBounds = slotBounds,
                            onDragStart = { drag = it },
                            onDragMove = { drag?.pos = it },
                            onDragEnd = {
                                drag?.let { finishDrag(it) }
                                drag = null
                            },
                        )

                        // Звенья
                        lines.forEachIndexed { lineIndex, line ->
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Звено ${lineIndex + 1}" +
                                        if (showSweaters) {
                                            lineSweaters[lineIndex]?.let { " (${it.display})" }.orEmpty()
                                        } else {
                                            ""
                                        },
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                if (lines.size > 1) {
                                    TextButton(onClick = {
                                        line.defenders.indices.forEach { line.defenders[it] = null }
                                        line.forwards.indices.forEach { line.forwards[it] = null }
                                        lines.removeAt(lineIndex)
                                    }) {
                                        Text(
                                            "Убрать",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                line.forwards.forEachIndexed { i, uid ->
                                    CompactLineupSlot(
                                        modifier = Modifier.weight(1f),
                                        label = "Н${i + 1}",
                                        slotId = "f${lineIndex}_$i",
                                        occupant = uid,
                                        names = names,
                                        drag = drag,
                                        slotBounds = slotBounds,
                                        onDragStart = { drag = it },
                                        onDragMove = { drag?.pos = it },
                                        onDragEnd = {
                                            drag?.let { finishDrag(it) }
                                            drag = null
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                line.defenders.forEachIndexed { i, uid ->
                                    CompactLineupSlot(
                                        modifier = Modifier.weight(1f),
                                        label = "З${i + 1}",
                                        slotId = "d${lineIndex}_$i",
                                        occupant = uid,
                                        names = names,
                                        drag = drag,
                                        slotBounds = slotBounds,
                                        onDragStart = { drag = it },
                                        onDragMove = { drag?.pos = it },
                                        onDragEnd = {
                                            drag?.let { finishDrag(it) }
                                            drag = null
                                        },
                                    )
                                }
                            }
                            if (showSweaters) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    Text(
                                        "Майки",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(92.dp),
                                    )
                                    listOf(SweaterColor.BLACK, SweaterColor.WHITE).forEach { color ->
                                        FilterChip(
                                            selected = lineSweaters[lineIndex] == color,
                                            onClick = {
                                                lineSweaters[lineIndex] =
                                                    if (lineSweaters[lineIndex] == color) null
                                                    else color
                                            },
                                            label = { SweaterDot(color) },
                                        )
                                        Spacer(Modifier.size(4.dp))
                                    }
                                }
                            }
                        }

                        if (lines.size < 4) {
                            TextButton(onClick = { lines.add(LineSlots()) }) {
                                Text("+ Добавить звено")
                            }
                        }
                    }

                    ErrorText(error)

                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss, enabled = !saving) { Text("Отмена") }
                        Spacer(Modifier.size(8.dp))
                        TextButton(
                            enabled = !saving && yesVoters.isNotEmpty(),
                            onClick = {
                                val slots = mutableListOf<LineupSlot>()
                                goalkeeper?.let {
                                    slots.add(
                                        LineupSlot(
                                            lineNumber = null, slotNumber = 1,
                                            role = LineupRole.GOALKEEPER, userId = it,
                                        ),
                                    )
                                }
                                lines.forEachIndexed { lineIndex, line ->
                                    val lineNumber = lineIndex + 1
                                    line.defenders.forEachIndexed { i, uid ->
                                        uid?.let {
                                            slots.add(
                                                LineupSlot(
                                                    lineNumber = lineNumber, slotNumber = i + 1,
                                                    role = LineupRole.DEFENDER, userId = it,
                                                ),
                                            )
                                        }
                                    }
                                    line.forwards.forEachIndexed { i, uid ->
                                        uid?.let {
                                            slots.add(
                                                LineupSlot(
                                                    lineNumber = lineNumber, slotNumber = i + 1,
                                                    role = LineupRole.FORWARD, userId = it,
                                                ),
                                            )
                                        }
                                    }
                                }
                                onSave(
                                    slots,
                                    if (showSweaters) buildSweaterMap(lines, lineSweaters) else emptyMap(),
                                )
                            },
                        ) { Text(if (saving) "Сохранение…" else "Сохранить") }
                    }
                }

                // «Призрак» перетаскиваемого игрока поверх контента
                drag?.let { d ->
                    Box(
                        Modifier
                            .offset {
                                IntOffset(
                                    (d.pos.x - containerTopLeft.x - d.chipSize.width / 2)
                                        .roundToInt(),
                                    (d.pos.y - containerTopLeft.y - d.chipSize.height / 2)
                                        .roundToInt(),
                                )
                            }
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            names[d.userId] ?: "user#${d.userId}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

/** Компактный слот схемы: три нападающих сверху, два защитника ниже. */
@Composable
private fun CompactLineupSlot(
    modifier: Modifier,
    label: String,
    slotId: String,
    occupant: Long?,
    names: Map<Long, String>,
    drag: LineupDrag?,
    slotBounds: MutableMap<String, Rect>,
    onDragStart: (LineupDrag) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    Column(
        modifier
            .padding(vertical = 3.dp)
            .onGloballyPositioned { slotBounds[slotId] = it.boundsInRoot() }
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 3.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (occupant != null) {
            DragChip(
                userId = occupant,
                name = names[occupant] ?: "user#$occupant",
                slotId = slotId,
                drag = drag,
                slotBounds = slotBounds,
                onDragStart = onDragStart,
                onDragMove = onDragMove,
                onDragEnd = onDragEnd,
            )
        } else {
            Text("—", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Строка слота заявки: подпись и место для игрока (drop-зона). */
@Composable
private fun LineupSlotRow(
    label: String,
    slotId: String,
    occupant: Long?,
    names: Map<Long, String>,
    drag: LineupDrag?,
    slotBounds: MutableMap<String, Rect>,
    onDragStart: (LineupDrag) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .onGloballyPositioned { slotBounds[slotId] = it.boundsInRoot() }
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(92.dp),
        )
        if (occupant != null) {
            DragChip(
                userId = occupant,
                name = names[occupant] ?: "user#$occupant",
                slotId = slotId,
                drag = drag,
                slotBounds = slotBounds,
                onDragStart = onDragStart,
                onDragMove = onDragMove,
                onDragEnd = onDragEnd,
            )
        } else {
            Text(
                "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Горизонтальный свайп между цветовыми столбцами без перехвата вертикальной прокрутки. */
@Composable
private fun SweaterChip(
    userId: Long,
    name: String,
    slotId: String,
    drag: LineupDrag?,
    onDragStart: (LineupDrag) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    var chipRect by remember { mutableStateOf(Rect.Zero) }
    Box(
        Modifier
            .padding(vertical = 2.dp)
            .onGloballyPositioned { chipRect = it.boundsInRoot() }
            .pointerInput(userId, slotId) {
                detectHorizontalDragGestures(
                    onDragStart = { start ->
                        onDragStart(
                            LineupDrag(
                                userId = userId,
                                from = slotId,
                                chipSize = chipRect.size,
                                pos = chipRect.topLeft + start,
                            ),
                        )
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onDragMove(chipRect.topLeft + change.position)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                )
            }
            .alpha(if (drag?.userId == userId) 0.3f else 1f)
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

/** Перетаскиваемый чип игрока между слотами заявки. */
@Composable
private fun DragChip(
    userId: Long,
    name: String,
    slotId: String,
    drag: LineupDrag?,
    slotBounds: MutableMap<String, Rect>,
    onDragStart: (LineupDrag) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    var chipRect by remember { mutableStateOf(Rect.Zero) }
    val beingDragged = drag?.userId == userId
    Box(
        Modifier
            .padding(vertical = 2.dp)
            .onGloballyPositioned { chipRect = it.boundsInRoot() }
            .pointerInput(userId, slotId) {
                detectDragGestures(
                    onDragStart = { start ->
                        onDragStart(
                            LineupDrag(
                                userId = userId,
                                from = slotId,
                                chipSize = chipRect.size,
                                pos = chipRect.topLeft + start,
                            ),
                        )
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onDragMove(chipRect.topLeft + change.position)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                )
            }
            .alpha(if (beingDragged) 0.3f else 1f)
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}
