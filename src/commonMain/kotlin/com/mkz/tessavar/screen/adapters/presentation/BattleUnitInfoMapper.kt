package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.battleUnit.domain.BattleUnit
import com.mkz.tessavar.battleUnit.usecases.queries.SearchAbilityAvailabilities.AbilityAvailability
import com.mkz.tessavar.effect.domain.Effect

internal object BattleUnitInfoMapper {
    fun battleUnitInfo(
        battleUnit: BattleUnit.Dto,
        isHumanPlayers: Boolean,
        searchEffect: (String) -> Effect.Dto?,
        isExploring: Boolean = false,
        newEffectIds: List<String> = emptyList(),
    ): BattleUnitInfo =
        BattleUnitInfo(
            unitId = battleUnit.unitId,
            remainingHealth = battleUnit.remainingHealth,
            maximumHealth = battleUnit.maximumHealth,
            remainingMana = battleUnit.remainingMana,
            maximumMana = battleUnit.maximumMana,
            effects = effects(battleUnit.ongoingEffects.effects, newEffectIds, searchEffect),
            isHumanPlayers = isHumanPlayers,
            battleUnitId = battleUnit.id,
            effectInfos = effectInfos(battleUnit.ongoingEffects.effects, isExploring, searchEffect) + equippedEffectInfos(battleUnit.equippedEffects, searchEffect),
            equippedEffects = equippedEffects(battleUnit.equippedEffects),
        )

    private fun equippedEffectIds(equippedEffects: Map<String, List<String>>): List<String> = equippedEffects.values.flatten()

    private fun equippedEffects(equippedEffects: Map<String, List<String>>): List<EffectsView.Effect> =
        equippedEffectIds(equippedEffects)
            .groupingBy { effectId -> effectId }
            .eachCount()
            .map { (effectId, stacks) -> EffectsView.Effect(effectId = effectId, stacks = stacks) }

    private fun equippedEffectInfos(
        equippedEffects: Map<String, List<String>>,
        searchEffect: (String) -> Effect.Dto?,
    ): Map<String, EffectInfoView.EffectInfo> =
        equippedEffectIds(equippedEffects)
            .groupingBy { effectId -> effectId }
            .eachCount()
            .mapNotNull { (effectId, stacks) ->
                searchEffect(effectId)?.let { effect ->
                    effectId to EffectInfoMapper.effectInfo(effect, List(stacks) { null }, isExploring = false, searchEffect)
                }
            }.toMap()

    private fun effectInfos(
        ongoingEffects: List<BattleUnit.Dto.OngoingEffectDto>,
        isExploring: Boolean,
        searchEffect: (String) -> Effect.Dto?,
    ): Map<String, EffectInfoView.EffectInfo> =
        ongoingEffects
            .groupBy { ongoingEffect -> ongoingEffect.effectId }
            .mapNotNull { (effectId, stacks) ->
                searchEffect(effectId)?.let { effect ->
                    effectId to EffectInfoMapper.effectInfo(effect, stacks.map { stack -> stack.remainingDuration }, isExploring, searchEffect)
                }
            }.toMap()

    fun effects(
        ongoingEffects: List<BattleUnit.Dto.OngoingEffectDto>,
        newEffectIds: List<String>,
        searchEffect: (String) -> Effect.Dto?,
    ): List<EffectsView.Effect> {
        val newEffects = newEffectIds.map { effectId -> BattleUnit.Dto.OngoingEffectDto(effectId, duration(effectId, searchEffect)) }
        val stacksByEffectId = linkedMapOf<String, MutableList<BattleUnit.Dto.OngoingEffectDto>>()
        (ongoingEffects + newEffects).forEach { ongoingEffect -> stacksByEffectId.getOrPut(ongoingEffect.effectId) { mutableListOf() } += ongoingEffect }
        return stacksByEffectId.map { (effectId, stacks) ->
            EffectsView.Effect(
                effectId = effectId,
                stacks = stacks.size,
                remainingDuration = stacks.mapNotNull { stack -> stack.remainingDuration }.maxOrNull(),
                isNew = newEffectIds.contains(effectId),
            )
        }
    }

    fun turnActions(battleUnit: BattleUnit.Dto): BattleUnitInfoView.TurnActions =
        BattleUnitInfoView.TurnActions(
            remainingSteps = battleUnit.remainingActions.remainingSteps,
            maximumSteps = battleUnit.remainingActions.maximumSteps,
            remainingCasts = battleUnit.remainingActions.remainingCasts,
            maximumCasts = battleUnit.remainingActions.maximumCasts,
        )

    fun abilities(abilityAvailabilities: List<AbilityAvailability>): List<BattleUnitInfoView.Ability> =
        abilityAvailabilities.map { abilityAvailability ->
            BattleUnitInfoView.Ability(
                abilityId = abilityAvailability.abilityId,
                remainingCooldown = abilityAvailability.remainingCooldown,
                hasEnoughMana = abilityAvailability.hasEnoughMana,
                manaCost = abilityAvailability.manaCost,
            )
        }

    private fun duration(
        effectId: String,
        searchEffect: (String) -> Effect.Dto?,
    ): Int? {
        val timing = searchEffect(effectId)?.timing ?: return null
        return timing.onPlayerTurnStarted?.duration ?: timing.whileOngoing?.duration
    }
}
