package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.terrain.usecases.queries.SearchTerrainById
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository

class PlaceNpc(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val searchTerrainById: SearchTerrainById,
    private val eventBus: EventBus,
) {
    operator fun invoke(
        npcId: String,
        row: Int,
        column: Int,
    ) {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val (events, placedWorldBuilder) =
            worldBuilder
                .placeNpc(
                    npcId = npcId,
                    row = row,
                    column = column,
                    isOccupiable = { terrainId -> searchTerrainById(terrainId)?.canBeOccupied == true },
                ).pullEvents()
        worldBuilderRepository.save(placedWorldBuilder)
        eventBus.publish(events)
    }
}
