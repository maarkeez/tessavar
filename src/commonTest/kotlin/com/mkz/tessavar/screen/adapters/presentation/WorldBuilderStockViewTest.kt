package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.clickOn
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class WorldBuilderStockViewTest : ViewsForTesting() {
    private val changes = mutableListOf<Pair<String, List<String>>>()
    private val delegate =
        object : WorldBuilderStockView.Delegate {
            override fun stockChanged(
                npcId: String,
                stock: List<String>,
            ) {
                changes += npcId to stock
            }
        }
    private val items =
        listOf("health-potion", "mana-potion", "hammer").map { lootableItemId ->
            WorldBuilderStockView.StockItem(lootableItemId = lootableItemId, name = lootableItemId, bitmap = Bitmap32(16, 16, Colors.RED))
        }

    private fun newStockView() = WorldBuilderStockView(MessagesMother.messages()).also { stockView -> stockView.setDelegate(delegate) }

    private fun WorldBuilderStockView.stockedItems(): List<String> = itemButtons.filterValues { button -> button.bgColorOut == WorldBuilderHudView.SELECTED_TOOL_COLOR }.keys.toList()

    @Test
    fun `Open should display every item, the ones the npc sells highlighted, under its name`() =
        sharedViewsTest {
            // Given
            val stockView = newStockView()
            // When
            stockView.open(npcId = "alchemist", npcName = "Alchemist", items = items, stock = listOf("mana-potion"))
            // Then
            stockView.isOpen shouldBe true
            stockView.titleText.text shouldBe "Alchemist sells"
            stockView.itemButtons.keys
                .toList()
                .shouldContainExactly("health-potion", "mana-potion", "hammer")
            stockView.stockedItems().shouldContainExactly("mana-potion")
        }

    @Test
    fun `Done should keep the items toggled, in the order they were added, and close it`() =
        sharedViewsTest {
            // Given
            val stockView = newStockView()
            stockView.open(npcId = "alchemist", npcName = "Alchemist", items = items, stock = listOf("mana-potion"))
            addChild(stockView)
            clickOn(stockView.itemButtons.getValue("hammer"))
            clickOn(stockView.itemButtons.getValue("health-potion"))
            clickOn(stockView.itemButtons.getValue("mana-potion"))
            // When
            clickOn(stockView.doneButton)
            // Then
            changes.shouldContainExactly("alchemist" to listOf("hammer", "health-potion"))
            stockView.isOpen shouldBe false
        }
}
