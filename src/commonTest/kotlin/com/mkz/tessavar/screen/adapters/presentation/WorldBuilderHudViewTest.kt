package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.GAME_WINDOW_SIZE
import com.mkz.tessavar.battlefield.domain.BattlefieldMother.battlefield
import com.mkz.tessavar.screen.adapters.presentation.GameSceneResourcesLoaderMother.loadedResourcesLoader
import com.mkz.tessavar.shared.adapters.presentation.clickOn
import com.mkz.tessavar.shared.adapters.presentation.nextFrame
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

class WorldBuilderHudViewTest : ViewsForTesting(windowSize = GAME_WINDOW_SIZE) {
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

    private val fortressBitmap = Bitmap32(64, 64, Colors.DARKGRAY)
    private val knightBitmap = Bitmap32(16, 16, Colors.BLUE)

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
    fun `PaletteTerrain should ask to select the terrain and close the card when it is clicked`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            hudView.displayTerrains(terrains, selectedTerrainId = "sand")
            addChild(hudView)
            clickBrush(hudView)
            // When
            clickOn(hudView.terrainButtons.getValue("lava"))
            // Then
            selectedTerrains.shouldContainExactly("lava")
            hudView.isCardOpen.shouldBeFalse()
        }

    @Test
    fun `WorldBuilderHudView should display the brush, paint, fill and erase in the pill with the card closed when it is created`() =
        sharedViewsTest {
            // When
            val hudView = newHudView()
            hudView.displayTerrains(terrains, selectedTerrainId = "sand")
            addChild(hudView)
            // Then
            hudView.brushBitmap() shouldBe sandBitmap
            hudView.pillTools().shouldContainExactly("PAINT", "FILL", "ERASE")
            hudView.toolIcons().shouldContainExactly(WorldBuilderHudView.ToolIcon.PAINT, WorldBuilderHudView.ToolIcon.FILL, WorldBuilderHudView.ToolIcon.ERASE)
            hudView.toolButtons.values
                .map { button -> button.text }
                .shouldContainExactly("", "", "")
            hudView.isCardOpen.shouldBeFalse()
            hudView.card.visible.shouldBeFalse()
        }

    @Test
    fun `Brush should open the card when it is clicked and close it when clicked again`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            addChild(hudView)
            // When
            clickBrush(hudView)
            // Then
            hudView.isCardOpen.shouldBeTrue()
            hudView.card.visible.shouldBeTrue()
            // When
            clickBrush(hudView)
            // Then
            hudView.isCardOpen.shouldBeFalse()
            hudView.card.visible.shouldBeFalse()
        }

    @Test
    fun `DisplayTab should show the option selected on the tab in the brush and on its tab when the tab is displayed`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            hudView.displayTerrains(terrains, selectedTerrainId = "sand")
            hudView.displayBuildings(listOf(WorldBuilderHudView.PaletteOption("fortress", fortressBitmap)))
            hudView.selectBuilding("fortress")
            // When
            hudView.displayTab(WorldBuilderHudView.Tab.BUILDINGS)
            // Then
            hudView.brushBitmap() shouldBe fortressBitmap
            hudView.tabIconBitmap(WorldBuilderHudView.Tab.TERRAIN) shouldBe sandBitmap
            hudView.tabIconBitmap(WorldBuilderHudView.Tab.BUILDINGS) shouldBe fortressBitmap
        }

    @Test
    fun `DisplayTab should show Stock instead of Fill in the pill when the Npcs tab is displayed`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            addChild(hudView)
            // When
            hudView.displayTab(WorldBuilderHudView.Tab.NPCS)
            // Then
            hudView.pillTools().shouldContainExactly("PAINT", "STOCK", "ERASE")
            // When
            hudView.displayTab(WorldBuilderHudView.Tab.UNITS)
            // Then
            hudView.pillTools().shouldContainExactly("PAINT", "FILL", "ERASE")
        }

    @Test
    fun `OutsideTapCatcher should close the card without asking to select a tile when the world is tapped with one finger`() =
        sharedViewsTest {
            // Given
            val tappedTiles = mutableListOf<String>()
            val battlefieldView = BattlefieldView(loadedResourcesLoader(), viewportSize = GAME_WINDOW_SIZE)
            battlefieldView.displayBattlefield(battlefield(rows = 16, columns = 16).toTerrain())
            battlefieldView.setDelegate(
                object : BattlefieldView.Delegate {
                    override fun tileSelected(
                        row: Int,
                        column: Int,
                    ) {
                        tappedTiles += "tile $row $column"
                    }
                },
            )
            addChild(battlefieldView)
            val hudView = newHudView()
            addChild(hudView)
            clickBrush(hudView)
            // When
            tapWithFingers(Point(100, 200))
            nextFrame()
            // Then
            hudView.isCardOpen.shouldBeFalse()
            tappedTiles.shouldBeEmpty()
        }

    @Test
    fun `OutsideTapCatcher should keep the card open when the world is tapped with two fingers`() =
        sharedViewsTest {
            // Given
            val hudView = newHudView()
            addChild(hudView)
            clickBrush(hudView)
            // When
            tapWithFingers(Point(100, 200), Point(200, 200))
            // Then
            hudView.isCardOpen.shouldBeTrue()
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
    fun `DisplayUnits should display a row of units for each player, titled with its name, in the card`() =
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
            hudView.playerTitles().shouldContainExactly("Human", "CPU")
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
            hudView.highlightedKey(WorldBuilderHudView.Tab.BUILDINGS) shouldBe "fortress"
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
