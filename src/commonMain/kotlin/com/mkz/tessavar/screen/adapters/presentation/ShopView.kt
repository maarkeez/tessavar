package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.HudFonts
import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.gameButton
import korlibs.image.color.Colors
import korlibs.image.text.TextAlignment
import korlibs.korge.input.SwipeDirection
import korlibs.korge.input.onClick
import korlibs.korge.input.onSwipe
import korlibs.korge.style.styles
import korlibs.korge.style.textAlignment
import korlibs.korge.style.textColor
import korlibs.korge.style.textFont
import korlibs.korge.style.textSize
import korlibs.korge.ui.UIContainer
import korlibs.korge.ui.UIText
import korlibs.korge.ui.uiText
import korlibs.korge.view.Container
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.math.geom.Size

open class ShopView(
    resourcesLoader: GameSceneResourcesLoader,
    private val messages: Messages,
) : Container() {
    companion object {
        const val STORAGE_SPACES = 12
        const val STORAGE_SPACES_PER_ROW = 6
        private const val STORAGE_SPACE_SIZE = 56.0
        private const val STORAGE_SPACE_SPACING = 6.0
        private const val SCREEN_WIDTH = HudStyle.CONTENT_WIDTH + 2 * HudStyle.SCREEN_MARGIN
        private const val WIDTH = HudStyle.CONTENT_WIDTH
        private const val LEFT = HudStyle.SCREEN_MARGIN
        private const val BAR_HEIGHT = 32.0
        private const val ARROW_WIDTH = 40.0
        private const val HEIGHT = 300.0

        private const val GAP = 29.0
        private const val STORAGE_SPACES_Y = GAP + BAR_HEIGHT + GAP
        private const val ITEM_Y = STORAGE_SPACES_Y + 2 * STORAGE_SPACE_SIZE + STORAGE_SPACE_SPACING + GAP

        const val SWIPE_THRESHOLD = 40.0
    }

    enum class Mode { BUY, SELL }

    data class StorageSpace(
        val lootableItemId: String,
        val isEquipped: Boolean,
        val isTradeable: Boolean,
    )

    data class Buyer(
        val name: String,
        val gold: Int,
        val usedStorageSpace: Int,
        val maximumStorageSpace: Int,
        val partyIndex: Int,
        val partySize: Int,
    )

    interface Delegate {
        fun shopStorageSpaceSelected(index: Int)

        fun shopBuyerStepped(step: Int)
    }

    private var delegate: Delegate? = null
    internal lateinit var previousBuyerButton: GameButton
    internal lateinit var nextBuyerButton: GameButton
    internal lateinit var buyerText: UIText
    internal val storageSpaceViews: List<StorageSpaceView> = List(STORAGE_SPACES) { StorageSpaceView(Size(STORAGE_SPACE_SIZE, STORAGE_SPACE_SIZE), resourcesLoader) }
    internal lateinit var itemNameText: UIText
    internal lateinit var itemPriceText: UIText

    init {
        solidRect(Size(SCREEN_WIDTH, HEIGHT), Colors.TRANSPARENT)
        addChild(header().position(LEFT, GAP))
        storageSpaceViews.chunked(STORAGE_SPACES_PER_ROW).forEachIndexed { rowIndex, rowStorageSpaces ->
            rowStorageSpaces.forEachIndexed { columnIndex, storageSpaceView ->
                addChild(storageSpaceView)
                storageSpaceView.position(LEFT + columnIndex * (STORAGE_SPACE_SIZE + STORAGE_SPACE_SPACING), STORAGE_SPACES_Y + rowIndex * (STORAGE_SPACE_SIZE + STORAGE_SPACE_SPACING))
            }
        }
        itemNameText =
            uiText("", size = Size(WIDTH, BAR_HEIGHT)) {
                styles.textColor = HudStyle.TEXT
                styles.textFont = HudFonts.TITLE
                styles.textSize = HudStyle.BODY
                styles.textAlignment = TextAlignment.MIDDLE_LEFT
                position(LEFT, ITEM_Y)
            }
        itemPriceText =
            uiText("", size = Size(WIDTH, BAR_HEIGHT)) {
                styles.textColor = HudStyle.TEXT
                styles.textFont = HudFonts.TITLE
                styles.textSize = HudStyle.BODY
                styles.textAlignment = TextAlignment.MIDDLE_RIGHT
                position(LEFT, ITEM_Y)
            }
        storageSpaceViews.forEachIndexed { index, storageSpaceView -> storageSpaceView.setOnSelected { delegate?.shopStorageSpaceSelected(index) } }
        onSwipe(threshold = SWIPE_THRESHOLD) { swipe ->
            when (swipe.direction) {
                SwipeDirection.LEFT -> swiped(-1)
                SwipeDirection.RIGHT -> swiped(1)
                else -> Unit
            }
        }
    }

    open fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    internal fun swiped(step: Int) {
        delegate?.shopBuyerStepped(step)
    }

    open fun display(
        storageSpaces: List<StorageSpace>,
        buyer: Buyer,
    ) {
        storageSpaceViews.forEachIndexed { index, storageSpaceView ->
            val storageSpace = storageSpaces.getOrNull(index)
            storageSpaceView.display(lootableItemId = storageSpace?.lootableItemId, isEquipped = storageSpace?.isEquipped ?: false, isDimmed = storageSpace?.isTradeable == false)
        }
        val inventory =
            if (buyer.usedStorageSpace >= buyer.maximumStorageSpace) {
                messages[UiMessage.SHOP_INVENTORY_FULL]
            } else {
                messages[UiMessage.SHOP_INVENTORY, "used" to buyer.usedStorageSpace, "maximum" to buyer.maximumStorageSpace]
            }
        buyerText.text = messages[UiMessage.SHOP_BUYER, "name" to buyer.name, "gold" to messages.gold(buyer.gold), "inventory" to inventory]
        previousBuyerButton.enabled = buyer.partyIndex > 0
        nextBuyerButton.enabled = buyer.partyIndex < buyer.partySize - 1
        selectStorageSpace(null)
    }

    open fun selectStorageSpace(index: Int?) {
        storageSpaceViews.forEachIndexed { storageSpaceIndex, storageSpaceView -> if (storageSpaceIndex == index) storageSpaceView.select() else storageSpaceView.unselect() }
    }

    open fun displayHint(hint: String) {
        itemNameText.text = hint
        itemPriceText.text = ""
    }

    open fun displayItem(
        itemName: String,
        itemPrice: Int,
    ) {
        itemNameText.text = itemName
        itemPriceText.text = messages.gold(itemPrice)
    }

    private fun header(): UIContainer =
        UIContainer(Size(WIDTH, BAR_HEIGHT)).apply {
            previousBuyerButton = button("<", ARROW_WIDTH) { delegate?.shopBuyerStepped(-1) }
            buyerText =
                uiText("", size = Size(WIDTH - 2 * ARROW_WIDTH, BAR_HEIGHT)) {
                    styles.textColor = HudStyle.TEXT
                    styles.textFont = HudFonts.TITLE
                    styles.textSize = HudStyle.BODY
                    styles.textAlignment = TextAlignment.MIDDLE_CENTER
                    position(ARROW_WIDTH, 0.0)
                }
            nextBuyerButton = button(">", ARROW_WIDTH) { delegate?.shopBuyerStepped(1) }.position(WIDTH - ARROW_WIDTH, 0.0)
        }

    private fun Container.button(
        label: String,
        width: Double,
        onSelected: () -> Unit,
    ): GameButton =
        gameButton(label, Size(width, BAR_HEIGHT)) {
            onClick { onSelected() }
        }
}
