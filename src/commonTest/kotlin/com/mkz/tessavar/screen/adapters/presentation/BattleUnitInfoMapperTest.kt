package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.battleUnit.domain.BattleUnit.Dto.OngoingEffectDto
import com.mkz.tessavar.effect.domain.Effect
import com.mkz.tessavar.effect.domain.EffectMother.damageEffect
import com.mkz.tessavar.effect.domain.EffectMother.resistEffect
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

class BattleUnitInfoMapperTest {
    private val venom = damageEffect(id = "venom-damage", damage = 3).toDto()
    private val soak = resistEffect(id = "soak", effectId = "burn", share = 50, duration = 2).toDto()

    private fun effects(
        ongoingEffects: List<OngoingEffectDto>,
        newEffectIds: List<String> = emptyList(),
    ): List<EffectsView.Effect> = BattleUnitInfoMapper.effects(ongoingEffects, newEffectIds, listOf(venom, soak).associateBy(Effect.Dto::id)::get)

    @Test
    fun `BattleUnitInfo should group the ongoing effects of an effect id into one icon when it is stacked`() {
        // Given
        val ongoingEffects = listOf(OngoingEffectDto("venom-damage", 2), OngoingEffectDto("soak", 1), OngoingEffectDto("venom-damage", 4))
        // When
        val effects = effects(ongoingEffects)
        // Then
        effects.shouldContainExactly(
            EffectsView.Effect(effectId = "venom-damage", stacks = 2, remainingDuration = 4),
            EffectsView.Effect(effectId = "soak", stacks = 1, remainingDuration = 1),
        )
    }

    @Test
    fun `BattleUnitInfo should keep the longest remaining duration when it is stacked`() {
        // Given
        val ongoingEffects = listOf(OngoingEffectDto("venom-damage", 5), OngoingEffectDto("venom-damage", 3))
        // When
        val effects = effects(ongoingEffects)
        // Then
        effects.shouldContainExactly(EffectsView.Effect(effectId = "venom-damage", stacks = 2, remainingDuration = 5))
    }

    @Test
    fun `BattleUnitInfo should keep no remaining duration when the effect is on defeated`() {
        // Given
        val ongoingEffects = listOf(OngoingEffectDto("venom-on-defeat", null))
        // When
        val effects = effects(ongoingEffects)
        // Then
        effects.shouldContainExactly(EffectsView.Effect(effectId = "venom-on-defeat", stacks = 1, remainingDuration = null))
    }

    @Test
    fun `BattleUnitInfo should mark the icon new with one more stack when a cast adds a stack`() {
        // Given
        val ongoingEffects = listOf(OngoingEffectDto("venom-damage", 2))
        // When
        val effects = effects(ongoingEffects, newEffectIds = listOf("venom-damage"))
        // Then
        effects.shouldContainExactly(EffectsView.Effect(effectId = "venom-damage", stacks = 2, remainingDuration = 5, isNew = true))
    }

    @Test
    fun `BattleUnitInfo should add a new icon with the effect's duration when a cast adds a new effect`() {
        // Given
        val ongoingEffects = listOf(OngoingEffectDto("venom-damage", 2))
        // When
        val effects = effects(ongoingEffects, newEffectIds = listOf("soak"))
        // Then
        effects.shouldContainExactly(
            EffectsView.Effect(effectId = "venom-damage", stacks = 1, remainingDuration = 2),
            EffectsView.Effect(effectId = "soak", stacks = 1, remainingDuration = 2, isNew = true),
        )
    }
}
