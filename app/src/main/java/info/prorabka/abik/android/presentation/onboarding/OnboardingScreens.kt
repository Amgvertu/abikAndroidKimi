package info.prorabka.abik.android.presentation.onboarding

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
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import info.prorabka.abik.android.core.ui.components.AbikButton
import info.prorabka.abik.android.core.ui.components.AbikTextField
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox

/** Онбординг: заполнение профиля (обязательные поля по ТЗ). */
@Composable
fun OnboardingProfileScreen(onDone: () -> Unit) {
    ProfileFormScreen(
        title = "Заполните профиль",
        submitText = "Сохранить и продолжить",
        prefill = false,
        onDone = onDone,
    )
}

/** Онбординг: принять приглашение или создать свою команду. */
@Composable
fun OnboardingTeamScreen(
    onDone: () -> Unit,
    viewModel: TeamOnboardingViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    if (ui.loading) {
        LoadingBox()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text("Команда", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Примите приглашение или создайте свою команду",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (ui.invitations.isNotEmpty()) {
            Text("Входящие приглашения", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ui.invitations.forEach { invitation ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(invitation.teamName, style = MaterialTheme.typography.titleSmall)
                        }
                        TextButton(onClick = { viewModel.decline(invitation.id) }) {
                            Text("Отклонить")
                        }
                        TextButton(
                            onClick = { viewModel.accept(invitation.id, onDone) },
                            enabled = !ui.saving,
                        ) {
                            Text("Принять")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Создать команду", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        AbikTextField(value = ui.name, onValueChange = viewModel::onName, label = "Название команды")
        Spacer(modifier = Modifier.height(12.dp))

        RegionDropdown(ui, viewModel)
        Spacer(modifier = Modifier.height(12.dp))
        CityDropdown(ui, viewModel)
        Spacer(modifier = Modifier.height(12.dp))

        AbikTextField(
            value = ui.description,
            onValueChange = viewModel::onDescription,
            label = "Описание (необязательно)",
            singleLine = false,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AbikTextField(
            value = ui.season,
            onValueChange = viewModel::onSeason,
            label = "Сезон, например 2026/27 (необязательно)",
        )

        ErrorText(ui.error)
        if (ui.info != null) {
            Text(
                text = ui.info!!,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        AbikButton(
            text = "Создать команду",
            onClick = { viewModel.createTeam(onDone) },
            loading = ui.saving,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionDropdown(
    ui: TeamOnboardingViewModel.UiState,
    viewModel: TeamOnboardingViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = ui.selectedRegion?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Домашний регион") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ui.regions.forEach { region ->
                DropdownMenuItem(
                    text = { Text(region.name) },
                    onClick = {
                        viewModel.selectRegion(region)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityDropdown(
    ui: TeamOnboardingViewModel.UiState,
    viewModel: TeamOnboardingViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (ui.selectedRegion != null) expanded = it },
    ) {
        OutlinedTextField(
            value = ui.selectedCity?.name ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = ui.selectedRegion != null,
            label = { Text("Город") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ui.cities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city.name) },
                    onClick = {
                        viewModel.selectCity(city)
                        expanded = false
                    },
                )
            }
        }
    }
}
