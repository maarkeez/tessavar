package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.QuestRemoved
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class RemoveQuestTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()
    private val useCase = RemoveQuest(worldBuilderRepository, eventBus)

    @Test
    fun `RemoveQuest should take the quest away and announce it`() {
        // Given
        worldBuilderRepository.save(
            WorldBuilder
                .create()
                .addQuest(WorldBuilder.Dto.QuestTypeDto.MAIN)
                .pullEvents()
                .second,
        )
        // When
        useCase("quest-1")
        // Then
        eventBus.shouldHavePublishedEvents(QuestRemoved(questId = "quest-1"))
    }

    @Test
    fun `RemoveQuest should fail when the world has not been created`() {
        // When
        val run = { useCase("quest-1") }
        // Then
        shouldThrow<WorldNotCreated> { run() }
    }
}
