package com.mkz.tessavar.shared.adapters.presentation

import korlibs.image.color.RGBA
import korlibs.korge.ui.UIButton
import korlibs.korge.view.Container
import korlibs.korge.view.addTo
import korlibs.math.geom.Point
import korlibs.math.geom.Size

class GameButton(
    size: Size,
    label: String,
    color: RGBA = GREY,
    overColor: RGBA = GREY_OVER,
    private val labelColor: RGBA = LABEL,
) : UIButton(size, label) {
    companion object {
        val GREY: RGBA = HudStyle.BUTTON
        val GREY_OVER: RGBA = HudStyle.BUTTON_PRESSED
        val LABEL: RGBA = HudStyle.BUTTON_LABEL
        val BORDER: RGBA = HudStyle.BUTTON_BORDER
        val DISABLED: RGBA = HudStyle.DISABLED
        val DISABLED_LABEL: RGBA = HudStyle.DISABLED_LABEL
        val DISABLED_BORDER: RGBA = HudStyle.DISABLED_BORDER
    }

    init {
        bgColorOut = color
        bgColorOver = overColor
        bgColorDisabled = DISABLED
        background.borderColor = BORDER
        textColor = labelColor
        applyHudFont()
    }

    override fun updatedUIButton(
        down: Boolean?,
        over: Boolean?,
        enable: Boolean?,
        pos: Point,
        immediate: Boolean,
    ) {
        super.updatedUIButton(down, over, enable, pos, immediate)
        val isDisabled = enable == false || !enabled
        textColor = if (isDisabled) DISABLED_LABEL else labelColor
        background.borderColor = if (isDisabled) DISABLED_BORDER else BORDER
    }
}

fun Container.gameButton(
    label: String,
    size: Size,
    color: RGBA = GameButton.GREY,
    overColor: RGBA = GameButton.GREY_OVER,
    labelColor: RGBA = GameButton.LABEL,
    block: GameButton.() -> Unit = {},
): GameButton = GameButton(size, label, color, overColor, labelColor).addTo(this).apply(block)
