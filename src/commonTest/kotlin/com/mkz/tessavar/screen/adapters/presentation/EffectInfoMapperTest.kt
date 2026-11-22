package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.effect.domain.Effect
import com.mkz.tessavar.effect.domain.EffectMother.applyEffectToAdjacentAlliesEffect
import com.mkz.tessavar.effect.domain.EffectMother.damageEffect
import com.mkz.tessavar.effect.domain.EffectMother.healEffect
import com.mkz.tessavar.effect.domain.EffectMother.negateActionsEffect
import com.mkz.tessavar.effect.domain.EffectMother.resistEffect
import com.mkz.tessavar.screen.adapters.presentation.EffectInfoView.EffectInfo
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class EffectInfoMapperTest {
    private val venom = damageEffect(id = "venom-damage", damage = 3, timingType = "ON_PLAYER_TURN_STARTED").toDto()

    private fun effectInfo(
        effect: Effect.Dto,
        remainingDurations: List<Int?> = listOf(4),
        isExploring: Boolean = false,
    ): EffectInfo = EffectInfoMapper.effectInfo(effect, remainingDurations, isExploring, listOf(venom).associateBy(Effect.Dto::id)::get)

    @Test
    fun `EffectInfo should say the damage at the start of each turn when the effect deals damage on player turn started`() {
        // Given
        // When
        val effectInfo = effectInfo(venom)
        // Then
        effectInfo.sentence shouldBe EffectInfo.Sentence.DamageOnPlayerTurnStarted(damage = 3)
        effectInfo.isExploring shouldBe false
    }

    @Test
    fun `EffectInfo should say the damage each second when exploring`() {
        // Given
        // When
        val effectInfo = effectInfo(venom, isExploring = true)
        // Then
        effectInfo.sentence shouldBe EffectInfo.Sentence.DamageOnPlayerTurnStarted(damage = 3)
        effectInfo.isExploring shouldBe true
    }

    @Test
    fun `EffectInfo should say the share and the resisted effect when the effect resists`() {
        // Given
        val soak = resistEffect(id = "soak", effectId = "burn", share = 50, duration = 2).toDto()
        // When
        val effectInfo = effectInfo(soak)
        // Then
        effectInfo.sentence shouldBe EffectInfo.Sentence.Resist(resistedEffectId = "burn", share = 50)
    }

    @Test
    fun `EffectInfo should say the actions are lost when the effect negates actions`() {
        // Given
        val stun = negateActionsEffect(id = "stun", duration = 1).toDto()
        // When
        val effectInfo = effectInfo(stun, isExploring = true)
        // Then
        effectInfo.sentence shouldBe EffectInfo.Sentence.NegateActions
    }

    @Test
    fun `EffectInfo should say the venom it spreads when the effect applies an effect to adjacent allies on defeated`() {
        // Given
        val venomBurst = applyEffectToAdjacentAlliesEffect(id = "venom-on-defeat", effectId = "venom-damage").toDto()
        // When
        val effectInfo = effectInfo(venomBurst, remainingDurations = listOf(null))
        // Then
        effectInfo.sentence shouldBe EffectInfo.Sentence.OnDefeatedDamageAdjacentAllies(damage = 3, turns = 5)
        effectInfo.remainingDurations.shouldBeEmpty()
    }

    @Test
    fun `EffectInfo should list the remaining durations longest first when it is stacked`() {
        // Given
        // When
        val effectInfo = effectInfo(venom, remainingDurations = listOf(2, 5, 4))
        // Then
        effectInfo.stacks shouldBe 3
        effectInfo.remainingDurations.shouldContainExactly(5, 4, 2)
    }

    @Test
    fun `EffectInfo should fail when the effect has an outcome and timing it can not describe`() {
        // Given
        val heal = healEffect(id = "mending-heal").toDto()
        // When
        val effectInfo = { effectInfo(heal) }
        // Then
        shouldThrow<IllegalStateException> { effectInfo() }.message.shouldContain("mending-heal")
    }
}
