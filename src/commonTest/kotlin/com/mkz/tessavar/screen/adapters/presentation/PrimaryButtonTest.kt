package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class PrimaryButtonTest : ViewsForTesting() {
    @Test
    fun `Init should display confirm text when is initialized`() =
        sharedViewsTest {
            // Given
            // When
            val primaryButton = PrimaryButton(label = "Confirm", onConfirmed = {})
            // Then
            primaryButton.buttonText() shouldBe "Confirm"
        }

    @Test
    fun `ButtonClick should confirm when button is clicked`() =
        sharedViewsTest {
            // Given
            var confirmed = false
            val primaryButton = PrimaryButton(label = "Confirm", onConfirmed = { confirmed = true })
            addChild(primaryButton)
            // When
            clickPrimary(primaryButton)
            // Then
            confirmed.shouldBeTrue()
        }

    @Test
    fun `PrimaryButton should write its label in the text color when it is given one`() =
        sharedViewsTest {
            // When
            val primaryButton = PrimaryButton(label = "Wishlist", textColor = HudStyle.PANEL, onConfirmed = {})
            // Then
            primaryButton.buttonTextColor() shouldBe HudStyle.PANEL
        }
}
