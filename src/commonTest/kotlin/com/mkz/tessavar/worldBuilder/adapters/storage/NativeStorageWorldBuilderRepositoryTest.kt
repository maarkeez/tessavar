package com.mkz.tessavar.worldBuilder.adapters.storage

import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldDecoder
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldEncoder
import com.mkz.tessavar.battlefield.usecases.queries.ExportBattlefield
import com.mkz.tessavar.battlefield.usecases.queries.ParseBattlefield
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import korlibs.korge.service.storage.InmemoryStorage
import kotlin.test.Test

class NativeStorageWorldBuilderRepositoryTest {
    private val storage = InmemoryStorage()
    private val exportBattlefield = ExportBattlefield(TomlBattlefieldEncoder())

    private fun newRepository() = NativeStorageWorldBuilderRepository(storage, exportBattlefield, ParseBattlefield(TomlBattlefieldDecoder()))

    private val worldBuilder =
        WorldBuilder
            .create()
            .selectTerrain("lava")
            .paint(row = 0, column = 1) { _, _ -> true }
            .resize(rows = 2, columns = 3)
            .pullEvents()
            .second

    @Test
    fun `Search should find the world as it was saved when it is searched while the world builder is open`() {
        // Given
        val repository = newRepository()
        repository.save(worldBuilder)
        // When
        val found = repository.search()
        // Then
        found shouldBe worldBuilder
    }

    @Test
    fun `Save should write the world file to the storage when a world is saved`() {
        // When
        newRepository().save(worldBuilder)
        // Then
        storage.getOrNull(NativeStorageWorldBuilderRepository.KEY) shouldBe exportBattlefield(worldBuilder.toDto().tiles)
    }

    @Test
    fun `Search should read the tiles of the world kept in the storage when the world builder is opened again`() {
        // Given
        newRepository().save(worldBuilder)
        // When
        val found = newRepository().search()
        // Then
        found?.toDto()?.tiles shouldBe listOf(listOf("sand", "lava", "sand"), listOf("sand", "sand", "sand"))
    }

    @Test
    fun `Search should find nothing when no world is kept`() {
        // When
        val found = newRepository().search()
        // Then
        found.shouldBeNull()
    }

    @Test
    fun `Search should find nothing when the world kept is not a world file`() {
        // Given
        storage[NativeStorageWorldBuilderRepository.KEY] = "Hello, world builder!"
        // When
        val found = newRepository().search()
        // Then
        found.shouldBeNull()
    }
}
