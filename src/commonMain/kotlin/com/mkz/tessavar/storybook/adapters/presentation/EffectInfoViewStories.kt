package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.AbilityInfoView
import com.mkz.tessavar.screen.adapters.presentation.EffectInfoView
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ControlDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto
import korlibs.korge.view.View

object EffectInfoViewStories : StoryBinding {
    private const val STACKS_LIMIT = 3

    private const val EFFECT = "effect"
    private const val STACKS = "stacks"
    private const val EXPLORING = "exploring"
    private const val ENDING = "ending"

    private val venom =
        StoryDto(
            id = "screen-effect-info--venom",
            name = "Venom",
            description = "A venom in a battle: the damage it deals at the start of each turn, and the turns it has left.",
            args = emptyMap(),
        )
    private val venomStacked =
        StoryDto(
            id = "screen-effect-info--venom-stacked",
            name = "Venom stacked",
            description = "A venom received twice: its name with the count, and the turns each stack has left, longest first.",
            args = mapOf(STACKS to ValueDto.int(2)),
        )
    private val soak =
        StoryDto(
            id = "screen-effect-info--soak",
            name = "Soak",
            description = "A soak: the share of the damage of burn the battle unit no longer takes.",
            args = mapOf(EFFECT to ValueDto.text("soak")),
        )
    private val stun =
        StoryDto(
            id = "screen-effect-info--stun",
            name = "Stun",
            description = "A stun: the battle unit loses its actions at the start of each turn.",
            args = mapOf(EFFECT to ValueDto.text("stun")),
        )
    private val stunEnding =
        StoryDto(
            id = "screen-effect-info--stun-ending",
            name = "Stun ending",
            description = "A stun on the turn it took the actions away: its stack is at 0 and ends when the battle unit's turn finishes.",
            args = mapOf(EFFECT to ValueDto.text("stun"), ENDING to ValueDto.boolean(true)),
        )
    private val maximumHealth =
        StoryDto(
            id = "screen-effect-info--maximum-health",
            name = "Maximum health",
            description = "An equipped effect, the rune hammer's: the maximum health it gives while equipped, and no time left.",
            args = mapOf(EFFECT to ValueDto.text("maximum-health-30")),
        )
    private val venomBurst =
        StoryDto(
            id = "screen-effect-info--venom-burst",
            name = "Venom burst",
            description = "An effect that triggers when the battle unit is defeated: what it does then, and no time left.",
            args = mapOf(EFFECT to ValueDto.text("venom-on-defeat")),
        )
    private val exploring =
        StoryDto(
            id = "screen-effect-info--exploring",
            name = "Exploring",
            description = "A venom received twice while exploring: the damage it deals each second, and the seconds each stack has left.",
            args = mapOf(STACKS to ValueDto.int(2), EXPLORING to ValueDto.boolean(true)),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-effect-info",
            group = "Screen",
            title = "Effect info",
            description =
                "What an ongoing effect of the selected battle unit does and how long each of its stacks has left, shown in place of its abilities when the player taps its icon. " +
                    "Tapping it closes it. stacks: how many times the battle unit received it. exploring: seconds instead of turns. " +
                    "ending: the shortest stack at 0, as on the turn it triggered for the last time.",
            controls =
                listOf(
                    ControlDto.pick(EFFECT, context.effectIds()),
                    ControlDto.int(STACKS, minimum = 1, maximum = STACKS_LIMIT),
                    ControlDto.boolean(EXPLORING),
                    ControlDto.boolean(ENDING),
                ),
            args = mapOf(EFFECT to ValueDto.text("venom-damage"), STACKS to ValueDto.int(1), EXPLORING to ValueDto.boolean(false), ENDING to ValueDto.boolean(false)),
            stories = listOf(venom, venomStacked, soak, stun, stunEnding, venomBurst, maximumHealth, exploring),
        )

    override fun render(
        args: Args,
        context: StoryContext,
    ): View =
        EffectInfoView(AbilityInfoView.WIDTH, context.resourcesLoader, context.messages).apply {
            setOnTapped { context.action("onTapped") }
            display(context.effectInfo(effectId = args.text(EFFECT), stacks = args.int(STACKS), isExploring = args.boolean(EXPLORING)).ending(args.boolean(ENDING)))
        }

    private fun EffectInfoView.EffectInfo.ending(isEnding: Boolean): EffectInfoView.EffectInfo =
        if (isEnding && remainingDurations.isNotEmpty()) copy(remainingDurations = remainingDurations.dropLast(1) + 0) else this
}
