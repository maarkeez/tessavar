package com.mkz.tessavar.battleUnit.adapters.resources

import com.mkz.tessavar.battleUnit.usecases.services.BattleUnitLoader
import com.mkz.tessavar.battleUnit.usecases.services.BattleUnitLoader.Deployment
import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.shared.adapters.toml.readTomlSection
import korlibs.io.file.VfsFile
import kotlinx.serialization.Serializable

class ResourcesBattleUnitLoader(
    private val worldFile: VfsFile = ResourcesBundle.root[ResourcesBundle.CAMPAIGN_WORLD_FILE],
) : BattleUnitLoader {
    private var deployments = emptyList<Deployment>()

    override suspend fun initResources() {
        deployments = worldFile.readTomlSection<BattleUnitsSection>().battleUnits.map(DeploymentDefinition::toDeployment)
    }

    override fun loadDeployments(): List<Deployment> = deployments

    @Serializable
    data class BattleUnitsSection(
        val battleUnits: List<DeploymentDefinition> = emptyList(),
    )

    @Serializable
    data class DeploymentDefinition(
        val id: String,
        val unitId: String,
        val playerId: String,
        val row: Int,
        val column: Int,
    ) {
        fun toDeployment(): Deployment =
            Deployment(
                battleUnitId = id,
                unitId = unitId,
                playerId = playerId,
                row = row,
                column = column,
            )
    }
}
