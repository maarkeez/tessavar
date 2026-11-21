package com.mkz.tessavar.shared.adapters.e2e

import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderHudView
import com.mkz.tessavar.screen.adapters.presentation.battleUnitBitmapAt
import com.mkz.tessavar.screen.adapters.presentation.battlefieldView
import com.mkz.tessavar.screen.adapters.presentation.clickBuildWorld
import com.mkz.tessavar.screen.adapters.presentation.clickMenuButton
import com.mkz.tessavar.screen.adapters.presentation.clickMenuOption
import com.mkz.tessavar.screen.adapters.presentation.clickPaletteTerrain
import com.mkz.tessavar.screen.adapters.presentation.clickPaletteUnit
import com.mkz.tessavar.screen.adapters.presentation.clickRowsStep
import com.mkz.tessavar.screen.adapters.presentation.clickTab
import com.mkz.tessavar.screen.adapters.presentation.clickTile
import com.mkz.tessavar.screen.adapters.presentation.clickTool
import com.mkz.tessavar.screen.adapters.presentation.clickUndo
import com.mkz.tessavar.screen.adapters.presentation.dragPaletteRow
import com.mkz.tessavar.screen.adapters.presentation.hasBattleUnitAt
import com.mkz.tessavar.screen.adapters.presentation.isBattlefieldDisplayed
import com.mkz.tessavar.screen.adapters.presentation.isSlotInView
import com.mkz.tessavar.screen.adapters.presentation.menuView
import com.mkz.tessavar.screen.adapters.presentation.paletteKeys
import com.mkz.tessavar.screen.adapters.presentation.paletteRowOf
import com.mkz.tessavar.screen.adapters.presentation.paletteTerrainIds
import com.mkz.tessavar.screen.adapters.presentation.subtitleText
import com.mkz.tessavar.screen.adapters.presentation.tapPaletteSlot
import com.mkz.tessavar.screen.adapters.presentation.tapWithFingersOn
import com.mkz.tessavar.screen.adapters.presentation.terrainBitmapAt
import com.mkz.tessavar.screen.adapters.presentation.tileCount
import com.mkz.tessavar.screen.adapters.presentation.worldBuilderHudView
import com.mkz.tessavar.screen.adapters.presentation.worldBuilderInfoView
import com.mkz.tessavar.screen.adapters.presentation.worldBuilderMenuView
import com.mkz.tessavar.screen.adapters.presentation.worldBuilderResizeView
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class WorldBuilderEndToEndTest : ViewsForTesting() {
    @Test
    fun `should display a 16 x 16 world when the world builder is opened from the menu`() =
        sharedViewsTest {
            // When
            startWorldBuilder()
            // Then
            battlefieldView()!!.tileCount() shouldBe 256
            worldBuilderInfoView()!!.subtitleText() shouldBe "16 x 16"
        }

    @Test
    fun `should paint the tile with the selected terrain when a tile is tapped`() =
        sharedViewsTest {
            // Given
            startWorldBuilder()
            val battlefieldView = battlefieldView()!!
            val sandBitmap = battlefieldView.terrainBitmapAt(row = 2, column = 2)
            clickPaletteTerrain(worldBuilderHudView()!!, "lava")
            // When
            clickTile(battlefieldView, row = 2, column = 2)
            // Then
            waitUntil { battlefieldView.terrainBitmapAt(row = 2, column = 2) != sandBitmap }
            battlefieldView.terrainBitmapAt(row = 2, column = 2) shouldBe battlefieldView.terrainTileBitmap("lava", terrainTransition = null)
        }

    @Test
    fun `should paint every tile of the area with the selected terrain when a tile is tapped with the fill tool`() =
        sharedViewsTest {
            // Given
            startWorldBuilder()
            val battlefieldView = battlefieldView()!!
            val hudView = worldBuilderHudView()!!
            val lavaBitmap = battlefieldView.terrainTileBitmap("lava", terrainTransition = null)
            clickTool(hudView, WorldBuilderHudView.Tool.FILL)
            clickPaletteTerrain(hudView, "lava")
            // When
            clickTile(battlefieldView, row = 2, column = 2)
            // Then
            waitUntil { battlefieldView()!!.terrainBitmapAt(row = 15, column = 15) == lavaBitmap }
            battlefieldView()!!.terrainBitmapAt(row = 0, column = 0) shouldBe lavaBitmap
        }

    @Test
    fun `should bring the tile back when the paint is undone`() =
        sharedViewsTest {
            // Given
            startWorldBuilder()
            val battlefieldView = battlefieldView()!!
            val hudView = worldBuilderHudView()!!
            val sandBitmap = battlefieldView.terrainBitmapAt(row = 2, column = 2)
            clickPaletteTerrain(hudView, "lava")
            clickTile(battlefieldView, row = 2, column = 2)
            val menuView = worldBuilderMenuView()!!
            clickMenuButton(menuView)
            waitUntil { menuView.undoButton?.enabled == true }
            // When
            clickUndo(menuView)
            // Then
            waitUntil { battlefieldView()!!.terrainBitmapAt(row = 2, column = 2) == sandBitmap }
            battlefieldView()!!.terrainBitmapAt(row = 2, column = 2) shouldBe sandBitmap
        }

    @Test
    fun `should bring the tile back when two fingers tap the world after a paint`() =
        sharedViewsTest {
            // Given
            startWorldBuilder()
            val battlefieldView = battlefieldView()!!
            val hudView = worldBuilderHudView()!!
            val sandBitmap = battlefieldView.terrainBitmapAt(row = 2, column = 2)
            clickPaletteTerrain(hudView, "lava")
            clickTile(battlefieldView, row = 2, column = 2)
            waitUntil { battlefieldView()!!.terrainBitmapAt(row = 2, column = 2) != sandBitmap }
            // When
            tapWithFingersOn(battlefieldView()!!, fingers = 2)
            // Then
            waitUntil { battlefieldView()!!.terrainBitmapAt(row = 2, column = 2) == sandBitmap }
            battlefieldView()!!.terrainBitmapAt(row = 2, column = 2) shouldBe sandBitmap
        }

    @Test
    fun `should reject the tile and display why when the terrain can not be next to an adjacent tile`() =
        sharedViewsTest {
            // Given
            startWorldBuilder()
            val battlefieldView = battlefieldView()!!
            val hudView = worldBuilderHudView()!!
            clickPaletteTerrain(hudView, "lava")
            clickTile(battlefieldView, row = 2, column = 2)
            clickPaletteTerrain(hudView, "water")
            val sandBitmap = battlefieldView.terrainBitmapAt(row = 2, column = 3)
            // When
            clickTile(battlefieldView, row = 2, column = 3)
            // Then
            waitUntil { worldBuilderInfoView()!!.subtitleText() == "Water can not be next to Lava" }
            worldBuilderInfoView()!!.subtitleText() shouldBe "Water can not be next to Lava"
            battlefieldView.terrainBitmapAt(row = 2, column = 3) shouldBe sandBitmap
        }

    @Test
    fun `should display the resized world when the size is stepped`() =
        sharedViewsTest {
            // Given
            startWorldBuilder()
            val builderMenuView = worldBuilderMenuView()!!
            clickMenuButton(builderMenuView)
            clickMenuOption(builderMenuView, "Resize")
            // When
            clickRowsStep(worldBuilderResizeView()!!, step = -8)
            // Then
            waitUntil { battlefieldView()?.tileCount() == 128 }
            battlefieldView()!!.tileCount() shouldBe 128
        }

    @Test
    fun `should go back to the menu at once when back is selected`() =
        sharedViewsTest {
            // Given
            startWorldBuilder()
            val builderMenuView = worldBuilderMenuView()!!
            clickMenuButton(builderMenuView)
            // When
            clickMenuOption(builderMenuView, "Back")
            // Then
            waitUntil { menuView() != null && worldBuilderHudView() == null }
            menuView().shouldNotBeNull()
            worldBuilderHudView() shouldBe null
        }

    @Test
    fun `should keep the world painted when the world builder is opened again after going back`() =
        sharedViewsTest {
            // Given
            startWorldBuilder()
            val lavaBitmap = battlefieldView()!!.terrainTileBitmap("lava", terrainTransition = null)
            clickPaletteTerrain(worldBuilderHudView()!!, "lava")
            clickTile(battlefieldView()!!, row = 2, column = 2)
            waitUntil { battlefieldView()!!.terrainBitmapAt(row = 2, column = 2) == lavaBitmap }
            val builderMenuView = worldBuilderMenuView()!!
            clickMenuButton(builderMenuView)
            clickMenuOption(builderMenuView, "Back")
            waitUntil { menuView() != null && worldBuilderHudView() == null }
            // When
            clickBuildWorld(menuView()!!)
            // Then
            waitUntil { battlefieldView()?.isBattlefieldDisplayed() == true && worldBuilderHudView()?.paletteTerrainIds()?.isNotEmpty() == true }
            battlefieldView()!!.terrainBitmapAt(row = 2, column = 2) shouldBe battlefieldView()!!.terrainTileBitmap("lava", terrainTransition = null)
        }

    @Test
    fun `should tell the file can not be read when importing without a file picker`() =
        sharedViewsTest {
            // Given
            startWorldBuilder()
            val builderMenuView = worldBuilderMenuView()!!
            clickMenuButton(builderMenuView)
            // When
            clickMenuOption(builderMenuView, "Import")
            // Then
            waitUntil { worldBuilderInfoView()!!.subtitleText() == "The file can not be read" }
            worldBuilderInfoView()!!.subtitleText() shouldBe "The file can not be read"
            battlefieldView()!!.tileCount() shouldBe 256
        }

    @Test
    fun `should place a battle unit of the player selected on the tapped tile when the Units tab is displayed`() =
        sharedViewsTest {
            // Given
            startWorldBuilder()
            val hudView = worldBuilderHudView()!!
            clickTab(hudView, WorldBuilderHudView.Tab.UNITS)
            clickPaletteUnit(hudView, "player-two", "rat")
            // When
            clickTile(battlefieldView()!!, row = 3, column = 4)
            // Then
            waitUntil { battlefieldView()!!.hasBattleUnitAt(3, 4) }
            battlefieldView()!!.hasBattleUnitAt(3, 4) shouldBe true
        }

    @Test
    fun `should place a battle unit of the CPU's last unit when its row is scrolled on the Units tab`() =
        sharedViewsTest {
            // Given
            startWorldBuilder()
            val hudView = worldBuilderHudView()!!
            clickTab(hudView, WorldBuilderHudView.Tab.UNITS)
            val lastUnit = hudView.paletteKeys(WorldBuilderHudView.Tab.UNITS).last()
            val cpuRow = hudView.paletteRowOf(WorldBuilderHudView.Tab.UNITS, lastUnit)
            // When
            dragPaletteRow(cpuRow, -2000.0)
            tapPaletteSlot(hudView, WorldBuilderHudView.Tab.UNITS, lastUnit)
            clickTile(battlefieldView()!!, row = 3, column = 4)
            // Then
            waitUntil { battlefieldView()!!.hasBattleUnitAt(3, 4) }
            battlefieldView()!!.battleUnitBitmapAt(3, 4).shouldNotBeNull()
            cpuRow.isSlotInView(lastUnit) shouldBe true
        }
}
