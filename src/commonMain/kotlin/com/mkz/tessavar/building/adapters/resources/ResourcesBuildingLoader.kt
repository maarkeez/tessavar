package com.mkz.tessavar.building.adapters.resources

import com.mkz.tessavar.building.domain.Building
import com.mkz.tessavar.building.usecases.services.BuildingLoader
import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.shared.adapters.toml.fromResource
import com.mkz.tessavar.shared.adapters.toml.readTomlFiles
import com.mkz.tessavar.shared.adapters.toml.readTomlSection
import korlibs.io.file.VfsFile
import kotlinx.serialization.Serializable

class ResourcesBuildingLoader(
    private val worldFile: VfsFile = ResourcesBundle.root[ResourcesBundle.CAMPAIGN_WORLD_FILE],
    private val buildingRoot: VfsFile = ResourcesBundle.root["building"],
) : BuildingLoader {
    private var buildings = emptyList<Building.Dto>()

    override suspend fun initResources() {
        val catalog = buildingRoot.readTomlFiles<BuildingDefinition>()
        buildings =
            worldFile.readTomlSection<BuildingsSection>().buildings.map { placement ->
                fromResource(worldFile.path) {
                    val definition = requireNotNull(catalog[placement.id]) { "'buildings.id' must be a building of the game, but was '${placement.id}'" }
                    placement.toDto(definition)
                }
            }
    }

    override fun loadBuildings(): List<Building.Dto> = buildings

    @Serializable
    data class BuildingsSection(
        val buildings: List<BuildingPlacement> = emptyList(),
    )

    @Serializable
    data class BuildingPlacement(
        val id: String,
        val row: Int,
        val column: Int,
    ) {
        fun toDto(definition: BuildingDefinition): Building.Dto =
            Building.Dto(
                id = id,
                row = row,
                column = column,
                rows = definition.rows,
                columns = definition.columns,
            )
    }

    @Serializable
    data class BuildingDefinition(
        val rows: Int,
        val columns: Int,
    )
}
