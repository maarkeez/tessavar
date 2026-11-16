package com.mkz.tessavar.worldBuilder.adapters.resources

import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.shared.adapters.toml.readTomlSections
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.usecases.services.Catalog
import korlibs.io.file.VfsFile
import kotlinx.serialization.Serializable

class ResourcesCatalog(
    private val root: VfsFile = ResourcesBundle.root,
) : Catalog {
    private var catalog = WorldBuilder.Dto.CatalogDto(buildings = emptyList(), unitIds = emptyList(), npcs = emptyList(), playerIds = emptyList(), lootableItemIds = emptyList())

    override suspend fun initResources() {
        catalog =
            WorldBuilder.Dto.CatalogDto(
                buildings =
                    root["building"].readTomlSections<BuildingDefinition>().map { (id, building) ->
                        WorldBuilder.Dto.CatalogBuildingDto(id = id, rows = building.rows, columns = building.columns)
                    },
                unitIds = tomlNames(root["unit"]),
                npcs = root["npc"].readTomlSections<NpcDefinition>().map { (id, npc) -> WorldBuilder.Dto.CatalogNpcDto(id = id, unitId = npc.unitId) },
                playerIds = tomlNames(root["player"]),
                lootableItemIds = tomlNames(root["lootableItem"]),
            )
    }

    override fun loadCatalog(): WorldBuilder.Dto.CatalogDto = catalog

    private suspend fun tomlNames(directory: VfsFile): List<String> =
        directory
            .listNames()
            .filter { name -> name.endsWith(TOML_EXTENSION) }
            .map { name -> name.removeSuffix(TOML_EXTENSION) }
            .sorted()

    private companion object {
        const val TOML_EXTENSION = ".toml"
    }

    @Serializable
    data class BuildingDefinition(
        val rows: Int,
        val columns: Int,
    )

    @Serializable
    data class NpcDefinition(
        val unitId: String? = null,
    )
}
