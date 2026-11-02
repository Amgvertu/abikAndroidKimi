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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.ui.components.NoTeamPlaceholder
import info.prorabka.abik.android.domain.model.ChargeList
import info.prorabka.abik.android.domain.model.Invitation
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.TeamMember

/**
 * Экран «Команда»: вся команда и произвольные общие списки участников.
 * Счётчики по амплуа находятся над вкладками, игроки сгруппированы по амплуа.
 * «+» создаёт список, долгий тап по его вкладке включает редактирование состава.
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
    var listEditMode by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var addToListMember by remember { mutableStateOf<TeamMember?>(null) }
    var listRemoveTarget by remember { mutableStateOf<Pair<ChargeList, TeamMember>?>(null) }

    LaunchedEffect(ui.chargeLists.size) {
        if (selectedTab > ui.chargeLists.size) {
            selectedTab = 0
            listEditMode = false
        }
    }

    // После редактирования или удаления в «Списках получателях» обновляем вкладки.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshChargeLists()
        onPauseOrDispose { }
    }

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

    val selectedList = if (selectedTab > 0) {
        ui.chargeLists.getOrNull(selectedTab - 1)
    } else {
        null
    }
    val displayedMembers = if (selectedList == null) {
        ui.members
    } else {
        ui.members.filter { it.userId in selectedList.memberIds }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Счётчики текущей вкладки и действия команды — в одной строке.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PositionCounters(
                goalies = displayedMembers.count {
                    !it.isTrainer && it.positions.contains(Position.GOALKEEPER)
                },
                defenders = displayedMembers.count {
                    !it.isTrainer && it.positions.contains(Position.DEFENDER)
                },
                forwards = displayedMembers.count {
                    !it.isTrainer && it.positions.contains(Position.FORWARD)
                },
                trainers = displayedMembers.count { it.isTrainer },
                modifier = Modifier.weight(1f),
            )
            if (ui.isManager) {
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

        // Основная вкладка, затем пользовательские списки и кнопка создания.
        ScrollableTabRow(
            selectedTabIndex = selectedTab.coerceAtMost(ui.chargeLists.size),
            edgePadding = 0.dp,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    listEditMode = false
                },
            ) {
                Text("Вся команда", modifier = Modifier.padding(vertical = 12.dp))
            }
            ui.chargeLists.forEachIndexed { index, list ->
                val tabIndex = index + 1
                Box(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                selectedTab = tabIndex
                                listEditMode = false
                            },
                            onLongClick = {
                                if (ui.isManager) {
                                    selectedTab = tabIndex
                                    listEditMode = !listEditMode
                                }
                            },
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        list.name,
                        maxLines = 1,
                        color = if (selectedTab == tabIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            if (ui.isManager) {
                Text(
                    "+",
                    modifier = Modifier
                        .combinedClickable(onClick = { showCreateListDialog = true })
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }

        if (selectedTab == 0) {
            // ===== Вся команда, сгруппированная по амплуа =====
            val groups = groupMembersByPosition(ui.members)
            LazyColumn(modifier = Modifier.weight(1f)) {
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

                if (ui.isManager && ui.outgoingInvitations.isNotEmpty()) {
                    item(key = "outgoing_invitations") {
                        OutgoingInvitationsCard(
                            invitations = ui.outgoingInvitations,
                            onCancel = { cancelDialogInvitation = it },
                        )
                    }
                }

                groups.forEach { group ->
                    item(key = "team_group_${group.title}") {
                        PositionGroupHeader(group.title)
                    }
                    items(group.members, key = { it.userId }) { member ->
                        Box(modifier = Modifier.padding(start = 12.dp)) {
                            MemberRow(
                                member = member,
                                avatarUrl = viewModel.resolveUrl(member.avatarUrl),
                                isManager = ui.isManager,
                                isOwner = ui.isOwner,
                                showAddToList = ui.isManager && ui.chargeLists.isNotEmpty(),
                                onClick = { onMemberClick(member.userId) },
                                onAddPayment = { onAddPayment(member.userId) },
                                onAddToList = { addToListMember = member },
                                onSetJersey = { jerseyDialogMember = member },
                                onToggleModerator = { viewModel.toggleModerator(member) },
                                onToggleTrainer = { viewModel.toggleTrainer(member) },
                                onExclude = { excludeDialogMember = member },
                                onRestore = { viewModel.restore(member) },
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        } else {
            // ===== Пользовательский список, тоже сгруппированный по амплуа =====
            val currentList = selectedList
            if (currentList == null) {
                Text(
                    "Список не найден",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                val groups = groupMembersByPosition(displayedMembers)
                val candidates = ui.members.filter {
                    it.status == MemberStatus.ACTIVE && it.userId !in currentList.memberIds
                }
                if (listEditMode) {
                    Text(
                        "Редактирование списка «${currentList.name}». " +
                            "Долгий тап по вкладке — завершить.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    groups.forEach { group ->
                        item(key = "list_${currentList.id}_group_${group.title}") {
                            PositionGroupHeader(group.title)
                        }
                        items(
                            group.members,
                            key = { "list_${currentList.id}_member_${it.userId}" },
                        ) { member ->
                            ListMemberRow(
                                member = member,
                                editMode = listEditMode,
                                enabled = !ui.listSaving,
                                onRemove = {
                                    listRemoveTarget = currentList to member
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                    if (groups.isEmpty() && !listEditMode) {
                        item(key = "list_empty") {
                            Text(
                                "Список пока пуст",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                    if (listEditMode && candidates.isNotEmpty()) {
                        item(key = "list_candidates_header") {
                            Text(
                                "Добавить в список",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            )
                        }
                        items(candidates, key = { "candidate_${it.userId}" }) { member ->
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
                                    enabled = !ui.listSaving,
                                    onClick = {
                                        viewModel.addToChargeList(currentList.id, member.userId)
                                    },
                                ) { Text("Добавить") }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // Создание общего списка через «+».
    if (showCreateListDialog) {
        CreateChargeListDialog(
            members = ui.members.filter { it.status == MemberStatus.ACTIVE },
            saving = ui.listSaving,
            onDismiss = { showCreateListDialog = false },
            onSave = { name, memberIds ->
                viewModel.createChargeList(name, memberIds) { created ->
                    showCreateListDialog = false
                    selectedTab = viewModel.ui.value.chargeLists
                        .indexOfFirst { it.id == created.id } + 1
                }
            },
        )
    }

    // Включение игрока в выбранный список из контекстного меню.
    addToListMember?.let { member ->
        AddToChargeListDialog(
            member = member,
            lists = ui.chargeLists,
            onDismiss = { addToListMember = null },
            onSelect = { list ->
                viewModel.addToChargeList(list.id, member.userId)
                addToListMember = null
            },
        )
    }

    // Подтверждение исключения из пользовательского списка.
    listRemoveTarget?.let { (list, member) ->
        AlertDialog(
            onDismissRequest = { listRemoveTarget = null },
            title = { Text("Исключить из списка?") },
            text = { Text("${member.displayName}\nСписок: ${list.name}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        listRemoveTarget = null
                        viewModel.removeFromChargeList(list.id, member.userId)
                    },
                ) { Text("Исключить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { listRemoveTarget = null }) { Text("Отмена") }
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
    showAddToList: Boolean,
    onClick: () -> Unit,
    onAddPayment: () -> Unit,
    onAddToList: () -> Unit,
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
            if (showAddToList) {
                DropdownMenuItem(
                    text = { Text("Включить в список") },
                    onClick = {
                        menuExpanded = false
                        onAddToList()
                    },
                )
            }
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
private fun PositionCounters(
    goalies: Int,
    defenders: Int,
    forwards: Int,
    trainers: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        "В: $goalies   З: $defenders   Н: $forwards   Т: $trainers",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

private data class PositionGroup(
    val title: String,
    val members: List<TeamMember>,
)

/** Каждый игрок показывается один раз; тренеры и игроки без амплуа не теряются. */
private fun groupMembersByPosition(members: List<TeamMember>): List<PositionGroup> {
    val grouped = members.groupBy { member ->
        when {
            member.isTrainer -> 3
            member.positions.contains(Position.GOALKEEPER) -> 0
            member.positions.contains(Position.DEFENDER) -> 1
            member.positions.contains(Position.FORWARD) -> 2
            else -> 4
        }
    }
    return listOf(
        0 to "Вратари",
        1 to "Защитники",
        2 to "Нападающие",
        3 to "Тренеры",
        4 to "Без амплуа",
    ).mapNotNull { (index, title) ->
        grouped[index]
            ?.sortedBy { it.displayName }
            ?.let { PositionGroup(title, it) }
    }
}

@Composable
private fun PositionGroupHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

/** Строка игрока на вкладке списка: номер + ФИ, с отступом от заголовка группы. */
@Composable
private fun ListMemberRow(
    member: TeamMember,
    editMode: Boolean,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            member.jerseyNumber?.toString() ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(40.dp),
        )
        Text(
            member.displayName + if (member.isTrainer) " • Тренер" else "",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (editMode) {
            TextButton(enabled = enabled, onClick = onRemove) {
                Text("Исключить", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Создание списка с начальным составом; дальше состав редактируется на вкладке. */
@Composable
private fun CreateChargeListDialog(
    members: List<TeamMember>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Set<Long>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(emptySet<Long>()) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Новый список") },
        text = {
            Column {
                AbikTextField(
                    value = name,
                    onValueChange = { name = it.take(200) },
                    label = "Название",
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Участники (${checked.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(Modifier.height(300.dp)) {
                    items(members, key = { it.userId }) { member ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = member.userId in checked,
                                onCheckedChange = { on ->
                                    checked = if (on) checked + member.userId
                                    else checked - member.userId
                                },
                            )
                            Text(member.displayName)
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && checked.isNotEmpty() && !saving,
                onClick = { onSave(name, checked) },
            ) { Text(if (saving) "Сохранение…" else "Создать") }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Отмена") }
        },
    )
}

/** Выбор списка при долгом тапе на игроке во «Вся команда». */
@Composable
private fun AddToChargeListDialog(
    member: TeamMember,
    lists: List<ChargeList>,
    onDismiss: () -> Unit,
    onSelect: (ChargeList) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Включить в список") },
        text = {
            Column {
                Text(
                    member.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(Modifier.height(300.dp)) {
                    items(lists, key = { it.id }) { list ->
                        val alreadyAdded = member.userId in list.memberIds
                        TextButton(
                            enabled = !alreadyAdded,
                            onClick = { onSelect(list) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                list.name + if (alreadyAdded) " — уже в списке" else "",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
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
