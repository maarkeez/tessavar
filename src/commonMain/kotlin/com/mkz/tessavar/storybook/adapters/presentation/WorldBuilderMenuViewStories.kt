package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.UiMessage
import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderMenuView
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ControlDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto
import korlibs.korge.view.FixedSizeContainer
import korlibs.korge.view.View
import korlibs.korge.view.addUpdater
import korlibs.math.geom.Size

object WorldBuilderMenuViewStories : StoryBinding {
    private const val OPTIONS_LIMIT = 10
    private val BUTTON_SIZE = Size(WorldBuilderMenuView.BUTTON_WIDTH, WorldBuilderMenuView.BUTTON_HEIGHT)

    private val WORLD_BUILDER_OPTIONS =
        listOf(
            UiMessage.WORLD_BUILDER_RESIZE,
            UiMessage.WORLD_BUILDER_DETAILS,
            UiMessage.WORLD_BUILDER_QUESTS,
            UiMessage.WORLD_BUILDER_PLAYTEST,
            UiMessage.WORLD_BUILDER_IMPORT,
            UiMessage.WORLD_BUILDER_EXPORT,
            UiMessage.WORLD_BUILDER_HOW_TO_SHARE,
            UiMessage.WORLD_BUILDER_BACK,
        )

    private const val OPEN = "open"
    private const val OPTIONS = "options"
    private const val CAN_UNDO = "canUndo"
    private const val CAN_REDO = "canRedo"

    private val closed = StoryDto(id = "screen-world-builder-menu--closed", name = "Closed", description = "The hamburger button: tapping it opens the options.", args = emptyMap())
    private val open =
        StoryDto(
            id = "screen-world-builder-menu--open",
            name = "Open",
            description =
                "Below the button, Undo and Redo side by side as icons, " +
                    "then the options as the world builder lists them: Resize, Details, Quests, Playtest, Import, Export, How to share and Back.",
            args =
                mapOf(
                    OPEN to ValueDto.boolean(true),
                ),
        )
    private val severalOptions =
        StoryDto(
            id = "screen-world-builder-menu--several-options",
            name = "Several options",
            description = "More options than the world builder has, one under the other.",
            args = mapOf(OPEN to ValueDto.boolean(true), OPTIONS to ValueDto.int(OPTIONS_LIMIT)),
        )
    private val nothingToUndo =
        StoryDto(
            id = "screen-world-builder-menu--nothing-to-undo",
            name = "Nothing to undo",
            description = "A new world: Undo and Redo are disabled, their icons dimmed, until the world is edited.",
            args = mapOf(OPEN to ValueDto.boolean(true), CAN_UNDO to ValueDto.boolean(false), CAN_REDO to ValueDto.boolean(false)),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-world-builder-menu",
            group = "Screen",
            title = "World builder menu",
            description =
                "At the top right of the world builder: a hamburger button which opens, over the screen, Undo and Redo then a list of options. Undo and Redo keep it open. " +
                    "open: whether it is open. options: how many options. canUndo and canRedo: whether there is an edit to undo or to redo.",
            controls = listOf(ControlDto.boolean(OPEN), ControlDto.int(OPTIONS, minimum = 1, maximum = OPTIONS_LIMIT), ControlDto.boolean(CAN_UNDO), ControlDto.boolean(CAN_REDO)),
            args = mapOf(OPEN to ValueDto.boolean(false), OPTIONS to ValueDto.int(WORLD_BUILDER_OPTIONS.size), CAN_UNDO to ValueDto.boolean(true), CAN_REDO to ValueDto.boolean(true)),
            stories = listOf(closed, open, severalOptions, nothingToUndo),
        )

    override fun render(
        args: Args,
        context: StoryContext,
    ): View {
        val worldBuilderLabels = WORLD_BUILDER_OPTIONS.map { message -> context.messages[message] }
        val labels = (1..args.int(OPTIONS)).map { number -> worldBuilderLabels.getOrElse(number - 1) { "Option $number" } }
        val menuView =
            WorldBuilderMenuView().apply {
                displayHistoryIcons(WorldBuilderMenuView.historyIcons(context.resourcesLoader))
                displayHistory(canUndo = args.boolean(CAN_UNDO), canRedo = args.boolean(CAN_REDO))
                setDelegate(
                    object : WorldBuilderMenuView.Delegate {
                        override fun undoSelected() = context.action("undoSelected")

                        override fun redoSelected() = context.action("redoSelected")
                    },
                )
                displayOptions(
                    labels.map { label ->
                        WorldBuilderMenuView.Option(label = label) { context.action("onSelected", label, setArg = OPEN to ValueDto.boolean(false)) }
                    },
                )
                if (args.boolean(OPEN)) {
                    var opened = false
                    addUpdater {
                        if (!opened) {
                            opened = true
                            open()
                        }
                    }
                }
            }
        return FixedSizeContainer(BUTTON_SIZE).apply { addChild(menuView) }
    }
}
