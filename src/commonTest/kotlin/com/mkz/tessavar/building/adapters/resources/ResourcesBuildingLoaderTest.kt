package com.mkz.tessavar.building.adapters.resources

import com.mkz.tessavar.building.domain.Building
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import korlibs.io.file.std.MemoryVfsMix
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ResourcesBuildingLoaderTest {
    private val files =
        MemoryVfsMix(
            "world/world.toml" to "version = 1\n\n[[buildings]]\nid = \"fortress\"\nrow = 17\ncolumn = 13\n",
            "world/unknown.toml" to "[[buildings]]\nid = \"castle\"\nrow = 1\ncolumn = 1\n",
            "building/fortress.toml" to "rows = 4\ncolumns = 4\n",
        )

    @Test
    fun `ResourcesBuildingLoader should decode its section when given a world file, with the size of each building of the catalog`() =
        runTest {
            // Given
            val loader = ResourcesBuildingLoader(worldFile = files["world/world.toml"], buildingRoot = files["building"])
            // When
            loader.initResources()
            // Then
            loader.loadBuildings().shouldContainExactly(Building.Dto(id = "fortress", row = 17, column = 13, rows = 4, columns = 4))
        }

    @Test
    fun `ResourcesBuildingLoader should fail with the world file when a building is not in the catalog`() =
        runTest {
            // Given
            val loader = ResourcesBuildingLoader(worldFile = files["world/unknown.toml"], buildingRoot = files["building"])
            // When
            val initialisation = suspend { loader.initResources() }
            // Then
            shouldThrow<IllegalStateException> { initialisation() }.message.shouldContain("world/unknown.toml").shouldContain("castle")
        }
}
