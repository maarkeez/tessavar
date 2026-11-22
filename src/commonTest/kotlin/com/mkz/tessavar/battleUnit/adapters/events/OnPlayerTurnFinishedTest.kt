package com.mkz.tessavar.battleUnit.adapters.events

import com.mkz.tessavar.battle.domain.BattleEvent
import com.mkz.tessavar.battleUnit.domain.BattleUnitMother.battleUnit
import com.mkz.tessavar.battleUnit.usecases.commands.EndStacksAtZero
import com.mkz.tessavar.battleUnit.usecases.commands.ReduceRemainingSummonDurations
import com.mkz.tessavar.battlefield.domain.BattlefieldMother.battlefield
import com.mkz.tessavar.effect.domain.EffectMother.negateActionsEffect
import com.mkz.tessavar.player.domain.PlayerMother.player
import com.mkz.tessavar.shared.adapters.events.InMemoryEventBus
import com.mkz.tessavar.shared.usecases.InMemoryQueries
import io.kotest.matchers.collections.shouldBeEmpty
import kotlin.test.Test

class OnPlayerTurnFinishedTest {
    private val eventBus = InMemoryEventBus()
    private val queries = InMemoryQueries()
    private val onPlayerTurnFinished =
        OnPlayerTurnFinished(
            reduceRemainingSummonDurations = ReduceRemainingSummonDurations(queries.battleUnitRepository, queries.searchPosition, eventBus),
            endStacksAtZero = EndStacksAtZero(queries.battleUnitRepository),
            eventBus = eventBus,
        )

    @Test
    fun `PlayerTurnFinished should end the stacks at 0 of the player's battle units`() {
        // Given
        val stun = negateActionsEffect(id = "stun", duration = 1).toDto()
        queries.battlefieldRepository.create(battlefield("K .", occupants = mapOf('K' to "knight-1")))
        queries.battleUnitRepository.create(
            battleUnit(id = "knight-1", player = player(id = "human").toDto())
                .receiveOnPlayerTurnStartedEffect(effectId = "stun", remainingDuration = 1)
                .triggerOnPlayerTurnStartedEffects(effects = listOf(stun), currentRow = 0, currentColumn = 0)
                .pullEvents()
                .second,
        )
        eventBus.publish(BattleEvent.PlayerTurnFinished(playerId = "human", battleUnitIds = setOf("knight-1")))
        // When
        eventBus.dispatch()
        // Then
        queries.battleUnitRepository
            .searchById("knight-1")!!
            .toDto()
            .ongoingEffects.effects
            .shouldBeEmpty()
    }
}
