package com.mkz.tessavar.battleUnit.usecases.commands

import com.mkz.tessavar.battleUnit.domain.BattleUnitEvent
import com.mkz.tessavar.battleUnit.domain.BattleUnitRepository
import com.mkz.tessavar.battleUnit.usecases.services.AdjacentAllies
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectApplicationDto
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.APPLY_EFFECT_TO_ADJACENT_ALLIES
import com.mkz.tessavar.effect.domain.Effect.Dto.RecipientDto
import com.mkz.tessavar.effect.usecases.queries.SearchEffectById
import com.mkz.tessavar.shared.domain.EventBus

class TriggerOnDefeatedEffects(
    private val battleUnitRepository: BattleUnitRepository,
    private val searchEffectById: SearchEffectById,
    private val adjacentAllies: AdjacentAllies,
    private val eventBus: EventBus,
) {
    operator fun invoke(
        battleUnitId: String,
        defeatedAtRow: Int,
        defeatedAtColumn: Int,
    ) {
        val battleUnit = battleUnitRepository.searchById(battleUnitId) ?: return
        if (!battleUnit.isDefeated()) return
        val onDefeatedEffectIds = battleUnit.toDto().ongoingEffects.onDefeatedEffects
        onDefeatedEffectIds.forEach { onDefeatedEffectId ->
            val onDefeatedEffect = searchEffectById(onDefeatedEffectId) ?: return@forEach
            if (onDefeatedEffect.outcome.type != APPLY_EFFECT_TO_ADJACENT_ALLIES) return@forEach
            val appliedEffectId = onDefeatedEffect.outcome.applyEffectToAdjacentAllies!!.effectId
            adjacentAllies(battleUnit = battleUnit, row = defeatedAtRow, column = defeatedAtColumn)
                .forEach { adjacentAlly ->
                    eventBus.publish(
                        BattleUnitEvent.RequestApplyEffect(
                            application =
                                EffectApplicationDto(
                                    source = EffectApplicationDto.SourceDto.battleUnit(battleUnitId),
                                    recipient = RecipientDto.battleUnit(adjacentAlly.toDto().id),
                                    effectId = appliedEffectId,
                                    destination = null,
                                ),
                        ),
                    )
                }
        }
        val (events, updatedBattleUnit) = battleUnit.triggerOnDefeatedEffects().pullEvents()
        battleUnitRepository.update(updatedBattleUnit)
        eventBus.publish(events)
    }
}
