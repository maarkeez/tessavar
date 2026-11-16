package com.mkz.tessavar.battlefield.adapters.resources

import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import io.kotest.matchers.shouldBe
import korlibs.io.file.std.MemoryVfsMix
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TomlBattlefieldEncoderTest {
    private val encoder = TomlBattlefieldEncoder()

    private suspend fun load(toml: String): List<List<String>> {
        val loader = ResourcesBattlefieldLoader(worldFile = MemoryVfsMix("world/world.toml" to toml)["world/world.toml"])
        loader.initResources()
        return loader.loadTiles()
    }

    @Test
    fun `should write one row per line with the terrain ids aligned when the tiles are encoded`() {
        // Given
        val tiles = listOf(listOf("sand", "water"), listOf("lava", "sand"))
        // When
        val toml = encoder.encode(tiles)
        // Then
        toml shouldBe
            """
            |[battlefield]
            |tiles = [
            |    ["sand",   "water"],
            |    ["lava",   "sand"],
            |]
            |
            """.trimMargin()
    }

    @Test
    fun `should be loaded as the same tiles when the encoded tiles are loaded as the battlefield`() =
        runTest {
            // Given
            val tiles = listOf(listOf("sand", "void", "grass"), listOf("water", "sand", "lava"))
            // When
            val loadedTiles = load(encoder.encode(tiles))
            // Then
            loadedTiles shouldBe tiles
        }

    @Test
    fun `should write the campaign's battlefield exactly as its world file holds it when its tiles are encoded`() =
        runTest {
            // Given
            val worldFile = ResourcesBundle.root[ResourcesBundle.CAMPAIGN_WORLD_FILE].readString()
            val section = worldFile.substringAfter("[battlefield]\n").substringBefore("\n]\n") + "\n]\n"
            // When
            val toml = encoder.encode(load(worldFile))
            // Then
            toml shouldBe "[battlefield]\n$section"
        }
}
