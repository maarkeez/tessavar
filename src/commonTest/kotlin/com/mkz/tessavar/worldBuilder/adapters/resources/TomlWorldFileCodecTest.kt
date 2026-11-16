package com.mkz.tessavar.worldBuilder.adapters.resources

import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
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
    fun `TomlWorldFileCodec should read the header, tiles, buildings, battle units and npcs, and keep the other sections as they are written`() {
        // Given
        val text =
            "version = 1\nname = \"The \\\"Sunken\\\" Keep\"\nauthor = \"maarkeez\"\n\n[[npcs]]\nid = \"elf\"\nrow = 0\ncolumn = 0\nstock = [\"hammer\"]\n\n" +
                "[battlefield]\ntiles = [\n    [\"sand\"],\n]\n\n[[quests]]\nid = \"a\"\n\n[[buildings]]\nid = \"fortress\"\nrow = 1\ncolumn = 2\n\n" +
                "[[battleUnits]]\nid = \"rat-1\"\nunitId = \"rat\"\nplayerId = \"player-two\"\nrow = 3\ncolumn = 4\n"
        // When
        val file = codec.decode(text)
        // Then
        file shouldBe
            WorldBuilderMother.file(
                tiles = listOf(listOf("sand")),
                name = "The \"Sunken\" Keep",
                author = "maarkeez",
                keptSections = "[[quests]]\nid = \"a\"",
                buildings = listOf(WorldBuilder.Dto.BuildingDto(id = "fortress", row = 1, column = 2)),
                battleUnits = listOf(WorldBuilder.Dto.BattleUnitDto(id = "rat-1", unitId = "rat", playerId = "player-two", row = 3, column = 4)),
                npcs = listOf(WorldBuilder.Dto.NpcDto(id = "elf", row = 0, column = 0, stock = listOf("hammer"))),
            )
    }

    @Test
    fun `TomlWorldFileCodec should write the buildings, battle units and npcs after the battlefield, an npc's stock only when it sells something`() {
        // Given
        val file =
            WorldBuilderMother.file(
                tiles = listOf(listOf("sand")),
                name = "Keep",
                author = "maarkeez",
                buildings = listOf(WorldBuilder.Dto.BuildingDto(id = "fortress", row = 1, column = 2)),
                battleUnits = listOf(WorldBuilder.Dto.BattleUnitDto(id = "rat-1", unitId = "rat", playerId = "player-two", row = 3, column = 4)),
                npcs =
                    listOf(
                        WorldBuilder.Dto.NpcDto(id = "elf", row = 0, column = 0, stock = emptyList()),
                        WorldBuilder.Dto.NpcDto(id = "alchemist", row = 5, column = 6, stock = listOf("hammer", "mana-potion")),
                    ),
            )
        // When
        val text = codec.encode(file)
        // Then
        text shouldBe
            "version = 1\nname = \"Keep\"\nauthor = \"maarkeez\"\n\n[battlefield]\ntiles = [\n    [\"sand\"],\n]\n\n" +
            "[[buildings]]\nid = \"fortress\"\nrow = 1\ncolumn = 2\n\n" +
            "[[battleUnits]]\nid = \"rat-1\"\nunitId = \"rat\"\nplayerId = \"player-two\"\nrow = 3\ncolumn = 4\n\n" +
            "[[npcs]]\nid = \"elf\"\nrow = 0\ncolumn = 0\n\n" +
            "[[npcs]]\nid = \"alchemist\"\nrow = 5\ncolumn = 6\nstock = [\"hammer\", \"mana-potion\"]\n"
        codec.decode(text) shouldBe file
    }

    @Test
    fun `TomlWorldFileCodec should read nothing when the file has no world version`() {
        // When
        val file = codec.decode("[battlefield]\ntiles = [[\"sand\"]]\n")
        // Then
        file.shouldBeNull()
    }
}
