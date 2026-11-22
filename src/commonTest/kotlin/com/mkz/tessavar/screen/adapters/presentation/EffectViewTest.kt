package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.GameSceneResourcesLoaderMother.loadedResourcesLoader
import com.mkz.tessavar.shared.adapters.presentation.nextFrame
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import korlibs.korge.tests.ViewsForTesting
import korlibs.math.geom.Size
import kotlin.test.Test

class EffectViewTest : ViewsForTesting() {
    private companion object {
        const val BLINK_FRAMES = 150
    }

    private val size = Size(width = 16, height = 16)

    @Test
    fun `DisplayEffect should display effect icon when effect is displayed`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            // When
            effectView.displayEffect(EffectsView.Effect("venom-damage"))
            // Then
            effectView.isEffectIconDisplayed().shouldBeTrue()
        }

    @Test
    fun `DisplayEffect should replace previous effect icon when another effect is displayed`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            effectView.displayEffect(EffectsView.Effect("venom-damage"))
            // When
            effectView.displayEffect(EffectsView.Effect("venom-on-defeat"))
            // Then
            effectView.effectIconCount() shouldBe 1
        }

    @Test
    fun `DisplayEffect should blink the effect icon when it is a new effect`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            addChild(effectView)
            // When
            effectView.displayEffect(EffectsView.Effect("venom-damage", isNew = true))
            // Then
            val alphas = List(BLINK_FRAMES) { effectView.iconAlpha().also { nextFrame() } }
            effectView.isBlinking().shouldBeTrue()
            alphas.min() shouldBeLessThan 0.75
            alphas.max() shouldBeGreaterThan 0.75
        }

    @Test
    fun `DisplayEffect should not blink the effect icon when it is not a new effect`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            addChild(effectView)
            // When
            effectView.displayEffect(EffectsView.Effect("venom-damage"))
            // Then
            val alphas = List(BLINK_FRAMES) { effectView.iconAlpha().also { nextFrame() } }
            effectView.isBlinking().shouldBeFalse()
            alphas.distinct().shouldContainExactly(1.0)
        }

    @Test
    fun `Hide should stop blinking the effect icon when is hidden`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            addChild(effectView)
            effectView.displayEffect(EffectsView.Effect("venom-damage", isNew = true))
            // When
            effectView.hide()
            // Then
            effectView.isBlinking().shouldBeFalse()
            effectView.iconAlpha() shouldBe 1.0
        }

    @Test
    fun `DisplayEffect should fail when effect has no image`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            // When
            val effectDisplay = { effectView.displayEffect(EffectsView.Effect("unknown-effect")) }
            // Then
            shouldThrow<IllegalStateException> { effectDisplay() }.message.shouldContain("effect/unknown-effect.png")
        }

    @Test
    fun `Hide should not display effect icon when is hidden`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            effectView.displayEffect(EffectsView.Effect("venom-damage"))
            // When
            effectView.hide()
            // Then
            effectView.isEffectIconDisplayed().shouldBeFalse()
        }

    @Test
    fun `DisplayEffect should display the remaining duration when the effect has one`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            // When
            effectView.displayEffect(EffectsView.Effect("venom-damage", remainingDuration = 4))
            // Then
            effectView.remainingDurationText() shouldBe "4"
        }

    @Test
    fun `DisplayEffect should not display a remaining duration when the effect is on defeated`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            effectView.displayEffect(EffectsView.Effect("venom-damage", remainingDuration = 4))
            // When
            effectView.displayEffect(EffectsView.Effect("venom-on-defeat", remainingDuration = null))
            // Then
            effectView.remainingDurationText().shouldBeNull()
        }

    @Test
    fun `DisplayEffect should display the count when the effect is stacked`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            // When
            effectView.displayEffect(EffectsView.Effect("venom-damage", stacks = 2, remainingDuration = 4))
            // Then
            effectView.stacksText() shouldBe "×2"
        }

    @Test
    fun `DisplayEffect should not display a count when the effect is received once`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            // When
            effectView.displayEffect(EffectsView.Effect("venom-damage", remainingDuration = 4))
            // Then
            effectView.stacksText().shouldBeNull()
        }

    @Test
    fun `Select should ring the icon when it is selected`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            effectView.displayEffect(EffectsView.Effect("venom-damage", remainingDuration = 4))
            // When
            effectView.select()
            // Then
            effectView.chipStroke() shouldBe HudStyle.SELECTION
        }

    @Test
    fun `Unselect should ring the icon as usual when it is unselected`() =
        sharedViewsTest {
            // Given
            val effectView = EffectView(size, loadedResourcesLoader())
            effectView.displayEffect(EffectsView.Effect("venom-damage", remainingDuration = 4))
            effectView.select()
            // When
            effectView.unselect()
            // Then
            effectView.chipStroke() shouldBe HudStyle.STROKE
        }
}
