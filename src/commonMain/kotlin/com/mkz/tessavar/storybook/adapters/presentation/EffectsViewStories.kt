package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.EffectsView
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ControlDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto
import korlibs.korge.view.View

object EffectsViewStories : StoryBinding {
    private const val EFFECTS_LIMIT = 15
    private const val STACKS_LIMIT = 3

    private const val EFFECTS = "effects"
    private const val NEW_EFFECTS = "newEffects"
    private const val STACKS = "stacks"
    private const val NEW_STACK = "newStack"

    private val some =
        StoryDto(
            id = "screen-effects--some",
            name = "Some",
            description = "A few effects of a battle unit, one icon each with its remaining duration: burn, soak, stun, a longer stun, venom, and venom burst, which has none.",
            args = emptyMap(),
        )
    private val none = StoryDto(id = "screen-effects--none", name = "None", description = "A battle unit with no effect: nothing is drawn.", args = mapOf(EFFECTS to ValueDto.int(0)))
    private val withNewEffects =
        StoryDto(
            id = "screen-effects--with-new-effects",
            name = "With new effects",
            description = "The effects an ability cast would apply, blinking after the ones the battle unit has.",
            args = mapOf(NEW_EFFECTS to ValueDto.int(2)),
        )
    private val overflow =
        StoryDto(
            id = "screen-effects--overflow",
            name = "Overflow",
            description = "More effects than the ten icons: the first ten, followed by a plus sign.",
            args = mapOf(EFFECTS to ValueDto.int(12)),
        )
    private val stacked =
        StoryDto(
            id = "screen-effects--stacked",
            name = "Stacked",
            description = "An effect received twice, one icon with its count and the longest remaining duration of the two, next to another effect.",
            args = mapOf(EFFECTS to ValueDto.int(2), STACKS to ValueDto.int(2)),
        )
    private val withNewStack =
        StoryDto(
            id = "screen-effects--with-new-stack",
            name = "With new stack",
            description = "An ability cast would apply an effect the battle unit already has: its icon blinks with the new count.",
            args = mapOf(EFFECTS to ValueDto.int(2), STACKS to ValueDto.int(2), NEW_STACK to ValueDto.boolean(true)),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-effects",
            group = "Screen",
            title = "Effects",
            description =
                "The row of effect icons of a battle unit, in its info and in the previews, one per effect, followed by the effects an ability cast would apply. " +
                    "stacks: how many times the first effect was received. newStack: the cast would add one more.",
            controls =
                listOf(
                    ControlDto.int(EFFECTS, minimum = 0, maximum = EFFECTS_LIMIT),
                    ControlDto.int(NEW_EFFECTS, minimum = 0, maximum = EFFECTS_LIMIT),
                    ControlDto.int(STACKS, minimum = 1, maximum = STACKS_LIMIT),
                    ControlDto.boolean(NEW_STACK),
                ),
            args = mapOf(EFFECTS to ValueDto.int(6), NEW_EFFECTS to ValueDto.int(0), STACKS to ValueDto.int(1), NEW_STACK to ValueDto.boolean(false)),
            stories = listOf(some, none, withNewEffects, overflow, stacked, withNewStack),
        )

    override fun render(
        args: Args,
        context: StoryContext,
    ): View {
        val effects =
            context.effects(args.int(EFFECTS), args.int(NEW_EFFECTS)).mapIndexed { index, effect ->
                if (index == 0) effect.copy(stacks = args.int(STACKS), isNew = effect.isNew || args.boolean(NEW_STACK)) else effect
            }
        return EffectsView(context.resourcesLoader).apply { display(effects) }
    }
}
