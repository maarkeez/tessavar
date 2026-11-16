package com.mkz.tessavar.screen.adapters.presentation

class FakeWorldBuilderResizeView : WorldBuilderResizeView(MessagesMother.messages()) {
    var delegate: Delegate? = null
        private set
    var displayedSize: Pair<Int, Int>? = null
        private set
    var displayedAllowedSizes: IntRange? = null
        private set
    var opened = false
        private set

    override fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    override fun open() {
        opened = true
    }

    override fun displaySize(
        rows: Int,
        columns: Int,
        allowedSizes: IntRange,
    ) {
        displayedSize = rows to columns
        displayedAllowedSizes = allowedSizes
    }
}
