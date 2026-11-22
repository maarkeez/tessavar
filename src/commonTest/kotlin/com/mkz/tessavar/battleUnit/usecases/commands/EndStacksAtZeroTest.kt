package com.mkz.tessavar.battleUnit.usecases.commands

import com.mkz.tessavar.battleUnit.domain.BattleUnit
import com.mkz.tessavar.battleUnit.domain.BattleUnit.Dto.OngoingEffectDto
import com.mkz.tessavar.battleUnit.domain.BattleUnitMother.battleUnit
import com.mkz.tessavar.effect.domain.EffectMother.negateActionsEffect
import com.mkz.tessavar.player.domain.Player
import com.mkz.tessavar.player.domain.PlayerMother.player
import com.mkz.tessavar.shared.usecases.InMemoryQueries
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

class EndStacksAtZeroTest {
    private val queries = InMemoryQueries()
    private val battleUnitRepository = queries.battleUnitRepository
    private val endStacksAtZero = EndStacksAtZero(battleUnitRepository)
    private val human = player(id = "human").toDto()
    private val cpu = player(id = "cpu").toDto()
    private val stun = negateActionsEffect(id = "stun", duration = 1).toDto()

    private fun stunnedThisTurn(
        id: String,
        player: Player.Dto,
    ): BattleUnit =
        battleUnit(id = id, player = player)
            .receiveOnPlayerTurnStartedEffect(effectId = "stun", remainingDuration = 1)
            .triggerOnPlayerTurnStartedEffects(effects = listOf(stun), currentRow = 0, currentColumn = 0)
            .pullEvents()
            .second

    private fun effectsOf(battleUnitId: String): List<OngoingEffectDto> =
        battleUnitRepository
            .searchById(battleUnitId)!!
            .toDto()
            .ongoingEffects.effects

    @Test
    fun `OfPlayer should end the stacks at 0 of the player's battle units when the player turn finishes`() {
        // Given
        battleUnitRepository.create(stunnedThisTurn(id = "knight-1", player = human))
        // When
        endStacksAtZero.ofPlayer(playerId = "human", battleUnitIds = setOf("knight-1"))
        // Then
        effectsOf("knight-1").shouldBeEmpty()
    }

    @Test
    fun `OfPlayer should not end the stacks at 0 when the battle units are the other players'`() {
        // Given
        battleUnitRepository.create(stunnedThisTurn(id = "bat-1", player = cpu))
        // When
        endStacksAtZero.ofPlayer(playerId = "human", battleUnitIds = setOf("bat-1"))
        // Then
        effectsOf("bat-1").shouldContainExactly(OngoingEffectDto(effectId = "stun", remainingDuration = 0))
    }

    @Test
    fun `OfEveryBattleUnit should end the stacks at 0 of every battle unit when the battle finishes`() {
        // Given
        battleUnitRepository.create(stunnedThisTurn(id = "knight-1", player = human))
        battleUnitRepository.create(stunnedThisTurn(id = "bat-1", player = cpu))
        // When
        endStacksAtZero.ofEveryBattleUnit()
        // Then
        effectsOf("knight-1").shouldBeEmpty()
        effectsOf("bat-1").shouldBeEmpty()
    }
}
