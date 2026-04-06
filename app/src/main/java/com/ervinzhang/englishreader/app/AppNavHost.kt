package com.ervinzhang.englishreader.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ervinzhang.englishreader.core.navigation.Destinations
import com.ervinzhang.englishreader.feature.auth.ui.InviteScreen
import com.ervinzhang.englishreader.feature.auth.ui.LoginScreen
import com.ervinzhang.englishreader.feature.auth.ui.NicknameScreen
import com.ervinzhang.englishreader.feature.auth.ui.SplashScreen
import com.ervinzhang.englishreader.feature.bookshelf.ui.BookshelfScreen
import com.ervinzhang.englishreader.feature.profile.ui.ProfileScreen
import com.ervinzhang.englishreader.feature.reader.ui.ReaderScreen
import com.ervinzhang.englishreader.feature.vocabulary.ui.VocabularyScreen

@Composable
fun AppNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.Splash.route,
        modifier = modifier,
    ) {
        composable(Destinations.Splash.route) {
            SplashScreen(
                sessionStore = appContainer.sessionStore,
                onLoggedIn = {
                    navController.navigate(Destinations.Bookshelf.route) {
                        popUpTo(Destinations.Splash.route) { inclusive = true }
                    }
                },
                onLoggedOut = {
                    navController.navigate(Destinations.Login.route) {
                        popUpTo(Destinations.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Destinations.Login.route) {
            LoginScreen(
                sessionStore = appContainer.sessionStore,
                onLogin = {
                    navController.navigate(Destinations.Bookshelf.route) {
                        popUpTo(Destinations.Login.route) { inclusive = true }
                    }
                },
                onNeedInvite = {
                    navController.navigate(Destinations.Invite.route)
                },
            )
        }

        composable(Destinations.Invite.route) {
            InviteScreen(
                onRegisterSuccess = {
                    navController.navigate(Destinations.Nickname.route) {
                        popUpTo(Destinations.Invite.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Destinations.Nickname.route) {
            NicknameScreen(
                sessionStore = appContainer.sessionStore,
                onComplete = {
                    navController.navigate(Destinations.Bookshelf.route) {
                        popUpTo(Destinations.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Destinations.Bookshelf.route) {
            BookshelfScreen(
                onOpenBook = { bookId ->
                    navController.navigate(Destinations.Reader.createRoute(bookId))
                },
                onOpenProfile = {
                    navController.navigate(Destinations.Profile.route)
                },
            )
        }

        composable(
            route = Destinations.Reader.route,
            arguments = listOf(navArgument(Destinations.Reader.bookIdArg) { type = NavType.StringType }),
        ) { backStackEntry ->
            ReaderScreen(
                bookId = backStackEntry.arguments?.getString(Destinations.Reader.bookIdArg).orEmpty(),
                onBack = { navController.popBackStack() },
                onOpenVocabulary = { navController.navigate(Destinations.Vocabulary.route) },
            )
        }

        composable(Destinations.Vocabulary.route) {
            VocabularyScreen(onBack = { navController.popBackStack() })
        }

        composable(Destinations.Profile.route) {
            ProfileScreen(
                sessionStore = appContainer.sessionStore,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Destinations.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }
}
