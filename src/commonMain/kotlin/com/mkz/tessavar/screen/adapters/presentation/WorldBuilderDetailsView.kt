package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.gameButton
import korlibs.image.color.Colors
import korlibs.image.text.TextAlignment
import korlibs.korge.input.onClick
import korlibs.korge.style.styles
import korlibs.korge.style.textAlignment
import korlibs.korge.style.textSize
import korlibs.korge.ui.UIContainer
import korlibs.korge.ui.UIText
import korlibs.korge.ui.UITextInput
import korlibs.korge.ui.uiHorizontalStack
import korlibs.korge.ui.uiText
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.addTo
import korlibs.korge.view.solidRect
import korlibs.math.geom.Size

open class WorldBuilderDetailsView(
    messages: Messages,
) : UIContainer(Size(width = 390, height = 300)) {
    companion object {
        const val LABEL_WIDTH = 80.0
        const val INPUT_WIDTH = 290.0
        const val DONE_WIDTH = 120.0
        const val ROW_HEIGHT = 32.0
    }

    enum class Field { NAME, AUTHOR }

    interface Delegate {
        fun fieldTapped(field: Field)

        fun detailsChanged(
            name: String,
            author: String,
        )
    }

    private var delegate: Delegate? = null

    internal val titleText: UIText
    internal val nameInput: UITextInput
    internal val authorInput: UITextInput
    internal val doneButton: GameButton

    init {
        solidRect(size, Colors["#2b2b2b"])
        lateinit var title: UIText
        lateinit var name: UITextInput
        lateinit var author: UITextInput
        lateinit var done: GameButton
        uiVerticalStack(padding = 10.0) {
            title =
                uiText(text = messages[UiMessage.WORLD_BUILDER_DETAILS_TITLE], size = Size(width = width, height = 20)) {
                    styles.textSize = 18.0
                    styles.textAlignment = TextAlignment.MIDDLE_LEFT
                }
            uiHorizontalStack(padding = 4.0) {
                label(messages[UiMessage.WORLD_BUILDER_DETAILS_NAME])
                name = UITextInput(size = Size(INPUT_WIDTH, ROW_HEIGHT)).addTo(this)
            }
            uiHorizontalStack(padding = 4.0) {
                label(messages[UiMessage.WORLD_BUILDER_DETAILS_AUTHOR])
                author = UITextInput(size = Size(INPUT_WIDTH, ROW_HEIGHT)).addTo(this)
            }
            done = gameButton(label = messages[UiMessage.WORLD_BUILDER_DETAILS_DONE], size = Size(DONE_WIDTH, ROW_HEIGHT))
        }
        titleText = title
        nameInput = name
        authorInput = author
        doneButton = done
        nameInput.controller.onFocused { delegate?.fieldTapped(Field.NAME) }
        authorInput.controller.onFocused { delegate?.fieldTapped(Field.AUTHOR) }
        doneButton.onClick {
            delegate?.detailsChanged(name = nameInput.text.trim(), author = authorInput.text.trim())
            close()
        }
        visible = false
    }

    val isOpen: Boolean
        get() = visible

    open fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    open fun open(
        name: String,
        author: String,
    ) {
        nameInput.text = name
        authorInput.text = author
        visible = true
    }

    open fun close() {
        visible = false
    }

    open fun displayField(
        field: Field,
        text: String,
    ) {
        when (field) {
            Field.NAME -> nameInput.text = text
            Field.AUTHOR -> authorInput.text = text
        }
    }

    private fun korlibs.korge.view.Container.label(text: String) =
        uiText(text = text, size = Size(width = LABEL_WIDTH, height = ROW_HEIGHT)) {
            styles.textSize = 15.0
            styles.textAlignment = TextAlignment.MIDDLE_LEFT
        }
}
