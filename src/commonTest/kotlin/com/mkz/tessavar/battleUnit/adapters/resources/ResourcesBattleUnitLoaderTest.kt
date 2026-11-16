package com.mkz.tessavar.battleUnit.adapters.resources

import com.mkz.tessavar.battleUnit.usecases.services.BattleUnitLoader.Deployment
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import korlibs.io.file.std.MemoryVfsMix
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ResourcesBattleUnitLoaderTest {
    @Test
    fun `should load one deployment per battle unit of the world file when battle unit resources are initialised`() =
        runTest {
            // Given
            val worldFile =
                MemoryVfsMix(
                    "world/world.toml" to "version = 1\n\n[[battleUnits]]\nid = \"player-1-unit-1\"\nunitId = \"knight\"\nplayerId = \"player-one\"\nrow = 6\ncolumn = 7\n",
                )["world/world.toml"]
            val loader = ResourcesBattleUnitLoader(worldFile = worldFile)
            // When
            loader.initResources()
            // Then
            loader.loadDeployments().shouldContainExactly(
                Deployment(battleUnitId = "player-1-unit-1", unitId = "knight", playerId = "player-one", row = 6, column = 7),
            )
        }

    @Test
    fun `should fail with the file when a field is missing`() =
        runTest {
            // Given
            val worldFile = MemoryVfsMix("world/world.toml" to "[[battleUnits]]\nid = \"player-1-unit-1\"\nunitId = \"knight\"\n")["world/world.toml"]
            val loader = ResourcesBattleUnitLoader(worldFile = worldFile)
            // When
            val initialisation = suspend { loader.initResources() }
            // Then
            shouldThrow<IllegalStateException> { initialisation() }.message.shouldContain("world/world.toml")
        }
}
