package info.prorabka.abik.android.presentation.team

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.model.TeamMember

/**
 * Экран «Команда»: информация о команде, список участников.
 * Управляющие: приглашение, редактирование команды, долгий тап на игрока —
 * контекстное меню, долгий тап на логотип — изменить логотип.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamScreen(
    onMemberClick: (Long) -> Unit,
    onCreateTeam: () -> Unit,
    onRoster: () -> Unit,
    viewModel: TeamViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var jerseyDialogMember by remember { mutableStateOf<TeamMember?>(null) }

    if (ui.loading && ui.detail == null && !ui.noTeam) {
        LoadingBox()
        return
    }
    if (ui.noTeam) {
        NoTeamPlaceholder(onCreateTeam)
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // --- Шапка команды ---
        val pickLogo = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri -> uri?.let(viewModel::changeLogo) }
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Логотип: долгий тап (управляющие) — изменить (ТЗ v3)
                val logoUrl = viewModel.resolveUrl(ui.detail?.logoUrl)
                val logoModifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { if (ui.isManager) pickLogo.launch("image/*") },
                    )
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = "Логотип",
                        modifier = logoModifier,
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Логотип",
                        modifier = logoModifier,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.padding(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ui.detail?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        listOfNotNull(
                            ui.detail?.cityName,
                            ui.detail?.season?.let { "Сезон $it" },
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (ui.isManager) {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать команду")
                    }
                    IconButton(onClick = { showInviteDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Пригласить игрока")
                    }
                }
            }
            ui.detail?.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        }

        // --- Переход к составу регулярки ---
        TextButton(onClick = onRoster, modifier = Modifier.fillMaxWidth()) {
            Text("Состав регулярки")
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

        // --- Список участников ---
        LazyColumn {
            items(ui.members, key = { it.userId }) { member ->
                MemberRow(
                    member = member,
                    avatarUrl = viewModel.resolveUrl(member.avatarUrl),
                    isManager = ui.isManager,
                    isOwner = ui.isOwner,
                    onClick = { onMemberClick(member.userId) },
                    onSetJersey = { jerseyDialogMember = member },
                    onToggleModerator = { viewModel.toggleModerator(member) },
                    onExclude = { viewModel.exclude(member) },
                    onRestore = { viewModel.restore(member) },
                )
                HorizontalDivider()
            }
        }
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemberRow(
    member: TeamMember,
    avatarUrl: String?,
    isManager: Boolean,
    isOwner: Boolean,
    onClick: () -> Unit,
    onSetJersey: () -> Unit,
    onToggleModerator: () -> Unit,
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

/** Редактирование названия, описания и сезона команды (управляющие). */
@Composable
private fun TeamEditDialog(
    name: String,
    description: String,
    season: String,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, season: String) -> Unit,
) {
    var nameValue by remember { mutableStateOf(name) }
    var descriptionValue by remember { mutableStateOf(description) }
    var seasonValue by remember { mutableStateOf(season) }

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
                    value = seasonValue,
                    onValueChange = { seasonValue = it },
                    label = "Сезон (например, 2026/27)",
                )
                Spacer(modifier = Modifier.height(8.dp))
                AbikTextField(
                    value = descriptionValue,
                    onValueChange = { descriptionValue = it },
                    label = "Описание",
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(nameValue, descriptionValue, seasonValue) },
                enabled = nameValue.isNotBlank(),
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
