package com.mkz.tessavar.screen.adapters.presentation

class FakeInventoryView : InventoryView(GameSceneResourcesLoader(), MessagesMother.messages()) {
    var delegate: Delegate? = null
        private set
    val displayedStorageSpaces = mutableListOf<List<StorageSpace>>()
    var displayedGold: Int? = null
        private set
    var displayedOwner: Owner? = null
        private set
    var selectedStorageSpace: Int? = null
        private set

    override fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    override fun display(
        storageSpaces: List<StorageSpace>,
        gold: Int,
        owner: Owner?,
    ) {
        displayedStorageSpaces += storageSpaces
        displayedGold = gold
        displayedOwner = owner
        selectedStorageSpace = null
    }

    override fun selectStorageSpace(index: Int?) {
        selectedStorageSpace = index
    }
}
