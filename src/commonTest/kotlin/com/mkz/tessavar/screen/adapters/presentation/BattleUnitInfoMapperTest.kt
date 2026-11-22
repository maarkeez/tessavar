package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.battleUnit.domain.BattleUnit
import com.mkz.tessavar.battleUnit.domain.BattleUnit.Dto.OngoingEffectDto
import com.mkz.tessavar.battleUnit.domain.BattleUnitMother
import com.mkz.tessavar.effect.domain.Effect
import com.mkz.tessavar.effect.domain.EffectMother.damageEffect
import com.mkz.tessavar.effect.domain.EffectMother.increaseMaximumHealthEffect
import com.mkz.tessavar.effect.domain.EffectMother.increaseMaximumManaEffect
import com.mkz.tessavar.effect.domain.EffectMother.resistEffect
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class BattleUnitInfoMapperTest {
    private val venom = damageEffect(id = "venom-damage", damage = 3).toDto()
    private val soak = resistEffect(id = "soak", effectId = "burn", share = 50, duration = 2).toDto()
    private val maximumHealth30 = increaseMaximumHealthEffect(id = "maximum-health-30", maximumHealth = 30).toDto()
    private val maximumMana40 = increaseMaximumManaEffect(id = "maximum-mana-40", maximumMana = 40).toDto()
    private val maximumHealth20 = increaseMaximumHealthEffect(id = "maximum-health-20", maximumHealth = 20).toDto()
    private val searchEffect = listOf(venom, soak, maximumHealth30, maximumMana40, maximumHealth20).associateBy(Effect.Dto::id)::get

    private fun equipped(vararg items: Pair<String, List<Effect.Dto>>): BattleUnit.Dto =
        items
            .fold(BattleUnitMother.battleUnit()) { battleUnit, (lootableItemId, effects) ->
                effects.fold(battleUnit) { equippedBattleUnit, effect -> equippedBattleUnit.applyEquippedEffect(lootableItemId = lootableItemId, effect = effect) }
            }.toDto()

    @Test
    fun `BattleUnitInfo should list one equipped icon per effect without remaining duration when items are equipped`() {
        // Given
        val battleUnit = equipped("rune-hammer" to listOf(maximumHealth30, maximumMana40))
        // When
        val battleUnitInfo = BattleUnitInfoMapper.battleUnitInfo(battleUnit, isHumanPlayers = true, searchEffect = searchEffect)
        // Then
        battleUnitInfo.equippedEffects.shouldContainExactly(
            EffectsView.Effect(effectId = "maximum-health-30", stacks = 1, remainingDuration = null),
            EffectsView.Effect(effectId = "maximum-mana-40", stacks = 1, remainingDuration = null),
        )
        battleUnitInfo.effects.shouldBeEmpty()
    }

    @Test
    fun `BattleUnitInfo should group the equipped effects of an effect id when two items give it`() {
        // Given
        val battleUnit = equipped("hammer" to listOf(maximumHealth20), "other-hammer" to listOf(maximumHealth20))
        // When
        val battleUnitInfo = BattleUnitInfoMapper.battleUnitInfo(battleUnit, isHumanPlayers = true, searchEffect = searchEffect)
        // Then
        battleUnitInfo.equippedEffects.shouldContainExactly(EffectsView.Effect(effectId = "maximum-health-20", stacks = 2, remainingDuration = null))
    }

    @Test
    fun `BattleUnitInfo should give an effect info to each equipped effect`() {
        // Given
        val battleUnit = equipped("rune-hammer" to listOf(maximumHealth30, maximumMana40))
        // When
        val battleUnitInfo = BattleUnitInfoMapper.battleUnitInfo(battleUnit, isHumanPlayers = true, searchEffect = searchEffect)
        // Then
        battleUnitInfo.effectInfos.getValue("maximum-health-30").sentence shouldBe EffectInfoView.EffectInfo.Sentence.IncreaseMaximumHealth(amount = 30)
        battleUnitInfo.effectInfos
            .getValue("maximum-health-30")
            .remainingDurations
            .shouldBeEmpty()
        battleUnitInfo.effectInfos.getValue("maximum-mana-40").sentence shouldBe EffectInfoView.EffectInfo.Sentence.IncreaseMaximumMana(amount = 40)
    }

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
