package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.EffectInfoView.EffectInfo
import com.mkz.tessavar.screen.adapters.presentation.GameSceneResourcesLoaderMother.loadedResourcesLoader
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class EffectInfoViewTest : ViewsForTesting() {
    private val venom = EffectInfo("venom-damage", stacks = 1, sentence = EffectInfo.Sentence.DamageOnPlayerTurnStarted(3), remainingDurations = listOf(4), isExploring = false)

    private suspend fun effectInfoView(languageCode: String = "en") = EffectInfoView(AbilityInfoView.WIDTH, loadedResourcesLoader(), MessagesMother.messages(languageCode))

    @Test
    fun `Display should display the name, what the effect does and the turns it has left when it is displayed`() =
        sharedViewsTest {
            // Given
            val effectInfoView = effectInfoView()
            // When
            effectInfoView.display(venom)
            // Then
            effectInfoView.visible.shouldBeTrue()
            effectInfoView.title() shouldBe "Venom"
            effectInfoView.sentence() shouldBe "3 damage at the start of each turn."
            effectInfoView.remainingDurations() shouldBe "4 turns left"
        }

    @Test
    fun `Display should display the name with its count when it is stacked`() =
        sharedViewsTest {
            // Given
            val effectInfoView = effectInfoView()
            // When
            effectInfoView.display(venom.copy(stacks = 2, remainingDurations = listOf(4, 2)))
            // Then
            effectInfoView.title() shouldBe "Venom x2"
        }

    @Test
    fun `Display should display one remaining duration per stack when it is stacked`() =
        sharedViewsTest {
            // Given
            val effectInfoView = effectInfoView()
            // When
            effectInfoView.display(venom.copy(stacks = 2, remainingDurations = listOf(4, 1)))
            // Then
            effectInfoView.remainingDurations() shouldBe "4 turns left · 1 turn left"
        }

    @Test
    fun `Display should display seconds when exploring`() =
        sharedViewsTest {
            // Given
            val effectInfoView = effectInfoView()
            // When
            effectInfoView.display(venom.copy(isExploring = true, remainingDurations = listOf(3)))
            // Then
            effectInfoView.sentence() shouldBe "3 damage each second."
            effectInfoView.remainingDurations() shouldBe "3 seconds left"
        }

    @Test
    fun `Display should keep the battle sentence of a stun when exploring`() =
        sharedViewsTest {
            // Given
            val effectInfoView = effectInfoView()
            // When
            effectInfoView.display(EffectInfo("stun", stacks = 1, sentence = EffectInfo.Sentence.NegateActions, remainingDurations = listOf(1), isExploring = true))
            // Then
            effectInfoView.title() shouldBe "Stun"
            effectInfoView.sentence() shouldBe "Loses its actions at the start of each turn."
            effectInfoView.remainingDurations() shouldBe "1 second left"
        }

    @Test
    fun `Display should name the resisted effect when the effect resists`() =
        sharedViewsTest {
            // Given
            val effectInfoView = effectInfoView()
            // When
            effectInfoView.display(EffectInfo("soak", stacks = 1, sentence = EffectInfo.Sentence.Resist("burn", 50), remainingDurations = listOf(2), isExploring = false))
            // Then
            effectInfoView.title() shouldBe "Soak"
            effectInfoView.sentence() shouldBe "50% less damage from burn."
        }

    @Test
    fun `Display should not display a time line when the effect is on defeated`() =
        sharedViewsTest {
            // Given
            val effectInfoView = effectInfoView()
            // When
            effectInfoView.display(
                EffectInfo("venom-on-defeat", stacks = 1, sentence = EffectInfo.Sentence.OnDefeatedDamageAdjacentAllies(3, 5), remainingDurations = emptyList(), isExploring = false),
            )
            // Then
            effectInfoView.title() shouldBe "Venom burst"
            effectInfoView.sentence() shouldBe "When defeated: poisons its adjacent allies (3 damage each turn for 5 turns)."
            effectInfoView.remainingDurations().shouldBeNull()
        }

    @Test
    fun `Display should display the effect info in Spanish when the language is Spanish`() =
        sharedViewsTest {
            // Given
            val effectInfoView = effectInfoView(languageCode = "es")
            // When
            effectInfoView.display(venom.copy(stacks = 2, remainingDurations = listOf(4, 1)))
            // Then
            effectInfoView.title() shouldBe "Veneno x2"
            effectInfoView.sentence() shouldBe "3 de daño al empezar cada turno."
            effectInfoView.remainingDurations() shouldBe "quedan 4 turnos · queda 1 turno"
        }

    @Test
    fun `Hide should not be visible when is hidden`() =
        sharedViewsTest {
            // Given
            val effectInfoView = effectInfoView()
            effectInfoView.display(venom)
            // When
            effectInfoView.hide()
            // Then
            effectInfoView.visible.shouldBeFalse()
        }
}
