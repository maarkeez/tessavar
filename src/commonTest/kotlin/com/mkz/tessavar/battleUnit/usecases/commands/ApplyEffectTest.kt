package com.mkz.tessavar.battleUnit.usecases.commands

import com.mkz.tessavar.battleUnit.domain.BattleUnitError.FailedToReceiveAbilityEffects
import com.mkz.tessavar.battleUnit.domain.BattleUnitEvent.EffectReceived
import com.mkz.tessavar.battleUnit.domain.BattleUnitEvent.RequestApplyEffect
import com.mkz.tessavar.battleUnit.domain.BattleUnitMother.battleUnit
import com.mkz.tessavar.battleUnit.usecases.services.BattleUnitDeployment
import com.mkz.tessavar.battlefield.domain.BattlefieldMother.battlefield
import com.mkz.tessavar.effect.domain.Effect
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectApplicationDto
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectApplicationDto.SourceDto
import com.mkz.tessavar.effect.domain.Effect.Dto.RecipientDto
import com.mkz.tessavar.effect.domain.Effect.Dto.RecipientDto.TypeDto.BATTLE_UNIT
import com.mkz.tessavar.effect.domain.Effect.Dto.RecipientDto.TypeDto.POSITION
import com.mkz.tessavar.effect.domain.EffectMother
import com.mkz.tessavar.effect.domain.EffectMother.applyEffectToAdjacentAlliesEffect
import com.mkz.tessavar.effect.domain.EffectMother.applyIfOngoingEffect
import com.mkz.tessavar.effect.domain.EffectMother.damageEffect
import com.mkz.tessavar.effect.domain.EffectMother.resistEffect
import com.mkz.tessavar.player.domain.PlayerMother.player
import com.mkz.tessavar.shared.domain.FakeEventBus
import com.mkz.tessavar.shared.domain.shouldHavePublishedEvents
import com.mkz.tessavar.shared.domain.shouldNotHavePublishedEvents
import com.mkz.tessavar.shared.usecases.InMemoryQueries
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeTypeOf
import kotlin.test.Test

class ApplyEffectTest {
    private val queries = InMemoryQueries()
    private val eventBus = FakeEventBus()
    private val applyEffect =
        ApplyEffect(
            battleUnitRepository = queries.battleUnitRepository,
            eventBus = eventBus,
            searchEffectById = queries.searchEffectById,
            searchPosition = queries.searchPosition,
            battleUnitDeployment =
                BattleUnitDeployment(
                    queries.battleUnitRepository,
                    queries.searchUnitById,
                    queries.searchMaximumAbilities,
                    queries.searchPlayerById,
                    queries.isTileVacant,
                ),
            searchAbilityById = queries.searchAbilityById,
            adjacentAllies = queries.adjacentAllies,
        )

    @Test
    fun `should fail when the battle unit recipient has no battle unit id`() {
        // Given
        queries.effectRepository.create(damageEffect(id = "poison"))
        val recipient = RecipientDto(type = BATTLE_UNIT, battleUnitId = null, position = null)
        // When
        val result = runCatching { applyEffect(application(recipient)) }
        // Then
        result.exceptionOrNull().shouldBeTypeOf<FailedToReceiveAbilityEffects>()
        eventBus.shouldNotHavePublishedEvents()
    }

    @Test
    fun `should fail when the position recipient has no position`() {
        // Given
        queries.effectRepository.create(damageEffect(id = "poison"))
        val recipient = RecipientDto(type = POSITION, battleUnitId = "rat-1", position = null)
        // When
        val result = runCatching { applyEffect(application(recipient)) }
        // Then
        result.exceptionOrNull().shouldBeTypeOf<FailedToReceiveAbilityEffects>()
        eventBus.shouldNotHavePublishedEvents()
    }

    @Test
    fun `Apply if ongoing should apply its effect when the recipient has the ongoing effect`() {
        // Given
        queries.effectRepository.create(applyIfOngoingEffect(id = "stun-long-if-soaked", effectId = "stun-long", ongoingEffectId = "soak"))
        val rat = battleUnit(id = "rat-1").receiveWhileOngoingEffect(effectId = "soak", remainingDuration = 2).pullEvents().second
        queries.battleUnitRepository.create(rat)
        queries.battlefieldRepository.create(battlefield("R .", occupants = mapOf('R' to "rat-1")))
        val application =
            EffectApplicationDto(source = SourceDto.battleUnit("bat-1", abilityId = "thunder"), recipient = RecipientDto.battleUnit("rat-1"), effectId = "stun-long-if-soaked", destination = null)
        // When
        applyEffect(application)
        // Then
        eventBus.shouldHavePublishedEvents(RequestApplyEffect(application.copy(effectId = "stun-long")))
    }

    @Test
    fun `Apply if ongoing should apply nothing when the recipient lacks the ongoing effect`() {
        // Given
        queries.effectRepository.create(applyIfOngoingEffect(id = "stun-long-if-soaked", effectId = "stun-long", ongoingEffectId = "soak"))
        queries.battleUnitRepository.create(battleUnit(id = "rat-1"))
        queries.battlefieldRepository.create(battlefield("R .", occupants = mapOf('R' to "rat-1")))
        val application =
            EffectApplicationDto(source = SourceDto.battleUnit("bat-1", abilityId = "thunder"), recipient = RecipientDto.battleUnit("rat-1"), effectId = "stun-long-if-soaked", destination = null)
        // When
        applyEffect(application)
        // Then
        eventBus.shouldNotHavePublishedEvents()
    }

    @Test
    fun `Adjacent allies should apply the effect to the recipient's adjacent allies when it is timed immediately`() {
        // Given
        val venomSpread = applyEffectToAdjacentAlliesEffect(id = "venom-spread", effectId = "venom-damage").toDto().copy(timing = EffectMother.immediatelyTiming())
        queries.effectRepository.create(Effect.create(venomSpread).pullEvents().second)
        val cpu = player(id = "cpu").toDto()
        queries.battleUnitRepository.create(battleUnit(id = "rat-1", player = cpu))
        queries.battleUnitRepository.create(battleUnit(id = "rat-2", player = cpu))
        queries.battleUnitRepository.create(battleUnit(id = "hero-1", player = player(id = "human").toDto()))
        queries.battleUnitRepository.create(battleUnit(id = "rat-3", player = cpu))
        queries.battlefieldRepository.create(
            battlefield(
                """
                R S .
                H . T
                """,
                occupants = mapOf('R' to "rat-1", 'S' to "rat-2", 'H' to "hero-1", 'T' to "rat-3"),
            ),
        )
        val application =
            EffectApplicationDto(source = SourceDto.battleUnit("bat-1", abilityId = "fireball"), recipient = RecipientDto.battleUnit("rat-1"), effectId = "venom-spread", destination = null)
        // When
        applyEffect(application)
        // Then
        eventBus.shouldHavePublishedEvents(RequestApplyEffect(application.copy(recipient = RecipientDto.battleUnit("rat-2"), effectId = "venom-damage")))
    }

    @Test
    fun `Apply effect should receive the effect as ongoing when it is timed while ongoing`() {
        // Given
        queries.effectRepository.create(resistEffect(id = "soak", effectId = "burn", share = 50, duration = 2))
        queries.battleUnitRepository.create(battleUnit(id = "rat-1"))
        queries.battlefieldRepository.create(battlefield("R .", occupants = mapOf('R' to "rat-1")))
        // When
        applyEffect(EffectApplicationDto(source = SourceDto.terrain("water"), recipient = RecipientDto.battleUnit("rat-1"), effectId = "soak", destination = null))
        // Then
        queries.battleUnitRepository
            .searchById("rat-1")!!
            .toDto()
            .ongoingEffects.whileOngoing
            .shouldContainExactly("soak")
        eventBus.shouldHavePublishedEvents(EffectReceived(battleUnitId = "rat-1", effectId = "soak"))
    }

    private fun application(recipient: RecipientDto) = EffectApplicationDto(source = SourceDto.battleUnit("knight-1"), recipient = recipient, effectId = "poison", destination = null)
}
