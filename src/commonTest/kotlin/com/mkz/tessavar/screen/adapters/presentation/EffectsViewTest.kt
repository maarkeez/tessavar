package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.GameSceneResourcesLoaderMother.loadedResourcesLoader
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class EffectsViewTest : ViewsForTesting() {
    @Test
    fun `Display should be visible when is displayed`() =
        sharedViewsTest {
            // Given
            val effectsView = EffectsView(loadedResourcesLoader())
            // When
            effectsView.display(listOf(EffectsView.Effect("venom-damage")))
            // Then
            effectsView.isVisibleToUser().shouldBeTrue()
        }

    @Test
    fun `Display should display one icon per effect when effects are displayed`() =
        sharedViewsTest {
            // Given
            val effectsView = EffectsView(loadedResourcesLoader())
            // When
            effectsView.display(listOf(EffectsView.Effect("venom-damage"), EffectsView.Effect("venom-on-defeat")))
            // Then
            effectsView.displayedEffectCount() shouldBe 2
        }

    @Test
    fun `Display should hide icons of previous effects when fewer effects are displayed`() =
        sharedViewsTest {
            // Given
            val effectsView = EffectsView(loadedResourcesLoader())
            effectsView.display(listOf(EffectsView.Effect("venom-damage"), EffectsView.Effect("venom-on-defeat"), EffectsView.Effect("burn")))
            // When
            effectsView.display(listOf(EffectsView.Effect("venom-damage")))
            // Then
            effectsView.displayedEffectCount() shouldBe 1
        }

    @Test
    fun `Display should display as many icons as effect slots when there are more effects than slots`() =
        sharedViewsTest {
            // Given
            val effectsView = EffectsView(loadedResourcesLoader())
            val effects = List(effectsView.effectSlotCount() + 2) { EffectsView.Effect("venom-damage") }
            // When
            effectsView.display(effects)
            // Then
            effectsView.displayedEffectCount() shouldBe effectsView.effectSlotCount()
        }

    @Test
    fun `Display should blink the effects marked new when new effects are displayed`() =
        sharedViewsTest {
            // Given
            val effectsView = EffectsView(loadedResourcesLoader())
            // When
            effectsView.display(listOf(EffectsView.Effect("venom-damage", stacks = 2, isNew = true), EffectsView.Effect("soak"), EffectsView.Effect("venom-on-defeat", isNew = true)))
            // Then
            effectsView.blinkingEffects().shouldContainExactly(true, false, true)
        }

    @Test
    fun `Display should stop blinking the previous new effects when is displayed again`() =
        sharedViewsTest {
            // Given
            val effectsView = EffectsView(loadedResourcesLoader())
            effectsView.display(listOf(EffectsView.Effect("venom-damage", isNew = true)))
            // When
            effectsView.display(listOf(EffectsView.Effect("venom-damage")))
            // Then
            effectsView.blinkingEffects().shouldContainExactly(false)
        }

    @Test
    fun `Display should display the plus sign when there are more effects than slots`() =
        sharedViewsTest {
            // Given
            val effectsView = EffectsView(loadedResourcesLoader())
            val effects = List(effectsView.effectSlotCount()) { EffectsView.Effect("venom-damage") }
            // When
            effectsView.display(effects + EffectsView.Effect("venom-on-defeat", isNew = true))
            // Then
            effectsView.isOverflowDisplayed().shouldBeTrue()
        }

    @Test
    fun `Display should not display the plus sign when the effects fit in the slots`() =
        sharedViewsTest {
            // Given
            val effectsView = EffectsView(loadedResourcesLoader())
            val effects = List(effectsView.effectSlotCount()) { EffectsView.Effect("venom-damage") }
            // When
            effectsView.display(effects)
            // Then
            effectsView.isOverflowDisplayed().shouldBeFalse()
        }

    @Test
    fun `Hide should not be visible when is hidden`() =
        sharedViewsTest {
            // Given
            val effectsView = EffectsView(loadedResourcesLoader())
            effectsView.display(listOf(EffectsView.Effect("venom-damage")))
            // When
            effectsView.hide()
            // Then
            effectsView.isVisibleToUser().shouldBeFalse()
        }
}
