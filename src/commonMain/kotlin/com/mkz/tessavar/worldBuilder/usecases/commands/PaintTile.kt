package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.terrain.usecases.queries.HasTerrainTransition
import com.mkz.tessavar.terrain.usecases.queries.SearchTerrainById
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository

class PaintTile(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val hasTerrainTransition: HasTerrainTransition,
    private val searchTerrainById: SearchTerrainById,
    private val eventBus: EventBus,
) {
    operator fun invoke(
        row: Int,
        column: Int,
    ) {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val (events, paintedWorldBuilder) =
            worldBuilder
                .paint(
                    row = row,
                    column = column,
                    canBeAdjacent = { terrainId, adjacentTerrainId ->
                        hasTerrainTransition(terrainId, adjacentTerrainId) || hasTerrainTransition(adjacentTerrainId, terrainId)
                    },
                    isOccupiable = { terrainId -> searchTerrainById(terrainId)?.canBeOccupied == true },
                ).pullEvents()
        worldBuilderRepository.save(paintedWorldBuilder)
        eventBus.publish(events)
    }
}
