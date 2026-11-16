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
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.OccupantPlaced
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PlaceBattleUnitTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()
    private val terrainRepository =
        InMemoryTerrainRepository().apply {
            create(terrainThatCanBeOccupied(id = "sand"))
            create(terrainThatCanNotBeOccupied(id = "void"))
        }

    private val placeBattleUnit = PlaceBattleUnit(worldBuilderRepository, SearchTerrainById(terrainRepository), eventBus)

    @Test
    fun `PlaceBattleUnit should place a battle unit of the player when its tile is vacant and occupiable`() {
        // Given
        worldBuilderRepository.save(WorldBuilder.create().pullEvents().second)
        // When
        placeBattleUnit(unitId = "knight", playerId = "player-one", row = 1, column = 2)
        // Then
        worldBuilderRepository.search()!!.toDto().battleUnits shouldBe
            listOf(WorldBuilder.Dto.BattleUnitDto(id = "knight-1", unitId = "knight", playerId = "player-one", row = 1, column = 2))
        eventBus.shouldHavePublishedEvents(OccupantPlaced(occupantId = "knight-1", row = 1, column = 2))
    }

    @Test
    fun `PlaceBattleUnit should fail when the world has not been created`() {
        // When
        val place = { placeBattleUnit(unitId = "knight", playerId = "player-one", row = 1, column = 2) }
        // Then
        shouldThrow<WorldNotCreated> { place() }
    }
}
