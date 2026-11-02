package info.prorabka.abik.android.presentation.team

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.ui.components.NoTeamPlaceholder
import info.prorabka.abik.android.domain.model.Invitation
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.RosterItem
import info.prorabka.abik.android.domain.model.TeamMember

/**
 * Экран «Команда»: две вкладки — «Вся команда» и «Регулярка» (состав сезона).
 * Сверху счётчики по амплуа (В/З/Н/Т), ниже список с сортировкой по амплуа.
 * Долгий тап на вкладке «Регулярка» — режим редактирования состава (управляющие).
 * Действия управляющих собраны в меню ⋮ сверху справа,
 * долгий тап на игрока — контекстное меню.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamScreen(
    onMemberClick: (Long) -> Unit,
    onAddPayment: (Long) -> Unit,
    onCreateTeam: () -> Unit,
    viewModel: TeamViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var teamMenuExpanded by remember { mutableStateOf(false) }
    var jerseyDialogMember by remember { mutableStateOf<TeamMember?>(null) }
    var excludeDialogMember by remember { mutableStateOf<TeamMember?>(null) }
    var cancelDialogInvitation by remember { mutableStateOf<Invitation?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var rosterEditMode by remember { mutableStateOf(false) }
    var rosterRemoveTarget by remember { mutableStateOf<RosterItem?>(null) }

    // Выбор логотипа команды (из диалога редактирования, управляющие)
    val pickLogo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let(viewModel::changeLogo) }

    if (ui.loading && ui.detail == null && !ui.noTeam) {
        LoadingBox()
        return
    }
    if (ui.noTeam) {
        NoTeamPlaceholder(onCreateTeam)
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // --- Действия управляющих: всё в меню ⋮ ---
        if (ui.isManager) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Spacer(modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { teamMenuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Меню команды",
                        )
                    }
                    DropdownMenu(
                        expanded = teamMenuExpanded,
                        onDismissRequest = { teamMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Редактировать команду") },
                            onClick = {
                                teamMenuExpanded = false
                                showEditDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Пригласить игрока") },
                            onClick = {
                                teamMenuExpanded = false
                                showInviteDialog = true
                            },
                        )
                        if (ui.isOwner) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Удалить команду",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    teamMenuExpanded = false
                                    showDeleteDialog = true
                                },
                            )
                        }
                    }
                }
            }
        }

        ui.info?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        ui.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        // --- Вкладки: вся команда / состав регулярки ---
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
            ) {
                Text("Вся команда", modifier = Modifier.padding(vertical = 12.dp))
            }
            // Долгий тап по вкладке — режим редактирования состава (управляющие)
            Box(
                modifier = Modifier
                    .combinedClickable(
                        onClick = { selectedTab = 1 },
                        onLongClick = {
                            if (ui.isManager) {
                                selectedTab = 1
                                rosterEditMode = !rosterEditMode
                            }
                        },
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Регулярка",
                    color = if (selectedTab == 1) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        if (selectedTab == 0) {
            // ===== Вся команда =====
            val trainers = ui.members.count { it.isTrainer }
            PositionCounters(
                goalies = ui.members.count {
                    !it.isTrainer && it.positions.contains(Position.GOALKEEPER)
                },
                defenders = ui.members.count {
                    !it.isTrainer && it.positions.contains(Position.DEFENDER)
                },
                forwards = ui.members.count {
                    !it.isTrainer && it.positions.contains(Position.FORWARD)
                },
                trainers = trainers,
            )
            val sortedMembers = ui.members.sortedWith(
                compareBy({ positionOrder(it.positions) }, { it.displayName }),
            )

            LazyColumn {
                // Выход из команды — на видном месте сверху (удаление — в меню ⋮)
                if (!ui.isOwner) {
                    item(key = "team_actions") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(onClick = { showLeaveDialog = true }) {
                                Text(
                                    "Покинуть команду",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }

                // Исходящие приглашения (только управляющие, если есть)
                if (ui.isManager && ui.outgoingInvitations.isNotEmpty()) {
                    item(key = "outgoing_invitations") {
                        OutgoingInvitationsCard(
                            invitations = ui.outgoingInvitations,
                            onCancel = { cancelDialogInvitation = it },
                        )
                    }
                }

                items(sortedMembers, key = { it.userId }) { member ->
                    MemberRow(
                        member = member,
                        avatarUrl = viewModel.resolveUrl(member.avatarUrl),
                        isManager = ui.isManager,
                        isOwner = ui.isOwner,
                        onClick = { onMemberClick(member.userId) },
                        onAddPayment = { onAddPayment(member.userId) },
                        onSetJersey = { jerseyDialogMember = member },
                        onToggleModerator = { viewModel.toggleModerator(member) },
                        onToggleTrainer = { viewModel.toggleTrainer(member) },
                        onExclude = { excludeDialogMember = member },
                        onRestore = { viewModel.restore(member) },
                    )
                    HorizontalDivider()
                }
            }
        } else {
            // ===== Состав регулярки (текущий сезон) =====
            val season = ui.detail?.season
            if (season.isNullOrBlank()) {
                Text(
                    "Сезон команды не указан — состав регулярки недоступен",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                val trainersById = ui.members.filter { it.isTrainer }.map { it.userId }.toSet()
                PositionCounters(
                    goalies = ui.roster.count {
                        it.userId !in trainersById && it.positions.contains(Position.GOALKEEPER)
                    },
                    defenders = ui.roster.count {
                        it.userId !in trainersById && it.positions.contains(Position.DEFENDER)
                    },
                    forwards = ui.roster.count {
                        it.userId !in trainersById && it.positions.contains(Position.FORWARD)
                    },
                    trainers = ui.roster.count { it.userId in trainersById },
                )
                if (rosterEditMode) {
                    Text(
                        "Редактирование состава сезона $season. " +
                            "Долгий тап по вкладке — завершить.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                val sortedRoster = ui.roster.sortedWith(
                    compareBy({ positionOrder(it.positions) }, { it.fullName }),
                )
                LazyColumn {
                    items(sortedRoster, key = { "roster_${it.userId}" }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.fullName +
                                        if (item.userId in trainersById) " • Тренер" else "",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (item.positions.isNotEmpty()) {
                                    Text(
                                        item.positions.sortedBy { it.ordinal }
                                            .joinToString("/") { it.display },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (rosterEditMode) {
                                TextButton(onClick = { rosterRemoveTarget = item }) {
                                    Text(
                                        "Исключить",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    if (rosterEditMode && ui.rosterCandidates.isNotEmpty()) {
                        item(key = "roster_candidates_header") {
                            Text(
                                "Добавить в состав",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(ui.rosterCandidates, key = { "candidate_${it.userId}" }) { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    member.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    enabled = !ui.rosterSaving,
                                    onClick = { viewModel.addToRoster(member.userId) },
                                ) { Text("Добавить") }
                            }
                            HorizontalDivider()
                        }
                    }

                    if (sortedRoster.isEmpty() && !rosterEditMode) {
                        item(key = "roster_empty") {
                            Text(
                                "Состав регулярки пока пуст" +
                                    if (ui.isManager) {
                                        " — долгий тап по вкладке «Регулярка», чтобы заполнить"
                                    } else "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    // Подтверждение исключения из состава регулярки
    rosterRemoveTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { rosterRemoveTarget = null },
            title = { Text("Исключить из состава?") },
            text = { Text("${item.fullName}\nНачисления и платежи сохранятся.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        rosterRemoveTarget = null
                        viewModel.removeFromRoster(item.userId)
                    },
                ) { Text("Исключить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { rosterRemoveTarget = null }) { Text("Отмена") }
            },
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Покинуть команду?") },
            text = {
                Text(
                    "Вы выйдете из состава команды«${ui.detail?.name.orEmpty()}». " +
                        "Ваши долги и история сохранятся, вернуть вас сможет управляющий.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        viewModel.leaveTeam()
                    },
                ) { Text("Покинуть", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Отмена") }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить команду?") },
            text = {
                Text(
                    "Команда «${ui.detail?.name.orEmpty()}» будет удалена: " +
                        "она исчезнет из списков всех участников, начисления остановятся. " +
                        "Данные сохранятся в архиве.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.archiveTeam()
                    },
                ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            },
        )
    }

    if (showInviteDialog) {
        InviteDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { phone ->
                viewModel.invite(phone)
                showInviteDialog = false
            },
        )
    }

    if (showEditDialog) {
        ui.detail?.let { detail ->
            TeamEditDialog(
                name = detail.name,
                description = detail.description.orEmpty(),
                season = detail.season.orEmpty(),
                onChangeLogo = { pickLogo.launch("image/*") },
                onDismiss = { showEditDialog = false },
                onSave = { name, description, season ->
                    viewModel.updateTeam(name, description, season)
                    showEditDialog = false
                },
            )
        }
    }

    jerseyDialogMember?.let { member ->
        JerseyNumberDialog(
            member = member,
            taken = ui.members
                .filter { it.userId != member.userId && it.jerseyNumber != null }
                .associate { it.jerseyNumber!! to it.displayName },
            onDismiss = { jerseyDialogMember = null },
            onSelect = { number ->
                viewModel.setJerseyNumber(member, number)
                jerseyDialogMember = null
            },
        )
    }

    // Подтверждение исключения участника
    excludeDialogMember?.let { member ->
        AlertDialog(
            onDismissRequest = { excludeDialogMember = null },
            title = { Text("Исключить из команды?") },
            text = {
                Text(
                    "${member.displayName} будет исключён из команды. " +
                        "Его долги и история сохранятся, вернуть можно будет позже.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        excludeDialogMember = null
                        viewModel.exclude(member)
                    },
                ) { Text("Исключить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { excludeDialogMember = null }) { Text("Отмена") }
            },
        )
    }

    // Подтверждение отмены приглашения
    cancelDialogInvitation?.let { invitation ->
        AlertDialog(
            onDismissRequest = { cancelDialogInvitation = null },
            title = { Text("Отменить приглашение?") },
            text = {
                Text("Приглашение для ${invitation.invitedPhone} будет отменено.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cancelDialogInvitation = null
                        viewModel.cancelInvitation(invitation)
                    },
                ) { Text("Отменить приглашение", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { cancelDialogInvitation = null }) { Text("Назад") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemberRow(
    member: TeamMember,
    avatarUrl: String?,
    isManager: Boolean,
    isOwner: Boolean,
    onClick: () -> Unit,
    onAddPayment: () -> Unit,
    onSetJersey: () -> Unit,
    onToggleModerator: () -> Unit,
    onToggleTrainer: () -> Unit,
    onExclude: () -> Unit,
    onRestore: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { if (isManager) menuExpanded = true },
                )
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = member.jerseyNumber?.toString() ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    buildString {
                        append(member.role.display)
                        if (member.positions.isNotEmpty()) {
                            append(" • ")
                            append(
                                member.positions
                                    .sortedBy { it.ordinal }
                                    .joinToString("/") { it.display },
                            )
                        }
                        if (member.isTrainer) append(" • Тренер")
                        if (member.status != MemberStatus.ACTIVE) {
                            append(" • ${member.status.display}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Внести платёж") },
                onClick = {
                    menuExpanded = false
                    onAddPayment()
                },
            )
            DropdownMenuItem(
                text = { Text("Игровой номер") },
                onClick = {
                    menuExpanded = false
                    onSetJersey()
                },
            )
            DropdownMenuItem(
                text = { Text("Профиль игрока") },
                onClick = {
                    menuExpanded = false
                    onClick()
                },
            )
            if (isOwner && member.role != MemberRole.OWNER) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (member.role == MemberRole.MODERATOR) "Снять модератора"
                            else "Назначить модератором",
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleModerator()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (member.isTrainer) "Снять тренера"
                            else "Назначить тренером",
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleTrainer()
                    },
                )
            }
            if (member.role != MemberRole.OWNER) {
                if (member.status == MemberStatus.ACTIVE) {
                    DropdownMenuItem(
                        text = { Text("Исключить из команды") },
                        onClick = {
                            menuExpanded = false
                            onExclude()
                        },
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Вернуть в команду") },
                        onClick = {
                            menuExpanded = false
                            onRestore()
                        },
                    )
                }
            }
        }
    }
}

/** Счётчики участников по амплуа: вратари/защитники/нападающие/тренеры. */
@Composable
private fun PositionCounters(goalies: Int, defenders: Int, forwards: Int, trainers: Int) {
    Text(
        "В: $goalies   З: $defenders   Н: $forwards   Т: $trainers",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

/** Порядок сортировки по амплуа: вратари → защитники → нападающие → без амплуа. */
private fun positionOrder(positions: Set<Position>): Int = when {
    positions.contains(Position.GOALKEEPER) -> 0
    positions.contains(Position.DEFENDER) -> 1
    positions.contains(Position.FORWARD) -> 2
    else -> 3
}

/** Исходящие приглашения команды (PENDING) с возможностью отмены. */
@Composable
private fun OutgoingInvitationsCard(
    invitations: List<Invitation>,
    onCancel: (Invitation) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Отправленные приглашения",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            invitations.forEach { invitation ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            invitation.invitedPhone,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        invitation.expiresAt?.take(10)?.let { date ->
                            Text(
                                "Действует до $date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TextButton(onClick = { onCancel(invitation) }) {
                        Text("Отменить", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteDialog(onDismiss: () -> Unit, onInvite: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пригласить игрока") },
        text = {
            Column {
                Text(
                    "Игрок должен быть зарегистрирован в abik",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AbikTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Номер телефона",
                    keyboardType = KeyboardType.Phone,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onInvite(phone) }) { Text("Отправить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

/** Редактирование названия и описания команды (управляющие). */
@Composable
private fun TeamEditDialog(
    name: String,
    description: String,
    season: String,
    onChangeLogo: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, season: String) -> Unit,
) {
    var nameValue by remember { mutableStateOf(name) }
    var descriptionValue by remember { mutableStateOf(description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать команду") },
        text = {
            Column {
                AbikTextField(
                    value = nameValue,
                    onValueChange = { nameValue = it },
                    label = "Название",
                )
                Spacer(modifier = Modifier.height(8.dp))
                AbikTextField(
                    value = descriptionValue,
                    onValueChange = { descriptionValue = it },
                    label = "Описание",
                    singleLine = false,
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onChangeLogo) { Text("Изменить логотип") }
            }
        },
        confirmButton = {
            TextButton(
                // Сезон в форме не редактируется — сохраняется текущее значение
                onClick = { onSave(nameValue, descriptionValue, season) },
                enabled = nameValue.isNotBlank(),
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
