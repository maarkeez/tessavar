package com.mkz.tessavar.effect.adapters.resources

import com.mkz.tessavar.effect.domain.EffectMother.applyEffectToAdjacentAlliesEffect
import com.mkz.tessavar.effect.domain.EffectMother.applyIfOngoingEffect
import com.mkz.tessavar.effect.domain.EffectMother.damageEffect
import com.mkz.tessavar.effect.domain.EffectMother.healEffect
import com.mkz.tessavar.effect.domain.EffectMother.increaseMaximumHealthEffect
import com.mkz.tessavar.effect.domain.EffectMother.increaseMaximumManaEffect
import com.mkz.tessavar.effect.domain.EffectMother.learnAbilityEffect
import com.mkz.tessavar.effect.domain.EffectMother.negateActionsEffect
import com.mkz.tessavar.effect.domain.EffectMother.resistEffect
import com.mkz.tessavar.effect.domain.EffectMother.restoreManaEffect
import com.mkz.tessavar.effect.domain.EffectMother.summonEffect
import com.mkz.tessavar.effect.domain.EffectMother.teleportEffect
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import korlibs.io.file.std.MemoryVfsMix
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ResourcesEffectLoaderTest {
    @Test
    fun `should load one effect per toml file when effect resources are initialised`() =
        runTest {
            // Given
            val effectRoot =
                MemoryVfsMix(
                    "effect/venom.toml" to "[outcome]\ntype = \"DAMAGE\"\ndamage = 3\n\n[timing]\ntype = \"ON_PLAYER_TURN_STARTED\"\nduration = 5\n",
                    "effect/heal.toml" to "[outcome]\ntype = \"HEAL\"\nhealing = 7\n\n[timing]\ntype = \"IMMEDIATELY\"\n",
                    "effect/teleport.toml" to "[outcome]\ntype = \"TELEPORT\"\n\n[timing]\ntype = \"IMMEDIATELY\"\n",
                    "effect/curse.toml" to "[outcome]\ntype = \"APPLY_EFFECT_TO_ADJACENT_ALLIES\"\neffectId = \"venom\"\n\n[timing]\ntype = \"ON_DEFEATED\"\n",
                    "effect/summon.toml" to "[outcome]\ntype = \"SUMMON\"\nunitId = \"bee\"\n\n[timing]\ntype = \"IMMEDIATELY\"\n",
                    "effect/ether.toml" to "[outcome]\ntype = \"RESTORE_MANA\"\nmana = 15\n\n[timing]\ntype = \"IMMEDIATELY\"\n",
                    "effect/sturdy.toml" to "[outcome]\ntype = \"INCREASE_MAXIMUM_HEALTH\"\nmaximumHealth = 20\n\n[timing]\ntype = \"WHILE_EQUIPPED\"\n",
                    "effect/wise.toml" to "[outcome]\ntype = \"INCREASE_MAXIMUM_MANA\"\nmaximumMana = 5\n\n[timing]\ntype = \"WHILE_EQUIPPED\"\n",
                    "effect/learn-teleport.toml" to "[outcome]\ntype = \"LEARN_ABILITY\"\nabilityId = \"teleport\"\n\n[timing]\ntype = \"IMMEDIATELY\"\n",
                )["effect"]
            val loader = ResourcesEffectLoader(effectRoot = effectRoot)
            // When
            loader.initResources()
            // Then
            loader.loadEffects().shouldContainExactly(
                applyEffectToAdjacentAlliesEffect(id = "curse", effectId = "venom").toDto(),
                restoreManaEffect(id = "ether", mana = 15).toDto(),
                healEffect(id = "heal", healing = 7).toDto(),
                learnAbilityEffect(id = "learn-teleport", abilityId = "teleport").toDto(),
                increaseMaximumHealthEffect(id = "sturdy", maximumHealth = 20).toDto(),
                summonEffect(id = "summon", unitId = "bee").toDto(),
                teleportEffect(id = "teleport").toDto(),
                damageEffect(id = "venom", damage = 3).toDto(),
                increaseMaximumManaEffect(id = "wise", maximumMana = 5).toDto(),
            )
        }

    @Test
    fun `Loader should read a resist timed while ongoing when the effect file holds one`() =
        runTest {
            // Given
            val effectRoot =
                MemoryVfsMix(
                    "effect/soak.toml" to "[outcome]\ntype = \"RESIST\"\neffectId = \"burn\"\nshare = 50\n\n[timing]\ntype = \"WHILE_ONGOING\"\nduration = 2\n",
                )["effect"]
            val loader = ResourcesEffectLoader(effectRoot = effectRoot)
            // When
            loader.initResources()
            // Then
            loader.loadEffects().shouldContainExactly(resistEffect(id = "soak", effectId = "burn", share = 50, duration = 2).toDto())
        }

    @Test
    fun `Loader should read an apply if ongoing when the effect file holds one`() =
        runTest {
            // Given
            val effectRoot =
                MemoryVfsMix(
                    "effect/stun-long-if-soaked.toml" to "[outcome]\ntype = \"APPLY_IF_ONGOING\"\neffectId = \"stun-long\"\nongoingEffectId = \"soak\"\n\n[timing]\ntype = \"IMMEDIATELY\"\n",
                )["effect"]
            val loader = ResourcesEffectLoader(effectRoot = effectRoot)
            // When
            loader.initResources()
            // Then
            loader.loadEffects().shouldContainExactly(applyIfOngoingEffect(id = "stun-long-if-soaked", effectId = "stun-long", ongoingEffectId = "soak").toDto())
        }

    @Test
    fun `Loader should read a negate actions when the effect file holds one`() =
        runTest {
            // Given
            val effectRoot =
                MemoryVfsMix(
                    "effect/stun.toml" to "[outcome]\ntype = \"NEGATE_ACTIONS\"\n\n[timing]\ntype = \"ON_PLAYER_TURN_STARTED\"\nduration = 1\n",
                )["effect"]
            val loader = ResourcesEffectLoader(effectRoot = effectRoot)
            // When
            loader.initResources()
            // Then
            loader.loadEffects().shouldContainExactly(negateActionsEffect(id = "stun", duration = 1).toDto())
        }

    @Test
    fun `should fail with the file when a field required by the outcome type is missing`() =
        runTest {
            // Given
            val effectRoot =
                MemoryVfsMix(
                    "effect/venom.toml" to "[outcome]\ntype = \"DAMAGE\"\n\n[timing]\ntype = \"IMMEDIATELY\"\n",
                )["effect"]
            val loader = ResourcesEffectLoader(effectRoot = effectRoot)
            // When
            val initialisation = suspend { loader.initResources() }
            // Then
            shouldThrow<IllegalStateException> { initialisation() }.message.shouldContain("effect/venom.toml").shouldContain("'outcome.damage' is required")
        }

    @Test
    fun `should fail with the file when the timing type is unknown`() =
        runTest {
            // Given
            val effectRoot =
                MemoryVfsMix(
                    "effect/venom.toml" to "[outcome]\ntype = \"TELEPORT\"\n\n[timing]\ntype = \"SOMETIMES\"\n",
                )["effect"]
            val loader = ResourcesEffectLoader(effectRoot = effectRoot)
            // When
            val initialisation = suspend { loader.initResources() }
            // Then
            shouldThrow<IllegalStateException> { initialisation() }.message.shouldContain("effect/venom.toml").shouldContain("SOMETIMES")
        }
}
