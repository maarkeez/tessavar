package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.battleUnit.usecases.queries.PreviewCastOngoingEffects
import com.mkz.tessavar.effect.adapters.resources.ResourcesEffectLoader
import com.mkz.tessavar.localization.adapters.resources.PoFile
import com.mkz.tessavar.localization.adapters.resources.ResourcesLocalizationLoader.Companion.SOURCE_FILE_NAME
import com.mkz.tessavar.localization.adapters.resources.TestCatalogs
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class EffectNamesTest {
    @Test
    fun `should have a name when is an effect staying on the battle units`() =
        runTest {
            // Given
            val effects = ResourcesEffectLoader().also { loader -> loader.initResources() }.loadEffects()
            val sourceKeys =
                PoFile
                    .parse(TestCatalogs.files.getValue(SOURCE_FILE_NAME))
                    .entries
                    .mapNotNull { entry -> entry.context }
                    .toSet()
            // When
            val ongoingEffectIds = effects.filter { effect -> effect.timing.type in PreviewCastOngoingEffects.ONGOING_TIMINGS }.map { effect -> effect.id }
            val effectIdsWithoutName = ongoingEffectIds.filter { effectId -> "effect.$effectId.name" !in sourceKeys }
            // Then
            ongoingEffectIds.shouldNotBeEmpty()
            withClue("Add 'effect.<effect id>.name' to $SOURCE_FILE_NAME for the effects $effectIdsWithoutName") { effectIdsWithoutName.shouldBeEmpty() }
        }

    @Test
    fun `EffectInfoMapper should describe every effect staying on the battle units when it reads the game resources`() =
        runTest {
            // Given
            val effects = ResourcesEffectLoader().also { loader -> loader.initResources() }.loadEffects()
            val effectsById = effects.associateBy { effect -> effect.id }
            // When
            val failures =
                effects
                    .filter { effect -> effect.timing.type in PreviewCastOngoingEffects.ONGOING_TIMINGS }
                    .mapNotNull { effect -> runCatching { EffectInfoMapper.effectInfo(effect, listOf(1), isExploring = false, effectsById::get) }.exceptionOrNull()?.message }
            // Then
            withClue("Effects the effect info can not tell: $failures") { failures.shouldBeEmpty() }
        }
}
