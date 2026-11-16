package com.mkz.tessavar.worldBuilder.domain

import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.TileNotFound
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.AreaFilled
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.EditRedone
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.EditUndone
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.TerrainSelected
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.TilePaintRejected
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.TilePainted
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldImportRejected
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldImportRejected.Reason
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldImported
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldOpened
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldResized
import kotlin.jvm.JvmInline

@ConsistentCopyVisibility
data class WorldBuilder private constructor(
    private val tiles: Tiles,
    private val selectedTerrainId: TerrainId,
    private val events: Set<WorldBuilderEvent>,
    private val undoneTiles: List<Tiles> = emptyList(),
    private val redoneTiles: List<Tiles> = emptyList(),
) {
    companion object {
        const val MINIMUM_SIZE = 1
        const val MAXIMUM_SIZE = 128
        const val DEFAULT_SIZE = 16
        const val DEFAULT_TERRAIN_ID = "sand"
        const val UNDO_STEPS = 50

        fun restore(tiles: List<List<String>>): WorldBuilder {
            require(tiles.isNotEmpty() && tiles.all { row -> row.size == tiles.first().size } && isValidSize(tiles.size) && isValidSize(tiles.first().size)) {
                "The world kept is not 1 to 128 tiles wide and tall"
            }
            return WorldBuilder(tiles = Tiles(tiles.map { row -> row.map(::TerrainId) }), selectedTerrainId = TerrainId(DEFAULT_TERRAIN_ID), events = emptySet())
        }

        private fun isValidSize(size: Int): Boolean = size in MINIMUM_SIZE..MAXIMUM_SIZE

        fun create(): WorldBuilder {
            val tiles = Tiles.filled(rows = DEFAULT_SIZE, columns = DEFAULT_SIZE, terrainId = TerrainId(DEFAULT_TERRAIN_ID))
            return WorldBuilder(
                tiles = tiles,
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
            canUndo = undoneTiles.isNotEmpty(),
            canRedo = redoneTiles.isNotEmpty(),
        )

    fun resize(
        rows: Int,
        columns: Int,
    ): WorldBuilder {
        if (!isValidSize(rows) || !isValidSize(columns)) return this
        if (rows == tiles.rows && columns == tiles.columns) return this
        return edit(tiles.resize(rows = rows, columns = columns, fillTerrainId = TerrainId(DEFAULT_TERRAIN_ID)), WorldResized(rows = rows, columns = columns))
    }

    fun selectTerrain(terrainId: String): WorldBuilder = copy(selectedTerrainId = TerrainId(terrainId), events = events + TerrainSelected(terrainId = terrainId))

    fun paint(
        row: Int,
        column: Int,
        canBeAdjacent: (terrainId: String, adjacentTerrainId: String) -> Boolean,
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
        return edit(tiles.paint(row, column, selectedTerrainId), TilePainted(row = row, column = column, terrainId = terrainId))
    }

    fun fill(
        row: Int,
        column: Int,
        canBeAdjacent: (terrainId: String, adjacentTerrainId: String) -> Boolean,
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
        return edit(tiles.paint(area, selectedTerrainId), AreaFilled(row = row, column = column, terrainId = terrainId, tileCount = area.size))
    }

    fun import(
        tiles: List<List<String>>?,
        isKnownTerrain: (terrainId: String) -> Boolean,
        canBeAdjacent: (terrainId: String, adjacentTerrainId: String) -> Boolean,
    ): WorldBuilder {
        if (tiles == null) return rejectImport(Reason.NOT_A_WORLD)
        if (!isValidSize(tiles.size) || !isValidSize(tiles.first().size)) return rejectImport(Reason.SIZE_OUT_OF_RANGE)
        tiles.flatten().firstOrNull { terrainId -> !isKnownTerrain(terrainId) }?.let { terrainId ->
            return rejectImport(Reason.UNKNOWN_TERRAIN, terrainId = terrainId)
        }
        forbiddenAdjacentTerrains(tiles, canBeAdjacent)?.let { (terrainId, adjacentTerrainId) ->
            return rejectImport(Reason.FORBIDDEN_ADJACENT_TERRAINS, terrainId = terrainId, adjacentTerrainId = adjacentTerrainId)
        }
        val importedTiles = Tiles(tiles.map { row -> row.map(::TerrainId) })
        return edit(importedTiles, WorldImported(rows = importedTiles.rows, columns = importedTiles.columns))
    }

    fun undo(): WorldBuilder {
        val earlierTiles = undoneTiles.lastOrNull() ?: return this
        return copy(
            tiles = earlierTiles,
            undoneTiles = undoneTiles.dropLast(1),
            redoneTiles = redoneTiles + tiles,
            events = events + EditUndone(rows = earlierTiles.rows, columns = earlierTiles.columns),
        )
    }

    fun redo(): WorldBuilder {
        val laterTiles = redoneTiles.lastOrNull() ?: return this
        return copy(
            tiles = laterTiles,
            undoneTiles = undoneTiles + tiles,
            redoneTiles = redoneTiles.dropLast(1),
            events = events + EditRedone(rows = laterTiles.rows, columns = laterTiles.columns),
        )
    }

    private fun edit(
        editedTiles: Tiles,
        event: WorldBuilderEvent,
    ): WorldBuilder =
        copy(
            tiles = editedTiles,
            undoneTiles = (undoneTiles + tiles).takeLast(UNDO_STEPS),
            redoneTiles = emptyList(),
            events = events + event,
        )

    private fun rejectImport(
        reason: Reason,
        terrainId: String? = null,
        adjacentTerrainId: String? = null,
    ): WorldBuilder = copy(events = events + WorldImportRejected(reason = reason, terrainId = terrainId, adjacentTerrainId = adjacentTerrainId))

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
    )
}
