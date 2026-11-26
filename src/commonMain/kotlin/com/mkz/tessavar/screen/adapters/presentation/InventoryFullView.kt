package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.applyHudFont
import korlibs.korge.input.onClick
import korlibs.korge.ui.UIButton
import korlibs.korge.ui.UIContainer
import korlibs.korge.ui.uiButton
import korlibs.korge.ui.uiHorizontalStack
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.image
import korlibs.math.geom.Size

class InventoryFullView(
    messages: Messages,
    foundItemName: String,
    foundItemSellPrice: Int,
    foundItemId: String,
    resourcesLoader: GameSceneResourcesLoader,
    onLeft: () -> Unit,
) : Container() {
    companion object {
        private const val WIDTH = HudStyle.CONTENT_WIDTH
        private const val ICON_SIZE = 32.0
        private const val ICON_SPACING = 8.0
    }

    internal val titleView = UnitNameView(Size(WIDTH - ICON_SIZE - ICON_SPACING, 14))
    internal val messageView = UnitNameView(Size(WIDTH - ICON_SIZE - ICON_SPACING, 14))
    internal var foundItemIcon: Image? = null
    internal lateinit var leaveButton: UIButton

    init {
        uiVerticalStack(padding = 6.0) {
            x = HudStyle.SCREEN_MARGIN
            uiHorizontalStack(padding = ICON_SPACING) {
                val iconBox = UIContainer(Size(ICON_SIZE, ICON_SIZE))
                resourcesLoader.bitmapOrNull("lootableItem/$foundItemId.png")?.let { itemBitmap ->
                    foundItemIcon =
                        iconBox.image(itemBitmap) {
                            smoothing = false
                            scale = ICON_SIZE / itemBitmap.width
                        }
                }
                addChild(iconBox)
                uiVerticalStack(padding = 2.0) {
                    addChild(titleView)
                    addChild(messageView)
                }
            }
            leaveButton =
                uiButton(messages[UiMessage.INVENTORY_FULL_LEAVE]) {
                    textColor = HudStyle.BUTTON_LABEL
                    applyHudFont()
                    width = WIDTH
                    bgColorOut = HudStyle.BUTTON
                    bgColorOver = HudStyle.BUTTON_PRESSED
                    background.borderColor = HudStyle.BUTTON_BORDER
                    onClick { onLeft() }
                }
        }
        titleView.display(messages[UiMessage.INVENTORY_FULL_TITLE])
        messageView.display(messages[UiMessage.INVENTORY_FULL_MESSAGE, "item" to foundItemName, "sellPrice" to messages.gold(foundItemSellPrice)])
    }
}
