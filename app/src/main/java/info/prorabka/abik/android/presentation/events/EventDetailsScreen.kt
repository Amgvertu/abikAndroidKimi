package info.prorabka.abik.android.presentation.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.domain.model.EventDetail
import info.prorabka.abik.android.domain.model.EventVoter
import info.prorabka.abik.android.domain.model.LineupRole
import info.prorabka.abik.android.domain.model.LineupSlot
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.SweaterColor
import info.prorabka.abik.android.domain.model.VoteAnswer

/** Детали события: информация, голосование, списки, заявка (просмотр). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: EventDetailsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
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
        Box(Modifier.padding(padding).fillMaxSize()) {
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

    if (showSweatersDialog && ui.event != null) {
        SweatersDialog(
            voters = ui.event!!.voters.filter { it.answer == VoteAnswer.YES },
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
            onSave = viewModel::setLineup,
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
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoteAnswer.entries.forEach { answer ->
                val count = event.voters.count { it.answer == answer }
                FilterChip(
                    selected = event.myVote == answer,
                    onClick = { if (!acting) onVote(answer) },
                    label = { Text("${answer.display} ($count)") },
                )
            }
        }

        if (canEditSweaters) {
            TextButton(onClick = onEditSweaters) { Text("Назначить свитеры") }
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
                VoterGroup(main)
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

        // Заявка на игру (просмотр)
        event.lineup?.takeIf { it.isNotEmpty() }?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                "Заявка на игру",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            LineupView(event = event)
        }
        if (canEditLineup) {
            TextButton(onClick = onEditLineup) {
                Text(
                    if (event.lineup.isNullOrEmpty()) "Создать заявку на игру"
                    else "Изменить заявку",
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** Список проголосовавших с группировкой по амплуа. */
@Composable
private fun VoterGroup(voters: List<EventVoter>) {
    val order = listOf(
        Position.GOALKEEPER to "Вратари",
        Position.DEFENDER to "Защитники",
        Position.FORWARD to "Нападающие",
    )
    Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            var first = true
            order.forEach { (position, label) ->
                val inPosition = voters.filter { it.positions.contains(position) }
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
            val withoutPosition = voters.filter { it.positions.isEmpty() }
            if (withoutPosition.isNotEmpty()) {
                if (!first) Spacer(Modifier.height(6.dp))
                Text(
                    "Без амплуа",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                withoutPosition.forEach { VoterRow(it) }
            }
        }
    }
}

@Composable
private fun VoterRow(voter: EventVoter) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        voter.sweaterColor?.let { color ->
            SweaterDot(color)
            Spacer(Modifier.size(6.dp))
        }
        Text(voter.fullName, style = MaterialTheme.typography.bodyMedium)
        if (voter.isTrainer) {
            Spacer(Modifier.size(6.dp))
            Text(
                "тренер",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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

/** Заявка на игру (просмотр): вратарь и звенья. */
@Composable
private fun LineupView(event: EventDetail) {
    val names = event.voters.associate { it.userId to it.fullName }
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
                Text(
                    "Звено $lineNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val defenders = lineSlots.filter { it.role == LineupRole.DEFENDER }
                    .sortedBy { it.slotNumber }
                val forwards = lineSlots.filter { it.role == LineupRole.FORWARD }
                    .sortedBy { it.slotNumber }
                if (defenders.isNotEmpty()) {
                    Text(
                        "З: " + defenders.joinToString(", ") {
                            names[it.userId] ?: "user#${it.userId}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (forwards.isNotEmpty()) {
                    Text(
                        "Н: " + forwards.joinToString(", ") {
                            names[it.userId] ?: "user#${it.userId}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}


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
        androidx.compose.runtime.mutableStateMapOf<Long, SweaterColor?>().apply {
            voters.forEach { put(it.userId, it.sweaterColor) }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Свитеры") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (voters.isEmpty()) {
                    Text("Пока никто не ответил «Иду»")
                }
                voters.forEach { voter ->
                    Text(
                        voter.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SweaterColor.entries.forEach { color ->
                            FilterChip(
                                selected = colors[voter.userId] == color,
                                onClick = {
                                    colors[voter.userId] =
                                        if (colors[voter.userId] == color) null else color
                                },
                                label = { SweaterDot(color) },
                            )
                        }
                    }
                }
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(colors.toMap()) },
                enabled = !saving && voters.isNotEmpty(),
            ) { Text(if (saving) "Сохранение…" else "Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Отмена") }
        },
    )
}

// ====== Редактор заявки на игру (игра/матч; владелец/админ) ======
// Игроки распределяются по слотам звеньев перетаскиванием (long-press + drag).

/** Идентификатор слота: "gk", "d{звено}_{слот}", "f{звено}_{слот}", "pool". */
private fun slotAccepts(slotId: String): Position? = when {
    slotId == "gk" -> Position.GOALKEEPER
    slotId.startsWith("d") -> Position.DEFENDER
    slotId.startsWith("f") -> Position.FORWARD
    else -> null // pool принимает всех
}

private class LineupDrag(
    val userId: Long,
    val from: String,
    val chipSize: androidx.compose.ui.geometry.Size,
    var pos: Offset, // позиция пальца в root-координатах
)

private class LineSlots(
    val defenders: MutableList<Long?> = mutableListOf(null, null),
    val forwards: MutableList<Long?> = mutableListOf(null, null, null),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineupDialog(
    event: EventDetail,
    saving: Boolean,
    error: String?,
    onSave: (List<LineupSlot>) -> Unit,
    onDismiss: () -> Unit,
) {
    // Кандидаты — проголосовавшие «Иду», кроме тренеров
    val yesVoters = event.voters.filter { it.answer == VoteAnswer.YES && !it.isTrainer }
    val names = event.voters.associate { it.userId to it.fullName }
    val positions = event.voters.associate { it.userId to it.positions }

    // Исходное состояние из текущей заявки
    var goalkeeper by remember {
        mutableStateOf(
            event.lineup?.firstOrNull { it.role == LineupRole.GOALKEEPER }?.userId,
        )
    }
    val lines = remember {
        androidx.compose.runtime.mutableStateListOf<LineSlots>().apply {
            val existing = event.lineup.orEmpty()
                .filter { it.role != LineupRole.GOALKEEPER }
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

    fun finishDrag(d: LineupDrag) {
        val userPositions = positions[d.userId].orEmpty()
        val target = slotBounds.entries
            .firstOrNull { it.value.contains(d.pos) }?.key
        when {
            target != null && slotAccepts(target) in userPositions -> {
                val displaced = getSlot(target)
                setSlot(target, d.userId)
                if (d.from != "pool") {
                    // Обмен: вытесненный игрок встаёт в исходный слот, если подходит
                    if (displaced != null &&
                        slotAccepts(d.from) in positions[displaced].orEmpty()
                    ) {
                        setSlot(d.from, displaced)
                    } else {
                        setSlot(d.from, null)
                    }
                } else if (displaced != null) {
                    // Вытесненный возвращается в общий список (слот очищен выше)
                }
            }
            poolBounds?.contains(d.pos) == true && d.from != "pool" -> {
                setSlot(d.from, null)
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
                        "Заявка на игру",
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
                        Text("Пока никто не ответил «Иду» — заявку можно будет составить после голосования")
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
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    free.forEach { voter ->
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
                                    "Звено ${lineIndex + 1}",
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
                            line.defenders.forEachIndexed { i, uid ->
                                LineupSlotRow(
                                    label = "Защитник ${i + 1}",
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
                            line.forwards.forEachIndexed { i, uid ->
                                LineupSlotRow(
                                    label = "Нападающий ${i + 1}",
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
                                onSave(slots)
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

/** Перетаскиваемый чип игрока (long-press → drag). */
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
                detectDragGesturesAfterLongPress(
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
        )
    }
}
