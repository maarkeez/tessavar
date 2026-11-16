package com.mkz.tessavar.screen.adapters.presentation

class FakeWorldBuilderFormView : WorldBuilderFormView(MessagesMother.messages()) {
    var delegate: Delegate? = null
        private set
    var openedTitle: String? = null
        private set

    override fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
        super.setDelegate(delegate)
    }

    override fun open(
        formId: String,
        title: String,
        rows: List<Row>,
    ) {
        openedTitle = title
        super.open(formId, title, rows)
    }

    fun labels(): List<String> =
        rows.map { row ->
            when (row) {
                is Row.Heading -> row.text
                is Row.Text -> "${row.label}: ${row.value}"
                is Row.Choice -> "${row.label}: ${row.value}"
                is Row.Toggle -> "${row.label}: ${row.value}"
                is Row.Stepper -> "${row.label}: ${row.value}"
                is Row.Action -> row.label
            }
        }
}
