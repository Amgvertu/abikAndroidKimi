package info.prorabka.abik.android.presentation.subscription

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import info.prorabka.abik.android.core.ui.components.AbikPullToRefresh
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.core.ui.components.ErrorText
import info.prorabka.abik.android.core.ui.components.LoadingBox
import info.prorabka.abik.android.core.util.kopecksToRubles
import info.prorabka.abik.android.domain.model.ChargeDistribution
import info.prorabka.abik.android.domain.model.ChargeTemplate

/** Шаблоны систематических (ежемесячных) начислений команды. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onCreateTemplate: () -> Unit,
    onEditTemplate: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    info.prorabka.abik.android.presentation.guide.ScreenGuide("charge_templates")
    var deactivateTemplate by remember { mutableStateOf<ChargeTemplate?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Шаблоны начислений") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        floatingActionButton = {
            if (ui.isManager) {
                FloatingActionButton(onClick = onCreateTemplate) {
                    Icon(Icons.Default.Add, contentDescription = "Новый шаблон")
                }
            }
        },
    ) { padding ->
        AbikPullToRefresh(
            loading = ui.loading,
            onRefresh = viewModel::load,
        ) {
            when {
            ui.loading -> LoadingBox(Modifier.padding(padding))
            ui.templates.isEmpty() -> EmptyState(
                "Шаблонов пока нет.\nСоздайте шаблон, чтобы начисления формировались автоматически каждый месяц.",
                Modifier.padding(padding),
            )
            else -> LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(ui.templates, key = { it.id }) { template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        onClick = { if (ui.isManager) onEditTemplate(template.id) },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(template.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    buildString {
                                        append(ui.groups[template.groupId] ?: "Группа")
                                        append(" • ")
                                        append(template.amountKopecks.kopecksToRubles())
                                        template.dayOfMonth?.let { append(" • ${it}-е число") }
                                        if (template.distribution == ChargeDistribution.EQUAL) {
                                            append(" • поровну")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "Участников: ${template.memberIds.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = template.active,
                                onCheckedChange = {
                                    if (template.active) {
                                        // Отключение останавливает начисления — подтверждаем
                                        deactivateTemplate = template
                                    } else {
                                        viewModel.toggleActive(template)
                                    }
                                },
                                enabled = ui.isManager,
                            )
                        }
                    }
                }
            }
        }
        }
        ErrorText(ui.error)
    }

    // Подтверждение отключения шаблона
    deactivateTemplate?.let { template ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deactivateTemplate = null },
            title = { Text("Отключить шаблон?") },
            text = {
                Text(
                    "«${template.name}» перестанет формировать начисления. " +
                        "Уже созданные начисления сохранятся.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        deactivateTemplate = null
                        viewModel.toggleActive(template)
                    },
                ) { Text("Отключить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { deactivateTemplate = null },
                ) { Text("Отмена") }
            },
        )
    }
}
