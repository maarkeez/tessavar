package com.mkz.tessavar.battleUnit.adapters.events

import com.mkz.tessavar.battle.domain.BattleEvent
import com.mkz.tessavar.battleUnit.domain.BattleUnitMother.battleUnit
import com.mkz.tessavar.battleUnit.usecases.commands.EndStacksAtZero
import com.mkz.tessavar.battleUnit.usecases.commands.VanishSummons
import com.mkz.tessavar.battlefield.domain.BattlefieldMother.battlefield
import com.mkz.tessavar.effect.domain.EffectMother.resistEffect
import com.mkz.tessavar.player.domain.PlayerMother.player
import com.mkz.tessavar.shared.adapters.events.InMemoryEventBus
import com.mkz.tessavar.shared.usecases.InMemoryQueries
import io.kotest.matchers.collections.shouldBeEmpty
import kotlin.test.Test

class OnBattleFinishedTest {
    private val eventBus = InMemoryEventBus()
    private val queries = InMemoryQueries()
    private val onBattleFinished =
        OnBattleFinished(
            vanishSummons = VanishSummons(queries.battleUnitRepository, queries.searchPosition, eventBus),
            endStacksAtZero = EndStacksAtZero(queries.battleUnitRepository),
            eventBus = eventBus,
        )

    @Test
    fun `BattleFinished should end the stacks at 0 of every battle unit`() {
        // Given
        val soak = resistEffect(id = "soak", effectId = "burn", share = 50, duration = 2).toDto()
        queries.battlefieldRepository.create(battlefield("K B", occupants = mapOf('K' to "knight-1", 'B' to "bat-1")))
        listOf("knight-1" to "human", "bat-1" to "cpu").forEach { (battleUnitId, playerId) ->
            queries.battleUnitRepository.create(
                battleUnit(id = battleUnitId, player = player(id = playerId).toDto())
                    .receiveWhileOngoingEffect(effectId = "soak", remainingDuration = 1)
                    .triggerOnPlayerTurnStartedEffects(effects = listOf(soak), currentRow = 0, currentColumn = 0)
                    .pullEvents()
                    .second,
            )
        }
        eventBus.publish(BattleEvent.BattleFinished)
        // When
        eventBus.dispatch()
        // Then
        listOf("knight-1", "bat-1").forEach { battleUnitId ->
            queries.battleUnitRepository
                .searchById(battleUnitId)!!
                .toDto()
                .ongoingEffects.effects
                .shouldBeEmpty()
        }
    }
}
