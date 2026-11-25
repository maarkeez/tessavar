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

    private val CHARACTERS = CharacterSet.LATIN_ALL + CharacterSet(ELLIPSIS)

    private var title: Font = DefaultTtfFontAsBitmap
    private var body: Font = DefaultTtfFontAsBitmap

    val TITLE: Font get() = title

    val BODY: Font get() = body

    val isLoaded: Boolean get() = TITLE !== DefaultTtfFontAsBitmap && BODY !== DefaultTtfFontAsBitmap

    suspend fun load(root: VfsFile) {
        if (isLoaded) return
        title = BitmapFont(root[TITLE_FILE].readTtfFont(), ATLAS_SIZE, CHARACTERS)
        body = BitmapFont(root[BODY_FILE].readTtfFont(), ATLAS_SIZE, CHARACTERS)
    }

    internal fun unload() {
        title = DefaultTtfFontAsBitmap
        body = DefaultTtfFontAsBitmap
    }
}

fun UIButton.applyHudFont() {
    richText = RichTextData(text, font = HudFonts.TITLE, textSize = textSize, color = textColor)
}
