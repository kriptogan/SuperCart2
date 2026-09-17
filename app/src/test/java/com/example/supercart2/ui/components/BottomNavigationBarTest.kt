package com.example.supercart2.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.supercart2.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BottomNavigationBarTest {

    @get:Rule
    val compose = createComposeRule()

    private val routes = mutableListOf<String>()
    private var route by mutableStateOf("home")

    private fun show(initial: String) {
        route = initial
        compose.setContent { BottomNavigationBar(currentRoute = route, onRouteChange = { routes += it }) }
    }

    @Test
    fun `shows Home then Stores with labels and icon descriptions`() {
        show("home")
        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithText("Stores").assertIsDisplayed()
        compose.onNodeWithContentDescription("Home", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Stores", useUnmergedTree = true).assertExists()
        val home = compose.onNodeWithText("Home").fetchSemanticsNode().boundsInRoot
        val stores = compose.onNodeWithText("Stores").fetchSemanticsNode().boundsInRoot
        assert(home.left < stores.left)
    }

    @Test
    fun `shows Meals after Stores, selected by its route, and reports it when clicked`() {
        show("meals")
        compose.onNodeWithText("Meals").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithContentDescription("Meals", useUnmergedTree = true).assertExists()
        val stores = compose.onNodeWithText("Stores").fetchSemanticsNode().boundsInRoot
        val meals = compose.onNodeWithText("Meals").fetchSemanticsNode().boundsInRoot
        assert(stores.left < meals.left)
        compose.onNodeWithText("Stores").performClick()
        compose.onNodeWithText("Meals").performClick()
        assertEquals(listOf("stores", "meals"), routes)
    }

    @Test
    fun `selection follows currentRoute`() {
        show("home")
        compose.onNodeWithText("Home").assertIsSelected()
        compose.onNodeWithText("Stores").assertIsNotSelected()
        route = "stores"
        compose.onNodeWithText("Home").assertIsNotSelected()
        compose.onNodeWithText("Stores").assertIsSelected()
    }

    @Test
    fun `unknown route selects nothing`() {
        show("other")
        compose.onNodeWithText("Home").assertIsNotSelected()
        compose.onNodeWithText("Stores").assertIsNotSelected()
    }

    @Test
    fun `clicking items reports their routes, including the already selected one`() {
        show("home")
        compose.onNodeWithText("Stores").performClick()
        compose.onNodeWithText("Home").performClick()
        assertEquals(listOf("stores", "home"), routes)
    }

    @Test
    fun `NavigationItem is a plain data holder`() {
        val item = NavigationItem(route = "home", titleRes = R.string.nav_home, icon = R.drawable.ic_home)
        assertEquals(NavigationItem("home", R.string.nav_home, R.drawable.ic_home), item)
    }
}
