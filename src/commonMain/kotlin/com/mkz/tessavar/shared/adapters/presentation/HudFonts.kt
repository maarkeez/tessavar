package com.mkz.tessavar.shared.adapters.presentation

import korlibs.image.font.BitmapFont
import korlibs.image.font.CharacterSet
import korlibs.image.font.DefaultTtfFontAsBitmap
import korlibs.image.font.Font
import korlibs.image.font.readTtfFont
import korlibs.image.text.RichTextData
import korlibs.io.file.VfsFile
import korlibs.korge.ui.UIButton

object HudFonts {
    const val ELLIPSIS = "…"
    const val TITLE_FILE = "ui/fonts/alegreya-sc-bold.ttf"
    const val BODY_FILE = "ui/fonts/alegreya-medium.ttf"
    const val ATLAS_SIZE = 48.0
    private const val METRICS_SIZE = 1000.0
    private const val CAPITAL = 'H'

    private val CHARACTERS = CharacterSet.LATIN_ALL + CharacterSet(ELLIPSIS)

    private var title: Font = DefaultTtfFontAsBitmap
    private var body: Font = DefaultTtfFontAsBitmap
    private var titleCapitalHeight: Double = 0.0

    val TITLE: Font get() = title

    val BODY: Font get() = body

    val isLoaded: Boolean get() = TITLE !== DefaultTtfFontAsBitmap && BODY !== DefaultTtfFontAsBitmap

    suspend fun load(root: VfsFile) {
        if (isLoaded) return
        val titleTtf = root[TITLE_FILE].readTtfFont()
        titleCapitalHeight = titleTtf.getGlyphMetrics(METRICS_SIZE, CAPITAL.code, reader = null).bounds.bottom / METRICS_SIZE
        title = BitmapFont(titleTtf, ATLAS_SIZE, CHARACTERS)
        body = BitmapFont(root[BODY_FILE].readTtfFont(), ATLAS_SIZE, CHARACTERS)
    }

    internal fun unload() {
        title = DefaultTtfFontAsBitmap
        body = DefaultTtfFontAsBitmap
        titleCapitalHeight = 0.0
    }

    fun labelLift(textSize: Double): Double = if (isLoaded) (TITLE.getFontMetrics(textSize).ascent - titleCapitalHeight * textSize) / 2 else 0.0
}

fun UIButton.applyHudFont(textSize: Double = HudStyle.BODY) {
    richText = RichTextData(text, font = HudFonts.TITLE, textSize = textSize, color = textColor)
    textView.y = -HudFonts.labelLift(textSize)
}
