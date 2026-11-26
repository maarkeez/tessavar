package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.applyHudFont
import korlibs.image.color.RGBA
import korlibs.image.color.RGBA.Companion.invoke
import korlibs.korge.input.onClick
import korlibs.korge.ui.UIButton
import korlibs.korge.ui.uiButton
import korlibs.korge.view.Container

class PrimaryButton(
    label: String,
    buttonWidth: Double = 190.0,
    color: RGBA = COLOR,
    pressedColor: RGBA = PRESSED_COLOR,
    textColor: RGBA = HudStyle.BUTTON_LABEL,
    val onConfirmed: () -> Unit,
) : Container() {
    companion object {
        val COLOR = HudStyle.ACCENT
        val PRESSED_COLOR = HudStyle.ACCENT_PRESSED
    }

    internal val primaryButton: UIButton =
        uiButton {
            text = label
            this.textColor = textColor
            applyHudFont()
            width = buttonWidth
            bgColorOut = color
            bgColorOver = pressedColor
            background.borderColor = HudStyle.BUTTON_BORDER
            onClick {
                onConfirmed()
            }
        }
}
