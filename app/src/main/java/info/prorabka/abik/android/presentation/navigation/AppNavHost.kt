package info.prorabka.abik.android.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.prefs.SessionManager
import info.prorabka.abik.android.core.ui.components.EmptyState
import info.prorabka.abik.android.presentation.admin.AdminScreen
import info.prorabka.abik.android.presentation.auth.ForgotPasswordScreen
import info.prorabka.abik.android.presentation.auth.LoginScreen
import info.prorabka.abik.android.presentation.auth.RegisterScreen
import info.prorabka.abik.android.presentation.events.EventDetailsScreen
import info.prorabka.abik.android.presentation.events.EventFormScreen
import info.prorabka.abik.android.presentation.notifications.NotificationsScreen
import info.prorabka.abik.android.presentation.onboarding.OnboardingProfileScreen
import info.prorabka.abik.android.presentation.onboarding.OnboardingTeamScreen
import info.prorabka.abik.android.presentation.onboarding.ProfileFormScreen
import info.prorabka.abik.android.presentation.payment.PaymentFormScreen
import info.prorabka.abik.android.presentation.profile.ChangePhoneScreen
import info.prorabka.abik.android.presentation.reports.ReportsScreen
import info.prorabka.abik.android.presentation.roster.RosterScreen
import info.prorabka.abik.android.presentation.shell.MainShell
import info.prorabka.abik.android.presentation.splash.SplashScreen
import info.prorabka.abik.android.presentation.subscription.ChargeCreateScreen
import info.prorabka.abik.android.presentation.subscription.MemberFinanceScreen
import info.prorabka.abik.android.presentation.subscription.TemplateFormScreen
import info.prorabka.abik.android.presentation.subscription.TemplatesScreen
import info.prorabka.abik.android.presentation.team.MemberDetailsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    sessionManager: SessionManager,
    teamManager: CurrentTeamManager,
) {
    // Истёк refresh-токен — чистим командные данные и уходим на экран входа
    LaunchedEffect(Unit) {
        sessionManager.sessionExpired.collect {
            teamManager.clear()
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigate = { dest ->
                    navController.navigate(dest) { popUpTo(Routes.SPLASH) { inclusive = true } }
                },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.SPLASH) { popUpTo(0) { inclusive = true } }
                },
                onRegister = { navController.navigate(Routes.REGISTER) },
                onForgotPassword = { navController.navigate(Routes.FORGOT) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Routes.SPLASH) { popUpTo(0) { inclusive = true } }
                },
                onBackToLogin = { navController.popBackStack() },
            )
        }
        composable(Routes.FORGOT) {
            ForgotPasswordScreen(
                onPasswordChanged = {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onBackToLogin = { navController.popBackStack() },
            )
        }
        composable(Routes.ONBOARDING_PROFILE) {
            OnboardingProfileScreen(
                onDone = {
                    navController.navigate(Routes.SPLASH) { popUpTo(0) { inclusive = true } }
                },
            )
        }
        composable(Routes.ONBOARDING_TEAM) {
            OnboardingTeamScreen(
                onDone = {
                    navController.navigate(Routes.SPLASH) { popUpTo(0) { inclusive = true } }
                },
            )
        }
        composable(Routes.MAIN) {
            MainShell(rootNavController = navController)
        }
        composable(
            route = Routes.PAYMENT_FORM,
            arguments = listOf(
                navArgument("paymentId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("userId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            PaymentFormScreen(
                paymentId = entry.arguments?.getLong("paymentId") ?: -1L,
                preselectUserId = entry.arguments?.getLong("userId") ?: -1L,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.MEMBER_DETAILS,
            arguments = listOf(navArgument("userId") { type = NavType.LongType }),
        ) { entry ->
            MemberDetailsScreen(
                userId = entry.arguments?.getLong("userId") ?: 0L,
                onBack = { navController.popBackStack() },
                onEditProfile = { userId ->
                    navController.navigate(Routes.memberProfileEdit(userId))
                },
            )
        }
        composable(
            route = "member_profile_edit/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.LongType }),
        ) { entry ->
            ProfileFormScreen(
                title = "Профиль участника",
                submitText = "Сохранить",
                prefill = false,
                memberUserId = entry.arguments?.getLong("userId") ?: 0L,
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.ROSTER) {
            RosterScreen(
                onBack = { navController.popBackStack() },
                onCreateTeam = { navController.navigate(Routes.ONBOARDING_TEAM) },
            )
        }
        composable(
            route = Routes.EVENT_DETAILS,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType }),
        ) {
            EventDetailsScreen(
                onBack = { navController.popBackStack() },
                onEdit = { eventId -> navController.navigate(Routes.eventForm(eventId)) },
            )
        }
        composable(
            route = Routes.EVENT_FORM,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("date") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            EventFormScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.MEMBER_FINANCE,
            arguments = listOf(navArgument("userId") { type = NavType.LongType }),
        ) { entry ->
            MemberFinanceScreen(
                userId = entry.arguments?.getLong("userId") ?: 0L,
                onBack = { navController.popBackStack() },
                onEditPayment = { paymentId ->
                    navController.navigate(Routes.paymentForm(paymentId = paymentId))
                },
                onEditCharge = { chargeId ->
                    navController.navigate(Routes.chargeCreate(chargeId = chargeId))
                },
            )
        }
        composable(Routes.CHARGE_CREATE,
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("chargeId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            ChargeCreateScreen(
                preselectUserId = entry.arguments?.getLong("userId") ?: -1L,
                chargeId = entry.arguments?.getLong("chargeId") ?: -1L,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PROFILE_EDIT) {
            ProfileFormScreen(
                title = "Редактирование профиля",
                submitText = "Сохранить",
                prefill = true,
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.CHANGE_PHONE) {
            ChangePhoneScreen(
                onDone = {
                    // Сервер отозвал токены — на экран входа
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN) {
            AdminScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CHARGE_TEMPLATES) {
            TemplatesScreen(
                onCreateTemplate = { navController.navigate(Routes.templateForm()) },
                onEditTemplate = { templateId ->
                    navController.navigate(Routes.templateForm(templateId))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.TEMPLATE_FORM,
            arguments = listOf(
                navArgument("templateId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            TemplateFormScreen(
                templateId = entry.arguments?.getLong("templateId") ?: -1L,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
