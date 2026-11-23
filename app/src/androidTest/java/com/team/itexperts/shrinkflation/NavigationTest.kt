package com.team.itexperts.shrinkflation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun calculateTabWorksAfterCatalog() {
        composeTestRule.onNodeWithContentDescription("Каталог").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Сравнить", substring = true).assertExists()

        composeTestRule.onNodeWithText("Расчёт").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Расчёт на единицу").assertExists()
    }

    @Test
    fun allTabsSwitchCorrectly() {
        val tabs = listOf(
            "Шринкфляция" to "Тип: Масса",
            "Сравнение" to "Сравнение цен за единицу",
            "Настройки" to "Язык",
            "Расчёт" to "Расчёт на единицу"
        )
        tabs.forEach { (tab, marker) ->
            composeTestRule.onNodeWithText(tab).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(marker).assertExists()
        }
    }
}
