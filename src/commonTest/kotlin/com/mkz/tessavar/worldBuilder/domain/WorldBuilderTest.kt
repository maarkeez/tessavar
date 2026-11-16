package com.mkz.tessavar.worldBuilder.domain

import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.TileNotFound
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.AreaFilled
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.EditRedone
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.EditUndone
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.NpcRestocked
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.OccupantErased
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.OccupantPlaceRejected
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.OccupantPlaced
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.OccupiedTilePaintRejected
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.QuestAdded
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.QuestEdited
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.QuestRejected
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.QuestRemoved
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.TerrainSelected
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.TilePaintRejected
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.TilePainted
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldImportRejected
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldImportRejected.Reason
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldImported
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldOpened
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldResized
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class WorldBuilderTest {
    private val canBeAdjacent = { terrainId: String, adjacentTerrainId: String -> "sand" in setOf(terrainId, adjacentTerrainId) }
    private val isOccupiable = { terrainId: String -> terrainId != "void" }
    private val catalog = WorldBuilderMother.catalog()
    private val newWorldReasons =
        listOf(WorldBuilder.Dto.UnplayableReasonDto.NO_HUMAN_BATTLE_UNIT, WorldBuilder.Dto.UnplayableReasonDto.NO_CPU_BATTLE_UNIT, WorldBuilder.Dto.UnplayableReasonDto.NO_MAIN_QUEST)

    private fun newWorld(): WorldBuilder = WorldBuilder.create().pullEvents().second

    @Test
    fun `should be 16 x 16 sand tiles with sand selected when it is created`() {
        // When
        val (events, worldBuilder) = WorldBuilder.create().pullEvents()
        // Then
        worldBuilder.toDto() shouldBe
            WorldBuilder.Dto(
                rows = 16,
                columns = 16,
                tiles =
                    List(16) {
                        List(16) { "sand" }
                    },
                selectedTerrainId = "sand",
                canUndo = false,
                canRedo = false,
                name = "",
                author = "",
                buildings = emptyList(),
                battleUnits = emptyList(),
                npcs = emptyList(),
                quests = emptyList(),
                unplayableReasons = newWorldReasons,
            )
        events.shouldContainExactly(WorldOpened(rows = 16, columns = 16))
    }

    @Test
    fun `should keep the tiles that fit and fill the new ones with sand when it is resized`() {
        // Given
        val worldBuilder =
            newWorld()
                .selectTerrain("lava")
                .paint(row = 0, column = 1, canBeAdjacent, isOccupiable)
                .paint(row = 1, column = 0, canBeAdjacent, isOccupiable)
                .pullEvents()
                .second
        // When
        val (events, resized) = worldBuilder.resize(rows = 1, columns = 3).pullEvents()
        // Then
        resized.toDto().tiles shouldBe listOf(listOf("sand", "lava", "sand"))
        events.shouldContainExactly(WorldResized(rows = 1, columns = 3))
    }

    @Test
    fun `should grow up to 128 rows and columns when it is resized to the maximum size`() {
        // When
        val resized = newWorld().resize(rows = 128, columns = 128)
        // Then
        (resized.toDto().rows to resized.toDto().columns) shouldBe (128 to 128)
        resized
            .toDto()
            .tiles
            .flatten()
            .toSet() shouldBe setOf("sand")
    }

    @Test
    fun `should be ignored when it is resized out of 1 to 128 rows or columns`() {
        // Given
        val worldBuilder = newWorld()
        // When
        val resizedWorlds = listOf(worldBuilder.resize(rows = 0, columns = 16), worldBuilder.resize(rows = 16, columns = 129))
        // Then
        resizedWorlds.forEach { resized -> resized shouldBe worldBuilder }
    }

    @Test
    fun `should announce nothing when it is resized to its size`() {
        // When
        val (events, _) = newWorld().resize(rows = 16, columns = 16).pullEvents()
        // Then
        events.shouldBeEmpty()
    }

    @Test
    fun `should select the terrain the tiles are painted with when a terrain is selected`() {
        // When
        val (events, worldBuilder) = newWorld().selectTerrain("water").pullEvents()
        // Then
        worldBuilder.toDto().selectedTerrainId shouldBe "water"
        events.shouldContainExactly(TerrainSelected(terrainId = "water"))
    }

    @Test
    fun `should paint the tile with the selected terrain when its adjacent tiles can be next to it`() {
        // Given
        val worldBuilder = newWorld().selectTerrain("lava").pullEvents().second
        // When
        val (events, painted) = worldBuilder.paint(row = 2, column = 3, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        painted.toDto().tiles[2][3] shouldBe "lava"
        events.shouldContainExactly(TilePainted(row = 2, column = 3, terrainId = "lava"))
    }

    @Test
    fun `should reject the paint and keep the tile when the selected terrain can not be next to an adjacent tile`() {
        // Given
        val worldBuilder =
            newWorld()
                .selectTerrain("lava")
                .paint(row = 2, column = 3, canBeAdjacent, isOccupiable)
                .selectTerrain("water")
                .pullEvents()
                .second
        // When
        val (events, rejected) = worldBuilder.paint(row = 2, column = 4, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        rejected.toDto().tiles[2][4] shouldBe "sand"
        events.shouldContainExactly(TilePaintRejected(row = 2, column = 4, terrainId = "water", adjacentTerrainId = "lava"))
    }

    @Test
    fun `should announce nothing when the tile already has the selected terrain`() {
        // When
        val (events, _) = newWorld().paint(row = 0, column = 0, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        events.shouldBeEmpty()
    }

    @Test
    fun `should fail when the painted tile is out of the world`() {
        // When
        val paint = { newWorld().paint(row = 16, column = 0, canBeAdjacent, isOccupiable) }
        // Then
        shouldThrow<TileNotFound> { paint() }
    }

    private fun worldOf(vararg rows: String): WorldBuilder =
        newWorld()
            .import(
                WorldBuilderMother.file(
                    rows.map { row ->
                        row.split(" ")
                    },
                ),
                currentVersion = 1,
                catalog = catalog,
                isKnownTerrain = { true },
                canBeAdjacent = { _, _ -> true },
                isOccupiable = isOccupiable,
            ).pullEvents()
            .second

    @Test
    fun `should fill the connected area of one terrain when no tile would be adjacent to a forbidden terrain`() {
        // Given
        val worldBuilder =
            worldOf(
                "sand sand lava",
                "sand lava sand",
                "lava sand sand",
            ).selectTerrain("lava").pullEvents().second
        // When
        val (events, filled) = worldBuilder.fill(row = 0, column = 0, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        filled.toDto().tiles shouldBe
            listOf(
                listOf("lava", "lava", "lava"),
                listOf("lava", "lava", "sand"),
                listOf("lava", "sand", "sand"),
            )
        events.shouldContainExactly(AreaFilled(row = 0, column = 0, terrainId = "lava", tileCount = 3))
    }

    @Test
    fun `should reject the fill and keep every tile when one tile would be adjacent to a forbidden terrain`() {
        // Given
        val worldBuilder =
            worldOf(
                "sand sand sand",
                "sand sand sand",
                "sand sand lava",
            ).selectTerrain("water").pullEvents().second
        // When
        val (events, rejected) = worldBuilder.fill(row = 0, column = 0, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        rejected.toDto().tiles shouldBe worldBuilder.toDto().tiles
        events.shouldContainExactly(TilePaintRejected(row = 0, column = 0, terrainId = "water", adjacentTerrainId = "lava"))
    }

    @Test
    fun `should announce nothing when the filled area already has the selected terrain`() {
        // When
        val (events, _) = newWorld().fill(row = 0, column = 0, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        events.shouldBeEmpty()
    }

    @Test
    fun `should fail when the filled tile is out of the world`() {
        // When
        val fill = { newWorld().fill(row = 0, column = 16, canBeAdjacent, isOccupiable) }
        // Then
        shouldThrow<TileNotFound> { fill() }
    }

    private val isKnownTerrain = { terrainId: String -> terrainId in setOf("sand", "lava", "water") }

    @Test
    fun `should replace the tiles and keep the selected terrain when a world is imported`() {
        // Given
        val worldBuilder = newWorld().selectTerrain("lava").pullEvents().second
        val tiles = listOf(listOf("sand", "lava", "lava"), listOf("water", "sand", "sand"))
        // When
        val (events, imported) = worldBuilder.import(WorldBuilderMother.file(tiles), currentVersion = 1, catalog, isKnownTerrain, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        imported.toDto() shouldBe
            WorldBuilder.Dto(
                rows = 2,
                columns = 3,
                tiles = tiles,
                selectedTerrainId = "lava",
                canUndo = true,
                canRedo = false,
                name = "",
                author = "",
                buildings = emptyList(),
                battleUnits = emptyList(),
                npcs = emptyList(),
                quests = emptyList(),
                unplayableReasons = newWorldReasons,
            )
        events.shouldContainExactly(WorldImported(rows = 2, columns = 3))
    }

    @Test
    fun `should reject the import and keep the world when the imported text is not a world`() {
        // Given
        val worldBuilder = newWorld()
        // When
        val (events, rejected) = worldBuilder.import(null, currentVersion = 1, catalog, isKnownTerrain, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        rejected shouldBe worldBuilder
        events.shouldContainExactly(WorldImportRejected(reason = Reason.NOT_A_WORLD))
    }

    @Test
    fun `should reject the import when the imported world is larger than 128 rows or columns`() {
        // When
        val (events, _) = newWorld().import(WorldBuilderMother.file(List(129) { List(4) { "sand" } }), currentVersion = 1, catalog, isKnownTerrain, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        events.shouldContainExactly(WorldImportRejected(reason = Reason.SIZE_OUT_OF_RANGE))
    }

    @Test
    fun `should reject the import when the imported world has no columns`() {
        // When
        val (events, _) = newWorld().import(WorldBuilderMother.file(listOf(emptyList())), currentVersion = 1, catalog, isKnownTerrain, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        events.shouldContainExactly(WorldImportRejected(reason = Reason.SIZE_OUT_OF_RANGE))
    }

    @Test
    fun `should reject the import with the terrain when the imported world has an unknown terrain`() {
        // When
        val (events, _) = newWorld().import(WorldBuilderMother.file(listOf(listOf("sand", "forest"))), currentVersion = 1, catalog, isKnownTerrain, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        events.shouldContainExactly(WorldImportRejected(reason = Reason.UNKNOWN_TERRAIN, terrainId = "forest"))
    }

    @Test
    fun `should reject the import with the terrains when two adjacent tiles of the imported world can not be next to each other`() {
        // Given
        val tiles = listOf(listOf("lava", "sand"), listOf("water", "sand"))
        // When
        val (events, rejected) = newWorld().import(WorldBuilderMother.file(tiles), currentVersion = 1, catalog, isKnownTerrain, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        events.shouldContainExactly(WorldImportRejected(reason = Reason.FORBIDDEN_ADJACENT_TERRAINS, terrainId = "lava", adjacentTerrainId = "water"))
        rejected.toDto().rows shouldBe 16
    }

    private fun WorldBuilder.paintLava(vararg positions: Pair<Int, Int>): WorldBuilder =
        positions.fold(selectTerrain("lava")) { worldBuilder, (row, column) -> worldBuilder.paint(row, column, canBeAdjacent, isOccupiable) }.pullEvents().second

    @Test
    fun `should undo the last edit when undoing`() {
        // Given
        val worldBuilder = newWorld().paintLava(0 to 0, 0 to 1)
        // When
        val (events, undone) = worldBuilder.undo().pullEvents()
        // Then
        undone.toDto().tiles[0].take(2) shouldBe listOf("lava", "sand")
        undone.toDto().canRedo shouldBe true
        events.shouldContainExactly(EditUndone(rows = 16, columns = 16))
    }

    @Test
    fun `should undo the resize when undoing after a resize`() {
        // Given
        val worldBuilder =
            newWorld()
                .paintLava(15 to 15)
                .resize(rows = 8, columns = 4)
                .pullEvents()
                .second
        // When
        val (events, undone) = worldBuilder.undo().pullEvents()
        // Then
        undone.toDto().tiles[15][15] shouldBe "lava"
        events.shouldContainExactly(EditUndone(rows = 16, columns = 16))
    }

    @Test
    fun `should redo the undone edit when redoing`() {
        // Given
        val worldBuilder =
            newWorld()
                .paintLava(0 to 0)
                .undo()
                .pullEvents()
                .second
        // When
        val (events, redone) = worldBuilder.redo().pullEvents()
        // Then
        redone.toDto().tiles[0][0] shouldBe "lava"
        redone.toDto().canRedo shouldBe false
        events.shouldContainExactly(EditRedone(rows = 16, columns = 16))
    }

    @Test
    fun `should forget the undone edits when an edit is made after undoing`() {
        // Given
        val worldBuilder =
            newWorld()
                .paintLava(0 to 0)
                .undo()
                .pullEvents()
                .second
        // When
        val edited = worldBuilder.paintLava(1 to 1)
        // Then
        edited.toDto().canRedo shouldBe false
    }

    @Test
    fun `should forget the oldest edit when more than 50 edits are made`() {
        // Given
        val positions = (0 until 51).map { index -> index / 16 to index % 16 }
        val worldBuilder = newWorld().paintLava(*positions.toTypedArray())
        // When
        val undone = (1..50).fold(worldBuilder) { undoing, _ -> undoing.undo() }.pullEvents().second
        // Then
        undone.toDto().canUndo shouldBe false
        undone.toDto().tiles[0][0] shouldBe "lava"
        undone.toDto().tiles[0][1] shouldBe "sand"
    }

    @Test
    fun `should announce nothing when there is nothing to undo or redo`() {
        // When
        val (events, _) =
            newWorld()
                .undo()
                .redo()
                .pullEvents()
        // Then
        events.shouldBeEmpty()
    }

    @Test
    fun `should not count selecting a terrain as an edit when a terrain is selected`() {
        // When
        val worldBuilder = newWorld().selectTerrain("lava")
        // Then
        worldBuilder.toDto().canUndo shouldBe false
    }

    @Test
    fun `should hold the tiles kept with sand selected and nothing to undo when it is restored`() {
        // When
        val (events, restored) = WorldBuilder.restore(WorldBuilderMother.file(listOf(listOf("lava", "sand")), name = "The Keep", author = "maarkeez"), catalog).pullEvents()
        // Then
        restored.toDto() shouldBe
            WorldBuilder.Dto(
                rows = 1,
                columns = 2,
                tiles = listOf(listOf("lava", "sand")),
                selectedTerrainId = "sand",
                canUndo = false,
                canRedo = false,
                name = "The Keep",
                author = "maarkeez",
                buildings = emptyList(),
                battleUnits = emptyList(),
                npcs = emptyList(),
                quests = emptyList(),
                unplayableReasons = newWorldReasons,
            )
        events.shouldBeEmpty()
    }

    @Test
    fun `should fail when the tiles restored have no columns`() {
        // When
        val restore = { WorldBuilder.restore(WorldBuilderMother.file(listOf(emptyList())), catalog) }
        // Then
        shouldThrowAny { restore() }
    }

    @Test
    fun `should announce its size when it is opened again`() {
        // When
        val (events, _) =
            newWorld()
                .resize(rows = 3, columns = 5)
                .pullEvents()
                .second
                .reopen()
                .pullEvents()
        // Then
        events.shouldContainExactly(WorldOpened(rows = 3, columns = 5))
    }

    @Test
    fun `should hold the name and the author typed, and undo them, when it is renamed`() {
        // When
        val (events, renamed) = newWorld().rename(name = "The Sunken Keep", author = "maarkeez").pullEvents()
        // Then
        (renamed.toDto().name to renamed.toDto().author) shouldBe ("The Sunken Keep" to "maarkeez")
        events.shouldContainExactly(WorldBuilderEvent.WorldRenamed(name = "The Sunken Keep", author = "maarkeez"))
        renamed.undo().toDto().name shouldBe ""
    }

    @Test
    fun `should announce nothing when it is renamed to its name and author`() {
        // When
        val (events, _) = newWorld().rename(name = "", author = "").pullEvents()
        // Then
        events.shouldBeEmpty()
    }

    private fun voidAt(
        row: Int,
        column: Int,
    ): WorldBuilder =
        newWorld()
            .selectTerrain("void")
            .paint(row = row, column = column, canBeAdjacent, isOccupiable)
            .selectTerrain("sand")
            .pullEvents()
            .second

    @Test
    fun `WorldBuilder should place a battle unit of the player, named after its unit, when its tile is vacant and occupiable`() {
        // Given
        val worldBuilder = newWorld().placeBattleUnit(unitId = "rat", playerId = "player-two", row = 1, column = 1, isOccupiable).pullEvents().second
        // When
        val (events, placed) = worldBuilder.placeBattleUnit(unitId = "rat", playerId = "player-two", row = 2, column = 3, isOccupiable).pullEvents()
        // Then
        placed.toDto().battleUnits shouldBe
            listOf(
                WorldBuilder.Dto.BattleUnitDto(id = "rat-1", unitId = "rat", playerId = "player-two", row = 1, column = 1),
                WorldBuilder.Dto.BattleUnitDto(id = "rat-2", unitId = "rat", playerId = "player-two", row = 2, column = 3),
            )
        placed.toDto().canUndo shouldBe true
        events.shouldContainExactly(OccupantPlaced(occupantId = "rat-2", row = 2, column = 3))
    }

    @Test
    fun `WorldBuilder should reject placing a building when it would stand on void`() {
        // When
        val (events, rejected) = voidAt(row = 3, column = 3).placeBuilding(buildingId = "fortress", row = 2, column = 2, catalog, isOccupiable).pullEvents()
        // Then
        rejected.toDto().buildings.shouldBeEmpty()
        events.shouldContainExactly(OccupantPlaceRejected(row = 2, column = 2, reason = OccupantPlaceRejected.Reason.NOT_OCCUPIABLE, terrainId = "void"))
    }

    @Test
    fun `WorldBuilder should reject placing a battle unit when void stands above or below it and to one side of it, as in the game`() {
        // Given
        val worldBuilder =
            voidAt(row = 1, column = 2)
                .selectTerrain("void")
                .paint(row = 2, column = 1, canBeAdjacent, isOccupiable)
                .pullEvents()
                .second
        // When
        val (events, rejected) = worldBuilder.placeBattleUnit(unitId = "knight", playerId = "player-one", row = 2, column = 2, isOccupiable).pullEvents()
        // Then
        rejected.toDto().battleUnits.shouldBeEmpty()
        events.shouldContainExactly(OccupantPlaceRejected(row = 2, column = 2, reason = OccupantPlaceRejected.Reason.CORNERED, terrainId = "void"))
    }

    @Test
    fun `WorldBuilder should place a battle unit when void stands only above and below it`() {
        // Given
        val worldBuilder =
            voidAt(row = 1, column = 2)
                .selectTerrain("void")
                .paint(row = 3, column = 2, canBeAdjacent, isOccupiable)
                .pullEvents()
                .second
        // When
        val (events, placed) = worldBuilder.placeBattleUnit(unitId = "knight", playerId = "player-one", row = 2, column = 2, isOccupiable).pullEvents()
        // Then
        placed
            .toDto()
            .battleUnits
            .map(WorldBuilder.Dto.BattleUnitDto::id)
            .shouldContainExactly("knight-1")
        events.shouldContainExactly(OccupantPlaced(occupantId = "knight-1", row = 2, column = 2))
    }

    @Test
    fun `WorldBuilder should reject placing a building when it would stand partly out of the world`() {
        // When
        val (events, _) = newWorld().placeBuilding(buildingId = "fortress", row = 15, column = 0, catalog, isOccupiable).pullEvents()
        // Then
        events.shouldContainExactly(OccupantPlaceRejected(row = 15, column = 0, reason = OccupantPlaceRejected.Reason.OUT_OF_WORLD))
    }

    @Test
    fun `WorldBuilder should reject placing an npc when a building stands on its tile`() {
        // Given
        val worldBuilder = newWorld().placeBuilding(buildingId = "fortress", row = 2, column = 2, catalog, isOccupiable).pullEvents().second
        // When
        val (events, rejected) = worldBuilder.placeNpc(npcId = "alchemist", row = 3, column = 3, isOccupiable).pullEvents()
        // Then
        rejected.toDto().npcs.shouldBeEmpty()
        events.shouldContainExactly(OccupantPlaceRejected(row = 3, column = 3, reason = OccupantPlaceRejected.Reason.NOT_VACANT))
    }

    @Test
    fun `WorldBuilder should move a building when it is placed again, as it stands at most once in a world`() {
        // Given
        val worldBuilder = newWorld().placeBuilding(buildingId = "fortress", row = 2, column = 2, catalog, isOccupiable).pullEvents().second
        // When
        val (events, moved) = worldBuilder.placeBuilding(buildingId = "fortress", row = 3, column = 3, catalog, isOccupiable).pullEvents()
        // Then
        moved.toDto().buildings.shouldContainExactly(WorldBuilder.Dto.BuildingDto(id = "fortress", row = 3, column = 3))
        events.shouldContainExactly(OccupantPlaced(occupantId = "fortress", row = 3, column = 3))
    }

    @Test
    fun `WorldBuilder should keep what an npc sells when it is moved`() {
        // Given
        val worldBuilder =
            newWorld()
                .placeNpc(npcId = "alchemist", row = 2, column = 2, isOccupiable)
                .restock(npcId = "alchemist", stock = listOf("mana-potion"))
                .pullEvents()
                .second
        // When
        val moved = worldBuilder.placeNpc(npcId = "alchemist", row = 5, column = 6, isOccupiable)
        // Then
        moved.toDto().npcs.shouldContainExactly(WorldBuilder.Dto.NpcDto(id = "alchemist", row = 5, column = 6, stock = listOf("mana-potion")))
    }

    @Test
    fun `WorldBuilder should erase the building standing on any of its tiles, and undo bring it back`() {
        // Given
        val worldBuilder = newWorld().placeBuilding(buildingId = "fortress", row = 2, column = 2, catalog, isOccupiable).pullEvents().second
        // When
        val (events, erased) = worldBuilder.erase(row = 3, column = 3).pullEvents()
        // Then
        erased.toDto().buildings.shouldBeEmpty()
        events.shouldContainExactly(OccupantErased(occupantId = "fortress", row = 3, column = 3))
        erased
            .undo()
            .toDto()
            .buildings
            .shouldContainExactly(WorldBuilder.Dto.BuildingDto(id = "fortress", row = 2, column = 2))
    }

    @Test
    fun `WorldBuilder should announce nothing when the erased tile is vacant`() {
        // When
        val (events, _) = newWorld().erase(row = 3, column = 3).pullEvents()
        // Then
        events.shouldBeEmpty()
    }

    @Test
    fun `WorldBuilder should change what a placed npc sells when it is restocked`() {
        // Given
        val worldBuilder = newWorld().placeNpc(npcId = "alchemist", row = 2, column = 2, isOccupiable).pullEvents().second
        // When
        val (events, restocked) = worldBuilder.restock(npcId = "alchemist", stock = listOf("mana-potion", "health-potion")).pullEvents()
        // Then
        restocked
            .toDto()
            .npcs
            .single()
            .stock shouldBe listOf("mana-potion", "health-potion")
        events.shouldContainExactly(NpcRestocked(npcId = "alchemist", stock = listOf("mana-potion", "health-potion")))
    }

    @Test
    fun `WorldBuilder should reject painting a terrain nothing can stand on under a battle unit`() {
        // Given
        val worldBuilder =
            newWorld()
                .placeBattleUnit(unitId = "knight", playerId = "player-one", row = 2, column = 2, isOccupiable)
                .selectTerrain("void")
                .pullEvents()
                .second
        // When
        val (events, rejected) = worldBuilder.paint(row = 2, column = 2, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        rejected.toDto().tiles[2][2] shouldBe "sand"
        events.shouldContainExactly(OccupiedTilePaintRejected(row = 2, column = 2, terrainId = "void"))
    }

    @Test
    fun `WorldBuilder should take away what no longer fits when it is resized`() {
        // Given
        val worldBuilder =
            newWorld()
                .placeBattleUnit(unitId = "knight", playerId = "player-one", row = 1, column = 1, isOccupiable)
                .placeNpc(npcId = "alchemist", row = 5, column = 5, isOccupiable)
                .placeBuilding(buildingId = "fortress", row = 2, column = 2, catalog, isOccupiable)
                .pullEvents()
                .second
        // When
        val resized = worldBuilder.resize(rows = 3, columns = 3).toDto()
        // Then
        resized.battleUnits.map(WorldBuilder.Dto.BattleUnitDto::id).shouldContainExactly("knight-1")
        resized.npcs.shouldBeEmpty()
        resized.buildings.shouldBeEmpty()
    }

    @Test
    fun `WorldBuilder should import the buildings, battle units and npcs of a world file`() {
        // Given
        val file =
            WorldBuilderMother.file(
                tiles = List(4) { List(4) { "sand" } },
                buildings = listOf(WorldBuilder.Dto.BuildingDto(id = "fortress", row = 0, column = 0)),
                battleUnits = listOf(WorldBuilder.Dto.BattleUnitDto(id = "hero", unitId = "knight", playerId = "player-one", row = 3, column = 3)),
                npcs = listOf(WorldBuilder.Dto.NpcDto(id = "alchemist", row = 0, column = 3, stock = listOf("mana-potion"))),
            )
        // When
        val imported = newWorld().import(file, currentVersion = 1, catalog, isKnownTerrain = { true }, canBeAdjacent, isOccupiable).toDto()
        // Then
        imported.buildings shouldBe file.buildings
        imported.battleUnits shouldBe file.battleUnits
        imported.npcs shouldBe file.npcs
    }

    @Test
    fun `WorldBuilder should reject the import with the occupant when the world names one the game does not have`() {
        // Given
        val file = WorldBuilderMother.file(tiles = List(4) { List(4) { "sand" } }, buildings = listOf(WorldBuilder.Dto.BuildingDto(id = "castle", row = 0, column = 0)))
        // When
        val (events, _) = newWorld().import(file, currentVersion = 1, catalog, isKnownTerrain = { true }, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        events.shouldContainExactly(WorldImportRejected(reason = Reason.UNKNOWN_OCCUPANT, occupantId = "castle"))
    }

    @Test
    fun `WorldBuilder should reject painting void that would corner a battle unit next to it`() {
        // Given
        val worldBuilder =
            voidAt(row = 1, column = 2)
                .placeBattleUnit(unitId = "knight", playerId = "player-one", row = 2, column = 2, isOccupiable)
                .selectTerrain("void")
                .pullEvents()
                .second
        // When
        val (events, rejected) = worldBuilder.paint(row = 2, column = 1, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        rejected.toDto().tiles[2][1] shouldBe "sand"
        events.shouldContainExactly(OccupiedTilePaintRejected(row = 2, column = 1, terrainId = "void"))
    }

    @Test
    fun `WorldBuilder should reject the import with the occupant when it stands in a corner of void`() {
        // Given
        val file =
            WorldBuilderMother.file(
                tiles = listOf(listOf("void", "void", "void"), listOf("void", "sand", "sand"), listOf("void", "sand", "sand")),
                npcs = listOf(WorldBuilder.Dto.NpcDto(id = "alchemist", row = 1, column = 1, stock = emptyList())),
            )
        // When
        val (events, _) = newWorld().import(file, currentVersion = 1, catalog, isKnownTerrain = { true }, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        events.shouldContainExactly(WorldImportRejected(reason = Reason.MISPLACED_OCCUPANT, occupantId = "alchemist"))
    }

    @Test
    fun `WorldBuilder should reject the import with the occupant when two of them stand on one tile`() {
        // Given
        val file =
            WorldBuilderMother.file(
                tiles = List(4) { List(4) { "sand" } },
                buildings = listOf(WorldBuilder.Dto.BuildingDto(id = "fortress", row = 0, column = 0)),
                npcs = listOf(WorldBuilder.Dto.NpcDto(id = "alchemist", row = 1, column = 1, stock = emptyList())),
            )
        // When
        val (events, _) = newWorld().import(file, currentVersion = 1, catalog, isKnownTerrain = { true }, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        events.shouldContainExactly(WorldImportRejected(reason = Reason.MISPLACED_OCCUPANT, occupantId = "alchemist"))
    }

    @Test
    fun `WorldBuilder should reject the import with the occupant when the world places it twice`() {
        // Given
        val knight = WorldBuilder.Dto.BattleUnitDto(id = "hero", unitId = "knight", playerId = "player-one", row = 0, column = 0)
        val file = WorldBuilderMother.file(tiles = List(4) { List(4) { "sand" } }, battleUnits = listOf(knight, knight.copy(row = 1)))
        // When
        val (events, _) = newWorld().import(file, currentVersion = 1, catalog, isKnownTerrain = { true }, canBeAdjacent, isOccupiable).pullEvents()
        // Then
        events.shouldContainExactly(WorldImportRejected(reason = Reason.DUPLICATE_OCCUPANT, occupantId = "hero"))
    }

    @Test
    fun `WorldBuilder should add a quest named after its number, with nothing in it yet, when a quest is added`() {
        // When
        val (events, added) = newWorld().addQuest(WorldBuilder.Dto.QuestTypeDto.SIDE).pullEvents()
        // Then
        val quest = added.toDto().quests.single()
        (quest.id to quest.type) shouldBe ("quest-1" to WorldBuilder.Dto.QuestTypeDto.SIDE)
        quest.tasks.shouldBeEmpty()
        events.shouldContainExactly(QuestAdded(questId = "quest-1"))
    }

    @Test
    fun `WorldBuilder should reject a second main quest when one is added or edited into one`() {
        // Given
        val worldBuilder =
            newWorld()
                .addQuest(WorldBuilder.Dto.QuestTypeDto.MAIN)
                .addQuest(WorldBuilder.Dto.QuestTypeDto.SIDE)
                .pullEvents()
                .second
        val sideQuest = worldBuilder.toDto().quests.last()
        // When
        val (addEvents, _) = worldBuilder.addQuest(WorldBuilder.Dto.QuestTypeDto.MAIN).pullEvents()
        val (editEvents, edited) = worldBuilder.editQuest(sideQuest.copy(type = WorldBuilder.Dto.QuestTypeDto.MAIN)).pullEvents()
        // Then
        addEvents.shouldContainExactly(QuestRejected(reason = QuestRejected.Reason.MAIN_QUEST_TAKEN))
        editEvents.shouldContainExactly(QuestRejected(reason = QuestRejected.Reason.MAIN_QUEST_TAKEN))
        edited
            .toDto()
            .quests
            .last()
            .type shouldBe WorldBuilder.Dto.QuestTypeDto.SIDE
    }

    @Test
    fun `WorldBuilder should replace the quest, keeping only its tiles in the world, when it is edited`() {
        // Given
        val worldBuilder = newWorld().addQuest(WorldBuilder.Dto.QuestTypeDto.MAIN).pullEvents().second
        val quest = worldBuilder.toDto().quests.single()
        val tiles = listOf(WorldBuilder.Dto.TileDto(row = 2, column = 3), WorldBuilder.Dto.TileDto(row = 20, column = 3))
        // When
        val (events, edited) = worldBuilder.editQuest(quest.copy(name = "The Keep", startTiles = tiles)).pullEvents()
        // Then
        edited
            .toDto()
            .quests
            .single()
            .name shouldBe "The Keep"
        edited
            .toDto()
            .quests
            .single()
            .startTiles
            .shouldContainExactly(WorldBuilder.Dto.TileDto(row = 2, column = 3))
        events.shouldContainExactly(QuestEdited(questId = quest.id))
    }

    @Test
    fun `WorldBuilder should take the quest away, and undo bring it back, when it is removed`() {
        // Given
        val worldBuilder = newWorld().addQuest(WorldBuilder.Dto.QuestTypeDto.SIDE).pullEvents().second
        // When
        val (events, removed) = worldBuilder.removeQuest("quest-1").pullEvents()
        // Then
        removed.toDto().quests.shouldBeEmpty()
        events.shouldContainExactly(QuestRemoved(questId = "quest-1"))
        removed
            .undo()
            .toDto()
            .quests
            .map(WorldBuilder.Dto.QuestDto::id)
            .shouldContainExactly("quest-1")
    }

    @Test
    fun `WorldBuilder should remove the tasks naming a battle unit when it is erased, the last of its unit`() {
        // Given
        val worldBuilder =
            newWorld()
                .placeBattleUnit(unitId = "rat", playerId = "player-two", row = 1, column = 1, isOccupiable)
                .placeBattleUnit(unitId = "rat", playerId = "player-two", row = 2, column = 2, isOccupiable)
                .addQuest(WorldBuilder.Dto.QuestTypeDto.MAIN)
                .pullEvents()
                .second
        val quest = worldBuilder.toDto().quests.single()
        val withTasks = worldBuilder.editQuest(quest.copy(tasks = listOf(WorldBuilderMother.defeatTask("rat"), WorldBuilderMother.talkTask())))
        // When
        val oneErased = withTasks.erase(row = 1, column = 1)
        val bothErased = oneErased.erase(row = 2, column = 2)
        // Then
        oneErased
            .toDto()
            .quests
            .single()
            .tasks.size shouldBe 2
        bothErased
            .toDto()
            .quests
            .single()
            .tasks
            .shouldContainExactly(WorldBuilderMother.talkTask())
        bothErased
            .undo()
            .toDto()
            .quests
            .single()
            .tasks.size shouldBe 2
    }

    @Test
    fun `WorldBuilder should remove the talk tasks and the side quests of an npc when it is erased`() {
        // Given
        val worldBuilder =
            newWorld()
                .placeNpc(npcId = "alchemist", row = 1, column = 1, isOccupiable)
                .addQuest(WorldBuilder.Dto.QuestTypeDto.MAIN)
                .addQuest(WorldBuilder.Dto.QuestTypeDto.SIDE)
                .pullEvents()
                .second
        val (mainQuest, sideQuest) = worldBuilder.toDto().quests
        val withQuests =
            worldBuilder
                .editQuest(mainQuest.copy(tasks = listOf(WorldBuilderMother.talkTask("alchemist"), WorldBuilderMother.defeatTask())))
                .editQuest(sideQuest.copy(questGiverNpcId = "alchemist"))
        // When
        val erased = withQuests.erase(row = 1, column = 1).toDto()
        // Then
        erased.quests.map(WorldBuilder.Dto.QuestDto::id).shouldContainExactly(mainQuest.id)
        erased.quests
            .single()
            .tasks
            .shouldContainExactly(WorldBuilderMother.defeatTask())
    }

    @Test
    fun `WorldBuilder should be playable when it has a battle unit of each player and a main quest with start tiles`() {
        // Given
        val file =
            WorldBuilderMother.file(
                tiles = List(4) { List(4) { "sand" } },
                battleUnits =
                    listOf(
                        WorldBuilder.Dto.BattleUnitDto(id = "hero", unitId = "knight", playerId = "player-one", row = 0, column = 0),
                        WorldBuilder.Dto.BattleUnitDto(id = "rat-1", unitId = "rat", playerId = "player-two", row = 3, column = 3),
                    ),
                quests = listOf(WorldBuilderMother.quest()),
            )
        // When
        val imported = newWorld().import(file, currentVersion = 1, catalog, isKnownTerrain = { true }, canBeAdjacent, isOccupiable)
        // Then
        imported.toDto().unplayableReasons.shouldBeEmpty()
        imported.toDto().quests shouldBe file.quests
    }

    @Test
    fun `WorldBuilder should not be playable when its main quest has no start tiles and a quest has no tasks`() {
        // Given
        val file = WorldBuilderMother.file(tiles = List(4) { List(4) { "sand" } }, quests = listOf(WorldBuilderMother.quest(startTiles = emptyList(), tasks = emptyList())))
        // When
        val imported = newWorld().import(file, currentVersion = 1, catalog, isKnownTerrain = { true }, canBeAdjacent, isOccupiable)
        // Then
        imported.toDto().unplayableReasons.shouldContainExactly(
            WorldBuilder.Dto.UnplayableReasonDto.NO_HUMAN_BATTLE_UNIT,
            WorldBuilder.Dto.UnplayableReasonDto.NO_CPU_BATTLE_UNIT,
            WorldBuilder.Dto.UnplayableReasonDto.NO_START_TILES,
            WorldBuilder.Dto.UnplayableReasonDto.QUEST_WITHOUT_TASKS,
        )
    }
}
