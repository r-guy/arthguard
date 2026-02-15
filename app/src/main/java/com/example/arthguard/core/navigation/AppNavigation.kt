package com.example.arthguard.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.arthguard.features.expense_breakup.presentation.screen.ExpenseBreakupScreen
import com.example.arthguard.features.main.presentation.screen.MainScreen

@Composable
fun AppNavigation(startDestination: Any = Main) {
    val backStack = remember { mutableStateListOf<Any>(startDestination) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                MainScreen(onNavigateToExpenseBreakup = { backStack.add(ExpenseBreakup) })
            }
            entry<ExpenseBreakup> {
                ExpenseBreakupScreen(onBack = { backStack.removeLastOrNull() })
            }
        }
    )
}
