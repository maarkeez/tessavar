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
    private val selectedTools = mutableListOf<WorldBuilderHudView.Tool>()
    private val paletteSelections = mutableListOf<String>()
    private val delegate =
        object : WorldBuilderHudView.Delegate {
            override fun tabSelected(tab: WorldBuilderHudView.Tab) {
                paletteSelections += "tab ${tab.name}"
            }

            override fun buildingSelected(buildingId: String) {
                paletteSelections += "building $buildingId"
            }

            override fun unitSelected(
                playerId: String,
                unitId: String,
            ) {
                paletteSelections += "unit $playerId $unitId"
            }

            override fun npcSelected(npcId: String) {
                paletteSelections += "npc $npcId"
            }

            override fun stockSelected() {
                paletteSelections += "stock"
            }

            override fun toolSelected(tool: WorldBuilderHudView.Tool) {
                selectedTools += tool
            }

            override fun terrainSelected(terrainId: String) {
                selectedTerrains += terrainId
            }
        }

    private val sandBitmap = Bitmap32(16, 16, Colors.YELLOW)
    private val lavaBitmap = Bitmap32(16, 16, Colors.RED)
    private val terrains = listOf(WorldBuilderHudView.TerrainOption("lava", lavaBitmap), WorldBuilderHudView.TerrainOption("sand", sandBitmap))

    private val paintIcon = Bitmap32(16, 16, Colors.ORANGE)
    private val placeIcon = Bitmap32(16, 16, Colors.RED)
    private val fillIcon = Bitmap32(16, 16, Colors.BLUE)
    private val toolIcons =
        WorldBuilderHudView.ToolIcon.entries.associateWith { icon ->
            when (icon) {
                WorldBuilderHudView.ToolIcon.PAINT -> paintIcon
                WorldBuilderHudView.ToolIcon.PLACE -> placeIcon
                WorldBuilderHudView.ToolIcon.FILL -> fillIcon
                else -> Bitmap32(16, 16, Colors.WHITE)
            }
        }

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

    @Test
    fun `WorldBuilderHudView should display the paint, fill and erase tools as icons without a label when it is created`() =
        sharedViewsTest {
            // When
            val hudView = newHudView()
            // Then
            hudView.toolIcons().shouldContainExactly(WorldBuilderHudView.ToolIcon.PAINT, WorldBuilderHudView.ToolIcon.FILL, WorldBuilderHudView.ToolIcon.ERASE)
            hudView.toolButtons.values
                .map { button -> button.text }
                .shouldContainExactly("", "", "")
        }

    @Test
    fun `DisplayToolIcons should draw each tool's icon at twice its size and dim the icons of the disabled tools when the icons are displayed`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            hudView.displayTab(WorldBuilderHudView.Tab.UNITS)
            // When
            hudView.displayToolIcons(toolIcons)
            // Then
            hudView.toolIconImage(WorldBuilderHudView.Tool.ERASE).scale shouldBe WorldBuilderHudView.ICON_SCALE
            hudView.toolIconImage(WorldBuilderHudView.Tool.ERASE).isDimmed() shouldBe false
            hudView.toolIconImage(WorldBuilderHudView.Tool.FILL).bitmap.base shouldBe fillIcon
            hudView.toolIconImage(WorldBuilderHudView.Tool.FILL).isDimmed() shouldBe true
        }

    @Test
    fun `DisplayTab should show the place icon on Paint when a tab other than Terrain is displayed, and the paint icon again back on Terrain`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            hudView.displayToolIcons(toolIcons)
            hudView.displayTab(WorldBuilderHudView.Tab.UNITS)
            hudView.toolIconImage(WorldBuilderHudView.Tool.PAINT).bitmap.base shouldBe placeIcon
            // When
            hudView.displayTab(WorldBuilderHudView.Tab.TERRAIN)
            // Then
            hudView.toolIconImage(WorldBuilderHudView.Tool.PAINT).bitmap.base shouldBe paintIcon
        }

    @Test
    fun `SelectTool should highlight only the selected tool when a tool is selected`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            hudView.selectTool(WorldBuilderHudView.Tool.PAINT)
            // When
            hudView.selectTool(WorldBuilderHudView.Tool.FILL)
            // Then
            hudView.highlightedTools().shouldContainExactly(WorldBuilderHudView.Tool.FILL)
        }

    @Test
    fun `Tool should ask to select the tool when it is clicked`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            addChild(hudView)
            // When
            clickTool(hudView, WorldBuilderHudView.Tool.FILL)
            // Then
            selectedTools.shouldContainExactly(WorldBuilderHudView.Tool.FILL)
        }

    private val fortressBitmap = Bitmap32(64, 64, Colors.DARKGRAY)
    private val knightBitmap = Bitmap32(16, 16, Colors.BLUE)

    @Test
    fun `DisplayTab should display only its palette, show the place icon on Paint and disable Fill when a tab other than Terrain is displayed`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            // When
            hudView.displayTab(WorldBuilderHudView.Tab.BUILDINGS)
            // Then
            hudView.highlightedTab() shouldBe WorldBuilderHudView.Tab.BUILDINGS
            hudView.palettes
                .filterValues { palette -> palette.visible }
                .keys
                .shouldContainExactly(WorldBuilderHudView.Tab.BUILDINGS)
            hudView.toolIcons().shouldContainExactly(WorldBuilderHudView.ToolIcon.PLACE, WorldBuilderHudView.ToolIcon.FILL, WorldBuilderHudView.ToolIcon.ERASE)
            hudView.toolIconImage(WorldBuilderHudView.Tool.FILL).isDimmed() shouldBe true
            hudView.toolButtons.getValue(WorldBuilderHudView.Tool.FILL).enabled shouldBe false
        }

    @Test
    fun `Tab should ask to display the tab when it is clicked`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            addChild(hudView)
            // When
            clickTab(hudView, WorldBuilderHudView.Tab.NPCS)
            // Then
            paletteSelections.shouldContainExactly("tab NPCS")
        }

    @Test
    fun `DisplayUnits should display a row of units for each player, titled with its name, and a click ask to select the unit of that player`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            val units = listOf(WorldBuilderHudView.PaletteOption("knight", knightBitmap))
            hudView.displayUnits(listOf(WorldBuilderHudView.UnitRow("player-one", "Human", units), WorldBuilderHudView.UnitRow("player-two", "CPU", units)))
            hudView.displayTab(WorldBuilderHudView.Tab.UNITS)
            addChild(hudView)
            // When
            clickPaletteUnit(hudView, "player-two", "knight")
            // Then
            hudView.paletteKeys(WorldBuilderHudView.Tab.UNITS).shouldContainExactly("player-one/knight", "player-two/knight")
            paletteSelections.shouldContainExactly("unit player-two knight")
        }

    @Test
    fun `SelectBuilding should frame the building selected when the buildings are displayed`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            hudView.displayBuildings(listOf(WorldBuilderHudView.PaletteOption("fortress", fortressBitmap)))
            // When
            hudView.selectBuilding("fortress")
            // Then
            hudView.selectionBorders.keys.shouldContainExactly(WorldBuilderHudView.Tab.BUILDINGS)
        }

    @Test
    fun `Stock should ask to open what the npc sells only when it is enabled`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            hudView.displayNpcs(listOf(WorldBuilderHudView.PaletteOption("alchemist", knightBitmap)))
            hudView.displayTab(WorldBuilderHudView.Tab.NPCS)
            addChild(hudView)
            clickStock(hudView)
            hudView.displayStock(isEnabled = true)
            // When
            clickStock(hudView)
            // Then
            paletteSelections.shouldContainExactly("stock")
        }
}
