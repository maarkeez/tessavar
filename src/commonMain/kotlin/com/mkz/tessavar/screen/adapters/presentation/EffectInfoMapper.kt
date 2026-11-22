package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.effect.domain.Effect
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto.TimingTypeDto
import com.mkz.tessavar.screen.adapters.presentation.EffectInfoView.EffectInfo

internal object EffectInfoMapper {
    fun effectInfo(
        effect: Effect.Dto,
        remainingDurations: List<Int?>,
        isExploring: Boolean,
        searchEffect: (String) -> Effect.Dto?,
    ): EffectInfo =
        EffectInfo(
            effectId = effect.id,
            stacks = remainingDurations.size,
            sentence = sentence(effect, searchEffect),
            remainingDurations = remainingDurations.filterNotNull().sortedDescending(),
            isExploring = isExploring,
        )

    private fun sentence(
        effect: Effect.Dto,
        searchEffect: (String) -> Effect.Dto?,
    ): EffectInfo.Sentence {
        val outcome = effect.outcome
        val timing = effect.timing

        fun noSentence(): Nothing = error("The effect '${effect.id}' (${outcome.type}, ${timing.type}) has no sentence in EffectInfoMapper")
        return when {
            outcome.type == EffectOutcomeDto.TypeDto.DAMAGE && timing.type == TimingTypeDto.ON_PLAYER_TURN_STARTED ->
                EffectInfo.Sentence.DamageOnPlayerTurnStarted(damage = outcome.damage!!.damage)
            outcome.type == EffectOutcomeDto.TypeDto.NEGATE_ACTIONS && timing.type == TimingTypeDto.ON_PLAYER_TURN_STARTED -> EffectInfo.Sentence.NegateActions
            outcome.type == EffectOutcomeDto.TypeDto.RESIST && timing.type == TimingTypeDto.WHILE_ONGOING ->
                EffectInfo.Sentence.Resist(resistedEffectId = outcome.resist!!.effectId, share = outcome.resist.share)
            outcome.type == EffectOutcomeDto.TypeDto.APPLY_EFFECT_TO_ADJACENT_ALLIES && timing.type == TimingTypeDto.ON_DEFEATED -> {
                val nestedEffectId = outcome.applyEffectToAdjacentAllies!!.effectId
                val nestedEffect = searchEffect(nestedEffectId) ?: error("The effect '${effect.id}' applies the effect '$nestedEffectId', which does not exist")
                val isDamageOnPlayerTurnStarted = nestedEffect.outcome.type == EffectOutcomeDto.TypeDto.DAMAGE && nestedEffect.timing.type == TimingTypeDto.ON_PLAYER_TURN_STARTED
                if (!isDamageOnPlayerTurnStarted) noSentence()
                EffectInfo.Sentence.OnDefeatedDamageAdjacentAllies(damage = nestedEffect.outcome.damage!!.damage, turns = nestedEffect.timing.onPlayerTurnStarted!!.duration)
            }
            else -> noSentence()
        }
    }
}
