package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.OccupantErased
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class EraseOccupantTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()

    private val eraseOccupant = EraseOccupant(worldBuilderRepository, eventBus)

    @Test
    fun `EraseOccupant should take away the battle unit standing on the tile`() {
        // Given
        val worldBuilder = WorldBuilder.create().placeBattleUnit(unitId = "rat", playerId = "player-two", row = 1, column = 1, isOccupiable = { true })
        worldBuilderRepository.save(worldBuilder.pullEvents().second)
        // When
        eraseOccupant(row = 1, column = 1)
        // Then
        worldBuilderRepository.search()!!.toDto().battleUnits shouldBe emptyList()
        eventBus.shouldHavePublishedEvents(OccupantErased(occupantId = "rat-1", row = 1, column = 1))
    }

    @Test
    fun `EraseOccupant should fail when the world has not been created`() {
        // When
        val erase = { eraseOccupant(row = 1, column = 1) }
        // Then
        shouldThrow<WorldNotCreated> { erase() }
    }
}
