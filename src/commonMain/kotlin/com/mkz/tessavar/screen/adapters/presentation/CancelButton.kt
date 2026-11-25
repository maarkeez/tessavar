package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.applyUiFont
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
            text = label
            width = buttonWidth
            applyUiFont()
            bgColorOut = GameButton.GREY
            bgColorOver = GameButton.GREY_OVER
            background.borderColor = GameButton.BORDER
            onClick {
                onCancelled()
            }
        }
}
