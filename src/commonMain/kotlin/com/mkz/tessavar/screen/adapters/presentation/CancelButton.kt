package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import korlibs.image.color.RGBA.Companion.invoke
import korlibs.korge.input.onClick
import korlibs.korge.ui.UIButton
import korlibs.korge.ui.uiButton
import korlibs.korge.view.Container

class CancelButton(
    label: String,
    buttonWidth: Double = 190.0,
    val onCancelled: () -> Unit,
) : Container() {
    internal val cancelButton: UIButton =
        uiButton {
            textColor = HudStyle.BUTTON_LABEL
            text = label
            width = buttonWidth
            bgColorOut = HudStyle.BUTTON
            bgColorOver = HudStyle.BUTTON_PRESSED
            background.borderColor = HudStyle.BUTTON_BORDER
            onClick {
                onCancelled()
            }
        }
}
