package com.ervinzhang.englishreader.core.navigation

sealed class Destinations(val route: String) {
    data object Splash : Destinations("splash")
    data object Login : Destinations("login")
    data object Invite : Destinations("invite")
    data object Nickname : Destinations("nickname")
    data object Bookshelf : Destinations("bookshelf")
    data object Vocabulary : Destinations("vocabulary")
    data object Profile : Destinations("profile")

    data object Reader : Destinations("reader/{bookId}") {
        const val bookIdArg = "bookId"
        fun createRoute(bookId: String): String = "reader/$bookId"
    }
}
