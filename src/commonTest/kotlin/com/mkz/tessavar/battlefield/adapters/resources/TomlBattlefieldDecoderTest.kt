package com.mkz.tessavar.battlefield.adapters.resources

import com.mkz.tessavar.battlefield.domain.BattlefieldError.InvalidBattlefieldFormat
import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TomlBattlefieldDecoderTest {
    private val decoder = TomlBattlefieldDecoder()

    @Test
    fun `should read the tiles as rows when an encoded battlefield is decoded`() {
        // Given
        val tiles = listOf(listOf("sand", "void", "grass"), listOf("water", "sand", "lava"))
        // When
        val decodedTiles = decoder.decode(TomlBattlefieldEncoder().encode(tiles))
        // Then
        decodedTiles shouldBe tiles
    }

    @Test
    fun `should read the campaign's battlefield as the loader does when its world file is decoded`() =
        runTest {
            // Given
            val resource = ResourcesBundle.root[ResourcesBundle.CAMPAIGN_WORLD_FILE]
            val loader = ResourcesBattlefieldLoader(worldFile = resource).also { loader -> loader.initResources() }
            // When
            val decodedTiles = decoder.decode(resource.readString())
            // Then
            decodedTiles shouldBe loader.loadTiles()
        }

    @Test
    fun `should fail when the text is not a battlefield`() {
        // When
        val decode = { decoder.decode("Hello, world builder!") }
        // Then
        shouldThrow<InvalidBattlefieldFormat> { decode() }
    }

    @Test
    fun `should fail when the rows have different number of columns`() {
        // When
        val decode = { decoder.decode("[battlefield]\ntiles = [[\"sand\", \"void\"], [\"grass\"]]\n") }
        // Then
        shouldThrow<InvalidBattlefieldFormat> { decode() }
    }
}
