package com.mkz.tessavar.worldBuilder.adapters.resources

import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderMother
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TomlWorldFileCodecTest {
    private val codec = WorldFileCodecMother.codec()

    @Test
    fun `TomlWorldFileCodec should write the campaign exactly as its world file when it is decoded and encoded again`() =
        runTest {
            // Given
            val campaign = ResourcesBundle.root[ResourcesBundle.CAMPAIGN_WORLD_FILE].readString()
            // When
            val encoded = codec.encode(codec.decode(campaign)!!)
            // Then
            encoded shouldBe campaign
        }

    @Test
    fun `TomlWorldFileCodec should read the version, name, author and tiles, and keep the other sections as they are written`() {
        // Given
        val text =
            "version = 1\nname = \"The \\\"Sunken\\\" Keep\"\nauthor = \"maarkeez\"\n\n[[npcs]]\nid = \"elf\"\nrow = 0\ncolumn = 0\n\n" +
                "[battlefield]\ntiles = [\n    [\"sand\"],\n]\n\n[[quests]]\nid = \"a\"\n"
        // When
        val file = codec.decode(text)
        // Then
        file shouldBe
            WorldBuilderMother.file(
                tiles = listOf(listOf("sand")),
                name = "The \"Sunken\" Keep",
                author = "maarkeez",
                keptSections = "[[npcs]]\nid = \"elf\"\nrow = 0\ncolumn = 0\n\n[[quests]]\nid = \"a\"",
            )
    }

    @Test
    fun `TomlWorldFileCodec should read nothing when the file has no world version`() {
        // When
        val file = codec.decode("[battlefield]\ntiles = [[\"sand\"]]\n")
        // Then
        file.shouldBeNull()
    }
}
