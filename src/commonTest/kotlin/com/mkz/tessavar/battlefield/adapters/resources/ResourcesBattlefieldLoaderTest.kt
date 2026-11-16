package com.mkz.tessavar.battlefield.adapters.resources

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import korlibs.io.file.std.MemoryVfsMix
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ResourcesBattlefieldLoaderTest {
    @Test
    fun `should load the tiles as rows when the battlefield of the world file is initialised`() =
        runTest {
            // Given
            val worldFile =
                MemoryVfsMix(
                    "world/world.toml" to "version = 1\n[battlefield]\ntiles = [\n    [\"sand\", \"void\"],\n    [\"grass\", \"sand\"],\n]\n",
                )["world/world.toml"]
            val loader = ResourcesBattlefieldLoader(worldFile = worldFile)
            // When
            loader.initResources()
            // Then
            loader.loadTiles() shouldBe listOf(listOf("sand", "void"), listOf("grass", "sand"))
        }

    @Test
    fun `should fail with the file when the rows have different number of columns`() =
        runTest {
            // Given
            val worldFile =
                MemoryVfsMix("world/world.toml" to "version = 1\n[battlefield]\ntiles = [[\"sand\", \"void\"], [\"grass\"]]\n")["world/world.toml"]
            val loader = ResourcesBattlefieldLoader(worldFile = worldFile)
            // When
            val initialisation = suspend { loader.initResources() }
            // Then
            shouldThrow<IllegalStateException> { initialisation() }.message.shouldContain("world/world.toml").shouldContain("same number of columns")
        }

    @Test
    fun `should fail with the file when the world file does not exist`() =
        runTest {
            // Given
            val worldFile = MemoryVfsMix()["world/world.toml"]
            val loader = ResourcesBattlefieldLoader(worldFile = worldFile)
            // When
            val initialisation = suspend { loader.initResources() }
            // Then
            shouldThrow<IllegalStateException> { initialisation() }.message.shouldContain("world/world.toml")
        }
}
