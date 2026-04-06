package com.ervinzhang.englishreader.core.navigation

sealed class Destinations(val route: String) {
    data object Splash : Destinations("splash")
    data object Login : Destinations("login")
    data object Invite : Destinations("invite/{phone}") {
        const val phoneArg = "phone"

        fun createRoute(phone: String): String = "invite/$phone"
    }

    data object Nickname : Destinations("nickname/{entryPoint}") {
        const val entryPointArg = "entryPoint"
        const val postRegisterEntry = "post_register"
        const val profileEntry = "profile"

        fun createRoute(entryPoint: String): String = "nickname/$entryPoint"
    }

    data object Bookshelf : Destinations("bookshelf")
    data object Vocabulary : Destinations("vocabulary")
    data object Profile : Destinations("profile")

    data object Reader : Destinations("reader/{bookId}") {
        const val bookIdArg = "bookId"
        fun createRoute(bookId: String): String = "reader/$bookId"
    }
}
