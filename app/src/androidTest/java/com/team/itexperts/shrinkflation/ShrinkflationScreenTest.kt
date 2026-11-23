package com.team.itexperts.shrinkflation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.team.itexperts.shrinkflation.MainActivity
import org.junit.Rule
import org.junit.Test

class ShrinkflationScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches() {
        composeTestRule.onAllNodesWithText("Расчёт")[0].assertExists()
    }

    @Test
    fun calculateShowsResult() {
        composeTestRule.onNodeWithText("Шринкфляция").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("priceOld").performTextInput("100")
        composeTestRule.onNodeWithTag("qtyOld").performTextInput("1000")
        composeTestRule.onNodeWithTag("priceNew").performTextInput("100")
        composeTestRule.onNodeWithTag("qtyNew").performTextInput("900")
        composeTestRule.onNodeWithTag("calculateBtn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("старая", substring = true).assertExists()
    }
}
