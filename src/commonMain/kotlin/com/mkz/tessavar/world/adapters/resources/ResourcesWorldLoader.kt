package com.mkz.tessavar.world.adapters.resources

import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.shared.adapters.toml.readTomlSections
import com.mkz.tessavar.world.domain.World
import com.mkz.tessavar.world.usecases.services.WorldLoader
import korlibs.io.file.VfsFile
import kotlinx.serialization.Serializable

class ResourcesWorldLoader(
    private val worldRoot: VfsFile = ResourcesBundle.root[WORLD_FOLDER],
) : WorldLoader {
    companion object {
        const val WORLD_FOLDER = "world"

        fun campaignFile(): VfsFile = ResourcesBundle.root["$WORLD_FOLDER/${World.CAMPAIGN_ID}.toml"]
    }

    private var worlds = emptyList<World.Dto>()

    override suspend fun initResources() {
        worlds = worldRoot.readTomlSections<WorldDefinition>().map { (worldId, definition) -> definition.toDto(worldId) }
    }

    override fun loadWorlds(): List<World.Dto> = worlds

    fun worldFile(id: String): VfsFile = worldRoot["$id.toml"]

    @Serializable
    data class WorldDefinition(
        val version: Int,
        val name: String,
        val author: String,
    ) {
        fun toDto(worldId: String) = World.Dto(id = worldId, name = name, author = author, version = version)
    }
}
