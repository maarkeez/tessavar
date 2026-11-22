package com.mkz.tessavar.battleUnit.usecases.commands

import com.mkz.tessavar.battleUnit.domain.BattleUnitRepository
import com.mkz.tessavar.battleUnit.usecases.queries.SearchBattleUnitsByPlayerId
import com.mkz.tessavar.battleUnit.usecases.services.OngoingEffectsTrigger

class TriggerOnPlayerTurnStartedEffects(
    private val searchBattleUnitsByPlayerId: SearchBattleUnitsByPlayerId,
    private val battleUnitRepository: BattleUnitRepository,
    private val ongoingEffectsTrigger: OngoingEffectsTrigger,
) {
    operator fun invoke(
        playerId: String,
        battleUnitIds: Set<String>,
    ) {
        val battleUnits =
            searchBattleUnitsByPlayerId(playerId)
                .filter { battleUnit -> battleUnit.id in battleUnitIds }
                .map { battleUnit -> battleUnitRepository.searchById(battleUnit.id)!! }
        ongoingEffectsTrigger.trigger(battleUnits, inBattle = true)
    }
}
