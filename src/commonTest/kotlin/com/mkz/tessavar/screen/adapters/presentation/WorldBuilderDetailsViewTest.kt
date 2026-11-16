package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class WorldBuilderDetailsViewTest : ViewsForTesting() {
    private val details = mutableListOf<Pair<String, String>>()
    private val tappedFields = mutableListOf<WorldBuilderDetailsView.Field>()
    private val delegate =
        object : WorldBuilderDetailsView.Delegate {
            override fun fieldTapped(field: WorldBuilderDetailsView.Field) {
                tappedFields += field
            }

            override fun detailsChanged(
                name: String,
                author: String,
            ) {
                details += name to author
            }
        }

    private fun newDetailsView() = WorldBuilderDetailsView(MessagesMother.messages()).also { detailsView -> detailsView.setDelegate(delegate) }

    @Test
    fun `WorldBuilderDetailsView should be closed when it is created`() =
        sharedViewsTest {
            // When
            val detailsView = newDetailsView()
            // Then
            detailsView.isOpen shouldBe false
            detailsView.titleText.text shouldBe "Details of the world"
        }

    @Test
    fun `Open should display the name and the author of the world when it is opened`() =
        sharedViewsTest {
            // Given
            val detailsView = newDetailsView()
            // When
            detailsView.open(name = "The Sunken Keep", author = "maarkeez")
            // Then
            detailsView.isOpen shouldBe true
            detailsView.nameText() shouldBe "The Sunken Keep"
            detailsView.authorText() shouldBe "maarkeez"
        }

    @Test
    fun `Done should give the name and the author typed, without spaces around them, and close when it is clicked`() =
        sharedViewsTest {
            // Given
            val detailsView = newDetailsView()
            detailsView.open(name = "", author = "")
            addChild(detailsView)
            detailsView.typeName(" The Sunken Keep ")
            detailsView.typeAuthor("maarkeez")
            // When
            clickDone(detailsView)
            // Then
            details.shouldContainExactly("The Sunken Keep" to "maarkeez")
            detailsView.isOpen shouldBe false
        }

    @Test
    fun `Name should tell it was tapped when it is focused by a click`() =
        sharedViewsTest {
            // Given
            val detailsView = newDetailsView()
            detailsView.open(name = "", author = "")
            addChild(detailsView)
            // When
            clickName(detailsView)
            // Then
            tappedFields.shouldContainExactly(WorldBuilderDetailsView.Field.NAME)
        }

    @Test
    fun `DisplayField should put the text in the field when a field is displayed`() =
        sharedViewsTest {
            // Given
            val detailsView = newDetailsView()
            // When
            detailsView.displayField(WorldBuilderDetailsView.Field.AUTHOR, "maarkeez")
            // Then
            detailsView.authorText() shouldBe "maarkeez"
        }
}
