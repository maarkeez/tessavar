package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.SubdomainApis
import com.mkz.tessavar.terrain.domain.TerrainMother.terrainThatCanBeOccupied
import com.mkz.tessavar.terrain.domain.TerrainMother.terrainThatCanNotBeOccupied
import com.mkz.tessavar.worldBuilder.adapters.presentation.WorldBuilderApi
import com.mkz.tessavar.worldBuilder.adapters.resources.WorldFileCodecMother
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderMother
import com.mkz.tessavar.worldBuilder.usecases.services.FakeCatalog
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
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
    private val worldBuilderApi = WorldBuilderApi(apis.terrainApi, apis.battlefieldApi, eventBus, storage, FakeCatalog())
    private val battlefieldView = FakeBattlefieldView()
    private val infoView = FakeWorldBuilderInfoView()
    private val hudView = FakeWorldBuilderHudView()
    private val resizeView = FakeWorldBuilderResizeView()
    private val detailsView = FakeWorldBuilderDetailsView()
    private val stockView = FakeWorldBuilderStockView()
    private val formView = FakeWorldBuilderFormView()
    private val playtests = mutableListOf<String>()
    private val textPrompt = FakeTextPrompt(answer = "The Sunken Keep")
    private val menuView = FakeWorldBuilderMenuView()
    private val worldFileSaver = FakeWorldFileSaver()
    private val worldFilePicker = FakeWorldFilePicker()
    private val browser = FakeBrowser()
    private var leftCount = 0

    init {
        WorldBuilderPresenter(
            battlefieldView = battlefieldView,
            infoView = infoView,
            hudView = hudView,
            resizeView = resizeView,
            detailsView = detailsView,
            stockView = stockView,
            formView = formView,
            menuView = menuView,
            worldBuilderApi = worldBuilderApi,
            battlefieldApi = apis.battlefieldApi,
            terrainApi = apis.terrainApi,
            messages = MessagesMother.messages(),
            worldFileSaver = worldFileSaver,
            worldFilePicker = worldFilePicker,
            textPrompt = textPrompt,
            browser = browser,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            onLeft = { leftCount++ },
            onPlaytest = { worldFile -> playtests += worldFile },
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
        menuView.displayedHistory shouldBe (false to false)
    }

    @Test
    fun `TilePainted should display an edit to undo when a tile is painted`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        // When
        selectTile(row = 2, column = 3)
        // Then
        menuView.displayedHistory shouldBe (true to false)
    }

    @Test
    fun `EditUndone should display the world before the last edit and an edit to redo when undo is selected`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        selectTile(row = 2, column = 3)
        // When
        menuView.delegate!!.undoSelected()
        eventBus.dispatch()
        // Then
        battlefieldView.displayedBattlefields.last() shouldBe BattlefieldMapper.terrain(apis.battlefieldApi.previewBattlefield(List(16) { List(16) { "sand" } }))
        menuView.displayedHistory shouldBe (false to true)
    }

    @Test
    fun `EditRedone should display the world after the edit undone when redo is selected`() {
        // Given
        givenWorldCreated()
        resizeView.delegate!!.sizeChanged(rows = 4, columns = 2)
        menuView.delegate!!.undoSelected()
        eventBus.dispatch()
        // When
        menuView.delegate!!.redoSelected()
        eventBus.dispatch()
        // Then
        battlefieldView.displayedBattlefields.last() shouldBe BattlefieldMapper.terrain(apis.battlefieldApi.previewBattlefield(List(4) { List(2) { "sand" } }))
        resizeView.displayedSize shouldBe (4 to 2)
        menuView.displayedHistory shouldBe (true to false)
    }

    @Test
    fun `EditUndone should display Undo when undo is selected in the menu`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        selectTile(row = 2, column = 3)
        // When
        menuView.delegate!!.undoSelected()
        eventBus.dispatch()
        // Then
        infoView.infos.shouldContainExactly("Undo")
    }

    @Test
    fun `FingersTapped should undo the last edit and display Undo when two fingers tap the world`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        selectTile(row = 2, column = 3)
        // When
        battlefieldView.delegate!!.fingersTapped(2)
        eventBus.dispatch()
        // Then
        battlefieldView.displayedBattlefields.last() shouldBe BattlefieldMapper.terrain(apis.battlefieldApi.previewBattlefield(List(16) { List(16) { "sand" } }))
        menuView.displayedHistory shouldBe (false to true)
        infoView.infos.shouldContainExactly("Undo")
    }

    @Test
    fun `FingersTapped should redo the edit undone and display Redo when three fingers tap the world`() {
        // Given
        givenWorldCreated()
        resizeView.delegate!!.sizeChanged(rows = 4, columns = 2)
        battlefieldView.delegate!!.fingersTapped(2)
        eventBus.dispatch()
        // When
        battlefieldView.delegate!!.fingersTapped(3)
        eventBus.dispatch()
        // Then
        battlefieldView.displayedBattlefields.last() shouldBe BattlefieldMapper.terrain(apis.battlefieldApi.previewBattlefield(List(4) { List(2) { "sand" } }))
        menuView.displayedHistory shouldBe (true to false)
        infoView.infos.shouldContainExactly("Undo", "Redo")
    }

    @Test
    fun `FingersTapped should display Nothing to undo when two fingers tap a world without edits`() {
        // Given
        givenWorldCreated()
        // When
        battlefieldView.delegate!!.fingersTapped(2)
        eventBus.dispatch()
        // Then
        infoView.infos.shouldContainExactly("Nothing to undo")
        battlefieldView.displayedBattlefields.shouldHaveSize(1)
    }

    @Test
    fun `FingersTapped should display Nothing to redo when three fingers tap with nothing undone`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        selectTile(row = 2, column = 3)
        // When
        battlefieldView.delegate!!.fingersTapped(3)
        eventBus.dispatch()
        // Then
        infoView.infos.shouldContainExactly("Nothing to redo")
        menuView.displayedHistory shouldBe (true to false)
    }

    @Test
    fun `FingersTapped should do nothing when four fingers tap the world`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        selectTile(row = 2, column = 3)
        // When
        battlefieldView.delegate!!.fingersTapped(4)
        eventBus.dispatch()
        // Then
        infoView.infos.shouldBeEmpty()
        menuView.displayedHistory shouldBe (true to false)
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
    fun `Export should save the world in a world file and tell what it lacks to be playable when the world is exported`() {
        // Given
        givenWorldCreated()
        selectTerrain("lava")
        selectTile(row = 0, column = 0)
        // When
        menuView.select("Export")
        // Then
        worldFileSaver.savedFiles.shouldContainExactly("world.toml" to worldBuilderApi.exportWorld())
        infoView.infos.shouldContainExactly("World exported. To be playable, place a battle unit of Human")
    }

    @Test
    fun `Export should tell the world is exported when it is playable`() {
        // Given
        givenWorldCreated()
        val world =
            WorldBuilderMother.file(
                tiles = List(4) { List(4) { "sand" } },
                battleUnits =
                    listOf(
                        WorldBuilder.Dto.BattleUnitDto(id = "hero", unitId = "knight", playerId = "player-one", row = 1, column = 1),
                        WorldBuilder.Dto.BattleUnitDto(id = "rat-1", unitId = "rat", playerId = "player-two", row = 2, column = 2),
                    ),
                quests = listOf(WorldBuilderMother.quest()),
            )
        worldBuilderApi.importWorld(WorldFileCodecMother.codec().encode(world))
        eventBus.dispatch()
        // When
        menuView.select("Export")
        // Then
        infoView.infos.last() shouldBe "World exported"
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
        WorldBuilderApi(apis.terrainApi, apis.battlefieldApi, eventBus, storage, FakeCatalog()).openWorld()
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

    private fun worldFile(tiles: List<List<String>>) = WorldFileCodecMother.codec().encode(WorldBuilderMother.file(tiles))

    private fun importFile(text: String?) {
        worldFilePicker.pickedText = text
        menuView.select("Import")
        eventBus.dispatch()
    }

    @Test
    fun `WorldBuilderMenuView should offer resizing, the details, the quests, playtesting, importing, exporting, how to share and going back when the world builder is displayed`() {
        // Then
        menuView.displayedOptions.map(WorldBuilderMenuView.Option::label).shouldContainExactly("Resize", "Details", "Quests", "Playtest", "Import", "Export", "How to share", "Back")
    }

    @Test
    fun `HowToShare should open how to share a world in the browser when it is selected`() {
        // When
        menuView.select("How to share")
        // Then
        browser.openedUrls.shouldContainExactly("https://github.com/maarkeez/tessavar/blob/main/CONTRIBUTING.md#share-a-world")
    }

    @Test
    fun `Import should display the world in the file and tell it when a world is imported`() {
        // Given
        givenWorldCreated()
        val tiles = listOf(listOf("sand", "lava", "sand"), listOf("sand", "sand", "water"))
        // When
        importFile(worldFile(tiles))
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
        importFile(worldFile(List(129) { List(2) { "sand" } }))
        // Then
        infoView.errors.shouldContainExactly("The world must be 1 to 128 tiles wide and tall")
    }

    @Test
    fun `Import should display the terrain when the world in the file has an unknown terrain`() {
        // Given
        givenWorldCreated()
        // When
        importFile(worldFile(listOf(listOf("sand", "forest"))))
        // Then
        infoView.errors.shouldContainExactly("Unknown terrain: forest")
    }

    @Test
    fun `Import should display the terrains when two adjacent tiles of the world in the file can not be next to each other`() {
        // Given
        givenWorldCreated()
        // When
        importFile(worldFile(listOf(listOf("water", "lava"))))
        // Then
        infoView.errors.shouldContainExactly("Water can not be next to Lava")
    }

    @Test
    fun `Import should display why when the world in the file is of another world version`() {
        // Given
        givenWorldCreated()
        // When
        importFile(WorldFileCodecMother.codec().encode(WorldBuilderMother.file(listOf(listOf("sand")), version = 2)))
        // Then
        infoView.errors.shouldContainExactly("The world in the file is not of world version 1")
    }

    @Test
    fun `Details should open the details dialog with the name and the author of the world when Details is selected`() {
        // Given
        givenWorldCreated()
        worldBuilderApi.renameWorld(name = "The Sunken Keep", author = "maarkeez")
        // When
        menuView.select("Details")
        // Then
        detailsView.openedWith shouldBe ("The Sunken Keep" to "maarkeez")
    }

    @Test
    fun `DetailsChanged should name the world, and Export save it in a file named after its id, when the details are changed`() {
        // Given
        givenWorldCreated()
        // When
        detailsView.delegate!!.detailsChanged(name = "The Sunken Keep", author = "maarkeez")
        eventBus.dispatch()
        menuView.select("Export")
        // Then
        worldFileSaver.savedFiles.single().first shouldBe "the-sunken-keep.toml"
    }

    @Test
    fun `FieldTapped should ask for the text with a prompt and display it when the platform needs a prompt`() {
        // Given
        givenWorldCreated()
        // When
        detailsView.delegate!!.fieldTapped(WorldBuilderDetailsView.Field.NAME)
        // Then
        textPrompt.prompts.shouldContainExactly("Name" to "")
        detailsView.displayedFields.shouldContainExactly(WorldBuilderDetailsView.Field.NAME to "The Sunken Keep")
    }

    private fun selectTab(tab: WorldBuilderHudView.Tab) {
        hudView.delegate!!.tabSelected(tab)
        eventBus.dispatch()
    }

    @Test
    fun `WorldOpened should display the buildings, the units in a row for each player and the npcs of the catalog, the first of each selected`() {
        // When
        givenWorldCreated()
        // Then
        hudView.displayedBuildings.map(WorldBuilderHudView.PaletteOption::id).shouldContainExactly("fortress")
        hudView.displayedUnitRows.map { row -> row.title to row.units.map(WorldBuilderHudView.PaletteOption::id) } shouldBe
            listOf("Human" to listOf("knight", "rat"), "CPU" to listOf("knight", "rat"))
        hudView.displayedNpcs.map(WorldBuilderHudView.PaletteOption::id).shouldContainExactly("alchemist", "viking")
        hudView.selectedBuildingId shouldBe "fortress"
        hudView.selectedUnit shouldBe ("player-one" to "knight")
        hudView.selectedNpcId shouldBe "alchemist"
        hudView.isStockEnabled shouldBe false
    }

    @Test
    fun `TabSelected should display the tab and take Paint back from Fill when a tab other than Terrain is selected`() {
        // Given
        givenWorldCreated()
        selectTool(WorldBuilderHudView.Tool.FILL)
        // When
        selectTab(WorldBuilderHudView.Tab.UNITS)
        // Then
        hudView.displayedTabs.last() shouldBe WorldBuilderHudView.Tab.UNITS
        hudView.displayedTool shouldBe WorldBuilderHudView.Tool.PAINT
    }

    @Test
    fun `TileSelected should place a battle unit of the player selected on the tile when the Units tab is displayed`() {
        // Given
        givenWorldCreated()
        selectTab(WorldBuilderHudView.Tab.UNITS)
        hudView.delegate!!.unitSelected("player-two", "rat")
        // When
        selectTile(row = 3, column = 4)
        // Then
        battlefieldView.displayedBattleUnits.last() shouldBe FakeBattlefieldView.BattleUnitAt(row = 3, column = 4, unitId = "rat")
        worldBuilderApi
            .searchWorld()!!
            .battleUnits
            .single()
            .playerId shouldBe "player-two"
    }

    @Test
    fun `TileSelected should place the building selected on the tile when the Buildings tab is displayed`() {
        // Given
        givenWorldCreated()
        selectTab(WorldBuilderHudView.Tab.BUILDINGS)
        // When
        selectTile(row = 3, column = 4)
        // Then
        battlefieldView.displayedBuildings.last() shouldBe FakeBattlefieldView.BuildingAt(row = 3, column = 4, buildingId = "fortress")
    }

    @Test
    fun `OccupantPlaceRejected should display why when something already stands on the tile`() {
        // Given
        givenWorldCreated()
        selectTab(WorldBuilderHudView.Tab.NPCS)
        selectTile(row = 3, column = 4)
        hudView.delegate!!.npcSelected("viking")
        // When
        selectTile(row = 3, column = 4)
        // Then
        infoView.errors.last() shouldBe "Something already stands there"
    }

    @Test
    fun `TileSelected should erase what stands on the tile when Erase is selected, on any tab`() {
        // Given
        givenWorldCreated()
        selectTab(WorldBuilderHudView.Tab.NPCS)
        selectTile(row = 3, column = 4)
        selectTab(WorldBuilderHudView.Tab.TERRAIN)
        selectTool(WorldBuilderHudView.Tool.ERASE)
        battlefieldView.displayedNpcs.clear()
        // When
        selectTile(row = 3, column = 4)
        // Then
        worldBuilderApi.searchWorld()!!.npcs.shouldBeEmpty()
        battlefieldView.displayedNpcs.shouldBeEmpty()
    }

    @Test
    fun `OccupiedTilePaintRejected should display why when a terrain nothing can stand on is painted under a battle unit`() {
        // Given
        givenWorldCreated()
        selectTab(WorldBuilderHudView.Tab.UNITS)
        selectTile(row = 3, column = 4)
        selectTab(WorldBuilderHudView.Tab.TERRAIN)
        selectTerrain("lava")
        // When
        selectTile(row = 3, column = 4)
        // Then
        infoView.errors.last() shouldBe "Something stands on or next to this tile: Lava would leave it nowhere to stand"
    }

    @Test
    fun `StockSelected should open what the npc selected sells, and Done keep the items chosen, when the npc stands in the world`() {
        // Given
        givenWorldCreated()
        selectTab(WorldBuilderHudView.Tab.NPCS)
        selectTile(row = 3, column = 4)
        // When
        hudView.delegate!!.stockSelected()
        stockView.delegate!!.stockChanged("alchemist", listOf("mana-potion"))
        eventBus.dispatch()
        // Then
        hudView.isStockEnabled shouldBe true
        stockView.openedStock shouldBe emptyList()
        stockView.openedItems.map(WorldBuilderStockView.StockItem::lootableItemId).shouldContainExactly("health-potion", "mana-potion")
        worldBuilderApi
            .searchWorld()!!
            .npcs
            .single()
            .stock shouldBe listOf("mana-potion")
    }

    @Test
    fun `Playtest should play the world exported when it has a battle unit of the human player and a main quest`() {
        // Given
        givenWorldCreated()
        val world =
            WorldBuilderMother.file(
                tiles = List(4) { List(4) { "sand" } },
                battleUnits = listOf(WorldBuilder.Dto.BattleUnitDto(id = "hero", unitId = "knight", playerId = "player-one", row = 1, column = 1)),
                quests = listOf(WorldBuilderMother.quest()),
            )
        worldBuilderApi.importWorld(WorldFileCodecMother.codec().encode(world))
        eventBus.dispatch()
        // When
        menuView.select("Playtest")
        // Then
        playtests.shouldContainExactly(worldBuilderApi.exportWorld())
    }

    @Test
    fun `Playtest should display why and play nothing when the world has no battle unit of the human player`() {
        // Given
        givenWorldCreated()
        // When
        menuView.select("Playtest")
        // Then
        playtests.shouldBeEmpty()
        infoView.errors.last() shouldBe "To playtest, place a battle unit of Human"
    }

    @Test
    fun `Playtest should display why and play nothing when the world has no main quest`() {
        // Given
        givenWorldCreated()
        selectTab(WorldBuilderHudView.Tab.UNITS)
        selectTile(row = 3, column = 4)
        // When
        menuView.select("Playtest")
        // Then
        playtests.shouldBeEmpty()
        infoView.errors.last() shouldBe "To playtest, add one main quest"
    }

    private fun openQuests() {
        givenWorldCreated()
        menuView.select("Quests")
    }

    private fun form() = formView.delegate!!

    @Test
    fun `Quests should list the quests of the world, and offer adding the main quest and a side quest`() {
        // When
        openQuests()
        // Then
        formView.openedTitle shouldBe "Quests"
        formView.labels().shouldContainExactly("Add the main quest", "Add a side quest")
    }

    @Test
    fun `QuestAdded should open the quest added, with its type, name, start tiles, tasks and reward`() {
        // Given
        openQuests()
        // When
        form().actionSelected(WorldBuilderQuestForms.ADD_MAIN)
        eventBus.dispatch()
        // Then
        formView.openedTitle shouldBe "quest-1"
        formView.labels().take(5).shouldContainExactly("Type: Main quest", "Name: ", "Description: ", "Start tiles: 0 tiles: tap to pick", "Tasks")
    }

    @Test
    fun `TextChanged should name the quest, and Done list the quests again`() {
        // Given
        openQuests()
        form().actionSelected(WorldBuilderQuestForms.ADD_SIDE)
        eventBus.dispatch()
        // When
        form().textChanged(WorldBuilderQuestForms.NAME, "Rats in the Fields")
        eventBus.dispatch()
        form().formClosed()
        // Then
        worldBuilderApi
            .searchWorld()!!
            .quests
            .single()
            .name shouldBe "Rats in the Fields"
        formView.labels().first() shouldBe "Side quest: Rats in the Fields"
    }

    @Test
    fun `TileSelected should pick the start tiles tapped, and highlight them, while they are being picked`() {
        // Given
        openQuests()
        form().actionSelected(WorldBuilderQuestForms.ADD_MAIN)
        eventBus.dispatch()
        form().toggled(WorldBuilderQuestForms.START_TILES)
        // When
        selectTile(row = 2, column = 3)
        selectTile(row = 4, column = 5)
        selectTile(row = 2, column = 3)
        // Then
        worldBuilderApi
            .searchWorld()!!
            .quests
            .single()
            .startTiles
            .shouldContainExactly(WorldBuilder.Dto.TileDto(row = 4, column = 5))
        battlefieldView.tileSelections.last() shouldBe TilePosition(4, 5)
        worldBuilderApi.searchWorld()!!.tiles[2][3] shouldBe "sand"
    }

    @Test
    fun `ActionSelected should add a task to defeat a unit of the CPU player when Add a task is tapped`() {
        // Given
        givenWorldCreated()
        selectTab(WorldBuilderHudView.Tab.UNITS)
        hudView.delegate!!.unitSelected("player-two", "rat")
        selectTile(row = 3, column = 4)
        menuView.select("Quests")
        form().actionSelected(WorldBuilderQuestForms.ADD_SIDE)
        eventBus.dispatch()
        // When
        form().actionSelected(WorldBuilderQuestForms.ADD_TASK)
        eventBus.dispatch()
        // Then
        val task =
            worldBuilderApi
                .searchWorld()!!
                .quests
                .single()
                .tasks
                .single()
        (task.type to task.unitId) shouldBe (WorldBuilder.Dto.TaskTypeDto.DEFEAT to "rat")
        formView.labels() shouldContain "Unit: Rat"
    }

    @Test
    fun `ActionSelected should take the quest away and list the quests again when Remove the quest is tapped`() {
        // Given
        openQuests()
        form().actionSelected(WorldBuilderQuestForms.ADD_SIDE)
        eventBus.dispatch()
        // When
        form().actionSelected(WorldBuilderQuestForms.REMOVE)
        eventBus.dispatch()
        // Then
        worldBuilderApi.searchWorld()!!.quests.shouldBeEmpty()
        formView.openedTitle shouldBe "Quests"
    }

    @Test
    fun `QuestRejected should display why when a second main quest is added`() {
        // Given
        openQuests()
        form().actionSelected(WorldBuilderQuestForms.ADD_MAIN)
        eventBus.dispatch()
        // When
        worldBuilderApi.addQuest(WorldBuilder.Dto.QuestTypeDto.MAIN)
        eventBus.dispatch()
        // Then
        infoView.errors.last() shouldBe "The world already has a main quest"
    }
}
