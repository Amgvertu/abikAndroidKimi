package info.prorabka.abik.android.presentation.lists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.domain.model.ChargeList
import info.prorabka.abik.android.domain.model.TeamMember

/**
 * Списки получателей начислений: название + участники.
 * Используются как быстрый выбор получателей в формах начислений и шаблонов.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    onBack: () -> Unit,
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var editTarget by remember { mutableStateOf<ChargeList?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ChargeList?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Списки получателей") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Создать список")
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                ui.loading && ui.lists.isEmpty() -> LoadingBox()
                ui.lists.isEmpty() -> EmptyState(
                    "Списков пока нет.\nСоздайте список, чтобы быстро выбирать получателей начислений.",
                )
                else -> Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    ui.info?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    ErrorText(ui.error)
                    LazyColumn {
                        items(ui.lists, key = { it.id }) { list ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(
                                        start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp,
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            list.name,
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            "Участников: ${list.memberIds.size}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { editTarget = list }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Изменить")
                                    }
                                    IconButton(onClick = { deleteTarget = list }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Удалить",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ListEditDialog(
            title = "Новый список",
            initialName = "",
            initialMemberIds = emptySet(),
            members = ui.members,
            saving = ui.saving,
            onDismiss = { showCreateDialog = false },
            onSave = { name, ids ->
                viewModel.save(null, name, ids) { showCreateDialog = false }
            },
        )
    }

    editTarget?.let { list ->
        ListEditDialog(
            title = "Изменить список",
            initialName = list.name,
            initialMemberIds = list.memberIds,
            members = ui.members,
            saving = ui.saving,
            onDismiss = { editTarget = null },
            onSave = { name, ids ->
                viewModel.save(list.id, name, ids) { editTarget = null }
            },
        )
    }

    deleteTarget?.let { list ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить список?") },
            text = { Text("Список «${list.name}» будет удалён. Сами начисления не изменятся.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        viewModel.delete(list)
                    },
                ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Отмена") }
            },
        )
    }
}

/** Диалог создания/редактирования списка: название + чек-лист участников. */
@Composable
private fun ListEditDialog(
    title: String,
    initialName: String,
    initialMemberIds: Set<Long>,
    members: List<TeamMember>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Set<Long>) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var checked by remember { mutableStateOf(initialMemberIds) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(title) },
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
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
                enabled = !saving,
                onClick = { onSave(name, checked) },
            ) { Text(if (saving) "Сохранение…" else "Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Отмена") }
        },
    )
}
