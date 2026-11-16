package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldOpened
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class OpenWorldTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()
    private val openWorld = OpenWorld(worldBuilderRepository, eventBus)

    @Test
    fun `should store a new world and announce it when there is no world being built`() {
        // When
        openWorld()
        // Then
        worldBuilderRepository.search()?.toDto() shouldBe WorldBuilder.create().toDto()
        eventBus.shouldHavePublishedEvents(WorldOpened(rows = 16, columns = 16))
    }

    @Test
    fun `should open the world being built and announce it when there is one`() {
        // Given
        openWorld()
        ResizeWorld(worldBuilderRepository, FakeEventBus())(rows = 4, columns = 2)
        // When
        openWorld()
        // Then
        worldBuilderRepository.search()?.toDto()?.rows shouldBe 4
        eventBus.shouldHavePublishedEvents(WorldOpened(rows = 16, columns = 16), WorldOpened(rows = 4, columns = 2))
    }
}
