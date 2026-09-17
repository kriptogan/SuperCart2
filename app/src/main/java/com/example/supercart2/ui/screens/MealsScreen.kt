package com.example.supercart2.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Sub-screens of the Meals tab: list → prepare (a meal's checklist) ⇄ edit (null id = new meal). */
internal sealed class MealsSubScreen {
    object List : MealsSubScreen()
    data class Prepare(val mealId: String) : MealsSubScreen()
    data class Edit(val mealId: String?) : MealsSubScreen() {
        /** Cancelling returns to the list for a new meal and to the meal's prepare view otherwise. */
        fun cancelTarget(): MealsSubScreen = mealId?.let(::Prepare) ?: List
    }
}

/** Builds navigation callbacks that switch the current sub-screen through [go]. */
private class MealsScreenNavigator(val go: (MealsSubScreen) -> Unit) {
    fun to(target: MealsSubScreen): () -> Unit = { go(target) }
    fun toPrepare(): (String) -> Unit = { mealId -> go(MealsSubScreen.Prepare(mealId)) }
    fun toEdit(): (String) -> Unit = { mealId -> go(MealsSubScreen.Edit(mealId)) }
}

@Composable
fun MealsScreen() {
    var subScreen by remember { mutableStateOf<MealsSubScreen>(MealsSubScreen.List) }
    MealsScreenSubScreen(subScreen, MealsScreenNavigator { subScreen = it })
}

@Composable
private fun MealsScreenSubScreen(screen: MealsSubScreen, nav: MealsScreenNavigator) {
    when (screen) {
        is MealsSubScreen.List -> MealsListView(nav.to(MealsSubScreen.Edit(null)), nav.toPrepare(), nav.toEdit())
        is MealsSubScreen.Prepare ->
            MealsPrepareView(screen.mealId, nav.to(MealsSubScreen.List), nav.to(MealsSubScreen.Edit(screen.mealId)))
        is MealsSubScreen.Edit -> MealsEditorView(screen.mealId, nav.to(screen.cancelTarget()), nav.toPrepare())
    }
}
