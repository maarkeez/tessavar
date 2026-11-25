package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.HudFonts
import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.gameButton
import korlibs.image.text.TextAlignment
import korlibs.korge.input.onClick
import korlibs.korge.style.styles
import korlibs.korge.style.textAlignment
import korlibs.korge.style.textColor
import korlibs.korge.style.textFont
import korlibs.korge.style.textSize
import korlibs.korge.ui.UIContainer
import korlibs.korge.ui.UIText
import korlibs.korge.ui.uiHorizontalStack
import korlibs.korge.ui.uiText
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.Container
import korlibs.korge.view.solidRect
import korlibs.math.geom.Size

open class WorldBuilderResizeView(
    messages: Messages,
) : UIContainer(Size(width = 390, height = 300)) {
    companion object {
        val SIZE_STEPS = listOf(-8, -1, 1, 8)
        const val STEP_BUTTON_WIDTH = 44.0
        const val LABEL_WIDTH = 80.0
        const val VALUE_WIDTH = 50.0
        const val DONE_WIDTH = 120.0
        const val ROW_HEIGHT = 32.0
    }

    interface Delegate {
        fun sizeChanged(
            rows: Int,
            columns: Int,
        )
    }

    private var delegate: Delegate? = null
    private var rows = 0
    private var columns = 0
    private var allowedSizes = IntRange.EMPTY

    internal val titleText: UIText
    internal val rowsValue: UIText
    internal val columnsValue: UIText
    internal val rowsStepButtons: Map<Int, GameButton>
    internal val columnsStepButtons: Map<Int, GameButton>
    internal val doneButton: GameButton

    init {
        solidRect(size, HudStyle.BACKGROUND)
        lateinit var rowsStepper: Pair<UIText, Map<Int, GameButton>>
        lateinit var columnsStepper: Pair<UIText, Map<Int, GameButton>>
        lateinit var title: UIText
        lateinit var done: GameButton
        uiVerticalStack(padding = 10.0) {
            title =
                uiText(text = messages[UiMessage.WORLD_BUILDER_RESIZE_TITLE], size = Size(width = width, height = 20)) {
                    styles.textColor = HudStyle.TEXT
                    styles.textFont = HudFonts.TITLE
                    styles.textSize = 18.0
                    styles.textAlignment = TextAlignment.MIDDLE_LEFT
                }
            rowsStepper = stepper(label = messages[UiMessage.WORLD_BUILDER_RESIZE_ROWS]) { step -> delegate?.sizeChanged(rows = rows + step, columns = columns) }
            columnsStepper = stepper(label = messages[UiMessage.WORLD_BUILDER_RESIZE_COLUMNS]) { step -> delegate?.sizeChanged(rows = rows, columns = columns + step) }
            done =
                gameButton(label = messages[UiMessage.WORLD_BUILDER_RESIZE_DONE], size = Size(DONE_WIDTH, ROW_HEIGHT)) {
                    onClick { close() }
                }
        }
        titleText = title
        rowsValue = rowsStepper.first
        rowsStepButtons = rowsStepper.second
        columnsValue = columnsStepper.first
        columnsStepButtons = columnsStepper.second
        doneButton = done
        visible = false
    }

    val isOpen: Boolean
        get() = visible

    open fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    open fun open() {
        visible = true
    }

    open fun close() {
        visible = false
    }

    open fun displaySize(
        rows: Int,
        columns: Int,
        allowedSizes: IntRange,
    ) {
        this.rows = rows
        this.columns = columns
        this.allowedSizes = allowedSizes
        rowsValue.text = "$rows"
        columnsValue.text = "$columns"
        rowsStepButtons.forEach { (step, button) -> button.enabled = (rows + step) in allowedSizes }
        columnsStepButtons.forEach { (step, button) -> button.enabled = (columns + step) in allowedSizes }
    }

    private fun Container.stepper(
        label: String,
        onStep: (Int) -> Unit,
    ): Pair<UIText, Map<Int, GameButton>> {
        lateinit var value: UIText
        val stepButtons = mutableMapOf<Int, GameButton>()
        uiHorizontalStack(padding = 4.0) {
            uiText(text = label, size = Size(width = LABEL_WIDTH, height = ROW_HEIGHT)) {
                styles.textColor = HudStyle.TEXT
                styles.textFont = HudFonts.TITLE
                styles.textSize = 15.0
                styles.textAlignment = TextAlignment.MIDDLE_LEFT
            }
            SIZE_STEPS.filter { step -> step < 0 }.forEach { step -> stepButtons[step] = stepButton(step, onStep) }
            value =
                uiText(text = "", size = Size(width = VALUE_WIDTH, height = ROW_HEIGHT)) {
                    styles.textColor = HudStyle.TEXT
                    styles.textFont = HudFonts.TITLE
                    styles.textSize = 15.0
                    styles.textAlignment = TextAlignment.MIDDLE_CENTER
                }
            SIZE_STEPS.filter { step -> step > 0 }.forEach { step -> stepButtons[step] = stepButton(step, onStep) }
        }
        return value to stepButtons
    }

    private fun Container.stepButton(
        step: Int,
        onStep: (Int) -> Unit,
    ): GameButton =
        gameButton(label = if (step > 0) "+$step" else "$step", size = Size(STEP_BUTTON_WIDTH, ROW_HEIGHT)) {
            onClick { onStep(step) }
        }
}
