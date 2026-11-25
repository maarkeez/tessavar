package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.UiFont
import com.mkz.tessavar.shared.adapters.presentation.UiTheme
import korlibs.image.text.RichTextData
import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container
import korlibs.korge.view.TextBlock
import korlibs.korge.view.position
import korlibs.korge.view.textBlock
import korlibs.math.geom.Size

open class ConfirmDialogView : Container() {
    companion object {
        private const val WIDTH = 390.0
        private const val PADDING = 20.0
        private const val MESSAGE_Y = 95.0
        private const val MESSAGE_HEIGHT = 110.0
        private const val MESSAGE_TEXT_SIZE = 18.0
    }

    internal val messageText: TextBlock =
        textBlock(RichTextData(""), align = TextAlignment.MIDDLE_CENTER, size = Size(WIDTH - 2 * PADDING, MESSAGE_HEIGHT)) {
            position(PADDING, MESSAGE_Y)
            wordWrap = true
        }

    open fun display(message: String) {
        messageText.text = RichTextData(message, font = UiFont.current, textSize = MESSAGE_TEXT_SIZE, color = UiTheme.current.text)
    }
}
