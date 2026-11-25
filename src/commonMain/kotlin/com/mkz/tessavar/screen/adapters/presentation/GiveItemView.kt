package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.applyUiFont
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.ui.UIButton
import korlibs.korge.ui.uiButton
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.Container
import korlibs.math.geom.Size

class GiveItemView(
    messages: Messages,
    itemName: String,
    onCancelled: () -> Unit,
) : Container() {
    companion object {
        private const val WIDTH = 390.0
    }

    internal val messageView = UnitNameView(Size(WIDTH, 16))
    internal lateinit var cancelButton: UIButton

    init {
        uiVerticalStack(padding = 4.0) {
            addChild(messageView)
            cancelButton =
                uiButton(messages[UiMessage.GIVE_ITEM_CANCEL]) {
                    applyUiFont()
                    width = WIDTH
                    bgColorOut = GameButton.GREY
                    bgColorOver = GameButton.GREY_OVER
                    background.borderColor = Colors.LIGHTGRAY
                    onClick { onCancelled() }
                }
        }
        messageView.display(messages[UiMessage.GIVE_ITEM_MESSAGE, "item" to itemName])
    }
}
