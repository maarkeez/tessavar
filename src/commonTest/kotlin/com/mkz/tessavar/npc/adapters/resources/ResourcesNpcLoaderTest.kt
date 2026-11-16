package com.mkz.tessavar.npc.adapters.resources

import com.mkz.tessavar.npc.domain.Npc
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import korlibs.io.file.std.MemoryVfsMix
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ResourcesNpcLoaderTest {
    private val files =
        MemoryVfsMix(
            "world/world.toml" to
                "version = 1\n\n[[npcs]]\nid = \"the-guy\"\nrow = 10\ncolumn = 2\n\n" +
                "[[npcs]]\nid = \"viking\"\nrow = 12\ncolumn = 2\n\n" +
                "[[npcs]]\nid = \"merchant\"\nrow = 14\ncolumn = 2\nstock = [\"mana-potion\", \"hammer\"]\n",
            "world/no-row.toml" to "[[npcs]]\nid = \"the-guy\"\ncolumn = 2\n",
            "npc/the-guy.toml" to "# An npc of the game.\n",
            "npc/merchant.toml" to "",
            "npc/viking.toml" to "unitId = \"viking\"\n",
        )

    private suspend fun load(worldFile: String): List<Npc.Dto> = ResourcesNpcLoader(worldFile = files[worldFile], npcRoot = files["npc"]).also { loader -> loader.initResources() }.loadNpcs()

    @Test
    fun `ResourcesNpcLoader should decode its section when given a world file, with the unit of a recruitable npc and the stock of a merchant in its order`() =
        runTest {
            // When
            val npcs = load("world/world.toml")
            // Then
            npcs.shouldContainExactly(
                Npc.Dto(id = "the-guy", row = 10, column = 2, unitId = null, stock = emptyList()),
                Npc.Dto(id = "viking", row = 12, column = 2, unitId = "viking", stock = emptyList()),
                Npc.Dto(id = "merchant", row = 14, column = 2, unitId = null, stock = listOf("mana-potion", "hammer")),
            )
        }

    @Test
    fun `ResourcesNpcLoader should fail with the world file when an npc has no row`() =
        runTest {
            // When
            val loading = suspend { load("world/no-row.toml") }
            // Then
            shouldThrow<IllegalStateException> { loading() }.message.shouldContain("world/no-row.toml")
        }
}
