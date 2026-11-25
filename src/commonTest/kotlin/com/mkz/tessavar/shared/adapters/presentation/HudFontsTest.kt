package com.mkz.tessavar.shared.adapters.presentation

import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import korlibs.image.font.BitmapFont
import korlibs.image.font.DefaultTtfFontAsBitmap
import korlibs.korge.tests.ViewsForTesting
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
}
