package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.QuestAdded
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class AddQuestTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()
    private val useCase = AddQuest(worldBuilderRepository, eventBus)

    @Test
    fun `AddQuest should add a side quest, numbered after the quests there are, and announce it`() {
        // Given
        worldBuilderRepository.save(
            WorldBuilder
                .create()
                .addQuest(WorldBuilder.Dto.QuestTypeDto.MAIN)
                .pullEvents()
                .second,
        )
        // When
        useCase(WorldBuilder.Dto.QuestTypeDto.SIDE)
        // Then
        eventBus.shouldHavePublishedEvents(QuestAdded(questId = "quest-2"))
    }

    @Test
    fun `AddQuest should fail when the world has not been created`() {
        // When
        val run = { useCase(WorldBuilder.Dto.QuestTypeDto.SIDE) }
        // Then
        shouldThrow<WorldNotCreated> { run() }
    }
}
