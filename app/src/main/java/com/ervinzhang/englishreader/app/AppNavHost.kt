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
                authRepository = appContainer.authRepository,
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
                authRepository = appContainer.authRepository,
                onLogin = {
                    navController.navigate(Destinations.Bookshelf.route) {
                        popUpTo(Destinations.Login.route) { inclusive = true }
                    }
                },
                onNeedInvite = { phone ->
                    navController.navigate(Destinations.Invite.createRoute(phone))
                },
            )
        }

        composable(
            route = Destinations.Invite.route,
            arguments = listOf(navArgument(Destinations.Invite.phoneArg) { type = NavType.StringType }),
        ) { backStackEntry ->
            InviteScreen(
                phone = backStackEntry.arguments?.getString(Destinations.Invite.phoneArg).orEmpty(),
                authRepository = appContainer.authRepository,
                onBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(
                        Destinations.Nickname.createRoute(Destinations.Nickname.postRegisterEntry),
                    ) {
                        popUpTo(Destinations.Login.route)
                    }
                },
            )
        }

        composable(
            route = Destinations.Nickname.route,
            arguments = listOf(navArgument(Destinations.Nickname.entryPointArg) { type = NavType.StringType }),
        ) { backStackEntry ->
            val entryPoint =
                backStackEntry.arguments?.getString(Destinations.Nickname.entryPointArg).orEmpty()
            NicknameScreen(
                authRepository = appContainer.authRepository,
                entryPoint = entryPoint,
                onBack = { navController.popBackStack() },
                onSessionExpired = {
                    navController.navigate(Destinations.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onComplete = {
                    if (entryPoint == Destinations.Nickname.profileEntry) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Destinations.Bookshelf.route) {
                            popUpTo(Destinations.Login.route) { inclusive = true }
                        }
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
                authRepository = appContainer.authRepository,
                onBack = { navController.popBackStack() },
                onEditNickname = {
                    navController.navigate(
                        Destinations.Nickname.createRoute(Destinations.Nickname.profileEntry),
                    )
                },
                onLogout = {
                    navController.navigate(Destinations.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }
}
