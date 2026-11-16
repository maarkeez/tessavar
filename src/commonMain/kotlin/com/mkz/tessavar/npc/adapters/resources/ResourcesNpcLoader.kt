package com.mkz.tessavar.npc.adapters.resources

import com.mkz.tessavar.npc.domain.Npc
import com.mkz.tessavar.npc.usecases.services.NpcLoader
import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.shared.adapters.toml.fromResource
import com.mkz.tessavar.shared.adapters.toml.readTomlFiles
import com.mkz.tessavar.shared.adapters.toml.readTomlSection
import korlibs.io.file.VfsFile
import kotlinx.serialization.Serializable

class ResourcesNpcLoader(
    private val worldFile: VfsFile = ResourcesBundle.root[ResourcesBundle.CAMPAIGN_WORLD_FILE],
    private val npcRoot: VfsFile = ResourcesBundle.root["npc"],
) : NpcLoader {
    private var npcs = emptyList<Npc.Dto>()

    override suspend fun initResources() {
        val catalog = npcRoot.readTomlFiles<NpcDefinition>()
        npcs =
            worldFile.readTomlSection<NpcsSection>().npcs.map { placement ->
                fromResource(worldFile.path) {
                    val definition = requireNotNull(catalog[placement.id]) { "'npcs.id' must be an npc of the game, but was '${placement.id}'" }
                    placement.toDto(definition)
                }
            }
    }

    override fun loadNpcs(): List<Npc.Dto> = npcs

    @Serializable
    data class NpcsSection(
        val npcs: List<NpcPlacement> = emptyList(),
    )

    @Serializable
    data class NpcPlacement(
        val id: String,
        val row: Int,
        val column: Int,
        val stock: List<String> = emptyList(),
    ) {
        fun toDto(definition: NpcDefinition): Npc.Dto =
            Npc.Dto(
                id = id,
                row = row,
                column = column,
                unitId = definition.unitId,
                stock = stock,
            )
    }

    @Serializable
    data class NpcDefinition(
        val unitId: String? = null,
    )
}
