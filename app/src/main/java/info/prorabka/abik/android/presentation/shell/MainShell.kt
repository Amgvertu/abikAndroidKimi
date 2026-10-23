package info.prorabka.abik.android.presentation.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.core.util.kopecksToRubles
import info.prorabka.abik.android.presentation.home.HomeScreen
import info.prorabka.abik.android.presentation.navigation.Routes
import info.prorabka.abik.android.presentation.profile.ProfileScreen
import info.prorabka.abik.android.presentation.subscription.SubscriptionScreen
import info.prorabka.abik.android.presentation.team.TeamScreen

/** Вкладки нижнего меню по ТЗ v3. */
enum class MainTab(val route: String, val title: String) {
    HOME("tab_home", "Главная"),
    SUBSCRIPTION("tab_subscription", "Абонемент"),
    GAMES("tab_games", "Игры"),
    TEAM("tab_team", "Команда"),
    PROFILE("tab_profile", "Профиль"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    rootNavController: NavHostController,
    viewModel: ShellViewModel = hiltViewModel(),
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val ui by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TeamSelector(ui, viewModel)
                        Spacer(modifier = Modifier.width(8.dp))
                        ui.myBalanceKopecks?.let { balance ->
                            Text(
                                text = balance.kopecksToRubles(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (balance < 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { rootNavController.navigate(Routes.NOTIFICATIONS) }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Уведомления")
                    }
                    ShellMenu(
                        onReports = { rootNavController.navigate(Routes.REPORTS) },
                        onTemplates = { rootNavController.navigate(Routes.CHARGE_TEMPLATES) },
                        onLogout = {
                            viewModel.logout {
                                rootNavController.navigate(Routes.LOGIN) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    MainTab.HOME -> Icons.Default.Home
                                    MainTab.SUBSCRIPTION -> Icons.Default.Star
                                    MainTab.GAMES -> Icons.Default.DateRange
                                    MainTab.TEAM -> Icons.Default.AccountCircle
                                    MainTab.PROFILE -> Icons.Default.Person
                                },
                                contentDescription = tab.title,
                            )
                        },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = tabNavController,
            startDestination = MainTab.HOME.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(MainTab.HOME.route) {
                HomeScreen(
                    shellViewModel = viewModel,
                    onAddPayment = { rootNavController.navigate(Routes.paymentForm()) },
                )
            }
            composable(MainTab.SUBSCRIPTION.route) {
                SubscriptionScreen(
                    onMemberClick = { userId ->
                        rootNavController.navigate(Routes.memberFinance(userId))
                    },
                    onAddPayment = { userId ->
                        rootNavController.navigate(Routes.paymentForm(userId = userId))
                    },
                    onAddCharge = { userId ->
                        rootNavController.navigate(Routes.chargeCreate(userId))
                    },
                )
            }
            composable(MainTab.GAMES.route) {
                Box(Modifier.fillMaxSize()) {
                    EmptyState("Игры появятся после расширения бэкенда")
                }
            }
            composable(MainTab.TEAM.route) {
                TeamScreen(
                    onMemberClick = { userId ->
                        rootNavController.navigate(Routes.memberDetails(userId))
                    },
                )
            }
            composable(MainTab.PROFILE.route) {
                ProfileScreen(
                    onEditProfile = { rootNavController.navigate(Routes.PROFILE_EDIT) },
                    onChangePhone = { rootNavController.navigate(Routes.CHANGE_PHONE) },
                    onAdmin = { rootNavController.navigate(Routes.ADMIN) },
                )
            }
        }
    }
}

/** Логотип + выпадающий список команд в тулбаре. */
@Composable
private fun TeamSelector(ui: ShellViewModel.UiState, viewModel: ShellViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(enabled = ui.teams.size > 1) { expanded = true },
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = ui.selected?.name ?: "…",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ui.teams.forEach { team ->
                DropdownMenuItem(
                    text = { Text(team.name) },
                    onClick = {
                        viewModel.selectTeam(team)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ShellMenu(onReports: () -> Unit, onTemplates: () -> Unit, onLogout: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Menu, contentDescription = "Меню")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Отчёты") },
                onClick = {
                    expanded = false
                    onReports()
                },
            )
            DropdownMenuItem(
                text = { Text("Шаблоны начислений") },
                onClick = {
                    expanded = false
                    onTemplates()
                },
            )
            DropdownMenuItem(
                text = { Text("Выйти") },
                onClick = {
                    expanded = false
                    onLogout()
                },
            )
        }
    }
}
