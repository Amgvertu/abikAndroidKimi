package info.prorabka.abik.android.presentation.events

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.ui.components.NoTeamPlaceholder
import info.prorabka.abik.android.domain.model.EventListItem
import info.prorabka.abik.android.domain.model.EventSection
import java.time.LocalDate
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

/** Палитра цветных маркеров календаря: цвет = пара (время + тип события). */
private val MARKER_COLORS = listOf(
    Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFE53935),
    Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1),
)

fun markerColor(key: String): Color =
    MARKER_COLORS[kotlin.math.abs(key.hashCode()) % MARKER_COLORS.size]

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

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    when {
        ui.noTeam -> NoTeamPlaceholder(onCreateTeam = onCreateTeam)
        ui.loading && ui.events.isEmpty() -> LoadingBox()
        else -> Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // Разделы
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventSection.entries.forEach { section ->
                        FilterChip(
                            selected = ui.section == section,
                            onClick = { viewModel.selectSection(section) },
                            label = { Text(section.display) },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                MonthCalendar(
                    month = ui.month,
                    selectedDate = ui.selectedDate,
                    dayMarkers = ui.dayMarkerKeys.mapValues { (_, keys) ->
                        keys.map(::markerColor).distinct().take(4)
                    },
                    onSelectDate = viewModel::selectDate,
                    onPrev = viewModel::prevMonth,
                    onNext = viewModel::nextMonth,
                )

                ErrorText(ui.error)

                if (ui.upcoming.isNotEmpty()) {
                    SectionHeader("Ближайшие события")
                    ui.upcoming.forEach { event ->
                        EventCard(event = event, onClick = { onEventClick(event.id) })
                    }
                }

                SectionHeader("События ${formatDayMonth(ui.selectedDate)}")
                if (ui.selectedDayEvents.isEmpty()) {
                    Text(
                        "На эту дату событий нет",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    ui.selectedDayEvents.forEach { event ->
                        EventCard(event = event, onClick = { onEventClick(event.id) })
                    }
                }

                Spacer(Modifier.height(80.dp))
            }

            if (ui.isManager) {
                FloatingActionButton(
                    onClick = { onCreate(ui.selectedDate.toString()) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать событие")
                }
            }
        }
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
fun EventCard(event: EventListItem, onClick: () -> Unit) {
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
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
                    "Идут: ${event.votesYes}" +
                        (event.votesYesReserve?.takeIf { it > 0 }?.let { " (+$it в резерве)" } ?: "") +
                        " • Под вопросом: ${event.votesMaybe} • Не идут: ${event.votesNo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                event.myVote?.let { vote ->
                    Text(
                        vote.display,
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
                "В: ${event.votesYesGoalies}  З: ${event.votesYesDefenders}  Н: ${event.votesYesForwards}",
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
    selectedDate: LocalDate,
    dayMarkers: Map<LocalDate, List<Color>>,
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

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    markerColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(2.dp)
            .clickable(onClick = onClick),
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
                        isToday -> Modifier.border(
                            1.dp, MaterialTheme.colorScheme.primary, CircleShape,
                        )
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
    }
}
