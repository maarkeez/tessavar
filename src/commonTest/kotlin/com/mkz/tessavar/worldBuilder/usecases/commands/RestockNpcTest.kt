package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.NpcRestocked
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RestockNpcTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()

    private val restockNpc = RestockNpc(worldBuilderRepository, eventBus)

    @Test
    fun `RestockNpc should change what the npc sells when it stands in the world`() {
        // Given
        val worldBuilder = WorldBuilder.create().placeNpc(npcId = "alchemist", row = 1, column = 1, isOccupiable = { true })
        worldBuilderRepository.save(worldBuilder.pullEvents().second)
        // When
        restockNpc(npcId = "alchemist", stock = listOf("health-potion"))
        // Then
        worldBuilderRepository
            .search()!!
            .toDto()
            .npcs
            .single()
            .stock shouldBe listOf("health-potion")
        eventBus.shouldHavePublishedEvents(NpcRestocked(npcId = "alchemist", stock = listOf("health-potion")))
    }

    @Test
    fun `RestockNpc should fail when the world has not been created`() {
        // When
        val restock = { restockNpc(npcId = "alchemist", stock = emptyList()) }
        // Then
        shouldThrow<WorldNotCreated> { restock() }
    }
}
