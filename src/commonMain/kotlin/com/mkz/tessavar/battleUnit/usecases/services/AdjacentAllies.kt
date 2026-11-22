package com.mkz.tessavar.battleUnit.usecases.services

import com.mkz.tessavar.battleUnit.domain.BattleUnit
import com.mkz.tessavar.battleUnit.domain.BattleUnitRepository
import com.mkz.tessavar.battlefield.usecases.queries.SearchOccupant

class AdjacentAllies(
    private val battleUnitRepository: BattleUnitRepository,
    private val searchOccupant: SearchOccupant,
    private val adjacencyService: AdjacencyService,
) {
    operator fun invoke(
        battleUnit: BattleUnit,
        row: Int,
        column: Int,
    ): List<BattleUnit> =
        buildList {
            for (adjacentRow in row - 1..row + 1) {
                for (adjacentColumn in column - 1..column + 1) {
                    if (!adjacencyService.isAdjacent(fromRow = row, fromColumn = column, toRow = adjacentRow, toColumn = adjacentColumn)) continue
                    val occupantId = searchOccupant(adjacentRow, adjacentColumn) ?: continue
                    val occupant = battleUnitRepository.searchById(occupantId) ?: continue
                    if (occupant.toDto().id == battleUnit.toDto().id) continue
                    if (!occupant.isSamePlayer(battleUnit)) continue
                    if (occupant.isDefeated()) continue
                    add(occupant)
                }
            }
        }
}
