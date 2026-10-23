package info.prorabka.abik.android.presentation.subscription

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import java.time.format.DateTimeFormatter

/** Создание разового начисления: назначение, сумма, дата, примечание, участники. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargeCreateScreen(
    preselectUserId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ChargeCreateViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(preselectUserId) { viewModel.load(preselectUserId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новое начисление") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        if (ui.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            GroupAutocomplete(ui, viewModel)
            Spacer(modifier = Modifier.height(12.dp))

            if (ui.groupName.isNotBlank() &&
                ui.groups.any { it.name.equals(ui.groupName.trim(), true) }
            ) {
                SubgroupAutocomplete(ui, viewModel)
                Spacer(modifier = Modifier.height(12.dp))
            }

            AbikTextField(
                value = ui.amountRubles,
                onValueChange = viewModel::onAmount,
                label = "Сумма (₽)",
                keyboardType = KeyboardType.Number,
            )
            Spacer(modifier = Modifier.height(12.dp))

            info.prorabka.abik.android.core.ui.components.DateField(
                displayValue = ui.dueDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                label = "Дата начисления",
                onSelect = viewModel::onDueDate,
            )
            Spacer(modifier = Modifier.height(12.dp))

            AbikTextField(
                value = ui.note,
                onValueChange = viewModel::onNote,
                label = "Примечание",
                singleLine = false,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Участники (${ui.selectedUserIds.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::selectAll) { Text("Выбрать всех") }
            }
            Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                ui.members.forEach { member ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = member.userId in ui.selectedUserIds,
                            onCheckedChange = { viewModel.toggleMember(member.userId) },
                        )
                        Text(member.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            ErrorText(ui.error)
            Spacer(modifier = Modifier.height(16.dp))
            AbikButton(
                text = "Создать начисление",
                onClick = { viewModel.save(onDone) },
                loading = ui.saving,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupAutocomplete(
    ui: ChargeCreateViewModel.UiState,
    viewModel: ChargeCreateViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = ui.groupName,
            onValueChange = {
                viewModel.onGroupName(it)
                expanded = true
            },
            label = { Text("Назначение") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        val filtered = ui.groups.filter {
            ui.groupName.isBlank() || it.name.contains(ui.groupName.trim(), ignoreCase = true)
        }
        if (filtered.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                filtered.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.name) },
                        onClick = {
                            viewModel.onGroupName(group.name)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubgroupAutocomplete(
    ui: ChargeCreateViewModel.UiState,
    viewModel: ChargeCreateViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = ui.subgroupName,
            onValueChange = {
                viewModel.onSubgroupName(it)
                expanded = true
            },
            label = { Text("Подгруппа (необязательно)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        if (ui.subgroups.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ui.subgroups.forEach { subgroup ->
                    DropdownMenuItem(
                        text = { Text(subgroup.name) },
                        onClick = {
                            viewModel.onSubgroupName(subgroup.name)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
