package info.prorabka.abik.android.presentation.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.domain.model.EventDetail
import info.prorabka.abik.android.domain.model.EventVoter
import info.prorabka.abik.android.domain.model.LineupRole
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

    LaunchedEffect(ui.deleted) {
        if (ui.deleted) onBack()
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
                    onVote = viewModel::vote,
                )
            }
        }
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
    onVote: (VoteAnswer) -> Unit,
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
                Spacer(Modifier.height(12.dp))
                Text(
                    "${answer.display} (${group.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                VoterGroup(group)
            }
        }

        // Заявка на игру (просмотр)
        event.lineup?.takeIf { it.isNotEmpty() }?.let { slots ->
            Spacer(Modifier.height(16.dp))
            Text(
                "Заявка на игру",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            LineupView(event = event)
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
