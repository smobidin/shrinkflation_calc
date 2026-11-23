package com.team.itexperts.shrinkflation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.team.itexperts.shrinkflation.MainActivity
import org.junit.Rule
import org.junit.Test

class UnitPriceScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches() {
        composeTestRule.onNodeWithText("Расчёт на единицу").assertExists()
    }

    @Test
    fun calculateShowsResult() {
        composeTestRule.onNodeWithTag("price").performTextInput("100")
        composeTestRule.onNodeWithTag("qty").performTextInput("500")
        composeTestRule.onNodeWithText("Рассчитать").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Цена за", substring = true).assertExists()
    }
}
