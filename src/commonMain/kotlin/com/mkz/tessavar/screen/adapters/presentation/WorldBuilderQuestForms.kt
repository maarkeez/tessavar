package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderFormView.Row
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.QuestDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.QuestTypeDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.TaskDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.TaskTypeDto

object WorldBuilderQuestForms {
    const val ADD_MAIN = "addMain"
    const val ADD_SIDE = "addSide"
    const val QUEST = "quest."
    const val TYPE = "type"
    const val NAME = "name"
    const val DESCRIPTION = "description"
    const val GIVER = "giver"
    const val REPEATABLE = "repeatable"
    const val START_TILES = "startTiles"
    const val TASK = "task."
    const val TASK_TYPE = ".type"
    const val TASK_UNIT = ".unit"
    const val TASK_DEFEATS = ".defeats"
    const val TASK_NPC = ".npc"
    const val TASK_TILES = ".tiles"
    const val TASK_TEXT = ".text"
    const val TASK_LINE = ".line"
    const val TASK_REMOVE = ".remove"
    const val ADD_TASK = "addTask"
    const val GOLD = "gold"
    const val ITEM = "item."
    const val LINE_START = "line.start"
    const val LINE_REMINDER = "line.reminder"
    const val LINE_COMPLETED = "line.completed"
    const val LINE_START_AGAIN = "line.startAgain"
    const val REMOVE = "remove"

    fun taskKey(
        index: Int,
        field: String,
    ): String = "$TASK$index$field"

    fun questsRows(
        quests: List<QuestDto>,
        messages: Messages,
    ): List<Row> =
        quests.map { quest -> Row.Action(key = QUEST + quest.id, label = messages[UiMessage.WORLD_BUILDER_QUESTS_ENTRY, "type" to typeName(quest.type, messages), "name" to title(quest)]) } +
            listOfNotNull(
                Row.Action(key = ADD_MAIN, label = messages[UiMessage.WORLD_BUILDER_QUESTS_ADD_MAIN]).takeIf { quests.none { quest -> quest.type == QuestTypeDto.MAIN } },
                Row.Action(key = ADD_SIDE, label = messages[UiMessage.WORLD_BUILDER_QUESTS_ADD_SIDE]),
            )

    fun title(quest: QuestDto): String = quest.name.ifBlank { quest.id }

    fun questRows(
        quest: QuestDto,
        lootableItemIds: List<String>,
        pickingKey: String?,
        messages: Messages,
    ): List<Row> {
        val yes = messages[UiMessage.WORLD_BUILDER_QUEST_YES]
        val no = messages[UiMessage.WORLD_BUILDER_QUEST_NO]
        val header =
            listOf(
                Row.Choice(TYPE, messages[UiMessage.WORLD_BUILDER_QUEST_TYPE], typeName(quest.type, messages)),
                Row.Text(NAME, messages[UiMessage.WORLD_BUILDER_QUEST_NAME], quest.name),
                Row.Text(DESCRIPTION, messages[UiMessage.WORLD_BUILDER_QUEST_DESCRIPTION], quest.description),
            ) +
                when (quest.type) {
                    QuestTypeDto.MAIN -> listOf(tilesRow(START_TILES, messages[UiMessage.WORLD_BUILDER_QUEST_START_TILES], quest.startTiles.size, pickingKey, messages))
                    QuestTypeDto.SIDE ->
                        listOf(
                            Row.Choice(GIVER, messages[UiMessage.WORLD_BUILDER_QUEST_GIVER], quest.questGiverNpcId?.let(messages::npcName) ?: messages[UiMessage.WORLD_BUILDER_QUEST_NOBODY]),
                            Row.Toggle(REPEATABLE, messages[UiMessage.WORLD_BUILDER_QUEST_REPEATABLE], if (quest.repeatable) yes else no, quest.repeatable),
                        )
                }
        val tasks =
            listOf(Row.Heading(messages[UiMessage.WORLD_BUILDER_QUEST_TASKS])) +
                quest.tasks.flatMapIndexed { index, task -> taskRows(index, task, pickingKey, messages) } +
                Row.Action(ADD_TASK, messages[UiMessage.WORLD_BUILDER_QUEST_ADD_TASK])
        val reward =
            listOf(
                Row.Heading(messages[UiMessage.WORLD_BUILDER_QUEST_REWARD]),
                Row.Stepper(GOLD, messages[UiMessage.WORLD_BUILDER_QUEST_GOLD], quest.reward.gold),
            ) +
                lootableItemIds.map { lootableItemId ->
                    val isRewarded = lootableItemId in quest.reward.lootableItemIds
                    Row.Toggle(ITEM + lootableItemId, messages.lootableItemName(lootableItemId), if (isRewarded) yes else no, isRewarded)
                }
        val lines =
            if (quest.type == QuestTypeDto.SIDE) {
                listOf(
                    Row.Heading(messages[UiMessage.WORLD_BUILDER_QUEST_LINES]),
                    Row.Text(LINE_START, messages[UiMessage.WORLD_BUILDER_QUEST_LINE_START], quest.lines.start.orEmpty()),
                    Row.Text(LINE_REMINDER, messages[UiMessage.WORLD_BUILDER_QUEST_LINE_REMINDER], quest.lines.reminder.orEmpty()),
                    Row.Text(LINE_COMPLETED, messages[UiMessage.WORLD_BUILDER_QUEST_LINE_COMPLETED], quest.lines.completed.orEmpty()),
                    Row.Text(LINE_START_AGAIN, messages[UiMessage.WORLD_BUILDER_QUEST_LINE_START_AGAIN], quest.lines.startAgain.orEmpty()),
                )
            } else {
                emptyList()
            }
        return header + tasks + reward + lines + Row.Action(REMOVE, messages[UiMessage.WORLD_BUILDER_QUEST_REMOVE])
    }

    private fun taskRows(
        index: Int,
        task: TaskDto,
        pickingKey: String?,
        messages: Messages,
    ): List<Row> {
        val target =
            when (task.type) {
                TaskTypeDto.DEFEAT ->
                    listOf(
                        Row.Choice(taskKey(index, TASK_UNIT), messages[UiMessage.WORLD_BUILDER_QUEST_UNIT], task.unitId?.let(messages::unitName).orEmpty()),
                        Row.Stepper(taskKey(index, TASK_DEFEATS), messages[UiMessage.WORLD_BUILDER_QUEST_DEFEATS], task.requiredDefeats ?: 1),
                    )
                TaskTypeDto.TALK -> listOf(Row.Choice(taskKey(index, TASK_NPC), messages[UiMessage.WORLD_BUILDER_QUEST_NPC], task.npcId?.let(messages::npcName).orEmpty()))
                TaskTypeDto.VISIT -> listOf(tilesRow(taskKey(index, TASK_TILES), messages[UiMessage.WORLD_BUILDER_QUEST_VISIT_TILES], task.tiles.size, pickingKey, messages))
            }
        val number = index + 1
        return listOf(
            Row.Heading(messages[UiMessage.WORLD_BUILDER_QUEST_TASK, "number" to number]),
            Row.Choice(taskKey(index, TASK_TYPE), messages[UiMessage.WORLD_BUILDER_QUEST_KIND], taskTypeName(task.type, messages)),
        ) + target +
            listOf(
                Row.Text(taskKey(index, TASK_TEXT), messages[UiMessage.WORLD_BUILDER_QUEST_TASK_TEXT], task.text),
                Row.Text(taskKey(index, TASK_LINE), messages[UiMessage.WORLD_BUILDER_QUEST_TASK_LINE], task.line.orEmpty()),
                Row.Action(taskKey(index, TASK_REMOVE), messages[UiMessage.WORLD_BUILDER_QUEST_REMOVE_TASK, "number" to number]),
            )
    }

    private fun tilesRow(
        key: String,
        label: String,
        count: Int,
        pickingKey: String?,
        messages: Messages,
    ): Row {
        val isPicking = key == pickingKey
        val value = if (isPicking) messages[UiMessage.WORLD_BUILDER_QUEST_PICKING] else messages[UiMessage.WORLD_BUILDER_QUEST_TILES, "count" to count]
        return Row.Toggle(key, label, value, isPicking)
    }

    fun typeName(
        type: QuestTypeDto,
        messages: Messages,
    ): String =
        when (type) {
            QuestTypeDto.MAIN -> messages[UiMessage.WORLD_BUILDER_QUEST_MAIN]
            QuestTypeDto.SIDE -> messages[UiMessage.WORLD_BUILDER_QUEST_SIDE]
        }

    private fun taskTypeName(
        type: TaskTypeDto,
        messages: Messages,
    ): String =
        when (type) {
            TaskTypeDto.DEFEAT -> messages[UiMessage.WORLD_BUILDER_QUEST_DEFEAT]
            TaskTypeDto.TALK -> messages[UiMessage.WORLD_BUILDER_QUEST_TALK]
            TaskTypeDto.VISIT -> messages[UiMessage.WORLD_BUILDER_QUEST_VISIT]
        }
}
