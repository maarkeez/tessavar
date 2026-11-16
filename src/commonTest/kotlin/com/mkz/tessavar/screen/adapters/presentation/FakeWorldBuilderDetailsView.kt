package com.mkz.tessavar.screen.adapters.presentation

class FakeWorldBuilderDetailsView : WorldBuilderDetailsView(MessagesMother.messages()) {
    var delegate: Delegate? = null
        private set
    var openedWith: Pair<String, String>? = null
        private set
    val displayedFields = mutableListOf<Pair<Field, String>>()

    override fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    override fun open(
        name: String,
        author: String,
    ) {
        openedWith = name to author
    }

    override fun displayField(
        field: Field,
        text: String,
    ) {
        displayedFields += field to text
    }
}
