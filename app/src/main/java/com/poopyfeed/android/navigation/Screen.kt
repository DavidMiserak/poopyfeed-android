package com.poopyfeed.android.navigation

sealed class Screen(val route: String) {
    data object Greeting : Screen("greeting")

    data object Login : Screen("login")

    data object Signup : Screen("signup")

    data object ChildrenList : Screen("children_list")

    data object AddChild : Screen("children/add")

    data object EditChild : Screen("children/edit/{childId}") {
        fun createRoute(childId: Int) = "children/edit/$childId"
    }

    data object ChildDelete : Screen("children/delete/{childId}") {
        fun createRoute(childId: Int) = "children/delete/$childId"
    }

    data object ChildDashboard : Screen("child_dashboard/{childId}") {
        fun createRoute(childId: Int) = "child_dashboard/$childId"
    }

    data object FeedingsList : Screen("child_dashboard/{childId}/feedings") {
        fun createRoute(childId: Int) = "child_dashboard/$childId/feedings"
    }

    data object FeedingCreate : Screen("child_dashboard/{childId}/feedings/create") {
        fun createRoute(childId: Int) = "child_dashboard/$childId/feedings/create"
    }

    data object FeedingEdit : Screen("child_dashboard/{childId}/feedings/{feedingId}/edit") {
        fun createRoute(
            childId: Int,
            feedingId: Int,
        ) = "child_dashboard/$childId/feedings/$feedingId/edit"
    }

    data object FeedingDelete : Screen("child_dashboard/{childId}/feedings/{feedingId}/delete") {
        fun createRoute(
            childId: Int,
            feedingId: Int,
        ) = "child_dashboard/$childId/feedings/$feedingId/delete"
    }

    data object DiapersList : Screen("child_dashboard/{childId}/diapers") {
        fun createRoute(childId: Int) = "child_dashboard/$childId/diapers"
    }

    data object DiaperCreate : Screen("child_dashboard/{childId}/diapers/create") {
        fun createRoute(childId: Int) = "child_dashboard/$childId/diapers/create"
    }

    data object DiaperEdit : Screen("child_dashboard/{childId}/diapers/{diaperId}/edit") {
        fun createRoute(
            childId: Int,
            diaperId: Int,
        ) = "child_dashboard/$childId/diapers/$diaperId/edit"
    }

    data object DiaperDelete : Screen("child_dashboard/{childId}/diapers/{diaperId}/delete") {
        fun createRoute(
            childId: Int,
            diaperId: Int,
        ) = "child_dashboard/$childId/diapers/$diaperId/delete"
    }

    data object NapsList : Screen("child_dashboard/{childId}/naps") {
        fun createRoute(childId: Int) = "child_dashboard/$childId/naps"
    }

    data object NapCreate : Screen("child_dashboard/{childId}/naps/create") {
        fun createRoute(childId: Int) = "child_dashboard/$childId/naps/create"
    }

    data object NapEdit : Screen("child_dashboard/{childId}/naps/{napId}/edit") {
        fun createRoute(
            childId: Int,
            napId: Int,
        ) = "child_dashboard/$childId/naps/$napId/edit"
    }

    data object NapDelete : Screen("child_dashboard/{childId}/naps/{napId}/delete") {
        fun createRoute(
            childId: Int,
            napId: Int,
        ) = "child_dashboard/$childId/naps/$napId/delete"
    }

    data object Export : Screen("child_dashboard/{childId}/analytics/export") {
        fun createRoute(childId: Int) = "child_dashboard/$childId/analytics/export"
    }

    data object Sharing : Screen("child_dashboard/{childId}/sharing") {
        fun createRoute(childId: Int) = "child_dashboard/$childId/sharing"
    }

    data object AcceptInvite : Screen("invites/accept/{token}") {
        fun createRoute(token: String) = "invites/accept/$token"
    }

    data object Home : Screen("home")

    data object Profile : Screen("profile")
}
