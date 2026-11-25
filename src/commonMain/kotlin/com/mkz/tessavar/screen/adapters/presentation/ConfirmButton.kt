package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.UiTheme
import com.mkz.tessavar.shared.adapters.presentation.applyUiFont
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.color.RGBA.Companion.invoke
import korlibs.korge.input.onClick
import korlibs.korge.ui.UIButton
import korlibs.korge.ui.uiButton
import korlibs.korge.view.Container

class ConfirmButton(
    label: String,
    buttonWidth: Double = 190.0,
    color: RGBA = COLOR,
    pressedColor: RGBA = PRESSED_COLOR,
    textColor: RGBA = Colors.WHITE,
    val onConfirmed: () -> Unit,
) : Container() {
    companion object {
        val COLOR get() = UiTheme.current.accent
        val PRESSED_COLOR get() = UiTheme.current.accentPressed
    }

    internal val confirmButton: UIButton =
        uiButton {
            text = label
            this.textColor = textColor
            applyUiFont()
            width = buttonWidth
            bgColorOut = color
            bgColorOver = pressedColor
            background.borderColor = GameButton.BORDER
            onClick {
                onConfirmed()
            }
        }
}
