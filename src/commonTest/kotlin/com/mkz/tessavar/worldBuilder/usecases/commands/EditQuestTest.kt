package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.QuestEdited
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderMother
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class EditQuestTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()
    private val editQuest = EditQuest(worldBuilderRepository, eventBus)

    @Test
    fun `EditQuest should replace the quest with the one edited and announce it`() {
        // Given
        worldBuilderRepository.save(
            WorldBuilder
                .create()
                .addQuest(WorldBuilder.Dto.QuestTypeDto.MAIN)
                .pullEvents()
                .second,
        )
        val edited = WorldBuilderMother.quest(id = "quest-1")
        // When
        editQuest(edited)
        // Then
        worldBuilderRepository
            .search()!!
            .toDto()
            .quests
            .single() shouldBe edited
        eventBus.shouldHavePublishedEvents(QuestEdited(questId = "quest-1"))
    }

    @Test
    fun `EditQuest should fail when the world has not been created`() {
        // When
        val run = { editQuest(WorldBuilderMother.quest()) }
        // Then
        shouldThrow<WorldNotCreated> { run() }
    }
}
