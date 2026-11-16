package com.mkz.tessavar.shared.usecases.acceptance

import com.mkz.tessavar.shared.adapters.events.InMemoryEventBus
import com.mkz.tessavar.terrain.adapters.presentation.TerrainApi
import com.mkz.tessavar.terrain.adapters.resources.ResourcesTerrainLoader
import com.mkz.tessavar.world.adapters.presentation.WorldApi
import com.mkz.tessavar.world.adapters.resources.ResourcesWorldLoader
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import kotlin.test.Test

class TerrainTransitionsAcceptanceTest {
    private val eventBus = InMemoryEventBus()
    private val terrainApi = TerrainApi(eventBus, ResourcesTerrainLoader())
    private val worldApi = WorldApi(eventBus, ResourcesWorldLoader())

    private suspend fun setUp() {
        terrainApi.init()
    }

    @Test
    fun `should have only the sand to void transition when the game is set up`() =
        scenarioTest(::setUp) {
            // Given
            worldApi.setUpGame()
            // When
            eventBus.dispatch()
            // Then
            require(terrainApi.searchTerrainById("sand") != null)
            require(terrainApi.searchTerrainById("void") != null)
            terrainApi.hasTerrainTransition(terrainId = "sand", toTerrainId = "void").shouldBeTrue()
            terrainApi.hasTerrainTransition(terrainId = "void", toTerrainId = "sand").shouldBeFalse()
        }
}
