package com.mkz.tessavar.terrain.domain

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TerrainTest {
    @Test
    fun `Create should create terrain when the dto is valid`() {
        // Given
        val id = "sand"
        val canBeOccupied = true
        val transitionsTo = setOf("void")
        // When
        val terrain =
            Terrain.create(
                Terrain.Dto(
                    id = id,
                    canBeOccupied = canBeOccupied,
                    transitionsTo = transitionsTo,
                    effectIds = emptyList(),
                ),
            )
        // Then
        terrain.toDto() shouldBe Terrain.Dto(id = id, canBeOccupied = canBeOccupied, transitionsTo = transitionsTo, effectIds = emptyList())
    }

    @Test
    fun `Terrain should hold every effect id when it has several`() {
        // Given
        val id = "lava"
        val effectIds = listOf("lava-damage", "burn")
        // When
        val terrain = Terrain.create(Terrain.Dto(id = id, canBeOccupied = true, transitionsTo = emptySet(), effectIds = effectIds))
        // Then
        terrain.toDto().effectIds.shouldContainExactly("lava-damage", "burn")
    }

    @Test
    fun `Create should create terrain without effect ids when the dto does not contain any`() {
        // Given
        val id = "sand"
        // When
        val terrain = Terrain.create(Terrain.Dto(id = id, canBeOccupied = true, transitionsTo = emptySet(), effectIds = emptyList()))
        // Then
        terrain.toDto().effectIds.shouldBeEmpty()
    }

    @Test
    fun `Create should publish the terrain created event when a terrain is created`() {
        // Given
        val id = "sand"
        // When
        val terrain = Terrain.create(Terrain.Dto(id = id, canBeOccupied = true, transitionsTo = emptySet(), effectIds = emptyList()))
        val (events, _) = terrain.pullEvents()
        // Then
        events.shouldContainExactly(TerrainEvent.TerrainCreated(terrainId = id))
    }

    @Test
    fun `HasTransitionTo should be true when the terrain has the transition`() {
        // Given
        val terrain = TerrainMother.terrainThatCanBeOccupied(id = "sand", transitionsTo = setOf("void"))
        // When
        val hasTransition = terrain.hasTransitionTo("void")
        // Then
        hasTransition.shouldBeTrue()
    }

    @Test
    fun `HasTransitionTo should be false when the terrain does not have the transition`() {
        // Given
        val terrain = TerrainMother.terrainThatCanBeOccupied(id = "sand", transitionsTo = setOf("void"))
        // When
        val hasTransition = terrain.hasTransitionTo("forest")
        // Then
        hasTransition.shouldBeFalse()
    }

    @Test
    fun `HasTransitionTo should be false when the terrain has no transitions`() {
        // Given
        val terrain = TerrainMother.terrainThatCanBeOccupied(id = "sand")
        // When
        val hasTransition = terrain.hasTransitionTo("void")
        // Then
        hasTransition.shouldBeFalse()
    }

    @Test
    fun `ToDto should expose the terrain data when converted to dto`() {
        // Given
        val terrain = TerrainMother.terrainThatCanBeOccupied(id = "sand", transitionsTo = setOf("void"))
        // When
        val dto = terrain.toDto()
        // Then
        dto shouldBe Terrain.Dto(id = "sand", canBeOccupied = true, transitionsTo = setOf("void"), effectIds = emptyList())
    }

    @Test
    fun `ToDto should expose the effect id when the terrain has an effect assigned`() {
        // Given
        val terrain = Terrain.create(Terrain.Dto(id = "swamp", canBeOccupied = true, transitionsTo = emptySet(), effectIds = listOf("swamp-effect")))
        // When
        val dto = terrain.toDto()
        // Then
        dto.effectIds.shouldContainExactly("swamp-effect")
    }

    @Test
    fun `PullEvents should pull the created event when the terrain has pending events`() {
        // Given
        val terrain = TerrainMother.terrainThatCanBeOccupied(id = "sand")
        // When
        val (events, _) = terrain.pullEvents()
        // Then
        events.shouldNotBeEmpty()
    }

    @Test
    fun `PullEvents should not pull events when there are no pending events`() {
        // Given
        val (events, terrain) = TerrainMother.terrainThatCanBeOccupied(id = "sand").pullEvents()
        // When
        val (pulledEvents, _) = terrain.pullEvents()
        // Then
        events.shouldNotBeEmpty()
        pulledEvents.shouldBeEmpty()
    }
}
