package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import korlibs.image.color.RGBA
import korlibs.korge.ui.uiHorizontalStack
import korlibs.korge.ui.uiSpacing
import korlibs.korge.view.Container
import korlibs.math.geom.Size

class CancelAndConfirmView(
    val onCancelled: () -> Unit,
    val onConfirmed: () -> Unit,
    cancelLabel: String,
    confirmLabel: String,
    confirmColor: RGBA = ConfirmButton.COLOR,
    confirmPressedColor: RGBA = ConfirmButton.PRESSED_COLOR,
) : Container() {
    companion object {
        private const val SPACING = 10.0
        private const val BUTTON_WIDTH = (HudStyle.CONTENT_WIDTH - SPACING) / 2
    }

    internal val cancelButton = CancelButton(label = cancelLabel, buttonWidth = BUTTON_WIDTH, onCancelled = { onCancelled() })
    internal val confirmButton = ConfirmButton(label = confirmLabel, buttonWidth = BUTTON_WIDTH, color = confirmColor, pressedColor = confirmPressedColor, onConfirmed = { onConfirmed() })

    init {
        val layout =
            uiHorizontalStack {
                x = HudStyle.SCREEN_MARGIN
                addChild(cancelButton)
                uiSpacing(Size(SPACING, 0.0))
                addChild(confirmButton)
            }
        addChild(layout)
    }
}
