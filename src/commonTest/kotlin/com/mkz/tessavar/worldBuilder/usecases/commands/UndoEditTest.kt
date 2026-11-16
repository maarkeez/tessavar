package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.EditUndone
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class UndoEditTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()
    private val undoEdit = UndoEdit(worldBuilderRepository, eventBus)

    @Test
    fun `should undo the last edit of the world when undoing`() {
        // Given
        worldBuilderRepository.save(
            WorldBuilder
                .create()
                .resize(rows = 4, columns = 2)
                .pullEvents()
                .second,
        )
        // When
        undoEdit()
        // Then
        (worldBuilderRepository.search()!!.toDto().rows to worldBuilderRepository.search()!!.toDto().columns) shouldBe (16 to 16)
        eventBus.shouldHavePublishedEvents(EditUndone(rows = 16, columns = 16))
    }

    @Test
    fun `should fail when the world has not been created`() {
        // When
        val undo = { undoEdit() }
        // Then
        shouldThrow<WorldNotCreated> { undo() }
    }
}
