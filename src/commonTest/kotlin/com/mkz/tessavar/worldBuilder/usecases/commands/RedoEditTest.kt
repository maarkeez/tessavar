package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.EditRedone
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RedoEditTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()
    private val redoEdit = RedoEdit(worldBuilderRepository, eventBus)

    @Test
    fun `should redo the last edit undone when redoing`() {
        // Given
        worldBuilderRepository.save(
            WorldBuilder
                .create()
                .resize(rows = 4, columns = 2)
                .undo()
                .pullEvents()
                .second,
        )
        // When
        redoEdit()
        // Then
        (worldBuilderRepository.search()!!.toDto().rows to worldBuilderRepository.search()!!.toDto().columns) shouldBe (4 to 2)
        eventBus.shouldHavePublishedEvents(EditRedone(rows = 4, columns = 2))
    }

    @Test
    fun `should fail when the world has not been created`() {
        // When
        val redo = { redoEdit() }
        // Then
        shouldThrow<WorldNotCreated> { redo() }
    }
}
