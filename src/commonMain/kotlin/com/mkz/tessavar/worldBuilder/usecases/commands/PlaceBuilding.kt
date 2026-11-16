package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.terrain.usecases.queries.SearchTerrainById
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository
import com.mkz.tessavar.worldBuilder.usecases.services.Catalog

class PlaceBuilding(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val catalog: Catalog,
    private val searchTerrainById: SearchTerrainById,
    private val eventBus: EventBus,
) {
    operator fun invoke(
        buildingId: String,
        row: Int,
        column: Int,
    ) {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val (events, placedWorldBuilder) =
            worldBuilder
                .placeBuilding(
                    buildingId = buildingId,
                    row = row,
                    column = column,
                    catalog = catalog.loadCatalog(),
                    isOccupiable = { terrainId -> searchTerrainById(terrainId)?.canBeOccupied == true },
                ).pullEvents()
        worldBuilderRepository.save(placedWorldBuilder)
        eventBus.publish(events)
    }
}
