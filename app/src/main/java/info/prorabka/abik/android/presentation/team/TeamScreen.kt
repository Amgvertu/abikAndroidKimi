package info.prorabka.abik.android.presentation.team

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.model.TeamMember

/**
 * Экран «Команда»: информация о команде, список участников.
 * Управляющие: приглашение, долгий тап на игрока — контекстное меню.
 */
@Composable
fun TeamScreen(
    onMemberClick: (Long) -> Unit,
    viewModel: TeamViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }
    var jerseyDialogMember by remember { mutableStateOf<TeamMember?>(null) }

    if (ui.loading && ui.detail == null) {
        LoadingBox()
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // --- Шапка команды ---
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Логотип",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
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
                    IconButton(onClick = { showInviteDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Пригласить игрока")
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

        // --- Список участников ---
        LazyColumn {
            items(ui.members, key = { it.userId }) { member ->
                MemberRow(
                    member = member,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(member.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    buildString {
                        append(member.role.display)
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

/** Таблица свободных и занятых номеров (1–99) по ТЗ v3. */
@Composable
private fun JerseyNumberDialog(
    member: TeamMember,
    taken: Map<Int, String>,
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit,
) {
    var confirmFree by remember { mutableStateOf<Int?>(null) }
    var busyInfo by remember { mutableStateOf<Pair<Int, String>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Номер: ${member.displayName}") },
        text = {
            Column {
                Text(
                    "Текущий: ${member.jerseyNumber ?: "не назначен"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items((1..99).toList().chunked(6)) { rowNumbers ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            rowNumbers.forEach { number ->
                                val isTaken = number in taken
                                Text(
                                    text = number.toString(),
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .clickable {
                                            if (isTaken) {
                                                busyInfo = number to taken.getValue(number)
                                            } else {
                                                confirmFree = number
                                            }
                                        },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isTaken -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    fontWeight = if (isTaken) FontWeight.Normal else FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                if (member.jerseyNumber != null) {
                    TextButton(onClick = { onSelect(null) }) {
                        Text("Снять номер", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )

    confirmFree?.let { number ->
        AlertDialog(
            onDismissRequest = { confirmFree = null },
            title = { Text("Назначить номер $number?") },
            text = { Text(member.displayName) },
            confirmButton = {
                TextButton(onClick = { onSelect(number) }) { Text("Назначить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmFree = null }) { Text("Отмена") }
            },
        )
    }

    busyInfo?.let { (number, owner) ->
        AlertDialog(
            onDismissRequest = { busyInfo = null },
            title = { Text("Номер $number занят") },
            text = { Text("Сейчас под ним: $owner") },
            confirmButton = {
                TextButton(onClick = { busyInfo = null }) { Text("Понятно") }
            },
        )
    }
}
