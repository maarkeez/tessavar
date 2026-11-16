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
    private val editedTables = setOf("[battlefield]", "[[buildings]]", "[[battleUnits]]", "[[npcs]]", "[[quests]]", "[[quests.tasks]]", "[quests.reward]")
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
        val quests =
            try {
                occupants.quests.map(QuestDefinition::toDto)
            } catch (_: IllegalArgumentException) {
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
            quests = quests,
            keptSections = keptSections(text),
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
                    val stock = if (npc.stock.isEmpty()) emptyArray() else arrayOf("stock" to list(npc.stock))
                    table("[[npcs]]", "id" to quoted(npc.id), "row" to "${npc.row}", "column" to "${npc.column}", *stock)
                } +
                file.quests.flatMap(::questTables) +
                listOf(file.keptSections).filter(String::isNotBlank)
        return "$header\n${exportBattlefield(file.tiles)}" + tables.joinToString("") { table -> "\n$table\n" }
    }

    private fun questTables(quest: WorldBuilder.Dto.QuestDto): List<String> {
        val lines =
            listOfNotNull(
                quest.lines.start?.let { line -> "start = ${quoted(line)}" },
                quest.lines.reminder?.let { line -> "reminder = ${quoted(line)}" },
                quest.lines.completed?.let { line -> "completed = ${quoted(line)}" },
                quest.lines.startAgain?.let { line -> "startAgain = ${quoted(line)}" },
            )
        val questTable =
            table(
                "[[quests]]",
                *listOfNotNull(
                    "id" to quoted(quest.id),
                    "type" to quoted(quest.type.name),
                    quest.questGiverNpcId?.let { npcId -> "questGiverNpcId" to quoted(npcId) },
                    ("repeatable" to "true").takeIf { quest.repeatable },
                    ("startTiles" to tiles(quest.startTiles)).takeIf { quest.startTiles.isNotEmpty() },
                    "name" to quoted(quest.name),
                    "description" to quoted(quest.description),
                    ("lines" to lines.joinToString(", ", prefix = "{ ", postfix = " }")).takeIf { lines.isNotEmpty() },
                ).toTypedArray(),
            )
        val taskTables =
            quest.tasks.map { task ->
                val target =
                    when (task.type) {
                        WorldBuilder.Dto.TaskTypeDto.DEFEAT -> listOf("unitId" to quoted(task.unitId.orEmpty()), "requiredDefeats" to "${task.requiredDefeats ?: 1}")
                        WorldBuilder.Dto.TaskTypeDto.TALK -> listOf("npcId" to quoted(task.npcId.orEmpty()))
                        WorldBuilder.Dto.TaskTypeDto.VISIT -> listOf("tiles" to tiles(task.tiles))
                    }
                val keys = listOf("type" to quoted(task.type.name)) + target + listOfNotNull("text" to quoted(task.text), task.line?.let { line -> "line" to quoted(line) })
                table("[[quests.tasks]]", *keys.toTypedArray())
            }
        val rewardKeys = listOfNotNull("gold" to "${quest.reward.gold}", ("lootableItemIds" to list(quest.reward.lootableItemIds)).takeIf { quest.reward.lootableItemIds.isNotEmpty() })
        return listOf(questTable) + taskTables + table("[quests.reward]", *rewardKeys.toTypedArray())
    }

    private fun tiles(tiles: List<WorldBuilder.Dto.TileDto>): String = tiles.joinToString(", ", prefix = "[", postfix = "]") { tile -> "[${tile.row}, ${tile.column}]" }

    private fun list(values: List<String>): String = values.joinToString(", ", prefix = "[", postfix = "]", transform = ::quoted)

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
        val quests: List<QuestDefinition> = emptyList(),
    )

    @Serializable
    data class QuestDefinition(
        val id: String,
        val type: String,
        val questGiverNpcId: String? = null,
        val startTiles: List<List<Int>> = emptyList(),
        val repeatable: Boolean = false,
        val name: String = "",
        val description: String = "",
        val lines: LinesDefinition = LinesDefinition(),
        val tasks: List<TaskDefinition> = emptyList(),
        val reward: RewardDefinition = RewardDefinition(),
    ) {
        fun toDto() =
            WorldBuilder.Dto.QuestDto(
                id = id,
                type = WorldBuilder.Dto.QuestTypeDto.valueOf(type),
                questGiverNpcId = questGiverNpcId,
                repeatable = repeatable,
                startTiles = startTiles.map(::tileOf),
                name = name,
                description = description,
                lines = WorldBuilder.Dto.LinesDto(start = lines.start, reminder = lines.reminder, completed = lines.completed, startAgain = lines.startAgain),
                tasks = tasks.map(TaskDefinition::toDto),
                reward = WorldBuilder.Dto.RewardDto(gold = reward.gold, lootableItemIds = reward.lootableItemIds),
            )
    }

    @Serializable
    data class LinesDefinition(
        val start: String? = null,
        val reminder: String? = null,
        val completed: String? = null,
        val startAgain: String? = null,
    )

    @Serializable
    data class TaskDefinition(
        val type: String,
        val unitId: String? = null,
        val requiredDefeats: Int? = null,
        val npcId: String? = null,
        val tiles: List<List<Int>> = emptyList(),
        val text: String = "",
        val line: String? = null,
    ) {
        fun toDto() =
            WorldBuilder.Dto.TaskDto(
                type = WorldBuilder.Dto.TaskTypeDto.valueOf(type),
                unitId = unitId,
                requiredDefeats = requiredDefeats,
                npcId = npcId,
                tiles = tiles.map(::tileOf),
                text = text,
                line = line,
            )
    }

    @Serializable
    data class RewardDefinition(
        val gold: Int = 0,
        val lootableItemIds: List<String> = emptyList(),
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

private fun tileOf(tile: List<Int>): WorldBuilder.Dto.TileDto {
    require(tile.size == 2) { "A tile is [row, column], but was $tile" }
    return WorldBuilder.Dto.TileDto(row = tile[0], column = tile[1])
}
