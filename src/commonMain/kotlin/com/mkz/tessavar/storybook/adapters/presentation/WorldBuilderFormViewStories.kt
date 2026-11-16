package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.Messages
import com.mkz.tessavar.screen.adapters.presentation.UiMessage
import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderFormView
import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderFormView.Row
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ControlDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto
import korlibs.korge.view.View

object WorldBuilderFormViewStories : StoryBinding {
    private const val FORM = "form"
    private const val QUESTS = "QUESTS"
    private const val MAIN_QUEST = "MAIN_QUEST"
    private const val SIDE_QUEST = "SIDE_QUEST"
    private const val PICKING = "picking"

    private val campaignQuests =
        StoryDto(
            id = "screen-world-builder-form--campaign-quests",
            name = "Campaign quests",
            description = "Quests, from the world builder's menu: each quest of the world, a tap opens it, and Add a side quest; Add the main quest only while there is none.",
            args = emptyMap(),
        )
    private val mainQuestStory =
        StoryDto(
            id = "screen-world-builder-form--main-quest",
            name = "Main quest",
            description = "A main quest being edited: its start tiles, tasks and reward, scrolled by dragging.",
            args = mapOf(FORM to ValueDto.text(MAIN_QUEST)),
        )
    private val sideQuestStory =
        StoryDto(
            id = "screen-world-builder-form--side-quest",
            name = "Side quest",
            description = "A side quest being edited: its quest giver, whether it is repeatable, and the giver's lines at the end.",
            args = mapOf(FORM to ValueDto.text(SIDE_QUEST)),
        )
    private val pickingTiles =
        StoryDto(
            id = "screen-world-builder-form--picking-tiles",
            name = "Picking tiles",
            description = "The start tiles being picked: each tile tapped on the world is picked or unpicked, until the row is tapped again.",
            args = mapOf(FORM to ValueDto.text(MAIN_QUEST), PICKING to ValueDto.boolean(true)),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-world-builder-form",
            group = "Screen",
            title = "World builder form",
            description =
                "Over the world builder's HUD: the quests of the world, or the quest being edited, as rows of labels and fields scrolled by dragging, and Done. " +
                    "form: which form; picking: whether the start tiles are being picked.",
            controls = listOf(ControlDto.enum(FORM, listOf(QUESTS, MAIN_QUEST, SIDE_QUEST)), ControlDto.boolean(PICKING)),
            args = mapOf(FORM to ValueDto.text(QUESTS), PICKING to ValueDto.boolean(false)),
            stories = listOf(campaignQuests, mainQuestStory, sideQuestStory, pickingTiles),
        )

    override fun render(
        args: Args,
        context: StoryContext,
    ): View =
        WorldBuilderFormView(context.messages).apply {
            setDelegate(
                object : WorldBuilderFormView.Delegate {
                    override fun formClosed() = context.action("formClosed")

                    override fun textTapped(key: String) = context.action("textTapped", key)

                    override fun textChanged(
                        key: String,
                        text: String,
                    ) = context.action("textChanged", key, text)

                    override fun choiceStepped(
                        key: String,
                        step: Int,
                    ) = context.action("choiceStepped", key, "$step")

                    override fun toggled(key: String) = context.action("toggled", key)

                    override fun stepped(
                        key: String,
                        step: Int,
                    ) = context.action("stepped", key, "$step")

                    override fun actionSelected(key: String) = context.action("actionSelected", key)
                },
            )
            val messages = context.messages
            when (args.text(FORM)) {
                MAIN_QUEST -> open(FORM, "The Goat Wizard's Lair", mainQuestRows(messages, args.boolean(PICKING)) + rewardRows(messages, context.lootableItemIds(), 150))
                SIDE_QUEST -> open(FORM, "Rats in the Fields", sideQuestRows(messages) + rewardRows(messages, context.lootableItemIds(), 20) + lineRows(messages))
                else -> open(FORM, messages[UiMessage.WORLD_BUILDER_QUESTS_TITLE], questsRows(messages))
            }
        }

    private fun questsRows(messages: Messages): List<Row> =
        listOf(
            Row.Action("quest.main", messages[UiMessage.WORLD_BUILDER_QUESTS_ENTRY, "type" to messages[UiMessage.WORLD_BUILDER_QUEST_MAIN], "name" to "The Goat Wizard's Lair"]),
            Row.Action("quest.side", messages[UiMessage.WORLD_BUILDER_QUESTS_ENTRY, "type" to messages[UiMessage.WORLD_BUILDER_QUEST_SIDE], "name" to "Rats in the Fields"]),
            Row.Action("addSide", messages[UiMessage.WORLD_BUILDER_QUESTS_ADD_SIDE]),
        )

    private fun mainQuestRows(
        messages: Messages,
        isPicking: Boolean,
    ): List<Row> {
        val tiles = if (isPicking) messages[UiMessage.WORLD_BUILDER_QUEST_PICKING] else messages[UiMessage.WORLD_BUILDER_QUEST_TILES, "count" to 3]
        return listOf(
            Row.Choice("type", messages[UiMessage.WORLD_BUILDER_QUEST_TYPE], messages[UiMessage.WORLD_BUILDER_QUEST_MAIN]),
            Row.Text("name", messages[UiMessage.WORLD_BUILDER_QUEST_NAME], "The Goat Wizard's Lair"),
            Row.Text("description", messages[UiMessage.WORLD_BUILDER_QUEST_DESCRIPTION], "A goat swallowed a wizard's magic and now rules the lair down south."),
            Row.Toggle("startTiles", messages[UiMessage.WORLD_BUILDER_QUEST_START_TILES], tiles, isPicking),
            Row.Heading(messages[UiMessage.WORLD_BUILDER_QUEST_TASKS]),
        ) + talkTaskRows(messages) + defeatTaskRows(messages, number = 2, unitId = "goat-wizard", defeats = 1, text = "Defeat the Goat Wizard") +
            Row.Action("addTask", messages[UiMessage.WORLD_BUILDER_QUEST_ADD_TASK])
    }

    private fun sideQuestRows(messages: Messages): List<Row> =
        listOf(
            Row.Choice("type", messages[UiMessage.WORLD_BUILDER_QUEST_TYPE], messages[UiMessage.WORLD_BUILDER_QUEST_SIDE]),
            Row.Text("name", messages[UiMessage.WORLD_BUILDER_QUEST_NAME], "Rats in the Fields"),
            Row.Text("description", messages[UiMessage.WORLD_BUILDER_QUEST_DESCRIPTION], "Rats keep coming back to the southern fields."),
            Row.Choice("giver", messages[UiMessage.WORLD_BUILDER_QUEST_GIVER], messages.npcName("elf")),
            Row.Toggle("repeatable", messages[UiMessage.WORLD_BUILDER_QUEST_REPEATABLE], messages[UiMessage.WORLD_BUILDER_QUEST_YES], isOn = true),
            Row.Heading(messages[UiMessage.WORLD_BUILDER_QUEST_TASKS]),
        ) + defeatTaskRows(messages, number = 1, unitId = "rat", defeats = 2, text = "Defeat 2 Rats") +
            Row.Action("addTask", messages[UiMessage.WORLD_BUILDER_QUEST_ADD_TASK])

    private fun talkTaskRows(messages: Messages): List<Row> =
        listOf(
            Row.Heading(messages[UiMessage.WORLD_BUILDER_QUEST_TASK, "number" to 1]),
            Row.Choice("task.0.type", messages[UiMessage.WORLD_BUILDER_QUEST_KIND], messages[UiMessage.WORLD_BUILDER_QUEST_TALK]),
            Row.Choice("task.0.npc", messages[UiMessage.WORLD_BUILDER_QUEST_NPC], messages.npcName("elder")),
            Row.Text("task.0.text", messages[UiMessage.WORLD_BUILDER_QUEST_TASK_TEXT], "Talk to the Elder"),
            Row.Text("task.0.line", messages[UiMessage.WORLD_BUILDER_QUEST_TASK_LINE], "Long ago a goat chewed up a wizard's spellbook."),
            Row.Action("task.0.remove", messages[UiMessage.WORLD_BUILDER_QUEST_REMOVE_TASK, "number" to 1]),
        )

    private fun defeatTaskRows(
        messages: Messages,
        number: Int,
        unitId: String,
        defeats: Int,
        text: String,
    ): List<Row> {
        val index = number - 1
        return listOf(
            Row.Heading(messages[UiMessage.WORLD_BUILDER_QUEST_TASK, "number" to number]),
            Row.Choice("task.$index.type", messages[UiMessage.WORLD_BUILDER_QUEST_KIND], messages[UiMessage.WORLD_BUILDER_QUEST_DEFEAT]),
            Row.Choice("task.$index.unit", messages[UiMessage.WORLD_BUILDER_QUEST_UNIT], messages.unitName(unitId)),
            Row.Stepper("task.$index.defeats", messages[UiMessage.WORLD_BUILDER_QUEST_DEFEATS], defeats),
            Row.Text("task.$index.text", messages[UiMessage.WORLD_BUILDER_QUEST_TASK_TEXT], text),
            Row.Text("task.$index.line", messages[UiMessage.WORLD_BUILDER_QUEST_TASK_LINE], ""),
            Row.Action("task.$index.remove", messages[UiMessage.WORLD_BUILDER_QUEST_REMOVE_TASK, "number" to number]),
        )
    }

    private fun rewardRows(
        messages: Messages,
        lootableItemIds: List<String>,
        gold: Int,
    ): List<Row> =
        listOf(Row.Heading(messages[UiMessage.WORLD_BUILDER_QUEST_REWARD]), Row.Stepper("gold", messages[UiMessage.WORLD_BUILDER_QUEST_GOLD], gold)) +
            lootableItemIds.map { lootableItemId ->
                val isRewarded = lootableItemId == "health-potion"
                val value = if (isRewarded) messages[UiMessage.WORLD_BUILDER_QUEST_YES] else messages[UiMessage.WORLD_BUILDER_QUEST_NO]
                Row.Toggle("item.$lootableItemId", messages.lootableItemName(lootableItemId), value, isRewarded)
            }

    private fun lineRows(messages: Messages): List<Row> =
        listOf(
            Row.Heading(messages[UiMessage.WORLD_BUILDER_QUEST_LINES]),
            Row.Text("line.start", messages[UiMessage.WORLD_BUILDER_QUEST_LINE_START], "Thin them out in the southern fields, and I pay in gold."),
            Row.Text("line.reminder", messages[UiMessage.WORLD_BUILDER_QUEST_LINE_REMINDER], "Two rats, travellers."),
            Row.Text("line.completed", messages[UiMessage.WORLD_BUILDER_QUEST_LINE_COMPLETED], ""),
            Row.Text("line.startAgain", messages[UiMessage.WORLD_BUILDER_QUEST_LINE_START_AGAIN], "The rats are back."),
            Row.Action("remove", messages[UiMessage.WORLD_BUILDER_QUEST_REMOVE]),
        )
}
