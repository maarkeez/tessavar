package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.gameButton
import korlibs.korge.input.onClick
import korlibs.korge.ui.UIButton
import korlibs.korge.view.Container
import korlibs.math.geom.Size

class FinishPlayerTurnView(
    messages: Messages,
    onPlayerTurnFinished: () -> Unit,
) : Container() {
    internal val button =
        gameButton(messages[UiMessage.FINISH_PLAYER_TURN], Size(390.0, UIButton.DEFAULT_SIZE.height), color = HudStyle.ACCENT, overColor = HudStyle.ACCENT_PRESSED) {
            onClick {
                onPlayerTurnFinished()
            }
        }

    var isEnabled: Boolean
        get() = button.enabled
        set(value) {
            button.enabled = value
        }
}
