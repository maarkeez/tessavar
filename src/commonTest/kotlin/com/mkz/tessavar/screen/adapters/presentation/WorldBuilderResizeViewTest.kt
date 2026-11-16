package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class WorldBuilderResizeViewTest : ViewsForTesting() {
    private val sizes = mutableListOf<Pair<Int, Int>>()
    private val delegate =
        object : WorldBuilderResizeView.Delegate {
            override fun sizeChanged(
                rows: Int,
                columns: Int,
            ) {
                sizes += rows to columns
            }
        }

    private fun newResizeView() = WorldBuilderResizeView(MessagesMother.messages()).also { resizeView -> resizeView.setDelegate(delegate) }

    @Test
    fun `WorldBuilderResizeView should be closed when it is created`() =
        sharedViewsTest {
            // When
            val resizeView = newResizeView()
            // Then
            resizeView.isOpen shouldBe false
            resizeView.titleText.text shouldBe "Resize the world"
        }

    @Test
    fun `DisplaySize should display the rows and the columns when the size is displayed`() =
        sharedViewsTest {
            // Given
            val resizeView = newResizeView()
            // When
            resizeView.displaySize(rows = 12, columns = 40, allowedSizes = 1..128)
            // Then
            resizeView.rowsText() shouldBe "12"
            resizeView.columnsText() shouldBe "40"
        }

    @Test
    fun `DisplaySize should disable the steps out of 1 to 128 when the size is close to the limits`() =
        sharedViewsTest {
            // Given
            val resizeView = newResizeView()
            // When
            resizeView.displaySize(rows = 5, columns = 124, allowedSizes = 1..128)
            // Then
            resizeView.enabledRowsSteps().shouldContainExactly(-1, 1, 8)
            resizeView.enabledColumnsSteps().shouldContainExactly(-8, -1, 1)
            resizeView.fadedRowsSteps().shouldContainExactly(-8)
            resizeView.fadedColumnsSteps().shouldContainExactly(8)
        }

    @Test
    fun `DisplaySize should disable the steps out of the allowed sizes when the allowed sizes are 4 to 20`() =
        sharedViewsTest {
            // Given
            val resizeView = newResizeView()
            // When
            resizeView.displaySize(rows = 11, columns = 18, allowedSizes = 4..20)
            // Then
            resizeView.enabledRowsSteps().shouldContainExactly(-1, 1, 8)
            resizeView.enabledColumnsSteps().shouldContainExactly(-8, -1, 1)
        }

    @Test
    fun `RowsStep should ask for the size with the rows stepped when a rows step is clicked`() =
        sharedViewsTest {
            // Given
            val resizeView = newResizeView()
            resizeView.displaySize(rows = 16, columns = 20, allowedSizes = 1..128)
            resizeView.open()
            addChild(resizeView)
            // When
            clickRowsStep(resizeView, step = 8)
            // Then
            sizes.shouldContainExactly(24 to 20)
        }

    @Test
    fun `ColumnsStep should ask for the size with the columns stepped when a columns step is clicked`() =
        sharedViewsTest {
            // Given
            val resizeView = newResizeView()
            resizeView.displaySize(rows = 16, columns = 20, allowedSizes = 1..128)
            resizeView.open()
            addChild(resizeView)
            // When
            clickColumnsStep(resizeView, step = -1)
            // Then
            sizes.shouldContainExactly(16 to 19)
        }

    @Test
    fun `Done should close the dialog when it is clicked`() =
        sharedViewsTest {
            // Given
            val resizeView = newResizeView()
            resizeView.open()
            addChild(resizeView)
            // When
            clickDone(resizeView)
            // Then
            resizeView.isOpen shouldBe false
        }
}
