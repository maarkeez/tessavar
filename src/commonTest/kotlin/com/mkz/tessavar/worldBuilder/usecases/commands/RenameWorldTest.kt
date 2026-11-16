package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldRenamed
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RenameWorldTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val eventBus = FakeEventBus()
    private val renameWorld = RenameWorld(worldBuilderRepository, eventBus)

    @Test
    fun `RenameWorld should name the world and credit its author when they are typed`() {
        // Given
        worldBuilderRepository.save(WorldBuilder.create().pullEvents().second)
        // When
        renameWorld(name = "The Sunken Keep", author = "maarkeez")
        // Then
        worldBuilderRepository.search()!!.toDto().name shouldBe "The Sunken Keep"
        eventBus.shouldHavePublishedEvents(WorldRenamed(name = "The Sunken Keep", author = "maarkeez"))
    }

    @Test
    fun `RenameWorld should fail when the world has not been created`() {
        // When
        val rename = { renameWorld(name = "The Sunken Keep", author = "maarkeez") }
        // Then
        shouldThrow<WorldNotCreated> { rename() }
    }
}
