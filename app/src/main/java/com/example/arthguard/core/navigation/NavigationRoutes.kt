package com.example.arthguard.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Main

@Serializable
data object ExpenseBreakup

sealed interface MainRoute {
    @Serializable
    data object Dashboard : MainRoute
    @Serializable
    data object Settings : MainRoute
}
