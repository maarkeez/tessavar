package com.mkz.tessavar.worldBuilder.usecases.queries

import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldEncoder
import com.mkz.tessavar.worldBuilder.adapters.resources.WorldFileCodecMother
import com.mkz.tessavar.worldBuilder.adapters.storage.InMemoryWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ExportWorldTest {
    private val worldBuilderRepository = InMemoryWorldBuilderRepository()
    private val exportWorld = ExportWorld(worldBuilderRepository, WorldFileCodecMother.codec())

    @Test
    fun `should write the world file of version 1 with its tiles when the world is exported`() {
        // Given
        val worldBuilder =
            WorldBuilder
                .create()
                .resize(rows = 1, columns = 2)
                .selectTerrain("lava")
                .paint(row = 0, column = 1, canBeAdjacent = { _, _ -> true }, isOccupiable = { true })
        worldBuilderRepository.save(worldBuilder)
        // When
        val exported = exportWorld()
        // Then
        exported shouldBe "version = 1\nname = \"\"\nauthor = \"\"\n\n" + TomlBattlefieldEncoder().encode(listOf(listOf("sand", "lava")))
    }

    @Test
    fun `should fail when the world has not been created`() {
        // When
        val export = { exportWorld() }
        // Then
        shouldThrow<WorldNotCreated> { export() }
    }
}
