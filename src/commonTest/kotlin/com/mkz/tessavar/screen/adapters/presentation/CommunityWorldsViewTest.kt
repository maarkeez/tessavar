package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class CommunityWorldsViewTest : ViewsForTesting() {
    private val messages = MessagesMother.messages()
    private val selected = mutableListOf<String>()

    private val ashenPass = CommunityWorldsView.Entry(worldId = "ashen-pass", name = "Ashen Pass", author = "maarkeez")
    private val drownedMill = CommunityWorldsView.Entry(worldId = "the-drowned-mill", name = "The Drowned Mill", author = "maarkeez")

    private fun newCommunityWorldsView() =
        CommunityWorldsView(messages).apply {
            setDelegate(
                object : CommunityWorldsView.Delegate {
                    override fun communityWorldSelected(worldId: String) {
                        selected += worldId
                    }
                },
            )
        }

    @Test
    fun `CommunityWorldsView should list each world by its name with its author when it displays them`() =
        sharedViewsTest {
            // Given
            val communityWorldsView = newCommunityWorldsView()
            // When
            communityWorldsView.display(listOf(ashenPass, drownedMill))
            // Then
            communityWorldsView.title() shouldBe "Community worlds"
            communityWorldsView.worldLabels().shouldContainExactly("Ashen Pass, by maarkeez", "The Drowned Mill, by maarkeez")
            communityWorldsView.isEmptyMessageShown().shouldBeFalse()
            communityWorldsView.areWorldsInsideThePanel().shouldBeTrue()
        }

    @Test
    fun `CommunityWorldsView should say there is none yet when it displays no world`() =
        sharedViewsTest {
            // Given
            val communityWorldsView = newCommunityWorldsView()
            addChild(communityWorldsView)
            // When
            communityWorldsView.display(emptyList())
            // Then
            communityWorldsView.worldLabels().shouldBeEmpty()
            communityWorldsView.isEmptyMessageShown().shouldBeTrue()
            communityWorldsView.emptyMessage() shouldBe "No community worlds yet. Build one in Build a world and share it."
        }

    @Test
    fun `CommunityWorldsView should start the world when it is tapped`() =
        sharedViewsTest {
            // Given
            val communityWorldsView = newCommunityWorldsView()
            addChild(communityWorldsView)
            communityWorldsView.display(listOf(ashenPass, drownedMill))
            // When
            clickWorld(communityWorldsView, "the-drowned-mill")
            // Then
            selected.shouldContainExactly("the-drowned-mill")
        }

    @Test
    fun `CommunityWorldsView should show the top of the list again when it displays the worlds anew`() =
        sharedViewsTest {
            // Given
            val communityWorldsView = newCommunityWorldsView()
            communityWorldsView.display(List(10) { index -> ashenPass.copy(worldId = "world-$index") })
            // When
            communityWorldsView.display(listOf(ashenPass))
            // Then
            communityWorldsView.listTop() shouldBe 0.0
        }
}
