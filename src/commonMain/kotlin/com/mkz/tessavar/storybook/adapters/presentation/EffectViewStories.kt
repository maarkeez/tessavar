package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.EffectView
import com.mkz.tessavar.screen.adapters.presentation.EffectsView
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ControlDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto
import korlibs.korge.view.View
import korlibs.math.geom.Size

object EffectViewStories : StoryBinding {
    private val SIZE = Size(20, 20)

    private const val EFFECT = "effect"
    private const val IS_NEW = "isNew"
    private const val REMAINING_DURATION = "remainingDuration"
    private const val STACKS = "stacks"
    private const val DURATION_LIMIT = 9
    private const val STACKS_LIMIT = 3

    private val applied =
        StoryDto(
            id = "screen-effect--applied",
            name = "Applied",
            description = "An effect the battle unit has, as a small icon in a chip, with its remaining duration in the bottom-right corner.",
            args = emptyMap(),
        )
    private val stacked =
        StoryDto(
            id = "screen-effect--stacked",
            name = "Stacked",
            description = "An effect the battle unit received twice: the count in the top-right corner, and the longest remaining duration of the two.",
            args = mapOf(STACKS to ValueDto.int(2)),
        )
    private val noDuration =
        StoryDto(
            id = "screen-effect--no-duration",
            name = "No duration",
            description = "An effect that lasts until it triggers, as venom burst does when the battle unit is defeated: no number.",
            args = mapOf(REMAINING_DURATION to ValueDto.int(0)),
        )
    private val new =
        StoryDto(
            id = "screen-effect--new",
            name = "New",
            description = "An effect an ability cast would apply: its chip is outlined in gold and blinks.",
            args = mapOf(IS_NEW to ValueDto.boolean(true)),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-effect",
            group = "Screen",
            title = "Effect",
            description =
                "The icon of an effect a battle unit has, at 1x in a rounded chip, in its info and in the previews, with its remaining duration and, when stacked, its count. " +
                    "An effect an ability would apply is outlined in gold and blinks. remainingDuration: 0 for none.",
            controls =
                listOf(
                    ControlDto.pick(EFFECT, context.effectIds()),
                    ControlDto.boolean(IS_NEW),
                    ControlDto.int(REMAINING_DURATION, minimum = 0, maximum = DURATION_LIMIT),
                    ControlDto.int(STACKS, minimum = 1, maximum = STACKS_LIMIT),
                ),
            args = mapOf(EFFECT to ValueDto.text(context.effectIds().first()), IS_NEW to ValueDto.boolean(false), REMAINING_DURATION to ValueDto.int(3), STACKS to ValueDto.int(1)),
            stories = listOf(applied, new, stacked, noDuration),
        )

    override fun render(
        args: Args,
        context: StoryContext,
    ): View =
        EffectView(SIZE, context.resourcesLoader).apply {
            displayEffect(
                EffectsView.Effect(
                    effectId = args.text(EFFECT),
                    stacks = args.int(STACKS),
                    remainingDuration = args.int(REMAINING_DURATION).takeIf { remainingDuration -> remainingDuration > 0 },
                    isNew = args.boolean(IS_NEW),
                ),
            )
        }
}
