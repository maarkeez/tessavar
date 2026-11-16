package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.events.InMemoryEventBus
import com.mkz.tessavar.world.adapters.presentation.WorldApi
import com.mkz.tessavar.world.domain.World
import com.mkz.tessavar.world.usecases.services.FakeWorldLoader
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CommunityWorldsPresenterTest {
    private val messages = MessagesMother.messages()
    private val menuView =
        MenuView(messages, onNewGameSelected = {}, onLoadGameSelected = {}, onCommunityWorldsSelected = {}, onWorldBuilderSelected = {}, onPrivacySelected = {}, onWishlistSelected = {})
    private val communityWorldsView = CommunityWorldsView(messages)
    private val callToActionView = FakeCallToActionView()
    private val worlds =
        listOf(
            World.Dto(id = World.CAMPAIGN_ID, name = "Tessavar", author = "maarkeez", version = World.CURRENT_VERSION),
            World.Dto(id = "the-drowned-mill", name = "The Drowned Mill", author = "maarkeez", version = World.CURRENT_VERSION),
            World.Dto(id = "ashen-pass", name = "Ashen Pass", author = "maarkeez", version = World.CURRENT_VERSION),
        )
    private val worldApi = WorldApi(InMemoryEventBus(), FakeWorldLoader(worlds))
    private val startedWorldIds = mutableListOf<String>()

    private fun presenter() = CommunityWorldsPresenter(menuView, communityWorldsView, callToActionView, worldApi, messages) { worldId -> startedWorldIds += worldId }

    @Test
    fun `Open should list the community worlds alphabetically, without the campaign, in place of the options`() {
        // Given
        val presenter = presenter()
        // When
        presenter.open()
        // Then
        menuView.visible.shouldBeFalse()
        communityWorldsView.visible.shouldBeTrue()
        communityWorldsView.worldLabels().shouldContainExactly("Ashen Pass, by maarkeez", "The Drowned Mill, by maarkeez")
        callToActionView.actionLabels().shouldContainExactly("Back")
    }

    @Test
    fun `CommunityWorldsPresenter should start a new game in the world when one is chosen`() {
        // Given
        val presenter = presenter().apply { open() }
        // When
        presenter.communityWorldSelected("ashen-pass")
        // Then
        startedWorldIds.shouldContainExactly("ashen-pass")
        communityWorldsView.visible.shouldBeFalse()
        callToActionView.displayedActions shouldBe null
    }

    @Test
    fun `Back should display the options of the menu again when it is chosen`() {
        // Given
        presenter().open()
        // When
        callToActionView.chooseAction("Back")
        // Then
        communityWorldsView.visible.shouldBeFalse()
        menuView.visible.shouldBeTrue()
        menuView.areOptionsShown().shouldBeTrue()
        callToActionView.displayedActions shouldBe null
    }
}
