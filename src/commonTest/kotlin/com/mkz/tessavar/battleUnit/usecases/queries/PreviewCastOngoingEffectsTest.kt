package com.mkz.tessavar.battleUnit.usecases.queries

import com.mkz.tessavar.ability.domain.Ability.Dto.EffectTargetDto
import com.mkz.tessavar.ability.domain.Ability.Dto.EffectTargetDto.Type.CASTER
import com.mkz.tessavar.ability.domain.Ability.Dto.EffectTargetDto.Type.TARGETED_BATTLE_UNIT
import com.mkz.tessavar.ability.domain.Ability.Dto.TargetingDto
import com.mkz.tessavar.ability.domain.AbilityMother.ability
import com.mkz.tessavar.ability.domain.AbilityMother.abilityEffect
import com.mkz.tessavar.battleUnit.domain.BattleUnitMother.battleUnit
import com.mkz.tessavar.battlefield.domain.Battlefield.Dto.PositionDto
import com.mkz.tessavar.battlefield.domain.BattlefieldMother.battlefield
import com.mkz.tessavar.effect.domain.Effect
import com.mkz.tessavar.effect.domain.EffectMother.applyEffectToAdjacentAlliesEffect
import com.mkz.tessavar.effect.domain.EffectMother.applyIfOngoingEffect
import com.mkz.tessavar.effect.domain.EffectMother.damageEffect
import com.mkz.tessavar.effect.domain.EffectMother.negateActionsEffect
import com.mkz.tessavar.effect.domain.EffectMother.resistEffect
import com.mkz.tessavar.player.domain.PlayerMother.player
import com.mkz.tessavar.shared.usecases.InMemoryQueries
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PreviewCastOngoingEffectsTest {
    private val queries = InMemoryQueries()
    private val previewCastOngoingEffects =
        PreviewCastOngoingEffects(
            searchAbilityById = queries.searchAbilityById,
            searchEffectById = queries.searchEffectById,
            castResolution = queries.castResolution,
            battleUnitRepository = queries.battleUnitRepository,
        )

    private fun givenBattle(map: String) {
        val player = player(id = "player-1").toDto()
        val opponent = player(id = "player-2").toDto()
        val battleUnits = mapOf('C' to "caster", 'E' to "enemy-1", 'F' to "enemy-2")
        battleUnits.forEach { (letter, battleUnitId) ->
            queries.battleUnitRepository.create(battleUnit(id = battleUnitId, player = if (letter == 'C') player else opponent))
        }
        queries.battlefieldRepository.create(battlefield(map, occupants = battleUnits.filterKeys { letter -> letter in map }))
        queries.effectRepository.create(Effect.create(damageEffect(id = "venom", timingType = "ON_PLAYER_TURN_STARTED").toDto()))
        queries.effectRepository.create(Effect.create(applyEffectToAdjacentAlliesEffect(id = "curse", effectId = "venom").toDto()))
        queries.effectRepository.create(Effect.create(damageEffect(id = "slash", timingType = "IMMEDIATELY").toDto()))
        queries.effectRepository.create(negateActionsEffect(id = "stun", duration = 1))
        queries.effectRepository.create(negateActionsEffect(id = "stun-long", duration = 2))
        queries.effectRepository.create(applyIfOngoingEffect(id = "stun-long-if-soaked", effectId = "stun-long", ongoingEffectId = "soak"))
        queries.effectRepository.create(resistEffect(id = "soak", effectId = "venom", share = 50))
    }

    private fun givenAbility(
        targeting: TargetingDto,
        vararg abilityEffects: Pair<String, EffectTargetDto.Type>,
    ) {
        queries.abilityRepository.create(
            ability(id = "ability-1", targeting = targeting, abilityEffects = abilityEffects.map { (effectId, target) -> abilityEffect(effectId = effectId, target = target) }),
        )
    }

    @Test
    fun `should list the effects that stay on the target when the ability is cast on it`() {
        // Given
        givenBattle("C E")
        givenAbility(TargetingDto.ADJACENT_ENEMY, "venom" to TARGETED_BATTLE_UNIT, "curse" to TARGETED_BATTLE_UNIT)
        // When
        val ongoingEffects = previewCastOngoingEffects(battleUnitId = "caster", abilityId = "ability-1", castGroup = listOf(PositionDto(0, 1)))
        // Then
        ongoingEffects shouldBe mapOf("enemy-1" to listOf("venom", "curse"))
    }

    @Test
    fun `should list the effect an apply if ongoing applies when the target has the ongoing effect`() {
        // Given
        givenBattle("C E")
        givenAbility(TargetingDto.ADJACENT_ENEMY, "stun" to TARGETED_BATTLE_UNIT, "stun-long-if-soaked" to TARGETED_BATTLE_UNIT)
        queries.battleUnitRepository.update(
            queries.battleUnitRepository
                .searchById("enemy-1")!!
                .receiveWhileOngoingEffect(effectId = "soak", remainingDuration = 2)
                .pullEvents()
                .second,
        )
        // When
        val ongoingEffects = previewCastOngoingEffects(battleUnitId = "caster", abilityId = "ability-1", castGroup = listOf(PositionDto(0, 1)))
        // Then
        ongoingEffects shouldBe mapOf("enemy-1" to listOf("stun", "stun-long"))
    }

    @Test
    fun `should not list the effect an apply if ongoing applies when the target lacks the ongoing effect`() {
        // Given
        givenBattle("C E")
        givenAbility(TargetingDto.ADJACENT_ENEMY, "stun" to TARGETED_BATTLE_UNIT, "stun-long-if-soaked" to TARGETED_BATTLE_UNIT)
        // When
        val ongoingEffects = previewCastOngoingEffects(battleUnitId = "caster", abilityId = "ability-1", castGroup = listOf(PositionDto(0, 1)))
        // Then
        ongoingEffects shouldBe mapOf("enemy-1" to listOf("stun"))
    }

    @Test
    fun `should list the effects timed while ongoing when the ability is cast`() {
        // Given
        givenBattle("C E")
        givenAbility(TargetingDto.ADJACENT_ENEMY, "soak" to TARGETED_BATTLE_UNIT)
        // When
        val ongoingEffects = previewCastOngoingEffects(battleUnitId = "caster", abilityId = "ability-1", castGroup = listOf(PositionDto(0, 1)))
        // Then
        ongoingEffects shouldBe mapOf("enemy-1" to listOf("soak"))
    }

    @Test
    fun `should not list the effects applied immediately when the ability is cast`() {
        // Given
        givenBattle("C E")
        givenAbility(TargetingDto.ADJACENT_ENEMY, "slash" to TARGETED_BATTLE_UNIT)
        // When
        val ongoingEffects = previewCastOngoingEffects(battleUnitId = "caster", abilityId = "ability-1", castGroup = listOf(PositionDto(0, 1)))
        // Then
        ongoingEffects.shouldBeEmpty()
    }

    @Test
    fun `should list the effects of each target when the ability is cast on a cast group of several tiles`() {
        // Given
        givenBattle("C E\nF .")
        givenAbility(TargetingDto.ALL_ADJACENT_ENEMIES, "venom" to TARGETED_BATTLE_UNIT)
        // When
        val ongoingEffects =
            previewCastOngoingEffects(battleUnitId = "caster", abilityId = "ability-1", castGroup = listOf(PositionDto(0, 1), PositionDto(1, 0)))
        // Then
        ongoingEffects shouldBe mapOf("enemy-1" to listOf("venom"), "enemy-2" to listOf("venom"))
    }

    @Test
    fun `should list the caster effect once per tile when the ability is cast on a cast group of several tiles`() {
        // Given
        givenBattle("C E\nF .")
        givenAbility(TargetingDto.ALL_ADJACENT_ENEMIES, "venom" to CASTER)
        // When
        val ongoingEffects =
            previewCastOngoingEffects(battleUnitId = "caster", abilityId = "ability-1", castGroup = listOf(PositionDto(0, 1), PositionDto(1, 0)))
        // Then
        ongoingEffects shouldBe mapOf("caster" to listOf("venom", "venom"))
    }

    @Test
    fun `should not list any effect when the ability does not exist`() {
        // Given
        givenBattle("C E")
        // When
        val ongoingEffects = previewCastOngoingEffects(battleUnitId = "caster", abilityId = "unknown-ability", castGroup = listOf(PositionDto(0, 1)))
        // Then
        ongoingEffects.shouldBeEmpty()
    }
}
