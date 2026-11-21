package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.GAME_WINDOW_SIZE
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class PaletteRowViewTest : ViewsForTesting(windowSize = GAME_WINDOW_SIZE) {
    private val tappedSlots = mutableListOf<String>()

    private val slots = (0 until 10).map { index -> PaletteRowView.Slot(key = "unit-$index", bitmap = Bitmap32(16, 16, Colors.BLUE), imageName = "UNIT") }

    private fun newRowView() =
        PaletteRowView(rowWidth = 342.0).also { rowView ->
            rowView.display(slots) { key -> tappedSlots += key }
        }

    @Test
    fun `PaletteRowView should scroll the slots by the distance dragged when it is dragged sideways`() =
        sharedViewsTest {
            // Given
            val rowView = newRowView()
            addChild(rowView)
            // When
            dragPaletteRow(rowView, -100.0)
            // Then
            rowView.scroll shouldBe -100.0
        }

    @Test
    fun `PaletteRowView should not tap a slot when it is dragged more than 10 px`() =
        sharedViewsTest {
            // Given
            val rowView = newRowView()
            addChild(rowView)
            // When
            dragPaletteRow(rowView, -11.0)
            // Then
            tappedSlots.shouldBeEmpty()
            rowView.scroll shouldBe -11.0
        }

    @Test
    fun `PaletteRowView should tap the slot under the press when it moves 10 px or less`() =
        sharedViewsTest {
            // Given
            val rowView = newRowView()
            addChild(rowView)
            // When
            dragPaletteRow(rowView, -8.0)
            // Then
            tappedSlots.shouldContainExactly("unit-2")
            rowView.scroll shouldBe 0.0
        }

    @Test
    fun `PaletteRowView should stop at its last slot when it is dragged past it`() =
        sharedViewsTest {
            // Given
            val rowView = newRowView()
            addChild(rowView)
            // When
            dragPaletteRow(rowView, -2000.0)
            // Then
            rowView.scroll shouldBe 342.0 - (10 * 68.0 + 9 * 6.0)
            rowView.isSlotInView("unit-9") shouldBe true
            // When
            dragPaletteRow(rowView, 2000.0)
            // Then
            rowView.scroll shouldBe 0.0
        }

    @Test
    fun `ScrollTo should bring the slot into view when it is out of the row`() =
        sharedViewsTest {
            // Given
            val rowView = newRowView()
            addChild(rowView)
            rowView.isSlotInView("unit-7") shouldBe false
            // When
            rowView.scrollTo(7)
            // Then
            rowView.isSlotInView("unit-7") shouldBe true
            rowView.scroll shouldBe 342.0 - (8 * 68.0 + 7 * 6.0)
        }
}
