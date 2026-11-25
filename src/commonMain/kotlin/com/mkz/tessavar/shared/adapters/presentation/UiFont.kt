package com.mkz.tessavar.shared.adapters.presentation

import korlibs.datastructure.IntMap
import korlibs.datastructure.fastForEach
import korlibs.image.font.BitmapFont
import korlibs.image.font.DefaultTtfFontAsBitmap
import korlibs.image.font.Font
import korlibs.image.font.readTtfFont
import korlibs.image.text.RichTextData
import korlibs.io.file.VfsFile
import korlibs.korge.ui.UIButton

/**
 * PROTOTYPE: the font of the [UiTheme] selected. Each theme's font is drawn once into a bitmap font at [ATLAS_SIZE],
 * and told it is [UiTheme.textScale] times smaller than that, so every text is drawn that much bigger than the size its
 * view asks for, without changing any view.
 */
object UiFont {
    private const val ATLAS_SIZE = 48.0

    private val fonts = mutableMapOf<String, Font>()

    val current: Font
        get() = fonts[UiTheme.current.id] ?: DefaultTtfFontAsBitmap

    suspend fun load(root: VfsFile) {
        UiTheme.ALL.forEach { theme ->
            if (theme.id in fonts) return@forEach
            val file = theme.fontFile ?: return@forEach
            runCatching {
                val ttf = root["ui/fonts/$file"].readTtfFont()
                fonts[theme.id] = scaled(BitmapFont(ttf, ATLAS_SIZE), theme.textScale)
            }.onFailure { error -> println("UiFont: could not load $file: $error") }
        }
    }

    private fun scaled(
        font: BitmapFont,
        scale: Double,
    ): BitmapFont =
        BitmapFont(
            fontSize = font.fontSize / scale,
            lineHeight = font.lineHeight,
            base = font.base,
            glyphs =
                font.glyphs.let { glyphs ->
                    IntMap<BitmapFont.Glyph>().also { scaledGlyphs ->
                        glyphs.fastForEach { codePoint, glyph ->
                            scaledGlyphs[codePoint] =
                                glyph.copy(fontSize = glyph.fontSize / scale)
                        }
                    }
                },
            kernings = font.kernings,
            name = font.name,
        )
}

fun UIButton.applyUiFont() {
    richText = RichTextData(text, font = UiFont.current, textSize = textSize, color = textColor)
}
