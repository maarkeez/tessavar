package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.battleUnit.adapters.presentation.BattleUnitApi
import com.mkz.tessavar.battlefield.adapters.presentation.BattlefieldApi
import com.mkz.tessavar.inventory.adapters.presentation.InventoryApi
import com.mkz.tessavar.inventory.domain.Inventory.Dto.TradeRefusalDto
import com.mkz.tessavar.inventory.domain.InventoryEvent.ItemBought
import com.mkz.tessavar.inventory.domain.InventoryEvent.ItemSold
import com.mkz.tessavar.lootableItem.adapters.presentation.LootableItemApi
import com.mkz.tessavar.npc.domain.Npc
import com.mkz.tessavar.party.adapters.presentation.PartyApi
import com.mkz.tessavar.screen.adapters.presentation.ShopView.Mode
import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.shared.domain.Subscription
import com.mkz.tessavar.shared.domain.subscribe

class ShopPresenter(
    private val shopView: ShopView,
    private val battlefieldView: BattlefieldView,
    private val callToActionView: CallToActionView,
    private val partyApi: PartyApi,
    private val inventoryApi: InventoryApi,
    private val lootableItemApi: LootableItemApi,
    private val battleUnitApi: BattleUnitApi,
    private val battlefieldApi: BattlefieldApi,
    private val messages: Messages,
    private val timeline: GameSceneTimeline,
    eventBus: EventBus,
) : ShopView.Delegate {
    private var merchant: Npc.Dto? = null
    private var partyMemberIds: List<String> = emptyList()
    private var buyerIndex = 0
    private var mode = Mode.BUY
    private var displayedStorageSpaces: List<ShopView.StorageSpace> = emptyList()
    private var selectedStorageSpaceIndex: Int? = null
    private var onBack: (() -> Unit)? = null

    val isOpen: Boolean get() = merchant != null

    private val subscriptions =
        listOf(
            eventBus.subscribe<ItemBought> { event -> timeline.play { refresh(event.battleUnitId, keepsSelection = true) } },
            eventBus.subscribe<ItemSold> { event -> timeline.play { refresh(event.battleUnitId, keepsSelection = false) } },
        )

    init {
        shopView.visible = false
        shopView.setDelegate(this)
    }

    fun dispose() {
        subscriptions.forEach(Subscription::dispose)
    }

    fun setOnBack(listener: () -> Unit) {
        onBack = listener
    }

    suspend fun open(
        merchant: Npc.Dto,
        mode: Mode,
    ) {
        this.merchant = merchant
        partyMemberIds = partyApi.searchParty()?.battleUnitIds.orEmpty()
        buyerIndex = 0
        this.mode = mode
        displayShop()
        shopView.visible = true
        highlightBuyer()
    }

    fun close() {
        if (merchant == null) return
        merchant = null
        partyMemberIds = emptyList()
        displayedStorageSpaces = emptyList()
        selectedStorageSpaceIndex = null
        shopView.visible = false
        battlefieldView.resetTiles()
    }

    private fun buyerId(): String? = partyMemberIds.getOrNull(buyerIndex)

    private fun displayShop() {
        val merchant = merchant ?: return
        val buyerId = buyerId() ?: return
        val inventory = inventoryApi.searchInventoryByBattleUnitId(buyerId)
        displayedStorageSpaces =
            when (mode) {
                Mode.BUY -> merchant.stock.map { lootableItemId -> ShopView.StorageSpace(lootableItemId, isEquipped = false, isTradeable = refusal(buyerId, lootableItemId) == null) }
                Mode.SELL -> {
                    val equippedItems =
                        inventory
                            ?.equippedItems
                            ?.values
                            .orEmpty()
                            .toMutableList()
                    inventory?.storedItems.orEmpty().map { lootableItemId ->
                        ShopView.StorageSpace(lootableItemId, isEquipped = equippedItems.remove(lootableItemId), isTradeable = refusal(buyerId, lootableItemId) == null)
                    }
                }
            }
        val buyer =
            ShopView.Buyer(
                name = battleUnitApi.searchBattleUnitById(buyerId)?.let { battleUnit -> messages.unitName(battleUnit.unitId) }.orEmpty(),
                gold = inventory?.gold ?: 0,
                usedStorageSpace = inventory?.storedItems?.size ?: 0,
                maximumStorageSpace = inventory?.maximumStorageSpace ?: 0,
                partyIndex = buyerIndex,
                partySize = partyMemberIds.size,
            )
        shopView.display(storageSpaces = displayedStorageSpaces, buyer = buyer)
        clearSelection()
    }

    private suspend fun highlightBuyer() {
        val merchant = merchant ?: return
        val buyerPosition = buyerId()?.let { buyerId -> battlefieldApi.searchPosition(buyerId) } ?: return
        battlefieldView.resetTiles()
        battlefieldView.dimTilesExcept(setOf(BattlefieldMapper.position(buyerPosition), TilePosition(row = merchant.row, column = merchant.column)))
        battlefieldView.scrollToTile(buyerPosition.row, buyerPosition.column)
    }

    private fun clearSelection() {
        selectedStorageSpaceIndex = null
        shopView.selectStorageSpace(null)
        shopView.displayHint(
            when {
                mode == Mode.BUY -> messages[UiMessage.SHOP_TAP_TO_BUY]
                displayedStorageSpaces.isEmpty() -> messages[UiMessage.SHOP_NOTHING_TO_SELL]
                else -> messages[UiMessage.SHOP_TAP_TO_SELL]
            },
        )
        callToActionView.displayButtons(listOf(backAction()))
    }

    private fun backAction() = ButtonRowView.Action(messages[UiMessage.SHOP_BACK]) { whenIdle { onBack?.invoke() } }

    private fun selectItem(storageSpaceIndex: Int) {
        val buyerId = buyerId() ?: return
        if (storageSpaceIndex == selectedStorageSpaceIndex) return clearSelection()
        val lootableItemId = displayedStorageSpaces.getOrNull(storageSpaceIndex)?.lootableItemId ?: return clearSelection()
        val lootableItem = lootableItemApi.searchLootableItemById(lootableItemId) ?: return
        val (price, actionLabel) =
            when (mode) {
                Mode.BUY -> lootableItem.price to messages[UiMessage.SHOP_BUY]
                Mode.SELL -> (lootableItemApi.searchSellPrice(lootableItemId) ?: return) to messages[UiMessage.SHOP_SELL]
            }
        selectedStorageSpaceIndex = storageSpaceIndex
        shopView.selectStorageSpace(storageSpaceIndex)
        val refusal = refusal(buyerId, lootableItemId)
        val (color, pressedColor) =
            when (mode) {
                Mode.BUY -> PrimaryButton.COLOR to PrimaryButton.PRESSED_COLOR
                Mode.SELL -> GameButton.GREY to GameButton.GREY_OVER
            }
        shopView.displayItem(messages.lootableItemName(lootableItem.id), price)
        callToActionView.displayButtons(
            listOf(
                backAction(),
                ButtonRowView.Action(refusal?.let(::refusalText) ?: actionLabel, color, pressedColor, isEnabled = refusal == null) { whenIdle { trade() } },
            ),
        )
    }

    private fun refusal(
        buyerId: String,
        lootableItemId: String,
    ): TradeRefusalDto? =
        when (mode) {
            Mode.BUY -> inventoryApi.searchBuyRefusal(buyerId, lootableItemId)
            Mode.SELL -> inventoryApi.searchSellRefusal(buyerId, lootableItemId)
        }

    private fun refusalText(refusal: TradeRefusalDto): String? =
        when (refusal) {
            TradeRefusalDto.NOT_ENOUGH_GOLD -> messages[UiMessage.SHOP_NOT_ENOUGH_GOLD]
            TradeRefusalDto.INVENTORY_FULL -> messages[UiMessage.SHOP_NO_STORAGE_SPACE]
            TradeRefusalDto.EQUIPPED -> messages[UiMessage.SHOP_EQUIPPED]
            TradeRefusalDto.GOLD_FULL -> messages[UiMessage.SHOP_GOLD_FULL]
            TradeRefusalDto.NOT_TRADING, TradeRefusalDto.NOT_SOLD_BY_THE_MERCHANT, TradeRefusalDto.NOT_STORED -> null
        }

    private fun trade() {
        val buyerId = buyerId() ?: return
        val lootableItemId = selectedStorageSpaceIndex?.let { storageSpaceIndex -> displayedStorageSpaces.getOrNull(storageSpaceIndex) }?.lootableItemId ?: return
        if (refusal(buyerId, lootableItemId) != null) return
        when (mode) {
            Mode.BUY -> inventoryApi.buyItem(buyerId, lootableItemId)
            Mode.SELL -> inventoryApi.sellItem(buyerId, lootableItemId)
        }
    }

    private fun refresh(
        battleUnitId: String,
        keepsSelection: Boolean,
    ) {
        if (!isOpen || battleUnitId != buyerId()) return
        val selectedStorageSpace = selectedStorageSpaceIndex
        displayShop()
        if (keepsSelection && selectedStorageSpace != null) selectItem(selectedStorageSpace)
    }

    private fun switchBuyer(partyIndex: Int) =
        whenIdle {
            if (partyIndex !in partyMemberIds.indices || partyIndex == buyerIndex) return@whenIdle
            timeline.play {
                buyerIndex = partyIndex
                displayShop()
                highlightBuyer()
            }
        }

    private fun whenIdle(action: () -> Unit) {
        if (!timeline.isPlaying && isOpen) action()
    }

    override fun shopStorageSpaceSelected(index: Int) = whenIdle { timeline.play { selectItem(index) } }

    override fun shopBuyerStepped(step: Int) = switchBuyer(buyerIndex + step)
}
