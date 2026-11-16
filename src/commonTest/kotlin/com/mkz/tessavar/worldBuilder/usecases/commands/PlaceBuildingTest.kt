package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.terrain.adapters.storage.InMemoryTerrainRepository
import com.mkz.tessavar.terrain.domain.TerrainMother.terrainThatCanBeOccupied
import com.mkz.tessavar.terrain.domain.TerrainMother.terrainThatCanNotBeOccupied
import com.mkz.tessavar.terrain.usecases.queries.SearchTerrainById
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.OccupantPlaceRejected
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.OccupantPlaced
import com.mkz.tessavar.worldBuilder.usecases.services.FakeCatalog
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PlaceBuildingTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()
    private val terrainRepository =
        InMemoryTerrainRepository().apply {
            create(terrainThatCanBeOccupied(id = "sand"))
            create(terrainThatCanNotBeOccupied(id = "void"))
        }

    private val placeBuilding = PlaceBuilding(worldBuilderRepository, FakeCatalog(), SearchTerrainById(terrainRepository), eventBus)

    @Test
    fun `PlaceBuilding should place the building with its size from the catalog when its tiles are vacant`() {
        // Given
        worldBuilderRepository.save(WorldBuilder.create().pullEvents().second)
        // When
        placeBuilding(buildingId = "fortress", row = 2, column = 3)
        // Then
        worldBuilderRepository.search()!!.toDto().buildings shouldBe listOf(WorldBuilder.Dto.BuildingDto(id = "fortress", row = 2, column = 3))
        eventBus.shouldHavePublishedEvents(OccupantPlaced(occupantId = "fortress", row = 2, column = 3))
    }

    @Test
    fun `PlaceBuilding should reject the building when it would stand partly out of the world`() {
        // Given
        worldBuilderRepository.save(WorldBuilder.create().pullEvents().second)
        // When
        placeBuilding(buildingId = "fortress", row = 15, column = 15)
        // Then
        eventBus.shouldHavePublishedEvents(OccupantPlaceRejected(row = 15, column = 15, reason = OccupantPlaceRejected.Reason.OUT_OF_WORLD))
    }

    @Test
    fun `PlaceBuilding should fail when the world has not been created`() {
        // When
        val place = { placeBuilding(buildingId = "fortress", row = 2, column = 3) }
        // Then
        shouldThrow<WorldNotCreated> { place() }
    }
}
