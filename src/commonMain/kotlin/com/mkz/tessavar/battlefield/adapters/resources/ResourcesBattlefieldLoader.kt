package com.mkz.tessavar.battlefield.adapters.resources

import com.mkz.tessavar.battlefield.usecases.services.BattlefieldLoader
import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.shared.adapters.toml.fromResource
import com.mkz.tessavar.shared.adapters.toml.readTomlSection
import korlibs.io.file.VfsFile
import kotlinx.serialization.Serializable

class ResourcesBattlefieldLoader(
    private val worldFile: VfsFile = ResourcesBundle.root[ResourcesBundle.CAMPAIGN_WORLD_FILE],
) : BattlefieldLoader {
    private var tiles = emptyList<List<String>>()

    override suspend fun initResources() {
        val section = worldFile.readTomlSection<BattlefieldSection>()
        tiles = fromResource(worldFile.path) { section.battlefield.toTiles() }
    }

    override fun loadTiles(): List<List<String>> = tiles

    @Serializable
    data class BattlefieldSection(
        val battlefield: BattlefieldDefinition,
    )

    @Serializable
    data class BattlefieldDefinition(
        val tiles: List<List<String>>,
    ) {
        fun toTiles(): List<List<String>> {
            require(tiles.isNotEmpty()) { "'tiles' must have at least one row" }
            require(tiles.all { row -> row.size == tiles.first().size }) { "'tiles' rows must all have the same number of columns" }
            return tiles
        }
    }
}
