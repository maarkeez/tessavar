package com.mkz.tessavar.screen.adapters.presentation

class FakeWorldBuilderMenuView : WorldBuilderMenuView() {
    var displayedOptions: List<Option> = emptyList()
        private set
    var delegate: Delegate? = null
        private set
    var displayedHistory: Pair<Boolean, Boolean>? = null
        private set

    override fun displayOptions(options: List<Option>) {
        displayedOptions = options
    }

    override fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    override fun displayHistory(
        canUndo: Boolean,
        canRedo: Boolean,
    ) {
        displayedHistory = canUndo to canRedo
    }

    fun select(label: String) = displayedOptions.single { option -> option.label == label }.onSelected()
}
