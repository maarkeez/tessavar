package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.SubdomainApis
import com.mkz.tessavar.terrain.domain.TerrainMother.terrainThatCanBeOccupied
import com.mkz.tessavar.terrain.domain.TerrainMother.terrainThatCanNotBeOccupied
import com.mkz.tessavar.worldBuilder.adapters.presentation.WorldBuilderApi
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import korlibs.korge.service.storage.InmemoryStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test

class WorldBuilderPresenterTest {
    private val apis =
        SubdomainApis(
            terrains =
                listOf(
                    terrainThatCanBeOccupied(id = "sand", transitionsTo = setOf("lava", "water")).toDto(),
                    terrainThatCanNotBeOccupied(id = "lava").toDto(),
                    terrainThatCanBeOccupied(id = "water").toDto(),
                ),
        ).also(SubdomainApis::initialise)
    private val eventBus = apis.eventBus
    private val storage = InmemoryStorage()
    private val worldBuilderApi = WorldBuilderApi(apis.terrainApi, apis.battlefieldApi, eventBus, storage)
    private val battlefieldView = FakeBattlefieldView()
    private val infoView = FakeWorldBuilderInfoView()
    private val hudView = FakeWorldBuilderHudView()
    private val resizeView = FakeWorldBuilderResizeView()
    private val menuView = FakeWorldBuilderMenuView()
    private val worldFileSaver = FakeWorldFileSaver()
    private val worldFilePicker = FakeWorldFilePicker()
    private var leftCount = 0

    init {
        WorldBuilderPresenter(
            battlefieldView = battlefieldView,
            infoView = infoView,
            hudView = hudView,
            resizeView = resizeView,
            menuView = menuView,
            worldBuilderApi = worldBuilderApi,
            battlefieldApi = apis.battlefieldApi,
            terrainApi = apis.terrainApi,
            messages = MessagesMother.messages(),
            worldFileSaver = worldFileSaver,
            worldFilePicker = worldFilePicker,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            onLeft = { leftCount++ },
            eventBus = eventBus,
        )
    }

    private fun givenWorldCreated() {
        worldBuilderApi.openWorld()
        eventBus.dispatch()
    }

    private fun selectTile(
        row: Int,
        column: Int,
    ) {
        battlefieldView.delegate!!.tileSelected(row, column)
        eventBus.dispatch()
    }

    private fun selectTool(tool: WorldBuilderHudView.Tool) {
        hudView.delegate!!.toolSelected(tool)
        eventBus.dispatch()
    }

    private fun selectTerrain(terrainId: String) {
        hudView.delegate!!.terrainSelected(terrainId)
        eventBus.dispatch()
    }

    @Test
    fun `WorldOpened should display the world on the battlefield as the battle would when a new world is opened`() {
        // When
        givenWorldCreated()
        // Then
        battlefieldView.displayedBattlefields.shouldContainExactly(BattlefieldMapper.terrain(apis.battlefieldApi.previewBattlefield(List(16) { List(16) { "sand" } })))
        infoView.displayedSize shouldBe (16 to 16)
        resizeView.displayedSize shouldBe (16 to 16)
        resizeView.displayedAllowedSizes shouldBe (1..128)
    }

    @Test
    fun `WorldOpened should display every terrain with its tile image and sand selected when a new world is opened`() {
        // When
        givenWorldCreated()
        // Then
        hudView.displayedTerrains shouldBe
            listOf("lava", "sand", "water").map { terrainId -> WorldBuilderHudView.TerrainOption(terrainId, battlefieldView.terrainTileBitmap(terrainId, null)) }
        hudView.selectedTerrainId shouldBe "sand"
    }

    @Test
    fun `TerrainSelected should highlight the terrain when a terrain is selected in the palette`() {
        // Given
        givenWorldCreated()
        // When
        selectTerrain("lava")
        // Then
        hudView.selectedTerrainId shouldBe "lava"
    }

    @Test
    fun `TilePainted should draw again the painted tile and its adjacent tiles when a tile is selected`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        // When
        selectTile(row = 2, column = 3)
        // Then
        val updatedTerrain = battlefieldView.updatedTerrains.single()
        updatedTerrain.battlefield.tiles
            .getValue(TilePosition(2, 3))
            .terrainId shouldBe "lava"
        updatedTerrain.positions shouldBe setOf(TilePosition(2, 3), TilePosition(1, 3), TilePosition(2, 4), TilePosition(3, 3), TilePosition(2, 2))
    }

    @Test
    fun `TilePainted should only draw again the adjacent tiles inside the world when a corner tile is selected`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        // When
        selectTile(row = 0, column = 0)
        // Then
        battlefieldView.updatedTerrains.single().positions shouldBe setOf(TilePosition(0, 0), TilePosition(0, 1), TilePosition(1, 0))
    }

    @Test
    fun `WorldBuilderPresenter should select the paint tool when the world builder is displayed`() {
        // Then
        hudView.displayedTool shouldBe WorldBuilderHudView.Tool.PAINT
    }

    @Test
    fun `ToolSelected should highlight the tool when a tool is selected`() {
        // Given
        givenWorldCreated()
        // When
        selectTool(WorldBuilderHudView.Tool.FILL)
        // Then
        hudView.displayedTool shouldBe WorldBuilderHudView.Tool.FILL
    }

    @Test
    fun `AreaFilled should display the filled world when a tile is selected with the fill tool`() {
        // Given
        givenWorldCreated()
        selectTerrain("water")
        selectTool(WorldBuilderHudView.Tool.FILL)
        // When
        selectTile(row = 4, column = 5)
        // Then
        battlefieldView.displayedBattlefields.last() shouldBe BattlefieldMapper.terrain(apis.battlefieldApi.previewBattlefield(List(16) { List(16) { "water" } }))
        battlefieldView.updatedTerrains.shouldBeEmpty()
    }

    @Test
    fun `TilePaintRejected should display why and keep the area when the fill would put a terrain next to one it can not be`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        selectTile(row = 2, column = 3)
        selectTerrain("water")
        selectTool(WorldBuilderHudView.Tool.FILL)
        // When
        selectTile(row = 0, column = 0)
        // Then
        infoView.errors.shouldContainExactly("Water can not be next to Lava")
        battlefieldView.displayedBattlefields.shouldHaveSize(1)
    }

    @Test
    fun `WorldOpened should display nothing to undo or redo when a new world is opened`() {
        // When
        givenWorldCreated()
        // Then
        hudView.displayedHistory shouldBe (false to false)
    }

    @Test
    fun `TilePainted should display an edit to undo when a tile is painted`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        // When
        selectTile(row = 2, column = 3)
        // Then
        hudView.displayedHistory shouldBe (true to false)
    }

    @Test
    fun `EditUndone should display the world before the last edit and an edit to redo when undo is selected`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        selectTile(row = 2, column = 3)
        // When
        hudView.delegate!!.undoSelected()
        eventBus.dispatch()
        // Then
        battlefieldView.displayedBattlefields.last() shouldBe BattlefieldMapper.terrain(apis.battlefieldApi.previewBattlefield(List(16) { List(16) { "sand" } }))
        hudView.displayedHistory shouldBe (false to true)
    }

    @Test
    fun `EditRedone should display the world after the edit undone when redo is selected`() {
        // Given
        givenWorldCreated()
        resizeView.delegate!!.sizeChanged(rows = 4, columns = 2)
        hudView.delegate!!.undoSelected()
        eventBus.dispatch()
        // When
        hudView.delegate!!.redoSelected()
        eventBus.dispatch()
        // Then
        battlefieldView.displayedBattlefields.last() shouldBe BattlefieldMapper.terrain(apis.battlefieldApi.previewBattlefield(List(4) { List(2) { "sand" } }))
        resizeView.displayedSize shouldBe (4 to 2)
        hudView.displayedHistory shouldBe (true to false)
    }

    @Test
    fun `TilePaintRejected should display why and keep the tile when the terrain can not be next to an adjacent tile`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        selectTile(row = 2, column = 3)
        selectTerrain("water")
        // When
        selectTile(row = 2, column = 4)
        // Then
        infoView.errors.shouldContainExactly("Water can not be next to Lava")
        battlefieldView.updatedTerrains.shouldHaveSize(1)
    }

    @Test
    fun `SizeChanged should display the resized world when the size is changed`() {
        // Given
        givenWorldCreated()
        // When
        resizeView.delegate!!.sizeChanged(rows = 24, columns = 8)
        eventBus.dispatch()
        // Then
        battlefieldView.displayedBattlefields.last() shouldBe BattlefieldMapper.terrain(apis.battlefieldApi.previewBattlefield(List(24) { List(8) { "sand" } }))
        infoView.displayedSize shouldBe (24 to 8)
        resizeView.displayedSize shouldBe (24 to 8)
    }

    @Test
    fun `Resize should open the resize dialog when Resize is selected`() {
        // Given
        givenWorldCreated()
        // When
        menuView.select("Resize")
        // Then
        resizeView.opened shouldBe true
    }

    @Test
    fun `Export should save the world in a world file and tell it when the world is exported`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        selectTile(row = 0, column = 0)
        // When
        menuView.select("Export")
        // Then
        worldFileSaver.savedFiles.shouldContainExactly("world.toml" to worldBuilderApi.exportWorld())
        infoView.infos.shouldContainExactly("World exported")
    }

    @Test
    fun `Export should tell nothing when saving the file is cancelled`() {
        // Given
        givenWorldCreated()
        worldFileSaver.isCancelled = true
        // When
        menuView.select("Export")
        // Then
        infoView.infos.shouldBeEmpty()
        infoView.errors.shouldBeEmpty()
    }

    @Test
    fun `Export should display why when the file can not be saved`() {
        // Given
        givenWorldCreated()
        worldFileSaver.failure = IllegalStateException("The disk is full")
        // When
        menuView.select("Export")
        // Then
        infoView.errors.shouldContainExactly("The file can not be saved")
    }

    @Test
    fun `Back should leave the world builder at once when back is selected`() {
        // Given
        givenWorldCreated()
        // When
        menuView.select("Back")
        // Then
        leftCount shouldBe 1
        infoView.infos.shouldBeEmpty()
    }

    @Test
    fun `WorldOpened should display the world kept from the last time when the world builder is opened again`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        selectTile(row = 2, column = 3)
        // When
        WorldBuilderApi(apis.terrainApi, apis.battlefieldApi, eventBus, storage).openWorld()
        eventBus.dispatch()
        // Then
        battlefieldView.displayedBattlefields
            .last()
            .tiles
            .getValue(TilePosition(2, 3))
            .terrainId shouldBe "lava"
    }

    @Test
    fun `TileSelected should not paint when the tile already has the selected terrain`() {
        // Given
        givenWorldCreated()
        // When
        selectTile(row = 0, column = 0)
        // Then
        battlefieldView.updatedTerrains.shouldBeEmpty()
        infoView.errors.shouldBeEmpty()
    }

    private fun importFile(text: String?) {
        worldFilePicker.pickedText = text
        menuView.select("Import")
        eventBus.dispatch()
    }

    @Test
    fun `WorldBuilderMenuView should offer resizing, importing, exporting and going back when the world builder is displayed`() {
        // Then
        menuView.displayedOptions.map(WorldBuilderMenuView.Option::label).shouldContainExactly("Resize", "Import", "Export", "Back")
    }

    @Test
    fun `Import should display the world in the file and tell it when a world is imported`() {
        // Given
        givenWorldCreated()
        val tiles = listOf(listOf("sand", "lava", "sand"), listOf("sand", "sand", "water"))
        // When
        importFile(apis.battlefieldApi.exportBattlefield(tiles))
        // Then
        battlefieldView.displayedBattlefields.last() shouldBe BattlefieldMapper.terrain(apis.battlefieldApi.previewBattlefield(tiles))
        infoView.displayedSize shouldBe (2 to 3)
        resizeView.displayedSize shouldBe (2 to 3)
        infoView.infos.shouldContainExactly("World imported")
    }

    @Test
    fun `Import should keep the world and display nothing when no file is picked`() {
        // Given
        givenWorldCreated()
        // When
        importFile(null)
        // Then
        infoView.errors.shouldBeEmpty()
        infoView.infos.shouldBeEmpty()
        battlefieldView.displayedBattlefields.shouldHaveSize(1)
    }

    @Test
    fun `Import should display why and keep the world when the file can not be read`() {
        // Given
        givenWorldCreated()
        worldFilePicker.failure = IllegalStateException("The file was removed")
        // When
        importFile("Never read")
        // Then
        infoView.errors.shouldContainExactly("The file can not be read")
        battlefieldView.displayedBattlefields.shouldHaveSize(1)
    }

    @Test
    fun `Import should display why when the file has no world`() {
        // Given
        givenWorldCreated()
        // When
        importFile("Hello, world builder!")
        // Then
        infoView.errors.shouldContainExactly("The file has no world")
    }

    @Test
    fun `Import should display why when the file is empty`() {
        // Given
        givenWorldCreated()
        // When
        importFile("")
        // Then
        infoView.errors.shouldContainExactly("The file has no world")
    }

    @Test
    fun `Import should display why when the world in the file is larger than allowed`() {
        // Given
        givenWorldCreated()
        // When
        importFile(apis.battlefieldApi.exportBattlefield(List(129) { List(2) { "sand" } }))
        // Then
        infoView.errors.shouldContainExactly("The world must be 1 to 128 tiles wide and tall")
    }

    @Test
    fun `Import should display the terrain when the world in the file has an unknown terrain`() {
        // Given
        givenWorldCreated()
        // When
        importFile(apis.battlefieldApi.exportBattlefield(listOf(listOf("sand", "forest"))))
        // Then
        infoView.errors.shouldContainExactly("Unknown terrain: forest")
    }

    @Test
    fun `Import should display the terrains when two adjacent tiles of the world in the file can not be next to each other`() {
        // Given
        givenWorldCreated()
        // When
        importFile(apis.battlefieldApi.exportBattlefield(listOf(listOf("water", "lava"))))
        // Then
        infoView.errors.shouldContainExactly("Water can not be next to Lava")
    }
}
