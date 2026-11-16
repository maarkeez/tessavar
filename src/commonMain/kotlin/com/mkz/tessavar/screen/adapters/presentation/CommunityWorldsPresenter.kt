package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.world.adapters.presentation.WorldApi

class CommunityWorldsPresenter(
    private val menuView: MenuView,
    private val communityWorldsView: CommunityWorldsView,
    private val callToActionView: CallToActionView,
    private val worldApi: WorldApi,
    private val messages: Messages,
    private val onStart: (worldId: String) -> Unit,
) : CommunityWorldsView.Delegate {
    init {
        communityWorldsView.visible = false
        communityWorldsView.setDelegate(this)
    }

    fun open() {
        menuView.visible = false
        communityWorldsView.display(worldApi.searchCommunityWorlds().map { world -> CommunityWorldsView.Entry(worldId = world.id, name = world.name, author = world.author) })
        communityWorldsView.visible = true
        callToActionView.displayButtons(listOf(ButtonRowView.Action(messages[UiMessage.COMMUNITY_WORLDS_BACK]) { backSelected() }))
    }

    override fun communityWorldSelected(worldId: String) {
        communityWorldsView.visible = false
        callToActionView.hideButtons()
        onStart(worldId)
    }

    private fun backSelected() {
        communityWorldsView.visible = false
        menuView.visible = true
        menuView.displayOptions()
        callToActionView.hideButtons()
    }
}
