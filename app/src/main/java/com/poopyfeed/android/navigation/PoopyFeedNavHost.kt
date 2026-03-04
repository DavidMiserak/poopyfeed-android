package com.poopyfeed.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.poopyfeed.android.ui.screens.HomeScreen
import com.poopyfeed.android.ui.screens.catchup.CatchUpScreen
import com.poopyfeed.android.ui.screens.advancedtools.AdvancedToolsScreen
import com.poopyfeed.android.ui.screens.children.ChildDashboardScreen
import com.poopyfeed.android.ui.screens.children.ChildDeleteScreen
import com.poopyfeed.android.ui.screens.children.ChildFormScreen
import com.poopyfeed.android.ui.screens.children.ChildrenListScreen
import com.poopyfeed.android.ui.screens.diapers.DiaperDeleteScreen
import com.poopyfeed.android.ui.screens.diapers.DiaperFormScreen
import com.poopyfeed.android.ui.screens.diapers.DiapersListScreen
import com.poopyfeed.android.ui.screens.export.ExportScreen
import com.poopyfeed.android.ui.screens.feedings.FeedingDeleteScreen
import com.poopyfeed.android.ui.screens.pediatrician.PediatricianSummaryScreen
import com.poopyfeed.android.ui.screens.fussbus.FussBusScreen
import com.poopyfeed.android.ui.screens.feedings.FeedingFormScreen
import com.poopyfeed.android.ui.screens.feedings.FeedingsListScreen
import com.poopyfeed.android.ui.screens.greeting.GreetingScreen
import com.poopyfeed.android.ui.screens.invite.AcceptInviteScreen
import com.poopyfeed.android.ui.screens.login.LoginScreen
import com.poopyfeed.android.ui.screens.naps.NapDeleteScreen
import com.poopyfeed.android.ui.screens.naps.NapFormScreen
import com.poopyfeed.android.ui.screens.naps.NapsListScreen
import com.poopyfeed.android.ui.screens.profile.ProfileScreen
import com.poopyfeed.android.ui.screens.sharing.SharingScreen
import com.poopyfeed.android.ui.screens.signup.SignupScreen
import com.poopyfeed.android.ui.screens.timeline.TimelineScreen

@Composable
fun PoopyFeedNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    initialInviteToken: String? = null,
) {
    LaunchedEffect(initialInviteToken) {
        if (!initialInviteToken.isNullOrBlank()) {
            navController.navigate(Screen.AcceptInvite.createRoute(initialInviteToken)) {
                popUpTo(Screen.Greeting.route) { inclusive = true }
            }
        }
    }
    NavHost(
        navController = navController,
        startDestination = Screen.Greeting.route,
        modifier = modifier,
    ) {
        composable(Screen.Greeting.route) {
            GreetingScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.ChildrenList.route) {
                        popUpTo(Screen.Greeting.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Screen.ChildrenList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onSignupSuccess = {
                    navController.navigate(Screen.ChildrenList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.ChildrenList.route) {
            ChildrenListScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToChildDashboard = { childId ->
                    navController.navigate(Screen.ChildDashboard.createRoute(childId))
                },
                onNavigateToAddChild = {
                    navController.navigate(Screen.AddChild.route)
                },
            )
        }

        composable(Screen.AddChild.route) {
            ChildFormScreen(
                onNavigateBack = { navController.navigateUp() },
                onSuccess = { navController.navigateUp() },
            )
        }

        composable(
            route = Screen.EditChild.route,
            arguments = listOf(navArgument("childId") { type = androidx.navigation.NavType.StringType }),
        ) {
            ChildFormScreen(
                onNavigateBack = { navController.navigateUp() },
                onSuccess = { navController.navigateUp() },
            )
        }

        composable(
            route = Screen.ChildDelete.route,
            arguments = listOf(navArgument("childId") { type = androidx.navigation.NavType.StringType }),
        ) {
            ChildDeleteScreen(
                onNavigateBack = { navController.navigateUp() },
                onDeleteSuccess = {
                    navController.navigate(Screen.ChildrenList.route) {
                        popUpTo(Screen.ChildrenList.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Screen.ChildDashboard.route,
            arguments = listOf(navArgument("childId") { type = androidx.navigation.NavType.StringType }),
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId")?.toIntOrNull() ?: 0
            ChildDashboardScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToEditChild = {
                    navController.navigate(Screen.EditChild.createRoute(childId))
                },
                onNavigateToDeleteChild = {
                    navController.navigate(Screen.ChildDelete.createRoute(childId))
                },
                onNavigateToAddFeeding = { navController.navigate(Screen.FeedingCreate.createRoute(childId)) },
                onNavigateToAddDiaper = { navController.navigate(Screen.DiaperCreate.createRoute(childId)) },
                onNavigateToAddNap = { navController.navigate(Screen.NapCreate.createRoute(childId)) },
                onNavigateToFeedingsList = { navController.navigate(Screen.FeedingsList.createRoute(childId)) },
                onNavigateToDiapersList = { navController.navigate(Screen.DiapersList.createRoute(childId)) },
                onNavigateToNapsList = { navController.navigate(Screen.NapsList.createRoute(childId)) },
                onNavigateToCatchUp = { navController.navigate(Screen.CatchUp.createRoute(childId)) },
                onNavigateToTimeline = { navController.navigate(Screen.Timeline.createRoute(childId)) },
                onNavigateToSharing = {
                    navController.navigate(Screen.Sharing.createRoute(childId))
                },
                onNavigateToExport = {
                    navController.navigate(Screen.Export.createRoute(childId))
                },
                onNavigateToAdvancedTools = {
                    navController.navigate(Screen.AdvancedTools.createRoute(childId))
                },
                onNavigateToFussBus = {
                    navController.navigate(Screen.FussBus.createRoute(childId))
                },
            )
        }

        composable(
            route = Screen.CatchUp.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) {
            CatchUpScreen(
                onNavigateBack = { navController.navigateUp() },
                onSubmitSuccess = { navController.navigateUp() },
            )
        }

        composable(
            route = Screen.Timeline.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) {
            TimelineScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.Export.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) {
            ExportScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.PediatricianSummary.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) {
            PediatricianSummaryScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.AdvancedTools.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) { entry ->
            val cid = entry.arguments?.getString("childId")?.toIntOrNull() ?: 0
            AdvancedToolsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToExport = { navController.navigate(Screen.Export.createRoute(cid)) },
                onNavigateToPediatricianSummary = { navController.navigate(Screen.PediatricianSummary.createRoute(cid)) },
                onNavigateToFussBus = { navController.navigate(Screen.FussBus.createRoute(cid)) },
            )
        }

        composable(
            route = Screen.FussBus.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) {
            FussBusScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.Sharing.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) {
            SharingScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.AcceptInvite.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType }),
        ) { entry ->
            val token = entry.arguments?.getString("token") ?: ""
            AcceptInviteScreen(
                token = token,
                onAcceptSuccess = {
                    navController.navigate(Screen.ChildrenList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChildren = {
                    navController.navigate(Screen.ChildrenList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Screen.FeedingsList.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) { entry ->
            val cid = entry.arguments?.getString("childId")?.toIntOrNull() ?: 0
            FeedingsListScreen(
                childId = cid,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToCreate = { navController.navigate(Screen.FeedingCreate.createRoute(cid)) },
                onNavigateToEdit = { fid -> navController.navigate(Screen.FeedingEdit.createRoute(cid, fid)) },
                onNavigateToDelete = { fid -> navController.navigate(Screen.FeedingDelete.createRoute(cid, fid)) },
            )
        }
        composable(
            route = Screen.FeedingCreate.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) {
            FeedingFormScreen(
                onNavigateBack = { navController.navigateUp() },
                onSuccess = { navController.navigateUp() },
            )
        }
        composable(
            route = Screen.FeedingEdit.route,
            arguments =
                listOf(
                    navArgument("childId") { type = NavType.StringType },
                    navArgument("feedingId") { type = NavType.StringType },
                ),
        ) {
            FeedingFormScreen(
                onNavigateBack = { navController.navigateUp() },
                onSuccess = { navController.navigateUp() },
            )
        }
        composable(
            route = Screen.FeedingDelete.route,
            arguments =
                listOf(
                    navArgument("childId") { type = NavType.StringType },
                    navArgument("feedingId") { type = NavType.StringType },
                ),
        ) {
            FeedingDeleteScreen(
                onNavigateBack = { navController.navigateUp() },
                onDeleteSuccess = { navController.navigateUp() },
            )
        }

        composable(
            route = Screen.DiapersList.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) { entry ->
            val cid = entry.arguments?.getString("childId")?.toIntOrNull() ?: 0
            DiapersListScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToCreate = { navController.navigate(Screen.DiaperCreate.createRoute(cid)) },
                onNavigateToEdit = { did -> navController.navigate(Screen.DiaperEdit.createRoute(cid, did)) },
                onNavigateToDelete = { did -> navController.navigate(Screen.DiaperDelete.createRoute(cid, did)) },
            )
        }
        composable(
            route = Screen.DiaperCreate.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) {
            DiaperFormScreen(
                onNavigateBack = { navController.navigateUp() },
                onSuccess = { navController.navigateUp() },
            )
        }
        composable(
            route = Screen.DiaperEdit.route,
            arguments =
                listOf(
                    navArgument("childId") { type = NavType.StringType },
                    navArgument("diaperId") { type = NavType.StringType },
                ),
        ) {
            DiaperFormScreen(
                onNavigateBack = { navController.navigateUp() },
                onSuccess = { navController.navigateUp() },
            )
        }
        composable(
            route = Screen.DiaperDelete.route,
            arguments =
                listOf(
                    navArgument("childId") { type = NavType.StringType },
                    navArgument("diaperId") { type = NavType.StringType },
                ),
        ) {
            DiaperDeleteScreen(
                onNavigateBack = { navController.navigateUp() },
                onDeleteSuccess = { navController.navigateUp() },
            )
        }

        composable(
            route = Screen.NapsList.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) { entry ->
            val cid = entry.arguments?.getString("childId")?.toIntOrNull() ?: 0
            NapsListScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToCreate = { navController.navigate(Screen.NapCreate.createRoute(cid)) },
                onNavigateToEdit = { nid -> navController.navigate(Screen.NapEdit.createRoute(cid, nid)) },
                onNavigateToDelete = { nid -> navController.navigate(Screen.NapDelete.createRoute(cid, nid)) },
            )
        }
        composable(
            route = Screen.NapCreate.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) {
            NapFormScreen(
                onNavigateBack = { navController.navigateUp() },
                onSuccess = { navController.navigateUp() },
            )
        }
        composable(
            route = Screen.NapEdit.route,
            arguments =
                listOf(
                    navArgument("childId") { type = NavType.StringType },
                    navArgument("napId") { type = NavType.StringType },
                ),
        ) {
            NapFormScreen(
                onNavigateBack = { navController.navigateUp() },
                onSuccess = { navController.navigateUp() },
            )
        }
        composable(
            route = Screen.NapDelete.route,
            arguments =
                listOf(
                    navArgument("childId") { type = NavType.StringType },
                    navArgument("napId") { type = NavType.StringType },
                ),
        ) {
            NapDeleteScreen(
                onNavigateBack = { navController.navigateUp() },
                onDeleteSuccess = { navController.navigateUp() },
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateBack = {
                    navController.navigateUp()
                },
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onLogout = {
                    navController.navigate(Screen.Greeting.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDeleteSuccess = {
                    navController.navigate(Screen.Greeting.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
