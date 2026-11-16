package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.terrain.usecases.queries.SearchTerrainById
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository

class PlaceBattleUnit(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val searchTerrainById: SearchTerrainById,
    private val eventBus: EventBus,
) {
    operator fun invoke(
        unitId: String,
        playerId: String,
        row: Int,
        column: Int,
    ) {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val (events, placedWorldBuilder) =
            worldBuilder
                .placeBattleUnit(
                    unitId = unitId,
                    playerId = playerId,
                    row = row,
                    column = column,
                    isOccupiable = { terrainId -> searchTerrainById(terrainId)?.canBeOccupied == true },
                ).pullEvents()
        worldBuilderRepository.save(placedWorldBuilder)
        eventBus.publish(events)
    }
}
