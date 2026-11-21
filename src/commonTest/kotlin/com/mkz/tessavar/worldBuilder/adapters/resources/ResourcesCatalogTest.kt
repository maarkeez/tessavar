package com.mkz.tessavar.worldBuilder.adapters.resources

import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ResourcesCatalogTest {
    @Test
    fun `ResourcesCatalog should list the buildings with their size, the units, the npcs with their unit, the players and the lootable items of the game`() =
        runTest {
            // Given
            val catalog = ResourcesCatalog()
            // When
            catalog.initResources()
            // Then
            val loaded = catalog.loadCatalog()
            loaded.buildings.shouldContainExactly(WorldBuilder.Dto.CatalogBuildingDto(id = "fortress", rows = 4, columns = 4))
            loaded.unitIds shouldContain "hero"
            loaded.npcs shouldContain WorldBuilder.Dto.CatalogNpcDto(id = "viking", unitId = "viking-warrior")
            loaded.npcs shouldContain WorldBuilder.Dto.CatalogNpcDto(id = "alchemist", unitId = null)
            loaded.playerIds.shouldContainExactly("player-one", "player-two")
            loaded.lootableItemIds shouldContain "health-potion"
        }
}
