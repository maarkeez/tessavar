package com.mkz.tessavar.screen.adapters.presentation

class FakeWorldBuilderStockView : WorldBuilderStockView(MessagesMother.messages()) {
    var delegate: Delegate? = null
        private set
    var openedItems: List<StockItem> = emptyList()
        private set
    var openedStock: List<String>? = null
        private set

    override fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    override fun open(
        npcId: String,
        npcName: String,
        items: List<StockItem>,
        stock: List<String>,
    ) {
        openedItems = items
        openedStock = stock
    }
}
