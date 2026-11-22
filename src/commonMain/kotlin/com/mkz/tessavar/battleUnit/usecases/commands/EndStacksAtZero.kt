package com.mkz.tessavar.battleUnit.usecases.commands

import com.mkz.tessavar.battleUnit.domain.BattleUnitRepository

class EndStacksAtZero(
    private val battleUnitRepository: BattleUnitRepository,
) {
    operator fun invoke() {
        battleUnitRepository.searchAll().forEach { battleUnit -> battleUnitRepository.update(battleUnit.endStacksAtZero()) }
    }
}
