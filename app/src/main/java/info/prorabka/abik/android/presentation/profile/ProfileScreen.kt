package info.prorabka.abik.android.presentation.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import info.prorabka.abik.android.core.prefs.ThemeMode
import info.prorabka.abik.android.core.ui.components.AbikPullToRefresh
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.util.isoDateToDisplay
import info.prorabka.abik.android.core.util.phoneToDisplay
import info.prorabka.abik.android.domain.model.Invitation

/**
 * Вкладка «Профиль» нижнего меню: внутри две вкладки — «Профиль» и «Настройки».
 */
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onChangePhone: () -> Unit,
    onAdmin: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    info.prorabka.abik.android.core.ui.RealtimeRefresh { viewModel.load() }

    // Синхронизация профиля: обновляем данные при возврате на экран
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Профиль") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Настройки") })
        }

        when (tab) {
            0 -> AbikPullToRefresh(
                loading = ui.loading,
                onRefresh = viewModel::load,
            ) {
                ProfileTab(
                    ui = ui,
                    onEdit = onEditProfile,
                    viewModel = viewModel,
                )
            }
            1 -> SettingsTab(
                themeMode = themeMode,
                onThemeChange = viewModel::setTheme,
                isDebug = viewModel.isDebugBuild,
                isSuperAdmin = ui.isSuperAdmin,
                onUseRemote = viewModel::useRemoteServer,
                onUseLocal = viewModel::useLocalServer,
                onChangePhone = onChangePhone,
                onAdmin = onAdmin,
                notificationSettings = ui.notificationSettings,
                notificationSettingsSaving = ui.notificationSettingsSaving,
                notificationSettingsError = ui.notificationSettingsError,
                onSaveNotificationSettings = viewModel::saveNotificationSettings,
                biometricAvailable = viewModel.biometricAvailable,
                biometricEnabled = biometricEnabled,
                onBiometricChange = viewModel::setBiometricEnabled,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileTab(
    ui: ProfileViewModel.UiState,
    onEdit: () -> Unit,
    viewModel: ProfileViewModel,
) {
    if (ui.loading && ui.profile == null) {
        LoadingBox()
        return
    }
    val profile = ui.profile
    var avatarMenu by remember { mutableStateOf(false) }
    var showAvatarDeleteDialog by remember { mutableStateOf(false) }
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let(viewModel::uploadAvatar) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Аватар: долгий тап — установить/изменить/удалить (ТЗ v3)
                Box {
                    val avatarUrl = viewModel.resolveUrl(profile?.avatarUrl)
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Аватар",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { avatarMenu = true },
                                ),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Аватар",
                            modifier = Modifier
                                .size(64.dp)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { avatarMenu = true },
                                ),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    DropdownMenu(
                        expanded = avatarMenu,
                        onDismissRequest = { avatarMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (profile?.avatarUrl == null) {
                                        "Установить аватар"
                                    } else {
                                        "Изменить аватар"
                                    },
                                )
                            },
                            onClick = {
                                avatarMenu = false
                                pickImage.launch("image/*")
                            },
                        )
                        if (profile?.avatarUrl != null) {
                            DropdownMenuItem(
                                text = { Text("Удалить аватар") },
                                onClick = {
                                    avatarMenu = false
                                    showAvatarDeleteDialog = true
                                },
                            )
                        }
                    }
                    if (showAvatarDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showAvatarDeleteDialog = false },
                            title = { Text("Удалить аватар?") },
                            text = { Text("Фото профиля будет удалено.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showAvatarDeleteDialog = false
                                        viewModel.removeAvatar()
                                    },
                                ) {
                                    Text(
                                        "Удалить",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showAvatarDeleteDialog = false },
                                ) { Text("Отмена") }
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(6.dp))
                Column {
                    Text(
                        profile?.let {
                            info.prorabka.abik.android.core.util.NameFormatter.short(it.fullName)
                        } ?: "Профиль не заполнен",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        ui.phone.phoneToDisplay(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ui.jerseyNumber?.let {
                        Text(
                            "№$it",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (profile != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileRow("Дата рождения", profile.birthDate?.toString()?.isoDateToDisplay() ?: "—")
                    ProfileRow("Амплуа", profile.positions.joinToString(", ") { it.display })
                    ProfileRow("Статус", profile.playerStatus.display)
                    ProfileRow("Рост", "${profile.heightCm} см")
                    ProfileRow("Вес", "${profile.weightKg} кг")
                    ProfileRow("Хват", profile.stickHand.display)
                    ProfileRow(
                        "Город",
                        listOfNotNull(profile.cityName.ifBlank { null }, profile.regionName)
                            .joinToString(", ")
                            .ifBlank { "—" },
                    )
                    if (!profile.jerseySize.isNullOrBlank()) {
                        ProfileRow("Размер джерси", profile.jerseySize)
                    }
                    if (!profile.coversSize.isNullOrBlank()) {
                        ProfileRow("Размер чехлов", profile.coversSize)
                    }
                    if (!profile.gaitersSize.isNullOrBlank()) {
                        ProfileRow("Размер гамаш", profile.gaitersSize)
                    }
                }
            }
        }

        // --- Входящие приглашения в команды (вне онбординга) ---
        if (ui.invitations.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Приглашения в команды",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ui.invitations.forEach { invitation ->
                        InvitationRow(
                            invitation = invitation,
                            enabled = !ui.invitationSaving,
                            onAccept = { viewModel.acceptInvitation(invitation) },
                            onDecline = { viewModel.declineInvitation(invitation) },
                        )
                    }
                }
            }
        }

        ui.info?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        ui.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text("Редактировать профиль")
        }
    }
}

@Composable
private fun InvitationRow(
    invitation: Invitation,
    enabled: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(invitation.teamName, style = MaterialTheme.typography.bodyLarge)
            invitation.expiresAt?.take(10)?.let { date ->
                Text(
                    "Действует до ${date.isoDateToDisplay()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onDecline, enabled = enabled) {
            Text("Отклонить", color = MaterialTheme.colorScheme.error)
        }
        TextButton(onClick = onAccept, enabled = enabled) {
            Text("Принять")
        }
    }
}

@Composable
private fun SettingsTab(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    isDebug: Boolean,
    isSuperAdmin: Boolean,
    onUseRemote: () -> Unit,
    onUseLocal: () -> Unit,
    onChangePhone: () -> Unit,
    onAdmin: () -> Unit,
    notificationSettings: info.prorabka.abik.android.domain.model.NotificationSettings,
    notificationSettingsSaving: Boolean,
    notificationSettingsError: String?,
    onSaveNotificationSettings: (info.prorabka.abik.android.domain.model.NotificationSettings) -> Unit,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    onBiometricChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Тема оформления", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeChange(ThemeMode.SYSTEM) },
                        label = { Text("Системная") },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeChange(ThemeMode.LIGHT) },
                        label = { Text("Светлая") },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeChange(ThemeMode.DARK) },
                        label = { Text("Тёмная") },
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Безопасность", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Вход по биометрии")
                        Text(
                            if (biometricAvailable) {
                                "Отпечаток пальца или другая биометрия при открытии приложения"
                            } else {
                                "Биометрия недоступна или не настроена на устройстве"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = biometricEnabled && biometricAvailable,
                        onCheckedChange = onBiometricChange,
                        enabled = biometricAvailable,
                    )
                }
            }
        }

        NotificationSettingsCard(
            settings = notificationSettings,
            saving = notificationSettingsSaving,
            serverError = notificationSettingsError,
            onSave = onSaveNotificationSettings,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Аккаунт", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onChangePhone) { Text("Сменить номер телефона") }
            }
        }

        if (isSuperAdmin) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Администрирование", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onAdmin) { Text("Панель суперадмина") }
                }
            }
        }

        if (isDebug) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Сервер (debug)", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onUseRemote) { Text("varamy.online (прод)") }
                    TextButton(onClick = onUseLocal) { Text("192.168.0.119:8083 (локальный)") }
                }
            }
        }
    }
}

private enum class ReminderTimeUnit(val minutes: Int, val label: String) {
    MINUTES(1, "мин."),
    HOURS(60, "час."),
    DAYS(1440, "дн."),
}

private data class DurationParts(val value: String, val unit: ReminderTimeUnit)

private fun durationParts(minutes: Int): DurationParts = when {
    minutes % 1440 == 0 -> DurationParts((minutes / 1440).toString(), ReminderTimeUnit.DAYS)
    minutes % 60 == 0 -> DurationParts((minutes / 60).toString(), ReminderTimeUnit.HOURS)
    else -> DurationParts(minutes.toString(), ReminderTimeUnit.MINUTES)
}

@Composable
private fun NotificationSettingsCard(
    settings: info.prorabka.abik.android.domain.model.NotificationSettings,
    saving: Boolean,
    serverError: String?,
    onSave: (info.prorabka.abik.android.domain.model.NotificationSettings) -> Unit,
) {
    var enabled by remember(settings) { mutableStateOf(settings.remindersEnabled) }
    val initialLead = remember(settings) { durationParts(settings.leadTimeMinutes) }
    var leadValue by remember(settings) { mutableStateOf(initialLead.value) }
    var leadUnit by remember(settings) { mutableStateOf(initialLead.unit) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Напоминания о событиях", style = MaterialTheme.typography.titleSmall)
            serverError?.let {
                Text(
                    "Не удалось загрузить или сохранить настройки напоминаний: $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Напоминать", modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    enabled = !saving,
                )
            }
            if (enabled) {
                ReminderDurationField(
                    label = "За какое время до начала напомнить",
                    value = leadValue,
                    unit = leadUnit,
                    onValueChange = { leadValue = it },
                    onUnitChange = { leadUnit = it },
                )
            }
            validationError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            TextButton(
                onClick = {
                    validationError = null
                    if (!enabled) {
                        onSave(
                            settings.copy(
                                remindersEnabled = false,
                                reminderCount = 1,
                                intervalMinutes = 60,
                            ),
                        )
                        return@TextButton
                    }
                    val leadMinutes = leadValue.toLongOrNull()?.times(leadUnit.minutes)
                    if (leadMinutes == null || leadMinutes !in 1..43200) {
                        validationError = "Срок напоминания должен быть от 1 минуты до 30 дней"
                    } else {
                        onSave(
                            info.prorabka.abik.android.domain.model.NotificationSettings(
                                remindersEnabled = true,
                                reminderCount = 1,
                                leadTimeMinutes = leadMinutes.toInt(),
                                intervalMinutes = 60,
                            ),
                        )
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(if (saving) "Сохранение…" else "Сохранить настройки") }
            Text(
                "Уведомления о новом начислении и переводе из резерва приходят всегда.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ReminderDurationField(
    label: String,
    value: String,
    unit: ReminderTimeUnit,
    onValueChange: (String) -> Unit,
    onUnitChange: (ReminderTimeUnit) -> Unit,
) {
    Text(
        label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        info.prorabka.abik.android.core.ui.components.AbikTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(5)) },
            label = "Время",
            modifier = Modifier.weight(1f),
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
        )
        Box {
            var expanded by remember { mutableStateOf(false) }
            TextButton(onClick = { expanded = true }) { Text(unit.label) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ReminderTimeUnit.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onUnitChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
