package info.prorabka.abik.android.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Button
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
import info.prorabka.abik.android.core.ui.components.AbikPullToRefresh
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
    val showPollDetails = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    info.prorabka.abik.android.core.ui.RealtimeRefresh {
        viewModel.refresh()
        shellViewModel.refresh()
        shellViewModel.refreshUnreadCount()
    }

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
        // Верхний и нижний системные отступы уже применены в MainShell.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
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
        AbikPullToRefresh(
            loading = feed.loading || ui.loading,
            onRefresh = {
                viewModel.refresh()
                shellViewModel.refresh()
            },
            modifier = Modifier.padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                feed.subscriptionPoll?.let { poll ->
                    SubscriptionPollCard(
                        poll = poll,
                        voting = feed.pollVoting,
                        error = feed.pollError,
                        onVote = viewModel::voteSubscriptionPoll,
                        onDetails = { showPollDetails.value = true },
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BalanceCard(
                        title = "Мой баланс",
                        amount = ui.myBalanceKopecks,
                        modifier = Modifier.weight(1f),
                    )
                    if (ui.isOwnerOrAdmin) {
                        BalanceCard(
                            title = "Баланс команды",
                            amount = ui.teamBalanceKopecks,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (feed.activeSubscriptions.isNotEmpty()) {
                    ActiveSubscriptionsCard(feed.activeSubscriptions)
                }

                NearestEventCard(
                    event = feed.nearestEvent,
                    onClick = onEventClick,
                )

                feed.advertisement?.let { advertisement ->
                    AdvertisementCard(
                        advertisement = advertisement,
                        imageUrl = viewModel.resolveUrl(advertisement.imageUrl),
                        onClick = viewModel::trackAdClick,
                    )
                } ?: NotificationsCard(
                    notifications = feed.notifications,
                    onShowAll = onNotifications,
                )

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    if (showPollDetails.value && feed.subscriptionPoll != null) {
        SubscriptionPollDetailsDialog(
            poll = feed.subscriptionPoll!!,
            voting = feed.pollVoting,
            onVote = viewModel::voteSubscriptionPoll,
            onClose = { showPollDetails.value = false },
        )
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

/** Абонементы по итогам опросов во всех командах пользователя. */
@Composable
private fun ActiveSubscriptionsCard(
    subscriptions: List<info.prorabka.abik.android.domain.model.ActiveSubscription>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Действующие абонементы", style = MaterialTheme.typography.titleSmall)
            subscriptions.forEachIndexed { index, subscription ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = if (index == 0) 8.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            subscription.teamName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (subscription.title.isNotBlank() && subscription.title != "Абонемент") {
                            Text(
                                subscription.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        subscriptionMonth(subscription.validFrom),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
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
                        // Игра с лимитом: основной состав и резерв отдельно (как в деталях).
                        // «Под вопросом» в карточках не показываем — только в деталях события.
                        text = event.votesYesReserve?.let { reserve ->
                            "Идут: ${event.votesYes - reserve} • Резерв: $reserve • Не идут: ${event.votesNo}"
                        } ?: "Идут: ${event.votesYes} • Не идут: ${event.votesNo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    event.myVote?.let { vote ->
                        Text(
                            text = "Мой ответ: ${vote.display}" +
                                if (event.myInReserve == true) " (в резерве)" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/** Реклама заменяет блок уведомлений; при её отсутствии лента возвращается. */
@Composable
private fun AdvertisementCard(
    advertisement: info.prorabka.abik.android.domain.model.Advertisement,
    imageUrl: String?,
    onClick: () -> Unit,
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Реклама", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            imageUrl?.let {
                coil.compose.AsyncImage(
                    model = it, contentDescription = advertisement.title,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
            advertisement.title?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            advertisement.body?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (!advertisement.linkUrl.isNullOrBlank() && !advertisement.buttonText.isNullOrBlank()) {
                Button(onClick = { onClick(); uriHandler.openUri(advertisement.linkUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text(advertisement.buttonText)
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


@Composable
private fun SubscriptionPollCard(
    poll: info.prorabka.abik.android.domain.model.SubscriptionPoll,
    voting: Boolean,
    error: String?,
    onVote: (info.prorabka.abik.android.domain.model.SubscriptionPollAnswer) -> Unit,
    onDetails: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onDetails)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Абонемент (опрос) • ${subscriptionMonth(poll.validFrom)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            Text(poll.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Голосование: ${pollDate(poll.startDate)}–${pollDate(poll.endDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PollVoteButtons(poll, voting, onVote)
            if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = Danger)
            Text("Подробнее и список проголосовавших →", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PollVoteButtons(
    poll: info.prorabka.abik.android.domain.model.SubscriptionPoll,
    voting: Boolean,
    onVote: (info.prorabka.abik.android.domain.model.SubscriptionPollAnswer) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PollVoteButton(
            text = "+  ${poll.yesCount}",
            selected = poll.myVote == info.prorabka.abik.android.domain.model.SubscriptionPollAnswer.YES,
            enabled = !voting,
            modifier = Modifier.weight(1f),
            onClick = { onVote(info.prorabka.abik.android.domain.model.SubscriptionPollAnswer.YES) },
        )
        PollVoteButton(
            text = "−  ${poll.noCount}",
            selected = poll.myVote == info.prorabka.abik.android.domain.model.SubscriptionPollAnswer.NO,
            enabled = !voting,
            modifier = Modifier.weight(1f),
            onClick = { onVote(info.prorabka.abik.android.domain.model.SubscriptionPollAnswer.NO) },
        )
    }
}

@Composable
private fun PollVoteButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick, enabled = enabled, modifier = modifier) { Text(text) }
    } else {
        androidx.compose.material3.OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) {
            Text(text)
        }
    }
}

@Composable
private fun SubscriptionPollDetailsDialog(
    poll: info.prorabka.abik.android.domain.model.SubscriptionPoll,
    voting: Boolean,
    onVote: (info.prorabka.abik.android.domain.model.SubscriptionPollAnswer) -> Unit,
    onClose: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text(poll.title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Абонемент на ${pollDate(poll.validFrom)}–${pollDate(poll.validUntil)}",
                    style = MaterialTheme.typography.bodyMedium)
                PollVoteButtons(poll, voting, onVote)
                Text("Проголосовали «+»: ${poll.yesCount}", style = MaterialTheme.typography.titleSmall)
                if (poll.positiveVoters.isEmpty()) {
                    Text("Пока никто не проголосовал положительно",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    poll.positiveVoters.forEach { voter ->
                        Text(voter.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onClose) { Text("Закрыть") }
        },
    )
}

private fun pollDate(value: String): String = runCatching {
    java.time.LocalDate.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}.getOrDefault(value)

private fun subscriptionMonth(value: String): String = runCatching {
    java.time.YearMonth.from(java.time.LocalDate.parse(value))
        .format(java.time.format.DateTimeFormatter.ofPattern(
            "LLLL yyyy", java.util.Locale.forLanguageTag("ru")))
        .replaceFirstChar { it.titlecase() }
}.getOrDefault(value)
