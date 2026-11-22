package com.mkz.tessavar.battleUnit.usecases.commands

import com.mkz.tessavar.battleUnit.domain.BattleUnit
import com.mkz.tessavar.battleUnit.domain.BattleUnitRepository

class EndStacksAtZero(
    private val battleUnitRepository: BattleUnitRepository,
) {
    fun ofPlayer(
        playerId: String,
        battleUnitIds: Set<String>,
    ) {
        battleUnitRepository
            .searchByPlayerId(playerId)
            .filter { battleUnit -> battleUnit.toDto().id in battleUnitIds }
            .forEach(::endStacksAtZero)
    }

    fun ofEveryBattleUnit() {
        battleUnitRepository.searchAll().forEach(::endStacksAtZero)
    }

    private fun endStacksAtZero(battleUnit: BattleUnit) {
        battleUnitRepository.update(battleUnit.endStacksAtZero())
    }
}
