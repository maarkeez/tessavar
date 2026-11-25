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
) : UIButton(size, label) {
    companion object {
        val GREY: RGBA get() = UiTheme.current.button
        val GREY_OVER get() = UiTheme.current.buttonOver
        val LABEL: RGBA get() = UiTheme.current.buttonLabel
        val BORDER: RGBA get() = UiTheme.current.buttonBorder
        val DISABLED get() = UiTheme.current.disabled
        val DISABLED_LABEL get() = UiTheme.current.disabledLabel
        val DISABLED_BORDER get() = UiTheme.current.disabledBorder
    }

    init {
        bgColorOut = color
        bgColorOver = overColor
        bgColorDisabled = DISABLED
        background.borderColor = BORDER
        applyUiFont()
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
        textColor = if (isDisabled) DISABLED_LABEL else LABEL
        background.borderColor = if (isDisabled) DISABLED_BORDER else BORDER
    }
}

fun Container.gameButton(
    label: String,
    size: Size,
    color: RGBA = GameButton.GREY,
    overColor: RGBA = GameButton.GREY_OVER,
    block: GameButton.() -> Unit = {},
): GameButton = GameButton(size, label, color, overColor).addTo(this).apply(block)
