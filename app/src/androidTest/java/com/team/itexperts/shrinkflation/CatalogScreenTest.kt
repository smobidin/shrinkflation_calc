package com.team.itexperts.shrinkflation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun catalogOpensFromComparisonAndShowsFilters() {
        composeTestRule.onNodeWithText("Сравнение").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Выбрать из каталога").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Каталог").assertExists()
        composeTestRule.onNodeWithText("Тип").assertExists()
        composeTestRule.onNodeWithText("Бренд").assertExists()
        composeTestRule.onNodeWithText("Специфика").assertExists()
        composeTestRule.onNodeWithText("Сравнить (0)").assertExists()
    }

    @Test
    fun selectionOpensComparisonWithCatalogData() {
        fun seed(name: String, price: String, qty: String) {
            composeTestRule.onAllNodesWithText("Товар")[0].performTextInput(name)
            composeTestRule.onNodeWithText("Цена").performTextInput(price)
            composeTestRule.onNodeWithText("Кол-во").performTextInput(qty)
            composeTestRule.onNodeWithText("Рассчитать").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Сброс").performClick()
            composeTestRule.waitForIdle()
        }

        seed("Каталог A", "100", "500")
        seed("Каталог B", "120", "400")

        composeTestRule.onNodeWithContentDescription("Каталог").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Каталог A", substring = true).assertExists()
        composeTestRule.onNodeWithTag("catalog_checkbox_Каталог A").performClick()
        composeTestRule.onNodeWithTag("catalog_checkbox_Каталог B").performClick()
        composeTestRule.onNodeWithText("Сравнить (2)").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Каталог A", substring = true).assertExists()
        composeTestRule.onNodeWithText("Каталог B", substring = true).assertExists()
    }

    @Test
    fun brandSuggestionDisappearsAfterSelection() {
        composeTestRule.onAllNodesWithText("Товар")[0].performTextInput("Паста")
        composeTestRule.onAllNodesWithText("Бренд")[0].performTextInput("Макфа")
        composeTestRule.onNodeWithText("Цена").performTextInput("61")
        composeTestRule.onNodeWithText("Кол-во").performTextInput("450")
        composeTestRule.onNodeWithText("Рассчитать").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Каталог").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Бренд").performTextInput("Макф")
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("filter_suggestion").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithTag("filter_suggestion")[0].performClick()
        composeTestRule.waitForIdle()

        check(composeTestRule.onAllNodesWithTag("filter_suggestion").fetchSemanticsNodes().isEmpty()) {
            "Подсказка осталась после выбора"
        }
    }

    @Test
    fun savedProductAppearsInCatalog() {
        // Seed one product through the unit-price screen
        composeTestRule.onAllNodesWithText("Товар")[0].performTextInput("Тестовый каталог")
        composeTestRule.onNodeWithText("Цена").performTextInput("100")
        composeTestRule.onNodeWithText("Кол-во").performTextInput("500")
        composeTestRule.onNodeWithText("Рассчитать").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Каталог").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Тестовый каталог", substring = true).assertExists()
        composeTestRule.onAllNodesWithTag("catalog_delete")[0].assertExists()
        composeTestRule.onAllNodesWithTag("catalog_edit")[0].assertExists()
    }
}
