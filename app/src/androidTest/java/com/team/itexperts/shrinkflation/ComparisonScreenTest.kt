package com.team.itexperts.shrinkflation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComparisonScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun comparisonScreen_calculates() {
        composeTestRule.onNodeWithText("Сравнение").performClick()
        composeTestRule.waitForIdle()

        // First item
        composeTestRule.onAllNodesWithText("Товар")[0].performTextInput("A")
        composeTestRule.onAllNodesWithText("Цена")[0].performTextInput("100")
        composeTestRule.onAllNodesWithText("Кол-во")[0].performTextInput("500")

        // Second item
        composeTestRule.onAllNodesWithText("Товар")[1].performTextInput("B")
        composeTestRule.onAllNodesWithText("Цена")[1].performTextInput("120")
        composeTestRule.onAllNodesWithText("Кол-во")[1].performTextInput("400")

        composeTestRule.onNodeWithText("Сравнить").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify result shows a friendly per-kg price (100 / 500 g = 200 per kg)
        composeTestRule.onNodeWithText("200", substring = true).assertExists()
    }
}
