package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.UiFont
import com.mkz.tessavar.shared.adapters.presentation.UiTheme
import com.mkz.tessavar.shared.adapters.presentation.gameButton
import korlibs.image.bitmap.Bitmap
import korlibs.image.text.TextAlignment
import korlibs.korge.input.onClick
import korlibs.korge.style.styles
import korlibs.korge.style.textAlignment
import korlibs.korge.style.textColor
import korlibs.korge.style.textFont
import korlibs.korge.style.textSize
import korlibs.korge.ui.UIContainer
import korlibs.korge.ui.UIText
import korlibs.korge.ui.uiHorizontalStack
import korlibs.korge.ui.uiText
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.math.geom.Size

open class WorldBuilderStockView(
    private val messages: Messages,
) : UIContainer(Size(width = 390, height = 300)) {
    companion object {
        const val ITEM_BUTTON_WIDTH = 185.0
        const val ITEM_BUTTON_HEIGHT = 28.0
        const val ITEM_GAP = 6.0
        const val ITEM_ICON_SCALE = 1.5
        const val ITEM_ICON_MARGIN = 4.0
        const val COLUMNS = 2
        const val DONE_WIDTH = 120.0
        const val DONE_HEIGHT = 32.0
        const val TITLE_HEIGHT = 20.0
        const val SECTION_GAP = 10.0
    }

    data class StockItem(
        val lootableItemId: String,
        val name: String,
        val bitmap: Bitmap,
    )

    interface Delegate {
        fun stockChanged(
            npcId: String,
            stock: List<String>,
        )
    }

    private var delegate: Delegate? = null
    private var npcId: String? = null

    internal var stock: List<String> = emptyList()
        private set
    internal val titleText: UIText
    internal val itemList: Container
    internal val itemButtons = mutableMapOf<String, GameButton>()
    internal val doneButton: GameButton

    init {
        solidRect(size, UiTheme.current.background)
        titleText =
            uiText(text = "", size = Size(width = width, height = TITLE_HEIGHT)) {
                styles.textSize = 18.0
                styles.textAlignment = TextAlignment.MIDDLE_LEFT
                styles.textFont = UiFont.current
                styles.textColor = UiTheme.current.text
            }
        itemList = container { y = TITLE_HEIGHT + SECTION_GAP }
        doneButton =
            gameButton(label = messages[UiMessage.WORLD_BUILDER_STOCK_DONE], size = Size(DONE_WIDTH, DONE_HEIGHT)) {
                onClick { finish() }
            }
        visible = false
    }

    val isOpen: Boolean
        get() = visible

    open fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    open fun open(
        npcId: String,
        npcName: String,
        items: List<StockItem>,
        stock: List<String>,
    ) {
        this.npcId = npcId
        this.stock = stock
        titleText.text = messages[UiMessage.WORLD_BUILDER_STOCK_TITLE, "npc" to npcName]
        itemList.removeChildren()
        itemButtons.clear()
        val rows = items.chunked(COLUMNS)
        itemList.uiVerticalStack(padding = ITEM_GAP) {
            rows.forEach { rowItems ->
                uiHorizontalStack(padding = ITEM_GAP) {
                    rowItems.forEach { item -> itemButtons[item.lootableItemId] = itemButton(item) }
                }
            }
        }
        doneButton.y = itemList.y + rows.size * (ITEM_BUTTON_HEIGHT + ITEM_GAP) + SECTION_GAP - ITEM_GAP
        displayStock()
        visible = true
    }

    open fun close() {
        visible = false
    }

    private fun toggle(lootableItemId: String) {
        stock = if (lootableItemId in stock) stock - lootableItemId else stock + lootableItemId
        displayStock()
    }

    private fun finish() {
        val npcId = npcId ?: return
        close()
        delegate?.stockChanged(npcId, stock)
    }

    private fun displayStock() {
        itemButtons.forEach { (lootableItemId, button) ->
            val isStocked = lootableItemId in stock
            button.bgColorOut = if (isStocked) WorldBuilderHudView.SELECTED_TOOL_COLOR else GameButton.GREY
            button.bgColorOver = if (isStocked) WorldBuilderHudView.SELECTED_TOOL_OVER_COLOR else GameButton.GREY_OVER
        }
    }

    private fun Container.itemButton(item: StockItem): GameButton =
        gameButton(label = item.name, size = Size(ITEM_BUTTON_WIDTH, ITEM_BUTTON_HEIGHT)) {
            image(item.bitmap) {
                scale = ITEM_ICON_SCALE
                smoothing = false
                x = ITEM_ICON_MARGIN
                y = (ITEM_BUTTON_HEIGHT - item.bitmap.height * ITEM_ICON_SCALE) / 2
            }
            onClick { toggle(item.lootableItemId) }
        }
}
