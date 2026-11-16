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
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldRenamed
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldResized
import kotlin.jvm.JvmInline

@ConsistentCopyVisibility
data class WorldBuilder private constructor(
    private val tiles: Tiles,
    private val details: Details,
    private val occupants: Occupants,
    private val selectedTerrainId: TerrainId,
    private val events: Set<WorldBuilderEvent>,
    private val undone: List<Snapshot> = emptyList(),
    private val redone: List<Snapshot> = emptyList(),
) {
    companion object {
        const val MINIMUM_SIZE = 1
        const val MAXIMUM_SIZE = 128
        const val DEFAULT_SIZE = 16
        const val DEFAULT_TERRAIN_ID = "sand"
        const val UNDO_STEPS = 50
        const val HUMAN_PLAYER_ID = "player-one"
        const val CPU_PLAYER_ID = "player-two"

        fun restore(
            file: Dto.WorldFileDto,
            catalog: Dto.CatalogDto,
        ): WorldBuilder {
            val tiles = file.tiles
            require(tiles.isNotEmpty() && tiles.all { row -> row.size == tiles.first().size } && isValidSize(tiles.size) && isValidSize(tiles.first().size)) {
                "The world kept is not 1 to 128 tiles wide and tall"
            }
            return WorldBuilder(
                tiles = Tiles(tiles.map { row -> row.map(::TerrainId) }),
                details = Details.of(file),
                occupants = Occupants.of(file, catalog),
                selectedTerrainId = TerrainId(DEFAULT_TERRAIN_ID),
                events = emptySet(),
            )
        }

        private fun isValidSize(size: Int): Boolean = size in MINIMUM_SIZE..MAXIMUM_SIZE

        fun create(): WorldBuilder {
            val tiles = Tiles.filled(rows = DEFAULT_SIZE, columns = DEFAULT_SIZE, terrainId = TerrainId(DEFAULT_TERRAIN_ID))
            return WorldBuilder(
                tiles = tiles,
                details = Details(name = "", author = "", keptSections = "", quests = emptyList()),
                occupants = Occupants(buildings = emptyList(), battleUnits = emptyList(), npcs = emptyList()),
                selectedTerrainId = TerrainId(DEFAULT_TERRAIN_ID),
                events = setOf(WorldOpened(rows = tiles.rows, columns = tiles.columns)),
            )
        }
    }

    fun reopen(): WorldBuilder = copy(events = events + WorldOpened(rows = tiles.rows, columns = tiles.columns))

    fun pullEvents(): Pair<Set<WorldBuilderEvent>, WorldBuilder> = events to copy(events = emptySet())

    fun toDto() =
        Dto(
            rows = tiles.rows,
            columns = tiles.columns,
            tiles = tiles.toDto(),
            selectedTerrainId = selectedTerrainId.value,
            canUndo = undone.isNotEmpty(),
            canRedo = redone.isNotEmpty(),
            name = details.name,
            author = details.author,
            buildings = occupants.buildings.map(PlacedBuilding::toDto),
            battleUnits = occupants.battleUnits.map(PlacedBattleUnit::toDto),
            npcs = occupants.npcs.map(PlacedNpc::toDto),
            quests = details.quests,
            unplayableReasons = unplayableReasons(),
        )

    fun toWorldFile(currentVersion: Int) =
        Dto.WorldFileDto(
            version = currentVersion,
            name = details.name,
            author = details.author,
            tiles = tiles.toDto(),
            buildings = occupants.buildings.map(PlacedBuilding::toDto),
            battleUnits = occupants.battleUnits.map(PlacedBattleUnit::toDto),
            npcs = occupants.npcs.map(PlacedNpc::toDto),
            quests = details.quests,
            keptSections = details.keptSections,
        )

    fun resize(
        rows: Int,
        columns: Int,
    ): WorldBuilder {
        if (!isValidSize(rows) || !isValidSize(columns)) return this
        if (rows == tiles.rows && columns == tiles.columns) return this
        return edit(
            WorldResized(rows = rows, columns = columns),
            editedTiles = tiles.resize(rows = rows, columns = columns, fillTerrainId = TerrainId(DEFAULT_TERRAIN_ID)),
            editedOccupants = occupants.within(rows = rows, columns = columns),
            editedDetails =
                details.copy(
                    quests =
                        details.quests.map { quest ->
                            quest.copy(
                                startTiles = quest.startTiles.filter { tile -> tile.row < rows && tile.column < columns },
                                tasks = quest.tasks.map { task -> task.copy(tiles = task.tiles.filter { tile -> tile.row < rows && tile.column < columns }) },
                            )
                        },
                ),
        )
    }

    fun selectTerrain(terrainId: String): WorldBuilder = copy(selectedTerrainId = TerrainId(terrainId), events = events + TerrainSelected(terrainId = terrainId))

    fun paint(
        row: Int,
        column: Int,
        canBeAdjacent: (terrainId: String, adjacentTerrainId: String) -> Boolean,
        isOccupiable: (terrainId: String) -> Boolean,
    ): WorldBuilder {
        val currentTerrainId = tiles.terrainId(row, column) ?: throw TileNotFound()
        if (currentTerrainId == selectedTerrainId) return this
        val terrainId = selectedTerrainId.value
        val forbiddenAdjacentTerrainId =
            tiles
                .adjacentTerrainIds(row, column)
                .map(TerrainId::value)
                .firstOrNull { adjacentTerrainId -> adjacentTerrainId != terrainId && !canBeAdjacent(terrainId, adjacentTerrainId) }
        if (forbiddenAdjacentTerrainId != null) {
            return copy(events = events + TilePaintRejected(row = row, column = column, terrainId = terrainId, adjacentTerrainId = forbiddenAdjacentTerrainId))
        }
        if (!isOccupiable(terrainId) && !occupants.isVacant(row, column)) {
            return copy(events = events + OccupiedTilePaintRejected(row = row, column = column, terrainId = terrainId))
        }
        return edit(TilePainted(row = row, column = column, terrainId = terrainId), editedTiles = tiles.paint(row, column, selectedTerrainId))
    }

    fun fill(
        row: Int,
        column: Int,
        canBeAdjacent: (terrainId: String, adjacentTerrainId: String) -> Boolean,
        isOccupiable: (terrainId: String) -> Boolean,
    ): WorldBuilder {
        val areaTerrainId = tiles.terrainId(row, column) ?: throw TileNotFound()
        if (areaTerrainId == selectedTerrainId) return this
        val terrainId = selectedTerrainId.value
        val area = tiles.connectedArea(row, column)
        val forbiddenAdjacentTerrainId =
            area
                .asSequence()
                .flatMap { (areaRow, areaColumn) -> tiles.adjacentPositions(areaRow, areaColumn) }
                .filter { position -> position !in area }
                .mapNotNull { (adjacentRow, adjacentColumn) -> tiles.terrainId(adjacentRow, adjacentColumn)?.value }
                .firstOrNull { adjacentTerrainId -> adjacentTerrainId != terrainId && !canBeAdjacent(terrainId, adjacentTerrainId) }
        if (forbiddenAdjacentTerrainId != null) {
            return copy(events = events + TilePaintRejected(row = row, column = column, terrainId = terrainId, adjacentTerrainId = forbiddenAdjacentTerrainId))
        }
        if (!isOccupiable(terrainId) && area.any { (areaRow, areaColumn) -> !occupants.isVacant(areaRow, areaColumn) }) {
            return copy(events = events + OccupiedTilePaintRejected(row = row, column = column, terrainId = terrainId))
        }
        return edit(AreaFilled(row = row, column = column, terrainId = terrainId, tileCount = area.size), editedTiles = tiles.paint(area, selectedTerrainId))
    }

    fun import(
        file: Dto.WorldFileDto?,
        currentVersion: Int,
        catalog: Dto.CatalogDto,
        isKnownTerrain: (terrainId: String) -> Boolean,
        canBeAdjacent: (terrainId: String, adjacentTerrainId: String) -> Boolean,
        isOccupiable: (terrainId: String) -> Boolean,
    ): WorldBuilder {
        if (file == null) return rejectImport(Reason.NOT_A_WORLD)
        if (file.version != currentVersion) return rejectImport(Reason.OTHER_WORLD_VERSION)
        val tiles = file.tiles
        if (tiles.isEmpty() || tiles.any { row -> row.size != tiles.first().size }) return rejectImport(Reason.NOT_A_WORLD)
        if (!isValidSize(tiles.size) || !isValidSize(tiles.first().size)) return rejectImport(Reason.SIZE_OUT_OF_RANGE)
        tiles.flatten().firstOrNull { terrainId -> !isKnownTerrain(terrainId) }?.let { terrainId ->
            return rejectImport(Reason.UNKNOWN_TERRAIN, terrainId = terrainId)
        }
        forbiddenAdjacentTerrains(tiles, canBeAdjacent)?.let { (terrainId, adjacentTerrainId) ->
            return rejectImport(Reason.FORBIDDEN_ADJACENT_TERRAINS, terrainId = terrainId, adjacentTerrainId = adjacentTerrainId)
        }
        val importedTiles = Tiles(tiles.map { row -> row.map(::TerrainId) })
        unknownOccupant(file, catalog)?.let { (reason, id) -> return rejectImport(reason, occupantId = id) }
        val importedOccupants = Occupants.of(file, catalog)
        importedOccupants.misplacedOccupantId(importedTiles, isOccupiable)?.let { occupantId -> return rejectImport(Reason.MISPLACED_OCCUPANT, occupantId = occupantId) }
        return edit(
            WorldImported(rows = importedTiles.rows, columns = importedTiles.columns),
            editedTiles = importedTiles,
            editedDetails = Details.of(file),
            editedOccupants = importedOccupants,
        )
    }

    fun rename(
        name: String,
        author: String,
    ): WorldBuilder {
        if (name == details.name && author == details.author) return this
        return edit(WorldRenamed(name = name, author = author), editedDetails = details.copy(name = name, author = author))
    }

    fun placeBuilding(
        buildingId: String,
        row: Int,
        column: Int,
        catalog: Dto.CatalogDto,
        isOccupiable: (terrainId: String) -> Boolean,
    ): WorldBuilder {
        val size = catalog.buildings.firstOrNull { building -> building.id == buildingId } ?: return this
        if (occupants.buildings.any { building -> building.id == buildingId && building.row == row && building.column == column }) return this
        val others = occupants.copy(buildings = occupants.buildings.filterNot { building -> building.id == buildingId })
        val building = PlacedBuilding(id = buildingId, row = row, column = column, rows = size.rows, columns = size.columns)
        return place(building.id, row, column, building.tiles(), others, isOccupiable) { others.copy(buildings = others.buildings + building) }
    }

    fun placeBattleUnit(
        unitId: String,
        playerId: String,
        row: Int,
        column: Int,
        isOccupiable: (terrainId: String) -> Boolean,
    ): WorldBuilder {
        val takenIds = occupants.battleUnits.map(PlacedBattleUnit::id).toSet()
        val battleUnitId = generateSequence(1) { number -> number + 1 }.map { number -> "$unitId-$number" }.first { id -> id !in takenIds }
        val battleUnit = PlacedBattleUnit(id = battleUnitId, unitId = unitId, playerId = playerId, row = row, column = column)
        return place(battleUnitId, row, column, listOf(row to column), occupants, isOccupiable) { occupants.copy(battleUnits = occupants.battleUnits + battleUnit) }
    }

    fun placeNpc(
        npcId: String,
        row: Int,
        column: Int,
        isOccupiable: (terrainId: String) -> Boolean,
    ): WorldBuilder {
        val placedNpc = occupants.npcs.firstOrNull { npc -> npc.id == npcId }
        if (placedNpc?.row == row && placedNpc.column == column) return this
        val others = occupants.copy(npcs = occupants.npcs.filterNot { npc -> npc.id == npcId })
        val npc = PlacedNpc(id = npcId, row = row, column = column, stock = placedNpc?.stock.orEmpty())
        return place(npcId, row, column, listOf(row to column), others, isOccupiable) { others.copy(npcs = others.npcs + npc) }
    }

    fun erase(
        row: Int,
        column: Int,
    ): WorldBuilder {
        val (occupantId, remaining) = occupants.erase(row, column) ?: return this
        val erasedNpcId = occupants.npcs.firstOrNull { npc -> npc.id == occupantId }?.id
        val erasedUnitId =
            occupants.battleUnits
                .firstOrNull { battleUnit -> battleUnit.id == occupantId }
                ?.unitId
                ?.takeIf { unitId -> remaining.battleUnits.none { battleUnit -> battleUnit.unitId == unitId } }
        val quests =
            details.quests
                .filterNot { quest -> erasedNpcId != null && quest.type == Dto.QuestTypeDto.SIDE && quest.questGiverNpcId == erasedNpcId }
                .map { quest ->
                    quest.copy(
                        tasks =
                            quest.tasks.filterNot { task ->
                                (erasedNpcId != null && task.type == Dto.TaskTypeDto.TALK && task.npcId == erasedNpcId) ||
                                    (erasedUnitId != null && task.type == Dto.TaskTypeDto.DEFEAT && task.unitId == erasedUnitId)
                            },
                    )
                }
        return edit(OccupantErased(occupantId = occupantId, row = row, column = column), editedOccupants = remaining, editedDetails = details.copy(quests = quests))
    }

    fun addQuest(type: Dto.QuestTypeDto): WorldBuilder {
        if (type == Dto.QuestTypeDto.MAIN && details.quests.any { quest -> quest.type == Dto.QuestTypeDto.MAIN }) {
            return copy(events = events + QuestRejected(reason = QuestRejected.Reason.MAIN_QUEST_TAKEN))
        }
        val takenIds = details.quests.map(Dto.QuestDto::id).toSet()
        val questId = generateSequence(1) { number -> number + 1 }.map { number -> "quest-$number" }.first { id -> id !in takenIds }
        val quest =
            Dto.QuestDto(
                id = questId,
                type = type,
                questGiverNpcId = null,
                repeatable = false,
                startTiles = emptyList(),
                name = "",
                description = "",
                lines = Dto.LinesDto(start = null, reminder = null, completed = null, startAgain = null),
                tasks = emptyList(),
                reward = Dto.RewardDto(gold = 0, lootableItemIds = emptyList()),
            )
        return edit(QuestAdded(questId = questId), editedDetails = details.copy(quests = details.quests + quest))
    }

    fun removeQuest(questId: String): WorldBuilder {
        if (details.quests.none { quest -> quest.id == questId }) return this
        return edit(QuestRemoved(questId = questId), editedDetails = details.copy(quests = details.quests.filterNot { quest -> quest.id == questId }))
    }

    fun editQuest(edited: Dto.QuestDto): WorldBuilder {
        val quest = details.quests.firstOrNull { quest -> quest.id == edited.id } ?: return this
        if (quest == edited) return this
        if (edited.type == Dto.QuestTypeDto.MAIN && details.quests.any { other -> other.id != edited.id && other.type == Dto.QuestTypeDto.MAIN }) {
            return copy(events = events + QuestRejected(reason = QuestRejected.Reason.MAIN_QUEST_TAKEN))
        }
        val inWorld = edited.copy(startTiles = edited.startTiles.filter(::isInWorld), tasks = edited.tasks.map { task -> task.copy(tiles = task.tiles.filter(::isInWorld)) })
        return edit(QuestEdited(questId = edited.id), editedDetails = details.copy(quests = details.quests.map { other -> if (other.id == edited.id) inWorld else other }))
    }

    private fun isInWorld(tile: Dto.TileDto): Boolean = tiles.terrainId(tile.row, tile.column) != null

    private fun unplayableReasons(): List<Dto.UnplayableReasonDto> {
        val mainQuests = details.quests.filter { quest -> quest.type == Dto.QuestTypeDto.MAIN }
        return listOfNotNull(
            Dto.UnplayableReasonDto.NO_HUMAN_BATTLE_UNIT.takeIf { occupants.battleUnits.none { battleUnit -> battleUnit.playerId == HUMAN_PLAYER_ID } },
            Dto.UnplayableReasonDto.NO_CPU_BATTLE_UNIT.takeIf { occupants.battleUnits.none { battleUnit -> battleUnit.playerId == CPU_PLAYER_ID } },
            Dto.UnplayableReasonDto.NO_MAIN_QUEST.takeIf { mainQuests.size != 1 },
            Dto.UnplayableReasonDto.NO_START_TILES.takeIf { mainQuests.any { quest -> quest.startTiles.isEmpty() } },
            Dto.UnplayableReasonDto.QUEST_WITHOUT_TASKS.takeIf { details.quests.any { quest -> quest.tasks.isEmpty() } },
        )
    }

    fun restock(
        npcId: String,
        stock: List<String>,
    ): WorldBuilder {
        val npc = occupants.npcs.firstOrNull { placedNpc -> placedNpc.id == npcId } ?: return this
        if (npc.stock == stock) return this
        val restocked = occupants.copy(npcs = occupants.npcs.map { placedNpc -> if (placedNpc.id == npcId) placedNpc.copy(stock = stock) else placedNpc })
        return edit(NpcRestocked(npcId = npcId, stock = stock), editedOccupants = restocked)
    }

    private fun place(
        occupantId: String,
        row: Int,
        column: Int,
        footprint: List<Pair<Int, Int>>,
        others: Occupants,
        isOccupiable: (terrainId: String) -> Boolean,
        placed: () -> Occupants,
    ): WorldBuilder {
        footprint.forEach { (footprintRow, footprintColumn) ->
            val terrainId = tiles.terrainId(footprintRow, footprintColumn) ?: return rejectPlace(row, column, OccupantPlaceRejected.Reason.OUT_OF_WORLD)
            if (!isOccupiable(terrainId.value)) return rejectPlace(row, column, OccupantPlaceRejected.Reason.NOT_OCCUPIABLE, terrainId.value)
            if (!others.isVacant(footprintRow, footprintColumn)) return rejectPlace(row, column, OccupantPlaceRejected.Reason.NOT_VACANT)
        }
        return edit(OccupantPlaced(occupantId = occupantId, row = row, column = column), editedOccupants = placed())
    }

    private fun rejectPlace(
        row: Int,
        column: Int,
        reason: OccupantPlaceRejected.Reason,
        terrainId: String? = null,
    ): WorldBuilder = copy(events = events + OccupantPlaceRejected(row = row, column = column, reason = reason, terrainId = terrainId))

    private fun unknownOccupant(
        file: Dto.WorldFileDto,
        catalog: Dto.CatalogDto,
    ): Pair<Reason, String>? {
        val buildingIds = catalog.buildings.map(Dto.CatalogBuildingDto::id).toSet()
        val npcIds = catalog.npcs.map(Dto.CatalogNpcDto::id).toSet()
        file.buildings.firstOrNull { building -> building.id !in buildingIds }?.let { building -> return Reason.UNKNOWN_OCCUPANT to building.id }
        file.battleUnits.firstOrNull { battleUnit -> battleUnit.unitId !in catalog.unitIds }?.let { battleUnit -> return Reason.UNKNOWN_OCCUPANT to battleUnit.unitId }
        file.battleUnits.firstOrNull { battleUnit -> battleUnit.playerId !in catalog.playerIds }?.let { battleUnit -> return Reason.UNKNOWN_OCCUPANT to battleUnit.playerId }
        file.npcs.firstOrNull { npc -> npc.id !in npcIds }?.let { npc -> return Reason.UNKNOWN_OCCUPANT to npc.id }
        file.npcs.flatMap(Dto.NpcDto::stock).firstOrNull { lootableItemId -> lootableItemId !in catalog.lootableItemIds }?.let { lootableItemId ->
            return Reason.UNKNOWN_OCCUPANT to lootableItemId
        }
        val ids = file.buildings.map(Dto.BuildingDto::id) + file.battleUnits.map(Dto.BattleUnitDto::id) + file.npcs.map(Dto.NpcDto::id)
        ids
            .groupBy { id -> id }
            .values
            .firstOrNull { sameIds -> sameIds.size > 1 }
            ?.let { sameIds -> return Reason.DUPLICATE_OCCUPANT to sameIds.first() }
        return null
    }

    fun undo(): WorldBuilder {
        val earlier = undone.lastOrNull() ?: return this
        return copy(
            tiles = earlier.tiles,
            details = earlier.details,
            occupants = earlier.occupants,
            undone = undone.dropLast(1),
            redone = redone + Snapshot(tiles, details, occupants),
            events = events + EditUndone(rows = earlier.tiles.rows, columns = earlier.tiles.columns),
        )
    }

    fun redo(): WorldBuilder {
        val later = redone.lastOrNull() ?: return this
        return copy(
            tiles = later.tiles,
            details = later.details,
            occupants = later.occupants,
            undone = undone + Snapshot(tiles, details, occupants),
            redone = redone.dropLast(1),
            events = events + EditRedone(rows = later.tiles.rows, columns = later.tiles.columns),
        )
    }

    private fun edit(
        event: WorldBuilderEvent,
        editedTiles: Tiles = tiles,
        editedDetails: Details = details,
        editedOccupants: Occupants = occupants,
    ): WorldBuilder =
        copy(
            tiles = editedTiles,
            details = editedDetails,
            occupants = editedOccupants,
            undone = (undone + Snapshot(tiles, details, occupants)).takeLast(UNDO_STEPS),
            redone = emptyList(),
            events = events + event,
        )

    private fun rejectImport(
        reason: Reason,
        terrainId: String? = null,
        adjacentTerrainId: String? = null,
        occupantId: String? = null,
    ): WorldBuilder = copy(events = events + WorldImportRejected(reason = reason, terrainId = terrainId, adjacentTerrainId = adjacentTerrainId, occupantId = occupantId))

    private fun forbiddenAdjacentTerrains(
        tiles: List<List<String>>,
        canBeAdjacent: (terrainId: String, adjacentTerrainId: String) -> Boolean,
    ): Pair<String, String>? {
        tiles.forEachIndexed { row, terrainIds ->
            terrainIds.forEachIndexed { column, terrainId ->
                val eastAndSouthTerrainIds = listOfNotNull(terrainIds.getOrNull(column + 1), tiles.getOrNull(row + 1)?.get(column))
                eastAndSouthTerrainIds.forEach { adjacentTerrainId ->
                    if (adjacentTerrainId != terrainId && !canBeAdjacent(terrainId, adjacentTerrainId)) return terrainId to adjacentTerrainId
                }
            }
        }
        return null
    }

    private data class Details(
        val name: String,
        val author: String,
        val keptSections: String,
        val quests: List<Dto.QuestDto>,
    ) {
        companion object {
            fun of(file: Dto.WorldFileDto) = Details(name = file.name, author = file.author, keptSections = file.keptSections, quests = file.quests)
        }
    }

    private data class Snapshot(
        val tiles: Tiles,
        val details: Details,
        val occupants: Occupants,
    )

    private data class Occupants(
        val buildings: List<PlacedBuilding>,
        val battleUnits: List<PlacedBattleUnit>,
        val npcs: List<PlacedNpc>,
    ) {
        companion object {
            fun of(
                file: Dto.WorldFileDto,
                catalog: Dto.CatalogDto,
            ) = Occupants(
                buildings =
                    file.buildings.mapNotNull { building ->
                        catalog.buildings
                            .firstOrNull { catalogBuilding -> catalogBuilding.id == building.id }
                            ?.let { size -> PlacedBuilding(id = building.id, row = building.row, column = building.column, rows = size.rows, columns = size.columns) }
                    },
                battleUnits =
                    file.battleUnits.map { battleUnit ->
                        PlacedBattleUnit(id = battleUnit.id, unitId = battleUnit.unitId, playerId = battleUnit.playerId, row = battleUnit.row, column = battleUnit.column)
                    },
                npcs = file.npcs.map { npc -> PlacedNpc(id = npc.id, row = npc.row, column = npc.column, stock = npc.stock) },
            )
        }

        private fun footprints(): List<Pair<String, List<Pair<Int, Int>>>> =
            buildings.map { building -> building.id to building.tiles() } +
                battleUnits.map { battleUnit -> battleUnit.id to listOf(battleUnit.row to battleUnit.column) } +
                npcs.map { npc -> npc.id to listOf(npc.row to npc.column) }

        fun isVacant(
            row: Int,
            column: Int,
        ): Boolean = footprints().none { (_, tiles) -> (row to column) in tiles }

        fun within(
            rows: Int,
            columns: Int,
        ): Occupants {
            fun fits(tiles: List<Pair<Int, Int>>) = tiles.all { (row, column) -> row < rows && column < columns }
            return Occupants(
                buildings = buildings.filter { building -> fits(building.tiles()) },
                battleUnits = battleUnits.filter { battleUnit -> fits(listOf(battleUnit.row to battleUnit.column)) },
                npcs = npcs.filter { npc -> fits(listOf(npc.row to npc.column)) },
            )
        }

        fun erase(
            row: Int,
            column: Int,
        ): Pair<String, Occupants>? {
            battleUnits.firstOrNull { battleUnit -> battleUnit.row == row && battleUnit.column == column }?.let { battleUnit ->
                return battleUnit.id to copy(battleUnits = battleUnits - battleUnit)
            }
            npcs.firstOrNull { npc -> npc.row == row && npc.column == column }?.let { npc -> return npc.id to copy(npcs = npcs - npc) }
            buildings.firstOrNull { building -> (row to column) in building.tiles() }?.let { building -> return building.id to copy(buildings = buildings - building) }
            return null
        }

        fun misplacedOccupantId(
            tiles: Tiles,
            isOccupiable: (terrainId: String) -> Boolean,
        ): String? {
            val taken = mutableSetOf<Pair<Int, Int>>()
            footprints().forEach { (occupantId, footprint) ->
                footprint.forEach { position ->
                    val terrainId = tiles.terrainId(position.first, position.second)
                    if (terrainId == null || !isOccupiable(terrainId.value) || !taken.add(position)) return occupantId
                }
            }
            return null
        }
    }

    private data class PlacedBuilding(
        val id: String,
        val row: Int,
        val column: Int,
        val rows: Int,
        val columns: Int,
    ) {
        fun tiles(): List<Pair<Int, Int>> = (row until row + rows).flatMap { buildingRow -> (column until column + columns).map { buildingColumn -> buildingRow to buildingColumn } }

        fun toDto() = Dto.BuildingDto(id = id, row = row, column = column)
    }

    private data class PlacedBattleUnit(
        val id: String,
        val unitId: String,
        val playerId: String,
        val row: Int,
        val column: Int,
    ) {
        fun toDto() = Dto.BattleUnitDto(id = id, unitId = unitId, playerId = playerId, row = row, column = column)
    }

    private data class PlacedNpc(
        val id: String,
        val row: Int,
        val column: Int,
        val stock: List<String>,
    ) {
        fun toDto() = Dto.NpcDto(id = id, row = row, column = column, stock = stock)
    }

    @JvmInline
    private value class TerrainId(
        val value: String,
    )

    @JvmInline
    private value class Tiles(
        val value: List<List<TerrainId>>,
    ) {
        companion object {
            fun filled(
                rows: Int,
                columns: Int,
                terrainId: TerrainId,
            ) = Tiles(List(rows) { List(columns) { terrainId } })
        }

        val rows: Int get() = value.size

        val columns: Int get() = value.first().size

        fun terrainId(
            row: Int,
            column: Int,
        ): TerrainId? = value.getOrNull(row)?.getOrNull(column)

        fun adjacentTerrainIds(
            row: Int,
            column: Int,
        ): List<TerrainId> = adjacentPositions(row, column).mapNotNull { (adjacentRow, adjacentColumn) -> terrainId(adjacentRow, adjacentColumn) }

        fun adjacentPositions(
            row: Int,
            column: Int,
        ): List<Pair<Int, Int>> =
            listOf(row - 1 to column, row to column + 1, row + 1 to column, row to column - 1)
                .filter { (adjacentRow, adjacentColumn) -> terrainId(adjacentRow, adjacentColumn) != null }

        fun connectedArea(
            row: Int,
            column: Int,
        ): Set<Pair<Int, Int>> {
            val areaTerrainId = terrainId(row, column) ?: return emptySet()
            val area = mutableSetOf(row to column)
            val pending = ArrayDeque(listOf(row to column))
            while (pending.isNotEmpty()) {
                val (pendingRow, pendingColumn) = pending.removeFirst()
                adjacentPositions(pendingRow, pendingColumn)
                    .filter { (adjacentRow, adjacentColumn) -> terrainId(adjacentRow, adjacentColumn) == areaTerrainId }
                    .filter { position -> area.add(position) }
                    .forEach(pending::addLast)
            }
            return area
        }

        fun paint(
            positions: Set<Pair<Int, Int>>,
            terrainId: TerrainId,
        ) = Tiles(value.mapIndexed { rowIndex, terrainIds -> terrainIds.mapIndexed { columnIndex, current -> if ((rowIndex to columnIndex) in positions) terrainId else current } })

        fun paint(
            row: Int,
            column: Int,
            terrainId: TerrainId,
        ) = Tiles(value.mapIndexed { rowIndex, terrainIds -> if (rowIndex == row) terrainIds.mapIndexed { columnIndex, current -> if (columnIndex == column) terrainId else current } else terrainIds })

        fun resize(
            rows: Int,
            columns: Int,
            fillTerrainId: TerrainId,
        ) = Tiles(List(rows) { row -> List(columns) { column -> terrainId(row, column) ?: fillTerrainId } })

        fun toDto(): List<List<String>> = value.map { terrainIds -> terrainIds.map(TerrainId::value) }
    }

    data class Dto(
        val rows: Int,
        val columns: Int,
        val tiles: List<List<String>>,
        val selectedTerrainId: String,
        val canUndo: Boolean,
        val canRedo: Boolean,
        val name: String,
        val author: String,
        val buildings: List<BuildingDto>,
        val battleUnits: List<BattleUnitDto>,
        val npcs: List<NpcDto>,
        val quests: List<QuestDto>,
        val unplayableReasons: List<UnplayableReasonDto>,
    ) {
        data class WorldFileDto(
            val version: Int,
            val name: String,
            val author: String,
            val tiles: List<List<String>>,
            val buildings: List<BuildingDto>,
            val battleUnits: List<BattleUnitDto>,
            val npcs: List<NpcDto>,
            val quests: List<QuestDto>,
            val keptSections: String,
        )

        data class QuestDto(
            val id: String,
            val type: QuestTypeDto,
            val questGiverNpcId: String?,
            val repeatable: Boolean,
            val startTiles: List<TileDto>,
            val name: String,
            val description: String,
            val lines: LinesDto,
            val tasks: List<TaskDto>,
            val reward: RewardDto,
        )

        enum class QuestTypeDto { MAIN, SIDE }

        data class TileDto(
            val row: Int,
            val column: Int,
        )

        data class LinesDto(
            val start: String?,
            val reminder: String?,
            val completed: String?,
            val startAgain: String?,
        )

        data class TaskDto(
            val type: TaskTypeDto,
            val unitId: String?,
            val requiredDefeats: Int?,
            val npcId: String?,
            val tiles: List<TileDto>,
            val text: String,
            val line: String?,
        )

        enum class TaskTypeDto { DEFEAT, TALK, VISIT }

        data class RewardDto(
            val gold: Int,
            val lootableItemIds: List<String>,
        )

        enum class UnplayableReasonDto {
            NO_HUMAN_BATTLE_UNIT,
            NO_CPU_BATTLE_UNIT,
            NO_MAIN_QUEST,
            NO_START_TILES,
            QUEST_WITHOUT_TASKS,
        }

        data class BuildingDto(
            val id: String,
            val row: Int,
            val column: Int,
        )

        data class BattleUnitDto(
            val id: String,
            val unitId: String,
            val playerId: String,
            val row: Int,
            val column: Int,
        )

        data class NpcDto(
            val id: String,
            val row: Int,
            val column: Int,
            val stock: List<String>,
        )

        data class CatalogDto(
            val buildings: List<CatalogBuildingDto>,
            val unitIds: List<String>,
            val npcs: List<CatalogNpcDto>,
            val playerIds: List<String>,
            val lootableItemIds: List<String>,
        )

        data class CatalogBuildingDto(
            val id: String,
            val rows: Int,
            val columns: Int,
        )

        data class CatalogNpcDto(
            val id: String,
            val unitId: String?,
        )
    }
}
