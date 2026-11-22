package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.ability.adapters.resources.ResourcesAbilityLoader
import com.mkz.tessavar.effect.adapters.resources.ResourcesEffectLoader
import com.mkz.tessavar.localization.adapters.resources.PoFile
import com.mkz.tessavar.localization.adapters.resources.ResourcesLocalizationLoader.Companion.SOURCE_FILE_NAME
import com.mkz.tessavar.localization.adapters.resources.TestCatalogs
import com.mkz.tessavar.screen.adapters.presentation.AbilityInfoView.AbilityInfo
import com.mkz.tessavar.unit.adapters.resources.ResourcesUnitLoader
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AbilityInfoResourcesTest {
    @Test
    fun `AbilityInfoMapper should map every ability when it reads the game resources`() =
        runTest {
            // Given
            val abilities = ResourcesAbilityLoader().also { loader -> loader.initResources() }.loadAbilities()
            val effectsById = ResourcesEffectLoader().also { loader -> loader.initResources() }.loadEffects().associateBy { effect -> effect.id }
            val unitsById = ResourcesUnitLoader().also { loader -> loader.initResources() }.loadUnits().associateBy { unit -> unit.id }
            // When
            val failures =
                abilities.mapNotNull { ability ->
                    runCatching { AbilityInfoMapper.abilityInfo(ability, effectsById::get, unitsById::get) }.exceptionOrNull()?.message
                }
            // Then
            abilities.shouldNotBeEmpty()
            withClue("Abilities the ability info can not tell: $failures") { failures.shouldBeEmpty() }
        }

    @Test
    fun `AbilityInfoMapper should only show effect words the source catalog has when it reads the game resources`() =
        runTest {
            // Given
            val abilities = ResourcesAbilityLoader().also { loader -> loader.initResources() }.loadAbilities()
            val effectsById = ResourcesEffectLoader().also { loader -> loader.initResources() }.loadEffects().associateBy { effect -> effect.id }
            val unitsById = ResourcesUnitLoader().also { loader -> loader.initResources() }.loadUnits().associateBy { unit -> unit.id }
            val sourceKeys =
                PoFile
                    .parse(TestCatalogs.files.getValue(SOURCE_FILE_NAME))
                    .entries
                    .mapNotNull { entry -> entry.context }
                    .toSet()
            // When
            val missing =
                abilities
                    .flatMap { ability -> AbilityInfoMapper.abilityInfo(ability, effectsById::get, unitsById::get).effects }
                    .flatMap(::effectWordKeys)
                    .filter { key -> key !in sourceKeys }
                    .distinct()
            // Then
            withClue("Effect words the ability info shows, missing from $SOURCE_FILE_NAME: $missing") { missing.shouldBeEmpty() }
        }

    private fun effectWordKeys(effect: AbilityInfo.Effect): List<String> =
        when (effect) {
            is AbilityInfo.Effect.DamageOnPlayerTurnStarted -> listOf("effect.${effect.effectId}.verb")
            is AbilityInfo.Effect.DamageAdjacentAllies -> listOf("effect.${effect.effectId}.verb")
            is AbilityInfo.Effect.NegateActions -> listOf("effect.${effect.effectId}.verb")
            is AbilityInfo.Effect.Resist -> listOf("effect.${effect.effectId}.verb", "effect.${effect.resistedEffectId}.name")
            is AbilityInfo.Effect.IfOngoing -> listOf("effect.${effect.ongoingEffectId}.condition") + effectWordKeys(effect.effect)
            else -> emptyList()
        }
}
