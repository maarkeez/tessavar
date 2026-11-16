package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class WorldBuilderCallToActionViewTest : ViewsForTesting() {
    private val actions = mutableListOf<String>()

    @Test
    fun `DisplayLeaveConfirmation should display staying and leaving when it is displayed`() =
        sharedViewsTest {
            // Given
            val callToActionView = WorldBuilderCallToActionView(MessagesMother.messages())
            // When
            callToActionView.displayLeaveConfirmation(onLeft = {}, onStayed = {})
            // Then
            callToActionView.actionLabels() shouldBe ("Stay" to "Leave")
        }

    @Test
    fun `DisplayLeaveConfirmation should leave and stay when they are clicked`() =
        sharedViewsTest {
            // Given
            val callToActionView = WorldBuilderCallToActionView(MessagesMother.messages())
            callToActionView.displayLeaveConfirmation(onLeft = { actions += "leave" }, onStayed = { actions += "stay" })
            addChild(callToActionView)
            // When
            clickPrimaryAction(callToActionView)
            clickSecondaryAction(callToActionView)
            // Then
            actions.shouldContainExactly("leave", "stay")
        }

    @Test
    fun `Clear should remove the leave confirmation when it is cleared`() =
        sharedViewsTest {
            // Given
            val callToActionView = WorldBuilderCallToActionView(MessagesMother.messages())
            callToActionView.displayLeaveConfirmation(onLeft = {}, onStayed = {})
            // When
            callToActionView.clear()
            // Then
            callToActionView.actionLabels() shouldBe null
        }
}
