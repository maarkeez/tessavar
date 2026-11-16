package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.terrain.usecases.queries.HasTerrainTransition
import com.mkz.tessavar.terrain.usecases.queries.SearchTerrainById
import com.mkz.tessavar.world.domain.World
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository
import com.mkz.tessavar.worldBuilder.usecases.services.Catalog
import com.mkz.tessavar.worldBuilder.usecases.services.WorldFileCodec

class ImportWorld(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val worldFileCodec: WorldFileCodec,
    private val catalog: Catalog,
    private val searchTerrainById: SearchTerrainById,
    private val hasTerrainTransition: HasTerrainTransition,
    private val eventBus: EventBus,
) {
    operator fun invoke(text: String) {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val file = worldFileCodec.decode(text)
        val (events, importedWorldBuilder) =
            worldBuilder
                .import(
                    file = file,
                    currentVersion = World.CURRENT_VERSION,
                    catalog = catalog.loadCatalog(),
                    isKnownTerrain = { terrainId -> searchTerrainById(terrainId) != null },
                    canBeAdjacent = { terrainId, adjacentTerrainId ->
                        hasTerrainTransition(terrainId, adjacentTerrainId) || hasTerrainTransition(adjacentTerrainId, terrainId)
                    },
                    isOccupiable = { terrainId -> searchTerrainById(terrainId)?.canBeOccupied == true },
                ).pullEvents()
        worldBuilderRepository.save(importedWorldBuilder)
        eventBus.publish(events)
    }
}
