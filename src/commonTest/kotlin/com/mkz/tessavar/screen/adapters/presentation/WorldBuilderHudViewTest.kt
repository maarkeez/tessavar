package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class WorldBuilderHudViewTest : ViewsForTesting() {
    private val selectedTerrains = mutableListOf<String>()
    private val delegate =
        object : WorldBuilderHudView.Delegate {
            override fun terrainSelected(terrainId: String) {
                selectedTerrains += terrainId
            }
        }

    private val sandBitmap = Bitmap32(16, 16, Colors.YELLOW)
    private val lavaBitmap = Bitmap32(16, 16, Colors.RED)
    private val terrains = listOf(WorldBuilderHudView.TerrainOption("lava", lavaBitmap), WorldBuilderHudView.TerrainOption("sand", sandBitmap))

    private fun newHudView() = WorldBuilderHudView(MessagesMother.messages()).also { hudView -> hudView.setDelegate(delegate) }

    @Test
    fun `DisplayTerrains should display each terrain tile and highlight the selected one when the terrains are displayed`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            // When
            hudView.displayTerrains(terrains, selectedTerrainId = "sand")
            // Then
            hudView.paletteTerrainIds().shouldContainExactly("lava", "sand")
            hudView.paletteBitmap("lava") shouldBe lavaBitmap
            hudView.highlightedTerrainId() shouldBe "sand"
        }

    @Test
    fun `SelectTerrain should highlight only the selected terrain when another terrain is selected`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            hudView.displayTerrains(terrains, selectedTerrainId = "sand")
            // When
            hudView.selectTerrain("lava")
            // Then
            hudView.highlightedTerrainId() shouldBe "lava"
        }

    @Test
    fun `PaletteTerrain should ask to select the terrain when it is clicked`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            hudView.displayTerrains(terrains, selectedTerrainId = "sand")
            addChild(hudView)
            // When
            clickPaletteTerrain(hudView, "lava")
            // Then
            selectedTerrains.shouldContainExactly("lava")
        }
}
