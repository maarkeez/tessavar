package com.mkz.tessavar.battleUnit.usecases.commands

import com.mkz.tessavar.battleUnit.domain.BattleUnit
import com.mkz.tessavar.battleUnit.domain.BattleUnitEvent
import com.mkz.tessavar.battleUnit.domain.BattleUnitMother.battleUnit
import com.mkz.tessavar.battleUnit.usecases.services.OngoingEffectsTrigger
import com.mkz.tessavar.battlefield.domain.BattlefieldMother.battlefield
import com.mkz.tessavar.effect.domain.EffectMother.damageEffect
import com.mkz.tessavar.effect.domain.EffectMother.negateActionsEffect
import com.mkz.tessavar.effect.domain.EffectMother.resistEffect
import com.mkz.tessavar.player.domain.PlayerMother.player
import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.shared.usecases.InMemoryQueries
import com.mkz.tessavar.unit.domain.UnitMother.unit
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TriggerOnPlayerTurnStartedEffectsTest {
    private val queries = InMemoryQueries()
    private val battleUnitRepository = queries.battleUnitRepository
    private val eventBus = FakeEventBus()
    private val ongoingEffectsTrigger = OngoingEffectsTrigger(queries.searchEffectById, battleUnitRepository, queries.searchPosition, eventBus)
    private val triggerOnPlayerTurnStartedEffects =
        TriggerOnPlayerTurnStartedEffects(
            searchBattleUnitsByPlayerId = queries.searchBattleUnitsByPlayerId,
            battleUnitRepository = battleUnitRepository,
            ongoingEffectsTrigger = ongoingEffectsTrigger,
        )
    private val triggerOngoingEffectsWhileExploring = TriggerOngoingEffectsWhileExploring(battleUnitRepository, ongoingEffectsTrigger)

    @Test
    fun `should trigger the on player turn started effect when the battle unit has one`() {
        // Given
        val effectId = "effect-1"
        queries.effectRepository.create(damageEffect(id = effectId, damage = 3, timingType = "ON_PLAYER_TURN_STARTED"))
        val unit = unit(baseMaximumHealth = 10).toDto()
        val player = player(id = "player-1").toDto()
        val battleUnit =
            battleUnit(id = "knight-1", unit = unit, player = player)
                .receiveOnPlayerTurnStartedEffect(effectId = effectId, remainingDuration = 1)
                .pullEvents()
                .second
        battleUnitRepository.create(battleUnit)
        queries.battlefieldRepository.create(battlefield("K .", occupants = mapOf('K' to "knight-1")))
        // When
        triggerOnPlayerTurnStartedEffects("player-1", setOf("knight-1"))
        // Then
        val storedBattleUnit = battleUnitRepository.searchById(battleUnit.toDto().id)?.toDto()
        storedBattleUnit!!.remainingHealth shouldBe 7
        eventBus.shouldHavePublishedEvents(
            BattleUnitEvent.OnPlayerTurnStartedEffectTriggered(battleUnitId = battleUnit.toDto().id, effectId = effectId),
            BattleUnitEvent.BattleUnitDamaged(battleUnit.toDto().id),
            BattleUnitEvent.OngoingEffectsTriggered(battleUnitId = battleUnit.toDto().id),
        )
    }

    @Test
    fun `Trigger should publish ongoing effects triggered for each battle unit when exploring`() {
        // Given
        queries.effectRepository.create(resistEffect(id = "soak", effectId = "burn", share = 50, duration = 2))
        listOf("knight-1", "knight-2").forEach { battleUnitId ->
            battleUnitRepository.create(
                battleUnit(id = battleUnitId, player = player(id = "player-1").toDto())
                    .receiveWhileOngoingEffect(effectId = "soak", remainingDuration = 2)
                    .pullEvents()
                    .second,
            )
        }
        queries.battlefieldRepository.create(battlefield("K L", occupants = mapOf('K' to "knight-1", 'L' to "knight-2")))
        // When
        triggerOngoingEffectsWhileExploring(setOf("knight-1", "knight-2"))
        // Then
        eventBus.shouldHavePublishedEvents(
            BattleUnitEvent.OngoingEffectsTriggered(battleUnitId = "knight-1"),
            BattleUnitEvent.OngoingEffectsTriggered(battleUnitId = "knight-2"),
        )
    }

    @Test
    fun `Trigger should count down the effects timed while ongoing, keeping the ones at 0, when the player turn starts`() {
        // Given
        queries.effectRepository.create(resistEffect(id = "soak", effectId = "burn", share = 50, duration = 2))
        val battleUnit =
            battleUnit(id = "knight-1", player = player(id = "player-1").toDto())
                .receiveWhileOngoingEffect(effectId = "soak", remainingDuration = 2)
                .receiveWhileOngoingEffect(effectId = "soak", remainingDuration = 1)
                .pullEvents()
                .second
        battleUnitRepository.create(battleUnit)
        queries.battlefieldRepository.create(battlefield("K .", occupants = mapOf('K' to "knight-1")))
        // When
        triggerOnPlayerTurnStartedEffects("player-1", setOf("knight-1"))
        // Then
        battleUnitRepository
            .searchById("knight-1")!!
            .toDto()
            .ongoingEffects.effects
            .shouldContainExactly(BattleUnit.Dto.OngoingEffectDto("soak", remainingDuration = 1), BattleUnit.Dto.OngoingEffectDto("soak", remainingDuration = 0))
        eventBus.publishedEvents.shouldContainExactly(BattleUnitEvent.OngoingEffectsTriggered(battleUnitId = "knight-1"))
    }

    @Test
    fun `Trigger should lessen the damage by the ongoing resists when the player turn starts`() {
        // Given
        queries.effectRepository.create(damageEffect(id = "burn", damage = 4, timingType = "ON_PLAYER_TURN_STARTED"))
        queries.effectRepository.create(resistEffect(id = "soak", effectId = "burn", share = 50, duration = 2))
        val battleUnit =
            battleUnit(id = "knight-1", unit = unit(baseMaximumHealth = 10).toDto(), player = player(id = "player-1").toDto())
                .receiveWhileOngoingEffect(effectId = "soak", remainingDuration = 2)
                .receiveOnPlayerTurnStartedEffect(effectId = "burn", remainingDuration = 2)
                .pullEvents()
                .second
        battleUnitRepository.create(battleUnit)
        queries.battlefieldRepository.create(battlefield("K .", occupants = mapOf('K' to "knight-1")))
        // When
        triggerOnPlayerTurnStartedEffects("player-1", setOf("knight-1"))
        // Then
        battleUnitRepository.searchById("knight-1")!!.toDto().remainingHealth shouldBe 8
    }

    @Test
    fun `Trigger while exploring should trigger the ongoing effects of every player's battle units, without negating actions`() {
        // Given
        queries.effectRepository.create(damageEffect(id = "venom-damage", damage = 3, timingType = "ON_PLAYER_TURN_STARTED"))
        queries.effectRepository.create(negateActionsEffect(id = "stun", duration = 1))
        val knight =
            battleUnit(id = "knight-1", unit = unit(baseMaximumHealth = 10).toDto(), player = player(id = "human").toDto())
                .receiveOnPlayerTurnStartedEffect(effectId = "venom-damage", remainingDuration = 2)
                .pullEvents()
                .second
        val rat =
            battleUnit(id = "rat-1", player = player(id = "cpu").toDto())
                .receiveOnPlayerTurnStartedEffect(effectId = "stun", remainingDuration = 1)
                .pullEvents()
                .second
        battleUnitRepository.create(knight)
        battleUnitRepository.create(rat)
        queries.battlefieldRepository.create(battlefield("K R", occupants = mapOf('K' to "knight-1", 'R' to "rat-1")))
        // When
        triggerOngoingEffectsWhileExploring(setOf("knight-1", "rat-1"))
        // Then
        battleUnitRepository.searchById("knight-1")!!.toDto().remainingHealth shouldBe 7
        val storedRat = battleUnitRepository.searchById("rat-1")!!.toDto()
        storedRat.remainingActions shouldBe rat.toDto().remainingActions
        storedRat.ongoingEffects.onPlayerTurnStarted.shouldBeEmpty()
    }

    @Test
    fun `should not trigger any effect when the player has no battle units`() {
        // Given
        // When
        triggerOnPlayerTurnStartedEffects("player-1", emptySet())
        // Then
        eventBus.publishedEvents.shouldBeEmpty()
    }

    @Test
    fun `should not trigger any effect when the battle unit has no on player turn started effects`() {
        // Given
        val battleUnit = battleUnit(player = player(id = "player-1").toDto())
        battleUnitRepository.create(battleUnit)
        // When
        triggerOnPlayerTurnStartedEffects("player-1", setOf(battleUnit.toDto().id))
        // Then
        eventBus.publishedEvents.shouldBeEmpty()
    }

    @Test
    fun `should not trigger the on player turn started effect when the battle unit is not in the battle`() {
        // Given
        val effectId = "effect-1"
        queries.effectRepository.create(damageEffect(id = effectId, damage = 3, timingType = "ON_PLAYER_TURN_STARTED"))
        val battleUnit =
            battleUnit(id = "knight-1", unit = unit(baseMaximumHealth = 10).toDto(), player = player(id = "player-1").toDto())
                .receiveOnPlayerTurnStartedEffect(effectId = effectId, remainingDuration = 1)
                .pullEvents()
                .second
        battleUnitRepository.create(battleUnit)
        queries.battlefieldRepository.create(battlefield("K .", occupants = mapOf('K' to "knight-1")))
        // When
        triggerOnPlayerTurnStartedEffects("player-1", setOf("another-battle-unit"))
        // Then
        battleUnitRepository.searchById("knight-1")!!.toDto().remainingHealth shouldBe 10
        eventBus.publishedEvents.shouldBeEmpty()
    }

    @Test
    fun `should publish one defeated event when two venoms finish off a battle unit with 3 health`() {
        // Given
        queries.effectRepository.create(damageEffect(id = "venom-damage", damage = 3, timingType = "ON_PLAYER_TURN_STARTED"))
        val battleUnit =
            battleUnit(id = "goat-wizard-1", unit = unit(baseMaximumHealth = 3).toDto(), player = player(id = "player-2").toDto())
                .receiveOnPlayerTurnStartedEffect(effectId = "venom-damage", remainingDuration = 5)
                .receiveOnPlayerTurnStartedEffect(effectId = "venom-damage", remainingDuration = 5)
                .pullEvents()
                .second
        battleUnitRepository.create(battleUnit)
        queries.battlefieldRepository.create(battlefield("G .", occupants = mapOf('G' to "goat-wizard-1")))
        // When
        triggerOnPlayerTurnStartedEffects("player-2", setOf("goat-wizard-1"))
        // Then
        battleUnitRepository.searchById("goat-wizard-1")!!.toDto().remainingHealth shouldBe 0
        eventBus.publishedEvents.filterIsInstance<BattleUnitEvent.BattleUnitDefeated>().size shouldBe 1
    }

    @Test
    fun `should deal the damage of both venoms when the battle unit has two of them`() {
        // Given
        queries.effectRepository.create(damageEffect(id = "venom-damage", damage = 3, timingType = "ON_PLAYER_TURN_STARTED"))
        val battleUnit =
            battleUnit(id = "knight-1", unit = unit(baseMaximumHealth = 10).toDto(), player = player(id = "player-1").toDto())
                .receiveOnPlayerTurnStartedEffect(effectId = "venom-damage", remainingDuration = 5)
                .receiveOnPlayerTurnStartedEffect(effectId = "venom-damage", remainingDuration = 5)
                .pullEvents()
                .second
        battleUnitRepository.create(battleUnit)
        queries.battlefieldRepository.create(battlefield("K .", occupants = mapOf('K' to "knight-1")))
        // When
        triggerOnPlayerTurnStartedEffects("player-1", setOf("knight-1"))
        // Then
        battleUnitRepository.searchById("knight-1")!!.toDto().remainingHealth shouldBe 4
    }
}
