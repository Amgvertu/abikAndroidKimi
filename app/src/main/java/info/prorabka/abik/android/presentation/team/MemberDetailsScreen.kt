package info.prorabka.abik.android.presentation.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.util.isoDateToDisplay
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.model.TeamMember

/**
 * Карточка участника (ТЗ 9.2): членство в команде + профиль игрока.
 * Управляющие: игровой номер, модератор (владелец), тренер (владелец/админ).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailsScreen(
    userId: Long,
    onBack: () -> Unit,
    viewModel: MemberDetailsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var showJerseyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) { viewModel.load(userId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Игрок") },
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
        val details = ui.details
        if (details == null) {
            Column(Modifier.padding(padding).padding(24.dp)) {
                Text(ui.error ?: "Не удалось загрузить данные игрока")
            }
            return@Scaffold
        }
        val profile = details.profile

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Шапка: аватарка, ФИО, амплуа ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val avatarUrl = viewModel.resolveUrl(profile?.avatarUrl)
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Аватар",
                            modifier = Modifier.size(64.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.padding(6.dp))
                    Column {
                        Text(
                            profile?.fullName ?: (details.phone ?: "user#${details.userId}"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                            val positions = profile?.positions.orEmpty()
                        if (positions.isNotEmpty()) {
                            Text(
                                positions.joinToString(", ") { it.display },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            ui.info?.let {
                Text(it, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)
            }
            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            // --- Членство в команде ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Телефон", details.phone ?: "—")
                    InfoRow("Роль", details.role.display)
                    if (details.status != MemberStatus.ACTIVE) {
                        InfoRow("Статус", details.status.display)
                    }
                    if (details.isTrainer) {
                        InfoRow("Тренер", "Да")
                    }
                    InfoRow("Игровой номер", details.jerseyNumber?.toString() ?: "не назначен")
                    details.joinedAt?.take(10)?.let { date ->
                        InfoRow("В команде с", date.isoDateToDisplay())
                    }
                }
            }

            // --- Профиль игрока ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (profile == null) {
                        Text(
                            "Профиль не заполнен",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        InfoRow(
                            "Дата рождения",
                            profile.birthDate?.toString()?.isoDateToDisplay() ?: "—",
                        )
                        InfoRow("Рост", "${profile.heightCm} см")
                        InfoRow("Вес", "${profile.weightKg} кг")
                        InfoRow("Хват", profile.stickHand.display)
                        InfoRow(
                            "Город",
                            listOfNotNull(
                                profile.cityName.ifBlank { null },
                                profile.regionName,
                            ).joinToString(", ").ifBlank { "—" },
                        )
                        if (!profile.jerseySize.isNullOrBlank()) {
                            InfoRow("Размер джерси", profile.jerseySize)
                        }
                        if (!profile.coversSize.isNullOrBlank()) {
                            InfoRow("Размер чехлов", profile.coversSize)
                        }
                        if (!profile.gaitersSize.isNullOrBlank()) {
                            InfoRow("Размер гамаш", profile.gaitersSize)
                        }
                    }
                }
            }

            // --- Действия управляющего ---
            val targetIsOwner = details.role == MemberRole.OWNER
            if (ui.isManager && details.status == MemberStatus.ACTIVE) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        TextButton(onClick = { showJerseyDialog = true }) {
                            Text("Игровой номер")
                        }
                        if (ui.isOwner && !targetIsOwner) {
                            TextButton(onClick = {
                                viewModel.toggleModerator()
                            }) {
                                Text(
                                    if (details.role == MemberRole.MODERATOR) "Снять модератора"
                                    else "Назначить модератором",
                                )
                            }
                        }
                        if (ui.canManageTrainer && !targetIsOwner) {
                            TextButton(onClick = {
                                viewModel.toggleTrainer()
                            }) {
                                Text(
                                    if (details.isTrainer) "Снять тренера"
                                    else "Назначить тренером",
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJerseyDialog) {
        val details = ui.details
        if (details != null) {
            val member = ui.members.firstOrNull { it.userId == details.userId }
                ?: TeamMember(
                    id = 0L,
                    userId = details.userId,
                    phone = details.phone ?: "",
                    lastName = details.profile?.lastName ?: "",
                    firstName = details.profile?.firstName ?: "",
                    role = details.role,
                    status = details.status,
                    jerseyNumber = details.jerseyNumber,
                    isTrainer = details.isTrainer,
                )
            JerseyNumberDialog(
                member = member,
                taken = ui.members
                    .filter { it.userId != details.userId && it.jerseyNumber != null }
                    .associate { it.jerseyNumber!! to it.displayName },
                onDismiss = { showJerseyDialog = false },
                onSelect = { number ->
                    viewModel.setJerseyNumber(number)
                    showJerseyDialog = false
                },
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
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
