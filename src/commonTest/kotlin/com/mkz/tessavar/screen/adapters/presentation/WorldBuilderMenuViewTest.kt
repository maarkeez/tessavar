package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.isFaded
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.korge.tests.ViewsForTesting
import korlibs.math.geom.Point
import kotlin.test.Test

class WorldBuilderMenuViewTest : ViewsForTesting() {
    private val selections = mutableListOf<String>()
    private val delegate =
        object : WorldBuilderMenuView.Delegate {
            override fun undoSelected() {
                selections += "undo"
            }

            override fun redoSelected() {
                selections += "redo"
            }
        }

    private val undoIcon = Bitmap32(16, 16, Colors.ORANGE)
    private val redoIcon = Bitmap32(16, 16, Colors.BLUE)

    private fun newMenuView() =
        WorldBuilderMenuView().apply {
            displayOptions(listOf(WorldBuilderMenuView.Option("Import") { selections += "Import" }, WorldBuilderMenuView.Option("Help") { selections += "Help" }))
            displayHistoryIcons(mapOf(WorldBuilderMenuView.HistoryIcon.UNDO to undoIcon, WorldBuilderMenuView.HistoryIcon.REDO to redoIcon))
            setDelegate(delegate)
            x = 300.0
            y = 10.0
        }

    @Test
    fun `WorldBuilderMenuView should be closed when it is displayed`() =
        sharedViewsTest {
            // When
            val menuView = newMenuView()
            // Then
            menuView.isOpen.shouldBeFalse()
            menuView.optionLabels().shouldBeEmpty()
        }

    @Test
    fun `MenuButton should open the menu with Undo and Redo side by side above its options when it is clicked`() =
        sharedViewsTest {
            // Given
            val menuView = newMenuView()
            addChild(menuView)
            // When
            clickMenuButton(menuView)
            // Then
            menuView.isOpen.shouldBeTrue()
            menuView.optionLabels().shouldContainExactly("Import", "Help")
            menuView.isHistoryAboveOptions().shouldBeTrue()
            menuView.undoButton!!.text shouldBe ""
            menuView.historyIconImage(WorldBuilderMenuView.HistoryIcon.UNDO).bitmap.base shouldBe undoIcon
            menuView.historyIconImage(WorldBuilderMenuView.HistoryIcon.REDO).bitmap.base shouldBe redoIcon
            menuView.historyIconImage(WorldBuilderMenuView.HistoryIcon.REDO).scale shouldBe WorldBuilderMenuView.ICON_SCALE
        }

    @Test
    fun `WorldBuilderMenuView should disable undo and redo when it is opened before the history is displayed`() =
        sharedViewsTest {
            // Given
            val menuView = newMenuView()
            addChild(menuView)
            // When
            clickMenuButton(menuView)
            // Then
            menuView.undoButton!!.enabled shouldBe false
            menuView.redoButton!!.enabled shouldBe false
        }

    @Test
    fun `DisplayHistory should enable undo and dim redo when there is only an edit to undo`() =
        sharedViewsTest {
            // Given
            val menuView = newMenuView()
            addChild(menuView)
            clickMenuButton(menuView)
            // When
            menuView.displayHistory(canUndo = true, canRedo = false)
            // Then
            menuView.undoButton!!.enabled shouldBe true
            menuView.historyIconImage(WorldBuilderMenuView.HistoryIcon.UNDO).isDimmed() shouldBe false
            menuView.redoButton!!.isFaded() shouldBe true
            menuView.historyIconImage(WorldBuilderMenuView.HistoryIcon.REDO).isDimmed() shouldBe true
        }

    @Test
    fun `Undo and Redo should ask to undo and redo and keep the menu open when they are clicked`() =
        sharedViewsTest {
            // Given
            val menuView = newMenuView()
            menuView.displayHistory(canUndo = true, canRedo = true)
            addChild(menuView)
            clickMenuButton(menuView)
            // When
            clickUndo(menuView)
            clickRedo(menuView)
            // Then
            selections.shouldContainExactly("undo", "redo")
            menuView.isOpen.shouldBeTrue()
        }

    @Test
    fun `Undo should ask nothing when there is nothing to undo`() =
        sharedViewsTest {
            // Given
            val menuView = newMenuView()
            menuView.displayHistory(canUndo = false, canRedo = false)
            addChild(menuView)
            clickMenuButton(menuView)
            // When
            clickUndo(menuView)
            // Then
            selections.shouldBeEmpty()
        }

    @Test
    fun `MenuButton should close the menu when it is clicked while the menu is open`() =
        sharedViewsTest {
            // Given
            val menuView = newMenuView()
            addChild(menuView)
            clickMenuButton(menuView)
            // When
            clickMenuButton(menuView)
            // Then
            menuView.isOpen.shouldBeFalse()
        }

    @Test
    fun `MenuOption should close the menu and select the option when it is clicked`() =
        sharedViewsTest {
            // Given
            val menuView = newMenuView()
            addChild(menuView)
            clickMenuButton(menuView)
            // When
            clickMenuOption(menuView, "Help")
            // Then
            selections.shouldContainExactly("Help")
            menuView.isOpen.shouldBeFalse()
        }

    @Test
    fun `WorldBuilderMenuView should close the menu without selecting an option when the screen is clicked out of it`() =
        sharedViewsTest {
            // Given
            val menuView = newMenuView()
            addChild(menuView)
            clickMenuButton(menuView)
            // When
            clickOutOfMenu(x = 50.0, y = 400.0)
            // Then
            menuView.isOpen.shouldBeFalse()
            selections.shouldBeEmpty()
        }

    @Test
    fun `WorldBuilderMenuView should stay open when the screen is tapped out of it with two fingers`() =
        sharedViewsTest {
            // Given
            val menuView = newMenuView()
            addChild(menuView)
            clickMenuButton(menuView)
            // When
            tapWithFingers(Point(50, 400), Point(120, 400))
            // Then
            menuView.isOpen.shouldBeTrue()
        }

    @Test
    fun `WorldBuilderMenuView should close the menu when the screen is tapped out of it with one finger`() =
        sharedViewsTest {
            // Given
            val menuView = newMenuView()
            addChild(menuView)
            clickMenuButton(menuView)
            // When
            tapWithFingers(Point(50, 400))
            // Then
            menuView.isOpen.shouldBeFalse()
        }
}
