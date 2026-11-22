package com.mkz.tessavar.battleUnit.usecases.services

import com.mkz.tessavar.battleUnit.domain.BattleUnit
import com.mkz.tessavar.battleUnit.domain.BattleUnitRepository
import com.mkz.tessavar.battlefield.usecases.queries.SearchPosition
import com.mkz.tessavar.effect.usecases.queries.SearchEffectById
import com.mkz.tessavar.shared.domain.EventBus

class OngoingEffectsTrigger(
    private val searchEffectById: SearchEffectById,
    private val battleUnitRepository: BattleUnitRepository,
    private val searchPosition: SearchPosition,
    private val eventBus: EventBus,
) {
    fun trigger(
        battleUnits: List<BattleUnit>,
        inBattle: Boolean,
    ) {
        battleUnits
            .filter { battleUnit -> battleUnit.hasEffectsCountedDownOnPlayerTurnStarted() }
            .forEach { battleUnit ->
                val ongoingEffects = battleUnit.toDto().ongoingEffects
                val effects =
                    (ongoingEffects.onPlayerTurnStarted + ongoingEffects.whileOngoing)
                        .distinct()
                        .map { effectId -> searchEffectById(effectId)!! }
                val position = searchPosition(occupantId = battleUnit.toDto().id) ?: return@forEach
                val (events, updatedBattleUnit) =
                    battleUnit
                        .triggerOnPlayerTurnStartedEffects(
                            effects = effects,
                            currentRow = position.row,
                            currentColumn = position.column,
                            inBattle = inBattle,
                        ).pullEvents()
                battleUnitRepository.update(updatedBattleUnit)
                eventBus.publish(events)
            }
    }
}
