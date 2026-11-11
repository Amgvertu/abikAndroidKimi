package info.prorabka.abik.android.presentation.events

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import info.prorabka.abik.android.core.ui.components.AbikPullToRefresh
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.ui.components.NoTeamPlaceholder
import info.prorabka.abik.android.domain.model.EventListItem
import info.prorabka.abik.android.domain.model.EventSection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

private val MONTH_NAMES = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)

private val MONTH_NAMES_GEN = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

private val WEEKDAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

private val TRAINING_SECONDARY = Color(0xFF1E88E5)
private val TRAINING_DOMINANT = Color(0xFF43A047)
private val REGULAR_COLOR = Color(0xFFFB8C00)
private val SCRIMMAGE_COLOR = Color(0xFFFDD835)
private val FINISHED_EVENT_COLOR = Color(0xFF9E9E9E)

/** Яркие цвета остаются у будущих событий, завершённые обводятся серым. */
private fun calendarMarkerColor(
    event: EventListItem,
    dominantTrainingTimes: Set<String>,
    now: LocalDateTime = LocalDateTime.now(),
): Color {
    val startsAt = runCatching {
        LocalDateTime.of(LocalDate.parse(event.dateIso), LocalTime.parse(event.time))
    }.getOrNull()
    return if (startsAt != null && !startsAt.isAfter(now)) {
        FINISHED_EVENT_COLOR
    } else {
        markerColor(event, dominantTrainingTimes)
    }
}

/** Единые цвета событий в календаре и карточках. */
fun markerColor(event: EventListItem, dominantTrainingTimes: Set<String> = emptySet()): Color =
    when (event.section) {
        EventSection.REGULAR -> REGULAR_COLOR
        EventSection.SCRIMMAGE -> SCRIMMAGE_COLOR
        EventSection.TRAINING -> if (event.time in dominantTrainingTimes) {
            TRAINING_DOMINANT
        } else {
            TRAINING_SECONDARY
        }
    }

fun formatDayMonth(date: LocalDate): String =
    "${date.dayOfMonth} ${MONTH_NAMES_GEN[date.monthValue - 1]}"

fun formatDateTime(dateIso: String, time: String): String {
    val date = runCatching { LocalDate.parse(dateIso) }.getOrNull() ?: return "$dateIso $time"
    return "${formatDayMonth(date)} • $time"
}

/** Вкладка «Игры»: календарь и события команды. */
@Composable
fun EventsScreen(
    onEventClick: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onCreateTeam: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    info.prorabka.abik.android.core.ui.RealtimeRefresh { viewModel.refresh() }
    var eventChoices by remember { mutableStateOf<List<EventListItem>?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    fun openAttendance(events: List<EventListItem>) {
        if (events.size == 1) onEventClick(events.first().id) else eventChoices = events
    }

    when {
        ui.noTeam -> NoTeamPlaceholder(onCreateTeam = onCreateTeam)
        ui.loading && ui.events.isEmpty() -> LoadingBox()
        else -> Box(Modifier.fillMaxSize()) {
            AbikPullToRefresh(
                loading = ui.loading || ui.refreshing,
                onRefresh = viewModel::refresh,
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                // Фильтры разделов (прокручиваются, если шрифт крупный);
                // тап включает/выключает раздел на календаре
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    EventSection.entries.forEach { section ->
                        FilterChip(
                            selected = section in ui.sections,
                            onClick = { viewModel.toggleSection(section) },
                            label = { Text(section.display, maxLines = 1) },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                MonthCalendar(
                    month = ui.month,
                    selectedDate = ui.selectedDate,
                    dayMarkers = ui.dayMarkerEvents.mapValues { (_, events) ->
                        events.map { calendarMarkerColor(it, ui.dominantTrainingTimes) }
                            .distinct().take(4)
                    },
                    eventsByDate = ui.dayMarkerEvents,
                    // Все активные участники команды могут открыть голосование
                    // долгим тапом по дате, независимо от роли.
                    allowAttend = ui.myRole != null,
                    onAttend = ::openAttendance,
                    allowCreate = ui.isManager,
                    onCreate = { date -> onCreate(date.toString()) },
                    onSelectDate = viewModel::selectDate,
                    onPrev = viewModel::prevMonth,
                    onNext = viewModel::nextMonth,
                )

                ErrorText(ui.error)

                if (ui.selectedDate == null && ui.upcoming.isNotEmpty()) {
                    SectionHeader("Ближайшие события")
                    ui.upcoming.forEach { event ->
                        EventCard(
                            event = event,
                            accentColor = markerColor(event, ui.dominantTrainingTimes),
                            onClick = { onEventClick(event.id) },
                        )
                    }
                }

                ui.selectedDate?.let { date ->
                    SectionHeader("События ${formatDayMonth(date)}")
                    if (ui.selectedDayEvents.isEmpty()) {
                        Text(
                            "На эту дату событий нет",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        ui.selectedDayEvents.forEach { event ->
                            EventCard(
                                event = event,
                                accentColor = markerColor(event, ui.dominantTrainingTimes),
                                onClick = { onEventClick(event.id) },
                            )
                        }
                    }
                }

                    Spacer(Modifier.height(80.dp))
                }
            }

            if (ui.isManager) {
                FloatingActionButton(
                    onClick = { onCreate((ui.selectedDate ?: LocalDate.now()).toString()) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать событие")
                }
            }
        }
    }

    eventChoices?.let { choices ->
        AlertDialog(
            onDismissRequest = { eventChoices = null },
            title = { Text("Отметиться") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Выберите событие", style = MaterialTheme.typography.bodyMedium)
                    choices.sortedBy { it.time }.forEach { event ->
                        TextButton(
                            onClick = {
                                eventChoices = null
                                onEventClick(event.id)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "${event.time} • ${event.title}",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { eventChoices = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

/** Карточка события: название, дата, время, место, тип, мой ответ и счётчики. */
@Composable
fun EventCard(event: EventListItem, accentColor: Color? = null, onClick: () -> Unit) {
    val badgeColor = accentColor ?: MaterialTheme.colorScheme.primary
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    event.type.display,
                    fontSize = 11.sp,
                    color = badgeColor,
                    modifier = Modifier
                        .background(
                            badgeColor.copy(alpha = 0.12f),
                            CircleShape,
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                formatDateTime(event.dateIso, event.time),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                event.place,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    // Игра с лимитом: основной состав и резерв отдельно (как в деталях).
                    // «Под вопросом» в карточках не показываем — только в деталях события.
                    event.votesYesReserve?.let { reserve ->
                        "Идут: ${event.votesYes - reserve} • Резерв: $reserve • Не идут: ${event.votesNo}"
                    } ?: "Идут: ${event.votesYes} • Не идут: ${event.votesNo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                event.myVote?.let { vote ->
                    Text(
                        vote.display + if (event.myInReserve == true) " (в резерве)" else "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                CircleShape,
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            // Счётчики проголосовавших «Иду» по амплуа (приходят с сервера).
            Text(
                "В: ${event.votesYesGoalies}  З: ${event.votesYesDefenders}  " +
                    "Н: ${event.votesYesForwards}  Т: ${event.votesYesTrainers}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate?,
    dayMarkers: Map<LocalDate, List<Color>>,
    eventsByDate: Map<LocalDate, List<EventListItem>>,
    allowAttend: Boolean,
    onAttend: (List<EventListItem>) -> Unit,
    allowCreate: Boolean,
    onCreate: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Предыдущий месяц")
                }
                Text(
                    "${MONTH_NAMES[month.monthValue - 1]} ${month.year}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Следующий месяц")
                }
            }

            Row {
                WEEKDAYS.forEach { wd ->
                    Text(
                        wd,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val offset = month.atDay(1).dayOfWeek.value - 1
            val daysInMonth = month.lengthOfMonth()
            val weeks = (offset + daysInMonth + 6) / 7
            for (w in 0 until weeks) {
                Row {
                    for (d in 0 until 7) {
                        val dayNum = w * 7 + d - offset + 1
                        if (dayNum < 1 || dayNum > daysInMonth) {
                            Box(Modifier.weight(1f))
                        } else {
                            val date = month.atDay(dayNum)
                            DayCell(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == LocalDate.now(),
                                markerColors = dayMarkers[date].orEmpty(),
                                dayEvents = eventsByDate[date].orEmpty(),
                                allowAttend = allowAttend,
                                onAttend = onAttend,
                                allowCreate = allowCreate,
                                onCreate = onCreate,
                                onClick = { onSelectDate(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    markerColors: List<Color>,
    dayEvents: List<EventListItem>,
    allowAttend: Boolean,
    onAttend: (List<EventListItem>) -> Unit,
    allowCreate: Boolean,
    onCreate: (LocalDate) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayColor = MaterialTheme.colorScheme.primary
    var menuExpanded by remember(date) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .padding(2.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (allowCreate || (allowAttend && dayEvents.isNotEmpty())) {
                    { menuExpanded = true }
                } else {
                    null
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .then(
                    when {
                        isSelected -> Modifier.background(
                            MaterialTheme.colorScheme.primary, CircleShape,
                        )
                        // Текущая дата выделяется подчёркиванием
                        isToday -> Modifier.drawBehind {
                            val stroke = 2.dp.toPx()
                            val y = size.height - stroke / 2 - 1.dp.toPx()
                            drawLine(
                                color = todayColor,
                                start = Offset(size.width * 0.2f, y),
                                end = Offset(size.width * 0.8f, y),
                                strokeWidth = stroke,
                            )
                        }
                        else -> Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                fontSize = 13.sp,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            // Дата с событиями обводится кружком; каждый цвет — пара (время + тип события)
            if (markerColors.isNotEmpty()) {
                Canvas(Modifier.size(30.dp)) {
                    val stroke = Stroke(width = 2.dp.toPx())
                    val sweep = 360f / markerColors.size
                    val gap = if (markerColors.size > 1) 6f else 0f
                    markerColors.forEachIndexed { index, color ->
                        drawArc(
                            color = color,
                            startAngle = index * sweep + gap / 2,
                            sweepAngle = sweep - gap,
                            useCenter = false,
                            style = stroke,
                        )
                    }
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            if (allowAttend && dayEvents.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("Отметиться") },
                    onClick = {
                        menuExpanded = false
                        onAttend(dayEvents)
                    },
                )
            }
            if (allowCreate) {
                DropdownMenuItem(
                    text = { Text("Создать событие") },
                    onClick = {
                        menuExpanded = false
                        onCreate(date)
                    },
                )
            }
        }
    }
}
