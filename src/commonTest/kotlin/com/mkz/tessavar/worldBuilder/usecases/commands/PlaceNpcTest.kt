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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PlaceNpcTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()
    private val terrainRepository =
        InMemoryTerrainRepository().apply {
            create(terrainThatCanBeOccupied(id = "sand"))
            create(terrainThatCanNotBeOccupied(id = "void"))
        }

    private val placeNpc = PlaceNpc(worldBuilderRepository, SearchTerrainById(terrainRepository), eventBus)

    @Test
    fun `PlaceNpc should place the npc, selling nothing, when its tile is vacant and occupiable`() {
        // Given
        worldBuilderRepository.save(WorldBuilder.create().pullEvents().second)
        // When
        placeNpc(npcId = "alchemist", row = 4, column = 5)
        // Then
        worldBuilderRepository.search()!!.toDto().npcs shouldBe listOf(WorldBuilder.Dto.NpcDto(id = "alchemist", row = 4, column = 5, stock = emptyList()))
        eventBus.shouldHavePublishedEvents(OccupantPlaced(occupantId = "alchemist", row = 4, column = 5))
    }

    @Test
    fun `PlaceNpc should reject the npc when its terrain can not be occupied`() {
        // Given
        val voidWorld =
            WorldBuilder
                .create()
                .selectTerrain("void")
                .fill(row = 0, column = 0, canBeAdjacent = { _, _ -> true }, isOccupiable = { true })
                .pullEvents()
                .second
        worldBuilderRepository.save(voidWorld)
        // When
        placeNpc(npcId = "alchemist", row = 4, column = 5)
        // Then
        eventBus.shouldHavePublishedEvents(OccupantPlaceRejected(row = 4, column = 5, reason = OccupantPlaceRejected.Reason.NOT_OCCUPIABLE, terrainId = "void"))
    }

    @Test
    fun `PlaceNpc should fail when the world has not been created`() {
        // When
        val place = { placeNpc(npcId = "alchemist", row = 4, column = 5) }
        // Then
        shouldThrow<WorldNotCreated> { place() }
    }
}
