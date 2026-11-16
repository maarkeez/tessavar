package com.mkz.tessavar.screen.adapters.presentation

class FakeWorldBuilderCallToActionView : WorldBuilderCallToActionView(MessagesMother.messages()) {
    sealed interface Actions {
        data class LeaveConfirmation(
            val onLeft: () -> Unit,
            val onStayed: () -> Unit,
        ) : Actions
    }

    var displayedActions: Actions? = null
        private set

    override fun clear() {
        displayedActions = null
    }

    override fun displayLeaveConfirmation(
        onLeft: () -> Unit,
        onStayed: () -> Unit,
    ) {
        displayedActions = Actions.LeaveConfirmation(onLeft, onStayed)
    }
}
