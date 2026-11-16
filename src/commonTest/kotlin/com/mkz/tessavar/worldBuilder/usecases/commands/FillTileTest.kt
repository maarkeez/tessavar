package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.terrain.adapters.storage.InMemoryTerrainRepository
import com.mkz.tessavar.terrain.domain.TerrainMother.terrainThatCanBeOccupied
import com.mkz.tessavar.terrain.domain.TerrainMother.terrainThatCanNotBeOccupied
import com.mkz.tessavar.terrain.usecases.queries.HasTerrainTransition
import com.mkz.tessavar.terrain.usecases.queries.SearchTerrainById
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.AreaFilled
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.TilePaintRejected
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderMother
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FillTileTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()

    private val terrainRepository =
        InMemoryTerrainRepository().apply {
            create(terrainThatCanBeOccupied(id = "sand", transitionsTo = setOf("sand", "lava", "water")))
            create(terrainThatCanNotBeOccupied(id = "lava", transitionsTo = setOf("lava")))
            create(terrainThatCanBeOccupied(id = "water", transitionsTo = setOf("water")))
        }
    private val fillTile = FillTile(worldBuilderRepository, HasTerrainTransition(terrainRepository), SearchTerrainById(terrainRepository), eventBus)

    private fun givenWorld(
        tiles: List<List<String>>,
        selectedTerrainId: String,
    ) = worldBuilderRepository.save(
        WorldBuilder
            .create()
            .import(WorldBuilderMother.file(tiles), currentVersion = 1, catalog = WorldBuilderMother.catalog(), isKnownTerrain = { true }, canBeAdjacent = { _, _ -> true }, isOccupiable = { true })
            .selectTerrain(selectedTerrainId)
            .pullEvents()
            .second,
    )

    @Test
    fun `should fill the area when the selected terrain has a transition to the tiles around it`() {
        // Given
        givenWorld(listOf(listOf("lava", "lava", "water", "water")), selectedTerrainId = "sand")
        // When
        fillTile(row = 0, column = 3)
        // Then
        worldBuilderRepository.search()!!.toDto().tiles shouldBe listOf(listOf("lava", "lava", "sand", "sand"))
        eventBus.shouldHavePublishedEvents(AreaFilled(row = 0, column = 3, terrainId = "sand", tileCount = 2))
    }

    @Test
    fun `should fill the area when the tiles around it have a transition to the selected terrain`() {
        // Given
        givenWorld(listOf(listOf("water", "water", "sand")), selectedTerrainId = "lava")
        // When
        fillTile(row = 0, column = 0)
        // Then
        worldBuilderRepository.search()!!.toDto().tiles shouldBe listOf(listOf("lava", "lava", "sand"))
        eventBus.shouldHavePublishedEvents(AreaFilled(row = 0, column = 0, terrainId = "lava", tileCount = 2))
    }

    @Test
    fun `should reject the fill when neither terrain has a transition to the other`() {
        // Given
        givenWorld(listOf(listOf("sand", "sand", "lava")), selectedTerrainId = "water")
        // When
        fillTile(row = 0, column = 0)
        // Then
        worldBuilderRepository.search()!!.toDto().tiles shouldBe listOf(listOf("sand", "sand", "lava"))
        eventBus.shouldHavePublishedEvents(TilePaintRejected(row = 0, column = 0, terrainId = "water", adjacentTerrainId = "lava"))
    }

    @Test
    fun `should fail when the world has not been created`() {
        // When
        val fill = { fillTile(row = 0, column = 0) }
        // Then
        shouldThrow<WorldNotCreated> { fill() }
    }
}
