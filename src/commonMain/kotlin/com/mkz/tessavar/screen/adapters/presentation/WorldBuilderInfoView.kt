package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.HudFonts
import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import korlibs.image.color.RGBA
import korlibs.image.text.TextAlignment
import korlibs.korge.animate.Animator
import korlibs.korge.animate.animator
import korlibs.korge.animate.block
import korlibs.korge.animate.scaleTo
import korlibs.korge.animate.wait
import korlibs.korge.style.styles
import korlibs.korge.style.textAlignment
import korlibs.korge.style.textColor
import korlibs.korge.style.textFont
import korlibs.korge.style.textSize
import korlibs.korge.ui.UIContainer
import korlibs.korge.ui.uiText
import korlibs.korge.ui.uiVerticalStack
import korlibs.math.geom.Size
import korlibs.time.milliseconds
import korlibs.time.seconds

open class WorldBuilderInfoView(
    private val messages: Messages,
) : UIContainer(Size(width = 390, height = 33)) {
    companion object {
        val ERROR_COLOR = HudStyle.ACCENT_PRESSED
        val INFO_COLOR = HudStyle.MUTED

        const val POP_SCALE = 1.3
        val POP_DURATION = 400.milliseconds
        val MESSAGE_DURATION = 2.seconds
    }

    internal val title =
        uiText(text = messages[UiMessage.WORLD_BUILDER_INFO_TITLE], size = Size(width = width, height = 18)) {
            styles.textColor = HudStyle.TEXT
            styles.textFont = HudFonts.TITLE
            styles.textAlignment = TextAlignment.MIDDLE_LEFT
            styles.textSize = 18.0
        }
    internal val subtitle =
        uiText(text = "", size = Size(width = width, height = 15)) {
            styles.textColor = HudStyle.TEXT
            styles.textFont = HudFonts.TITLE
            styles.textAlignment = TextAlignment.MIDDLE_LEFT
            styles.textSize = 15.0
        }

    init {
        uiVerticalStack {
            addChild(title)
            addChild(subtitle)
        }
    }

    private var sizeText = ""

    internal var messageAnimator: Animator? = null

    open fun displaySize(
        rows: Int,
        columns: Int,
    ) {
        sizeText = messages[UiMessage.WORLD_BUILDER_INFO_SIZE, "rows" to rows, "columns" to columns]
        if (messageAnimator == null) displaySizeText()
    }

    open fun displayError(message: String) = displayMessage(message, ERROR_COLOR)

    open fun displayInfo(message: String) = displayMessage(message, INFO_COLOR)

    private fun displayMessage(
        message: String,
        color: RGBA,
    ) {
        messageAnimator?.cancel()
        subtitle.scale = 1.0
        subtitle.text = message
        subtitle.styles.textColor = color
        messageAnimator =
            animator {
                scaleTo(subtitle, POP_SCALE, time = POP_DURATION / 2)
                scaleTo(subtitle, 1.0, time = POP_DURATION / 2)
                wait(MESSAGE_DURATION)
                block {
                    messageAnimator = null
                    displaySizeText()
                }
            }
    }

    private fun displaySizeText() {
        subtitle.text = sizeText
        subtitle.styles.textColor = HudStyle.TEXT
    }
}
