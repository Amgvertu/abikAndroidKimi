package info.prorabka.abik.android.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import info.prorabka.abik.android.core.ui.theme.Danger
import info.prorabka.abik.android.core.ui.theme.Success
import info.prorabka.abik.android.core.ui.components.NoTeamPlaceholder
import info.prorabka.abik.android.core.util.isoDateTimeToDisplay
import info.prorabka.abik.android.core.util.kopecksToRubles
import info.prorabka.abik.android.domain.model.AppNotification
import info.prorabka.abik.android.domain.model.EventListItem
import info.prorabka.abik.android.presentation.events.formatDateTime
import info.prorabka.abik.android.presentation.shell.ShellViewModel

/**
 * Главная: мой баланс, баланс команды, ближайшее событие, уведомления.
 * Для управляющих — FAB «+» (внести платёж).
 */
@Composable
fun HomeScreen(
    shellViewModel: ShellViewModel,
    onAddPayment: () -> Unit,
    onCreateTeam: () -> Unit,
    onEventClick: (Long) -> Unit,
    onNotifications: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by shellViewModel.ui.collectAsState()
    val feed by viewModel.ui.collectAsState()

    // Обновляем ленту при возврате на экран (после голосования, платежа и т.п.)
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    if (ui.selected == null) {
        // Команда ещё не создана («Создать команду позже» на онбординге)
        NoTeamPlaceholder(onCreateTeam)
        return
    }

    Scaffold(
        floatingActionButton = {
            if (ui.isManager) {
                FloatingActionButton(
                    onClick = onAddPayment,
                    shape = CircleShape,
                ) {
                    Text("+", fontSize = 26.sp)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BalanceCard(
                    title = "Мой баланс",
                    amount = ui.myBalanceKopecks,
                    modifier = Modifier.weight(1f),
                )
                BalanceCard(
                    title = "Баланс команды",
                    amount = ui.teamBalanceKopecks,
                    modifier = Modifier.weight(1f),
                )
            }

            NearestEventCard(
                event = feed.nearestEvent,
                onClick = onEventClick,
            )

            NotificationsCard(
                notifications = feed.notifications,
                onShowAll = onNotifications,
            )

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun BalanceCard(title: String, amount: Long?, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amount?.kopecksToRubles() ?: "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    amount == null -> MaterialTheme.colorScheme.onSurface
                    amount < 0 -> Danger
                    else -> Success
                },
            )
        }
    }
}

/** Ближайшее событие команды; клик открывает детали события. */
@Composable
private fun NearestEventCard(event: EventListItem?, onClick: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Ближайшее событие", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (event == null) {
                Text(
                    text = "Нет предстоящих событий",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(event.id) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = event.type.display,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDateTime(event.dateIso, event.time),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (event.place.isNotBlank()) {
                        Text(
                            text = event.place,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Идут: ${event.votesYes} • Под вопросом: ${event.votesMaybe} • Не идут: ${event.votesNo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    event.myVote?.let { vote ->
                        Text(
                            text = "Мой ответ: ${vote.display}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/** Последние уведомления (до 3) + ссылка на полную ленту. */
@Composable
private fun NotificationsCard(
    notifications: List<AppNotification>,
    onShowAll: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Последние уведомления", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (notifications.isEmpty()) {
                Text(
                    text = "Уведомлений пока нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                notifications.forEachIndexed { index, notification ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Column {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (notification.body.isNotBlank()) {
                            Text(
                                text = notification.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = notification.sentAtIso.isoDateTimeToDisplay(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Все уведомления →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onShowAll() },
                )
            }
        }
    }
}
