package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class CancelAndConfirmViewTest : ViewsForTesting() {
    @Test
    fun `Init should lay cancel and confirm out 12 px in from each side of the screen when is initialized`() =
        sharedViewsTest {
            // When
            val cancelAndConfirmView = CancelAndConfirmView(cancelLabel = "Cancel", confirmLabel = "Confirm", onCancelled = {}, onConfirmed = {})
            // Then
            cancelAndConfirmView.buttonSides() shouldBe (12.0 to 378.0)
            cancelAndConfirmView.buttonWidths().shouldContainExactly(178.0, 178.0)
        }

    @Test
    fun `CancelClick should only cancel when cancel button is clicked`() =
        sharedViewsTest {
            // Given
            var cancelled = false
            var confirmed = false
            val cancelAndConfirmView = CancelAndConfirmView(cancelLabel = "Cancel", confirmLabel = "Confirm", onCancelled = { cancelled = true }, onConfirmed = { confirmed = true })
            addChild(cancelAndConfirmView)
            // When
            clickCancel(cancelAndConfirmView)
            // Then
            cancelled.shouldBeTrue()
            confirmed.shouldBeFalse()
        }

    @Test
    fun `ConfirmClick should only confirm when confirm button is clicked`() =
        sharedViewsTest {
            // Given
            var cancelled = false
            var confirmed = false
            val cancelAndConfirmView = CancelAndConfirmView(cancelLabel = "Cancel", confirmLabel = "Confirm", onCancelled = { cancelled = true }, onConfirmed = { confirmed = true })
            addChild(cancelAndConfirmView)
            // When
            clickConfirm(cancelAndConfirmView)
            // Then
            confirmed.shouldBeTrue()
            cancelled.shouldBeFalse()
        }
}
