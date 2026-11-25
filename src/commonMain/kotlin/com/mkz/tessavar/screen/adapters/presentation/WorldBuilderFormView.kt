package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.HudFonts
import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.gameButton
import korlibs.image.text.TextAlignment
import korlibs.korge.input.onClick
import korlibs.korge.input.onMouseDrag
import korlibs.korge.style.styles
import korlibs.korge.style.textAlignment
import korlibs.korge.style.textColor
import korlibs.korge.style.textFont
import korlibs.korge.style.textSize
import korlibs.korge.ui.UIContainer
import korlibs.korge.ui.UIText
import korlibs.korge.ui.UITextInput
import korlibs.korge.ui.uiText
import korlibs.korge.view.Container
import korlibs.korge.view.addTo
import korlibs.korge.view.clipContainer
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.math.clamp
import korlibs.math.geom.Size

open class WorldBuilderFormView(
    messages: Messages,
) : UIContainer(Size(width = 390, height = 300)) {
    companion object {
        const val HEADER_HEIGHT = 36.0
        const val ROW_HEIGHT = 30.0
        const val ROW_GAP = 6.0
        const val LABEL_WIDTH = 120.0
        const val STEP_WIDTH = 36.0
        const val DONE_WIDTH = 90.0
        const val MARGIN = 4.0
    }

    sealed interface Row {
        data class Heading(
            val text: String,
        ) : Row

        data class Text(
            val key: String,
            val label: String,
            val value: String,
        ) : Row

        data class Choice(
            val key: String,
            val label: String,
            val value: String,
        ) : Row

        data class Toggle(
            val key: String,
            val label: String,
            val value: String,
            val isOn: Boolean,
        ) : Row

        data class Stepper(
            val key: String,
            val label: String,
            val value: Int,
        ) : Row

        data class Action(
            val key: String,
            val label: String,
            val isHighlighted: Boolean = false,
        ) : Row
    }

    interface Delegate {
        fun formClosed()

        fun textTapped(key: String)

        fun textChanged(
            key: String,
            text: String,
        )

        fun choiceStepped(
            key: String,
            step: Int,
        )

        fun toggled(key: String)

        fun stepped(
            key: String,
            step: Int,
        )

        fun actionSelected(key: String)
    }

    private var delegate: Delegate? = null
    private var formId: String? = null

    internal var rows: List<Row> = emptyList()
        private set
    internal val titleText: UIText
    internal val doneButton: GameButton
    internal val textInputs = mutableMapOf<String, UITextInput>()
    internal val buttons = mutableMapOf<String, GameButton>()
    private val viewport: Container
    internal val content: Container

    init {
        solidRect(size, HudStyle.BACKGROUND)
        titleText =
            uiText(text = "", size = Size(width - DONE_WIDTH - 2 * MARGIN, HEADER_HEIGHT - MARGIN)) {
                styles.textColor = HudStyle.TEXT
                styles.textFont = HudFonts.TITLE
                x = MARGIN
                styles.textSize = HudStyle.NAME
                styles.textAlignment = TextAlignment.MIDDLE_LEFT
            }
        doneButton =
            gameButton(label = messages[UiMessage.WORLD_BUILDER_FORM_DONE], size = Size(DONE_WIDTH, HEADER_HEIGHT - 2 * MARGIN)) {
                x = this@WorldBuilderFormView.width - DONE_WIDTH - MARGIN
                y = MARGIN
                onClick { delegate?.formClosed() }
            }
        viewport = clipContainer(Size(width, height - HEADER_HEIGHT)) { y = HEADER_HEIGHT }
        content = viewport.container()
        var dragStart = 0.0
        viewport.onMouseDrag { event ->
            if (event.start) dragStart = content.y
            scrollTo(dragStart + event.localDXY(viewport).y)
        }
        visible = false
    }

    val isOpen: Boolean
        get() = visible

    open fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    open fun open(
        formId: String,
        title: String,
        rows: List<Row>,
    ) {
        val isSameForm = visible && formId == this.formId
        this.formId = formId
        titleText.text = title
        this.rows = rows
        val scroll = if (isSameForm) content.y else 0.0
        content.removeChildren()
        textInputs.clear()
        buttons.clear()
        rows.forEachIndexed { index, row -> content.row(row, y = index * (ROW_HEIGHT + ROW_GAP)) }
        visible = true
        scrollTo(scroll)
    }

    open fun close() {
        visible = false
    }

    open fun displayText(
        key: String,
        text: String,
    ) {
        textInputs[key]?.text = text
    }

    private fun scrollTo(y: Double) {
        val contentHeight = rows.size * (ROW_HEIGHT + ROW_GAP)
        val minimum = minOf(0.0, viewport.height - contentHeight)
        content.y = y.clamp(minimum, 0.0)
    }

    private fun Container.row(
        row: Row,
        y: Double,
    ) {
        val fieldX = LABEL_WIDTH + 2 * MARGIN
        val fieldWidth = this@WorldBuilderFormView.width - fieldX - MARGIN
        container {
            this.y = y
            x = MARGIN
            when (row) {
                is Row.Heading -> label(row.text, width = this@WorldBuilderFormView.width - 2 * MARGIN, textSize = HudStyle.BODY)
                is Row.Text -> {
                    label(row.label)
                    val input =
                        UITextInput(initialText = row.value, size = Size(fieldWidth, ROW_HEIGHT)).addTo(this).apply {
                            x = fieldX - MARGIN
                        }
                    input.controller.onFocused { delegate?.textTapped(row.key) }
                    input.controller.onFocusLost { delegate?.textChanged(row.key, input.text) }
                    input.controller.onReturnPressed { delegate?.textChanged(row.key, input.text) }
                    textInputs[row.key] = input
                }
                is Row.Choice -> {
                    label(row.label)
                    gameButton(label = "<", size = Size(STEP_WIDTH, ROW_HEIGHT)) {
                        x = fieldX - MARGIN
                        onClick { delegate?.choiceStepped(row.key, -1) }
                    }
                    uiText(text = row.value, size = Size(fieldWidth - 2 * STEP_WIDTH, ROW_HEIGHT)) {
                        styles.textColor = HudStyle.TEXT
                        styles.textFont = HudFonts.TITLE
                        x = fieldX - MARGIN + STEP_WIDTH
                        styles.textSize = HudStyle.BODY
                        styles.textAlignment = TextAlignment.MIDDLE_CENTER
                    }
                    gameButton(label = ">", size = Size(STEP_WIDTH, ROW_HEIGHT)) {
                        x = fieldX - MARGIN + fieldWidth - STEP_WIDTH
                        onClick { delegate?.choiceStepped(row.key, 1) }
                    }
                }
                is Row.Toggle -> {
                    label(row.label)
                    buttons[row.key] =
                        gameButton(label = row.value, size = Size(fieldWidth, ROW_HEIGHT)) {
                            x = fieldX - MARGIN
                            highlight(row.isOn)
                            onClick { delegate?.toggled(row.key) }
                        }
                }
                is Row.Stepper -> {
                    label(row.label)
                    gameButton(label = "-", size = Size(STEP_WIDTH, ROW_HEIGHT)) {
                        x = fieldX - MARGIN
                        onClick { delegate?.stepped(row.key, -1) }
                    }
                    uiText(text = "${row.value}", size = Size(fieldWidth - 2 * STEP_WIDTH, ROW_HEIGHT)) {
                        styles.textColor = HudStyle.TEXT
                        styles.textFont = HudFonts.TITLE
                        x = fieldX - MARGIN + STEP_WIDTH
                        styles.textSize = HudStyle.BODY
                        styles.textAlignment = TextAlignment.MIDDLE_CENTER
                    }
                    gameButton(label = "+", size = Size(STEP_WIDTH, ROW_HEIGHT)) {
                        x = fieldX - MARGIN + fieldWidth - STEP_WIDTH
                        onClick { delegate?.stepped(row.key, 1) }
                    }
                }
                is Row.Action ->
                    buttons[row.key] =
                        gameButton(label = row.label, size = Size(this@WorldBuilderFormView.width - 2 * MARGIN, ROW_HEIGHT)) {
                            highlight(row.isHighlighted)
                            onClick { delegate?.actionSelected(row.key) }
                        }
            }
        }
    }

    private fun Container.label(
        text: String,
        width: Double = LABEL_WIDTH,
        textSize: Double = 14.0,
    ) = uiText(text = text, size = Size(width, ROW_HEIGHT)) {
        styles.textColor = HudStyle.TEXT
        styles.textFont = HudFonts.TITLE
        styles.textSize = textSize
        styles.textAlignment = TextAlignment.MIDDLE_LEFT
    }

    private fun GameButton.highlight(isHighlighted: Boolean) {
        bgColorOut = if (isHighlighted) WorldBuilderHudView.SELECTED_TOOL_COLOR else GameButton.GREY
        bgColorOver = if (isHighlighted) WorldBuilderHudView.SELECTED_TOOL_OVER_COLOR else GameButton.GREY_OVER
    }
}
