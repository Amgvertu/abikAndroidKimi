package info.prorabka.abik.android.presentation.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.core.ui.theme.Danger
import info.prorabka.abik.android.core.ui.theme.Success
import info.prorabka.abik.android.core.util.kopecksToRubles
import info.prorabka.abik.android.presentation.events.EventsScreen
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

    // Возврат с форм (платёж/начисление/профиль) — обновить балансы и бейдж
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        viewModel.refreshUnreadCount()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TeamSelector(
                        ui = ui,
                        viewModel = viewModel,
                        onCreateTeam = { rootNavController.navigate(Routes.ONBOARDING_TEAM) },
                    )
                },
                actions = {
                    ui.myBalanceKopecks?.let { balance -> BalanceChip(balance) }
                    IconButton(onClick = { rootNavController.navigate(Routes.NOTIFICATIONS) }) {
                        BadgedBox(
                            badge = {
                                if (ui.unreadNotifications > 0) {
                                    Badge { Text(ui.unreadNotifications.toString()) }
                                }
                            },
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Уведомления")
                        }
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
                                    MainTab.SUBSCRIPTION -> Icons.Outlined.Email
                                    MainTab.GAMES -> Icons.Default.DateRange
                                    MainTab.TEAM -> Icons.Outlined.Person
                                    MainTab.PROFILE -> Icons.Default.Person
                                },
                                contentDescription = tab.title,
                            )
                        },
                        label = { Text(tab.title, fontSize = 10.sp) },
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
                    onCreateTeam = { rootNavController.navigate(Routes.ONBOARDING_TEAM) },
                    onEventClick = { eventId ->
                        rootNavController.navigate(Routes.eventDetails(eventId))
                    },
                    onNotifications = { rootNavController.navigate(Routes.NOTIFICATIONS) },
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
                    onCreateTeam = { rootNavController.navigate(Routes.ONBOARDING_TEAM) },
                )
            }
            composable(MainTab.GAMES.route) {
                EventsScreen(
                    onEventClick = { eventId ->
                        rootNavController.navigate(Routes.eventDetails(eventId))
                    },
                    onCreate = { rootNavController.navigate(Routes.eventForm()) },
                    onCreateTeam = { rootNavController.navigate(Routes.ONBOARDING_TEAM) },
                )
            }
            composable(MainTab.TEAM.route) {
                TeamScreen(
                    onMemberClick = { userId ->
                        rootNavController.navigate(Routes.memberDetails(userId))
                    },
                    onCreateTeam = { rootNavController.navigate(Routes.ONBOARDING_TEAM) },
                    onRoster = { rootNavController.navigate(Routes.ROSTER) },
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
private fun TeamSelector(
    ui: ShellViewModel.UiState,
    viewModel: ShellViewModel,
    onCreateTeam: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        val logoUrl = viewModel.resolveUrl(ui.selected?.logoUrl)
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "Логотип",
                modifier = Modifier.size(32.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(
                    text = ui.selected?.name ?: "Нет команды",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(" ▾", fontSize = 12.sp)
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
                if (ui.teams.isNotEmpty()) {
                    HorizontalDivider()
                }
                DropdownMenuItem(
                    text = { Text("Создать команду") },
                    onClick = {
                        expanded = false
                        onCreateTeam()
                    },
                )
            }
        }
    }
}

/** Баланс-плашка в тулбаре (как в прототипе): «+1 500 ₽» / «−500 ₽». */
@Composable
private fun BalanceChip(balanceKopecks: Long) {
    val color = if (balanceKopecks < 0) Danger else Success
    val sign = if (balanceKopecks < 0) "−" else "+"
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = sign + kotlin.math.abs(balanceKopecks).kopecksToRubles(),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
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
