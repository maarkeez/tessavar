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
    fun `should end the stacks at 0 of every battle unit`() {
        // Given
        battleUnitRepository.create(stunnedThisTurn(id = "knight-1", player = human))
        battleUnitRepository.create(stunnedThisTurn(id = "bat-1", player = cpu))
        // When
        endStacksAtZero()
        // Then
        effectsOf("knight-1").shouldBeEmpty()
        effectsOf("bat-1").shouldBeEmpty()
    }

    @Test
    fun `should keep the stacks with a remaining duration left`() {
        // Given
        battleUnitRepository.create(battleUnit(id = "knight-1", player = human).receiveOnPlayerTurnStartedEffect(effectId = "stun", remainingDuration = 1))
        // When
        endStacksAtZero()
        // Then
        effectsOf("knight-1").shouldContainExactly(OngoingEffectDto(effectId = "stun", remainingDuration = 1))
    }
}
