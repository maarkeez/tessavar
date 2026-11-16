package com.mkz.tessavar.quest.adapters.resources

import com.mkz.tessavar.quest.domain.Quest
import com.mkz.tessavar.quest.domain.Quest.Dto.LinesDto
import com.mkz.tessavar.quest.domain.Quest.Dto.PositionDto
import com.mkz.tessavar.quest.domain.Quest.Dto.ProgressDto
import com.mkz.tessavar.quest.domain.Quest.Dto.QuestTypeDto
import com.mkz.tessavar.quest.domain.Quest.Dto.RewardDto
import com.mkz.tessavar.quest.domain.Quest.Dto.TaskDto
import com.mkz.tessavar.quest.domain.Quest.Dto.TaskTextDto
import com.mkz.tessavar.quest.domain.Quest.Dto.TextsDto
import com.mkz.tessavar.quest.usecases.services.QuestLoader
import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.shared.adapters.toml.enumOf
import com.mkz.tessavar.shared.adapters.toml.fromResource
import com.mkz.tessavar.shared.adapters.toml.readTomlSection
import com.mkz.tessavar.shared.adapters.toml.required
import korlibs.io.file.VfsFile
import kotlinx.serialization.Serializable

class ResourcesQuestLoader(
    private val worldFile: VfsFile = ResourcesBundle.root[ResourcesBundle.CAMPAIGN_WORLD_FILE],
) : QuestLoader {
    private var quests = emptyList<Quest.Dto>()

    override suspend fun initResources() {
        quests =
            worldFile.readTomlSection<QuestsSection>().quests.map { definition ->
                fromResource(worldFile.path) { definition.toDto() }
            }
    }

    override fun loadQuests(): List<Quest.Dto> = quests

    @Serializable
    data class QuestsSection(
        val quests: List<QuestDefinition> = emptyList(),
    )

    @Serializable
    data class QuestDefinition(
        val id: String,
        val type: String,
        val questGiverNpcId: String? = null,
        val startTiles: List<List<Int>> = emptyList(),
        val repeatable: Boolean = false,
        val name: String,
        val description: String,
        val lines: LinesDefinition = LinesDefinition(),
        val tasks: List<TaskDefinition> = emptyList(),
        val reward: RewardDefinition,
    ) {
        fun toDto(): Quest.Dto =
            Quest.Dto(
                id = id,
                type = enumOf<QuestTypeDto>("type", type),
                questGiverNpcId = questGiverNpcId,
                startTiles = startTiles.map { tile -> positionOf("startTiles", tile) },
                repeatable = repeatable,
                tasks = tasks.map(TaskDefinition::toDto),
                reward = reward.toDto(),
                texts =
                    TextsDto(
                        name = name,
                        description = description,
                        tasks = tasks.map { task -> TaskTextDto(text = task.text, line = task.line) },
                        lines = lines.toDto(),
                    ),
                progress = ProgressDto.notStarted(id),
            )
    }

    @Serializable
    data class LinesDefinition(
        val start: String? = null,
        val reminder: String? = null,
        val completed: String? = null,
        val startAgain: String? = null,
    ) {
        fun toDto(): LinesDto = LinesDto(start = start, reminder = reminder, completed = completed, startAgain = startAgain)
    }

    @Serializable
    data class TaskDefinition(
        val type: String,
        val unitId: String? = null,
        val requiredDefeats: Int? = null,
        val npcId: String? = null,
        val tiles: List<List<Int>>? = null,
        val text: String,
        val line: String? = null,
    ) {
        fun toDto(): TaskDto =
            when (enumOf<TaskDto.TypeDto>("tasks.type", type)) {
                TaskDto.TypeDto.DEFEAT -> TaskDto.defeat(unitId = required("tasks.unitId", unitId), requiredDefeats = required("tasks.requiredDefeats", requiredDefeats))
                TaskDto.TypeDto.TALK -> TaskDto.talk(npcId = required("tasks.npcId", npcId))
                TaskDto.TypeDto.VISIT ->
                    TaskDto.visit(
                        tiles =
                            required("tasks.tiles", tiles).map { tile -> positionOf("tasks.tiles", tile) },
                    )
            }
    }

    @Serializable
    data class RewardDefinition(
        val gold: Int = 0,
        val lootableItemIds: List<String> = emptyList(),
    ) {
        fun toDto(): RewardDto = RewardDto(gold = gold, lootableItemIds = lootableItemIds)
    }

    private companion object {
        fun positionOf(
            field: String,
            tile: List<Int>,
        ): PositionDto {
            require(tile.size == 2) { "'$field' must hold lists of [row, column], but held $tile" }
            return PositionDto(row = tile[0], column = tile[1])
        }
    }
}
