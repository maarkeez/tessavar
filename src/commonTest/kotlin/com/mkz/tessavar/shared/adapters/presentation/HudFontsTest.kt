package com.mkz.tessavar.shared.adapters.presentation

import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import korlibs.image.font.BitmapFont
import korlibs.image.font.DefaultTtfFontAsBitmap
import korlibs.image.font.readTtfFont
import korlibs.korge.tests.ViewsForTesting
import korlibs.math.geom.Size
import kotlin.test.Test

class HudFontsTest : ViewsForTesting() {
    @Test
    fun `HudFonts should load the title and body fonts when it reads the game resources`() =
        sharedViewsTest {
            // Given
            val root = ResourcesBundle.root
            // When
            HudFonts.load(root)
            // Then
            HudFonts.isLoaded.shouldBeTrue()
            HudFonts.TITLE.shouldBeInstanceOf<BitmapFont>()
            HudFonts.BODY.shouldBeInstanceOf<BitmapFont>()
            HudFonts.TITLE shouldNotBe HudFonts.BODY
        }

    @Test
    fun `HudFonts should fall back to the default font when the fonts are not loaded`() =
        sharedViewsTest {
            // Given
            HudFonts.unload()
            // When
            val fonts = listOf(HudFonts.TITLE, HudFonts.BODY)
            // Then
            try {
                fonts.forEach { font -> font shouldBeSameInstanceAs DefaultTtfFontAsBitmap }
            } finally {
                HudFonts.load(ResourcesBundle.root)
            }
        }

    @Test
    fun `HudFonts should draw the ellipsis of a cut text when it reads the game resources`() =
        sharedViewsTest {
            // Given
            HudFonts.load(ResourcesBundle.root)
            // When
            val fonts = listOf(HudFonts.TITLE, HudFonts.BODY).map { font -> font as BitmapFont }
            // Then
            fonts.forEach { font -> font.glyphs[HudFonts.ELLIPSIS.single().code].shouldNotBeNull() }
        }

    @Test
    fun `applyHudFont should centre the capitals of a label on its button when the label is at the body size`() =
        sharedViewsTest {
            // Given
            val button = gameButton("Close", Size(80, BUTTON_HEIGHT))
            // When
            button.applyHudFont(HudStyle.BODY)
            // Then
            capitalsMiddle(button, HudStyle.BODY) shouldBe (BUTTON_HEIGHT / 2 plusOrMinus 0.5)
        }

    @Test
    fun `applyHudFont should centre the capitals of a label on its button when the label is at the title size`() =
        sharedViewsTest {
            // Given
            val button = gameButton("Close", Size(120, BUTTON_HEIGHT))
            // When
            button.applyHudFont(HudStyle.TITLE)
            // Then
            capitalsMiddle(button, HudStyle.TITLE) shouldBe (BUTTON_HEIGHT / 2 plusOrMinus 0.5)
        }

    private suspend fun capitalsMiddle(
        button: GameButton,
        textSize: Double,
    ): Double {
        val capitalHeight =
            ResourcesBundle.root[HudFonts.TITLE_FILE]
                .readTtfFont()
                .getGlyphMetrics(1000.0, 'H'.code, reader = null)
                .bounds.bottom / 1000.0 * textSize
        return button.labelBaseline() - capitalHeight / 2
    }

    private companion object {
        const val BUTTON_HEIGHT = 32.0
    }
}
