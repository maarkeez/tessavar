package com.mkz.tessavar.battleUnit.usecases.commands

import com.mkz.tessavar.battleUnit.domain.BattleUnitRepository
import com.mkz.tessavar.battleUnit.usecases.services.OngoingEffectsTrigger

class TriggerOngoingEffectsWhileExploring(
    private val battleUnitRepository: BattleUnitRepository,
    private val ongoingEffectsTrigger: OngoingEffectsTrigger,
) {
    operator fun invoke(battleUnitIds: Set<String>) {
        ongoingEffectsTrigger.trigger(battleUnitIds.mapNotNull { battleUnitId -> battleUnitRepository.searchById(battleUnitId) }, inBattle = false)
    }
}
