package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.clickOn
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class WorldBuilderFormViewTest : ViewsForTesting() {
    private val calls = mutableListOf<String>()
    private val delegate =
        object : WorldBuilderFormView.Delegate {
            override fun formClosed() {
                calls += "closed"
            }

            override fun textTapped(key: String) {
                calls += "tapped $key"
            }

            override fun textChanged(
                key: String,
                text: String,
            ) {
                calls += "text $key $text"
            }

            override fun choiceStepped(
                key: String,
                step: Int,
            ) {
                calls += "choice $key $step"
            }

            override fun toggled(key: String) {
                calls += "toggled $key"
            }

            override fun stepped(
                key: String,
                step: Int,
            ) {
                calls += "stepped $key $step"
            }

            override fun actionSelected(key: String) {
                calls += "action $key"
            }
        }

    private val rows =
        listOf(
            WorldBuilderFormView.Row.Heading("Tasks"),
            WorldBuilderFormView.Row.Text("name", "Name", "The Keep"),
            WorldBuilderFormView.Row.Toggle("repeatable", "Repeatable", "Yes", isOn = true),
            WorldBuilderFormView.Row.Action("addTask", "Add a task"),
        )

    private fun newFormView() = WorldBuilderFormView(MessagesMother.messages()).also { formView -> formView.setDelegate(delegate) }

    @Test
    fun `WorldBuilderFormView should be closed when it is created`() =
        sharedViewsTest {
            // When
            val formView = newFormView()
            // Then
            formView.isOpen shouldBe false
        }

    @Test
    fun `Open should display the title and a row for each field, the texts in their inputs`() =
        sharedViewsTest {
            // Given
            val formView = newFormView()
            // When
            formView.open(formId = "quest", title = "The Keep", rows = rows)
            // Then
            formView.isOpen shouldBe true
            formView.titleText.text shouldBe "The Keep"
            formView.content.numChildren shouldBe 4
            formView.textInputs.getValue("name").text shouldBe "The Keep"
        }

    @Test
    fun `Rows should ask to toggle, to act and to close when they and Done are clicked`() =
        sharedViewsTest {
            // Given
            val formView = newFormView()
            formView.open(formId = "quest", title = "The Keep", rows = rows)
            addChild(formView)
            // When
            clickOn(formView.buttons.getValue("repeatable"))
            clickOn(formView.buttons.getValue("addTask"))
            clickOn(formView.doneButton)
            // Then
            calls.shouldContainExactly("toggled repeatable", "action addTask", "closed")
        }
}
