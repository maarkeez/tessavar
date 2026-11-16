package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderResizeView
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ControlDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto
import korlibs.korge.view.View

object WorldBuilderResizeViewStories : StoryBinding {
    private const val MINIMUM_SIZE = 1
    private const val MAXIMUM_SIZE = 128

    private const val ROWS = "rows"
    private const val COLUMNS = "columns"

    private val default = StoryDto(id = "screen-world-builder-resize--default", name = "Default", description = "A new world: 16 x 16 tiles.", args = emptyMap())
    private val smallestWorld =
        StoryDto(
            id = "screen-world-builder-resize--smallest-world",
            name = "Smallest world",
            description = "The steps that would make the world smaller are disabled.",
            args = mapOf(ROWS to ValueDto.int(MINIMUM_SIZE), COLUMNS to ValueDto.int(MINIMUM_SIZE)),
        )
    private val largestWorld =
        StoryDto(
            id = "screen-world-builder-resize--largest-world",
            name = "Largest world",
            description = "The steps that would make the world larger are disabled.",
            args = mapOf(ROWS to ValueDto.int(MAXIMUM_SIZE), COLUMNS to ValueDto.int(MAXIMUM_SIZE)),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-world-builder-resize",
            group = "Screen",
            title = "World builder resize",
            description = "Opened from Resize in the world builder's menu, over its HUD: the rows and columns of the world, with steps to change them, and Done to close it.",
            controls =
                listOf(
                    ControlDto.int(ROWS, minimum = MINIMUM_SIZE, maximum = MAXIMUM_SIZE),
                    ControlDto.int(COLUMNS, minimum = MINIMUM_SIZE, maximum = MAXIMUM_SIZE),
                ),
            args = mapOf(ROWS to ValueDto.int(16), COLUMNS to ValueDto.int(16)),
            stories = listOf(default, smallestWorld, largestWorld),
        )

    override fun render(
        args: Args,
        context: StoryContext,
    ): View =
        WorldBuilderResizeView(context.messages).apply {
            setDelegate(
                object : WorldBuilderResizeView.Delegate {
                    override fun sizeChanged(
                        rows: Int,
                        columns: Int,
                    ) = context.action("sizeChanged", rows, columns, setArg = if (rows != args.int(ROWS)) ROWS to ValueDto.int(rows) else COLUMNS to ValueDto.int(columns))
                },
            )
            displaySize(rows = args.int(ROWS), columns = args.int(COLUMNS), allowedSizes = MINIMUM_SIZE..MAXIMUM_SIZE)
            open()
        }
}
