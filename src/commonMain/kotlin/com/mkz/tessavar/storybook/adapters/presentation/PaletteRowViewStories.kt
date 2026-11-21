package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.PaletteRowView
import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderHudView
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ControlDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto
import korlibs.korge.view.View

object PaletteRowViewStories : StoryBinding {
    private const val SCROLLED_TO = "scrolledTo"
    private const val LAST_SLOT = 9

    private val default =
        StoryDto(
            id = "screen-palette-row--default",
            name = "Default",
            description = "Ten units from the first, the fifth cut by the edge: dragging the row sideways scrolls it.",
            args = emptyMap(),
        )

    private val scrolled =
        StoryDto(
            id = "screen-palette-row--scrolled",
            name = "Scrolled",
            description = "Scrolled to the last unit, which stops at the row's right edge.",
            args = mapOf(SCROLLED_TO to ValueDto.int(LAST_SLOT)),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-palette-row",
            group = "Screen",
            title = "Palette row",
            description =
                "A row of the world builder's palette card, scrolling sideways when it holds more slots than fit: " +
                    "a press moving more than 10 px drags it, a shorter one taps the slot. " +
                    "scrolledTo: the slot brought into view.",
            controls = listOf(ControlDto.int(SCROLLED_TO, minimum = 0, maximum = LAST_SLOT)),
            args = mapOf(SCROLLED_TO to ValueDto.int(0)),
            stories = listOf(default, scrolled),
        )

    override fun render(
        args: Args,
        context: StoryContext,
    ): View =
        PaletteRowView(rowWidth = WorldBuilderHudView.ROW_WIDTH).apply {
            val slots =
                context.unitIds().take(LAST_SLOT + 1).mapNotNull { unitId ->
                    context.resourcesLoader.bitmapOrNull("unit/$unitId.png")?.let { bitmap -> PaletteRowView.Slot(key = unitId, bitmap = bitmap, imageName = WorldBuilderHudView.OCCUPANT_IMAGE) }
                }
            display(slots) { unitId -> context.action("slotTapped", unitId) }
            scrollTo(args.int(SCROLLED_TO))
        }
}
