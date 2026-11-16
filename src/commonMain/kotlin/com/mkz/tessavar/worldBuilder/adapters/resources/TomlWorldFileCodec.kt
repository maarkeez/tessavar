package com.mkz.tessavar.worldBuilder.adapters.resources

import com.mkz.tessavar.battlefield.domain.BattlefieldError.InvalidBattlefieldFormat
import com.mkz.tessavar.battlefield.usecases.queries.ExportBattlefield
import com.mkz.tessavar.battlefield.usecases.queries.ParseBattlefield
import com.mkz.tessavar.shared.adapters.toml.TomlService
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.usecases.services.WorldFileCodec
import kotlinx.serialization.Serializable

class TomlWorldFileCodec(
    private val parseBattlefield: ParseBattlefield,
    private val exportBattlefield: ExportBattlefield,
) : WorldFileCodec {
    private companion object {
        const val MAIN_QUEST = "MAIN"
    }

    private val editedTables = setOf("[battlefield]", "[[buildings]]", "[[battleUnits]]", "[[npcs]]")
    private val tableHeader = Regex("""^\[\[?\s*[A-Za-z0-9_.\-]+\s*\]\]?\s*(#.*)?$""")

    override fun decode(text: String): WorldBuilder.Dto.WorldFileDto? {
        val header =
            try {
                TomlService().deserializeSection<WorldHeader>(text)
            } catch (_: Exception) {
                return null
            }
        val version = header.version ?: return null
        val tiles =
            try {
                parseBattlefield(text)
            } catch (_: InvalidBattlefieldFormat) {
                return null
            }
        val occupants =
            try {
                TomlService().deserializeSection<OccupantSections>(text)
            } catch (_: Exception) {
                return null
            }
        return WorldBuilder.Dto.WorldFileDto(
            version = version,
            name = header.name,
            author = header.author,
            tiles = tiles,
            buildings = occupants.buildings.map { building -> WorldBuilder.Dto.BuildingDto(id = building.id, row = building.row, column = building.column) },
            battleUnits =
                occupants.battleUnits.map { battleUnit ->
                    WorldBuilder.Dto.BattleUnitDto(id = battleUnit.id, unitId = battleUnit.unitId, playerId = battleUnit.playerId, row = battleUnit.row, column = battleUnit.column)
                },
            npcs = occupants.npcs.map { npc -> WorldBuilder.Dto.NpcDto(id = npc.id, row = npc.row, column = npc.column, stock = npc.stock) },
            keptSections = keptSections(text),
            mainQuestCount = occupants.quests.count { quest -> quest.type == MAIN_QUEST },
        )
    }

    override fun encode(file: WorldBuilder.Dto.WorldFileDto): String {
        val header = "version = ${file.version}\nname = ${quoted(file.name)}\nauthor = ${quoted(file.author)}\n"
        val tables =
            file.buildings.map { building -> table("[[buildings]]", "id" to quoted(building.id), "row" to "${building.row}", "column" to "${building.column}") } +
                file.battleUnits.map { battleUnit ->
                    table(
                        "[[battleUnits]]",
                        "id" to quoted(battleUnit.id),
                        "unitId" to quoted(battleUnit.unitId),
                        "playerId" to quoted(battleUnit.playerId),
                        "row" to "${battleUnit.row}",
                        "column" to "${battleUnit.column}",
                    )
                } +
                file.npcs.map { npc ->
                    val stock = if (npc.stock.isEmpty()) emptyArray() else arrayOf("stock" to npc.stock.joinToString(", ", prefix = "[", postfix = "]", transform = ::quoted))
                    table("[[npcs]]", "id" to quoted(npc.id), "row" to "${npc.row}", "column" to "${npc.column}", *stock)
                } +
                listOf(file.keptSections).filter(String::isNotBlank)
        return "$header\n${exportBattlefield(file.tiles)}" + tables.joinToString("") { table -> "\n$table\n" }
    }

    private fun table(
        header: String,
        vararg keys: Pair<String, String>,
    ): String = (listOf(header) + keys.map { (key, value) -> "$key = $value" }).joinToString("\n")

    private fun keptSections(text: String): String {
        val tables = mutableListOf<MutableList<String>>()
        text.lines().forEach { line ->
            if (tableHeader.matches(line.trim()) && !line.startsWith(" ")) tables += mutableListOf(line) else tables.lastOrNull()?.add(line)
        }
        return tables
            .filterNot { table -> table.first().trim() in editedTables }
            .joinToString("\n") { table -> table.joinToString("\n").trimEnd() + "\n" }
            .trimEnd()
    }

    private fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    @Serializable
    data class OccupantSections(
        val buildings: List<BuildingPlacement> = emptyList(),
        val battleUnits: List<BattleUnitPlacement> = emptyList(),
        val npcs: List<NpcPlacement> = emptyList(),
        val quests: List<QuestType> = emptyList(),
    )

    @Serializable
    data class QuestType(
        val type: String = "",
    )

    @Serializable
    data class BuildingPlacement(
        val id: String,
        val row: Int,
        val column: Int,
    )

    @Serializable
    data class BattleUnitPlacement(
        val id: String,
        val unitId: String,
        val playerId: String,
        val row: Int,
        val column: Int,
    )

    @Serializable
    data class NpcPlacement(
        val id: String,
        val row: Int,
        val column: Int,
        val stock: List<String> = emptyList(),
    )

    @Serializable
    data class WorldHeader(
        val version: Int? = null,
        val name: String = "",
        val author: String = "",
    )
}
