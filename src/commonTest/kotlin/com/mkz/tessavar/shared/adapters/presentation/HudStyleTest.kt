package com.mkz.tessavar.shared.adapters.presentation

import io.kotest.matchers.collections.shouldBeEmpty
import korlibs.image.color.RGBA
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test

class HudStyleTest {
    @Test
    fun `HudStyle should draw every text colour at a contrast of at least four and a half to one when it is on a panel`() {
        // Given
        val pairs = HudStyle.TEXT_COLORS.flatMap { text -> HudStyle.PANELS.map { panel -> text to panel } }
        // When
        val unreadable = pairs.filter { (text, panel) -> contrast(text, panel) < MINIMUM_CONTRAST }
        // Then
        unreadable.map { (text, panel) -> "${text.hexString} on ${panel.hexString}: ${contrast(text, panel)}" }.shouldBeEmpty()
    }

    @Test
    fun `HudStyle should draw every button label at a contrast of at least four and a half to one when it is on its button`() {
        // Given
        val pairs = HudStyle.BUTTON_LABELS
        // When
        val unreadable = pairs.filter { (button, label) -> contrast(label, button) < MINIMUM_CONTRAST }
        // Then
        unreadable.map { (button, label) -> "${label.hexString} on ${button.hexString}: ${contrast(label, button)}" }.shouldBeEmpty()
    }

    private companion object {
        const val MINIMUM_CONTRAST = 4.5

        fun contrast(
            first: RGBA,
            second: RGBA,
        ): Double {
            val firstLuminance = luminance(first)
            val secondLuminance = luminance(second)
            return (max(firstLuminance, secondLuminance) + 0.05) / (min(firstLuminance, secondLuminance) + 0.05)
        }

        fun luminance(color: RGBA): Double = 0.2126 * channel(color.r) + 0.7152 * channel(color.g) + 0.0722 * channel(color.b)

        fun channel(value: Int): Double {
            val ratio = value / 255.0
            return if (ratio <= 0.03928) ratio / 12.92 else ((ratio + 0.055) / 1.055).pow(2.4)
        }
    }
}
