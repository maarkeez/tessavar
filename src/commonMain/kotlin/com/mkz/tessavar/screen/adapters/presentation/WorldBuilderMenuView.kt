package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.GAME_WINDOW_SIZE
import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.gameButton
import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.Bitmaps
import korlibs.image.bitmap.slice
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.ui.UIButton
import korlibs.korge.ui.uiButton
import korlibs.korge.ui.uiHorizontalStack
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.SolidRect
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Point
import korlibs.math.geom.Size

open class WorldBuilderMenuView : Container() {
    companion object {
        const val BUTTON_WIDTH = 36.0
        const val BUTTON_HEIGHT = 30.0
        const val BAR_WIDTH = 20.0
        const val BAR_HEIGHT = 3.0
        const val BAR_GAP = 4.0
        const val OPTION_WIDTH = 150.0
        const val OPTION_HEIGHT = 32.0
        const val DROPDOWN_GAP = 4.0
        const val ICON_SCALE = 2.0
        const val DISABLED_ICON_ALPHA = 0.35
        const val HISTORY_ICON = "HISTORY_ICON"

        fun historyIcons(resourcesLoader: GameSceneResourcesLoader): Map<HistoryIcon, Bitmap> =
            HistoryIcon.entries
                .mapNotNull { icon -> resourcesLoader.bitmapOrNull("ui/tool-${icon.name.lowercase()}.png")?.let { bitmap -> icon to bitmap } }
                .toMap()
    }

    enum class HistoryIcon { UNDO, REDO }

    data class Option(
        val label: String,
        val onSelected: () -> Unit,
    )

    interface Delegate {
        fun undoSelected()

        fun redoSelected()
    }

    private var delegate: Delegate? = null

    private var options: List<Option> = emptyList()

    private var historyIcons: Map<HistoryIcon, Bitmap> = emptyMap()

    private var canUndo = false

    private var canRedo = false

    private val fingerTap = FingerTap(this)

    internal var undoButton: GameButton? = null
    internal var redoButton: GameButton? = null

    internal var outsideTapCatcher: SolidRect? = null
    internal var dropdown: Container? = null
    internal val optionButtons = mutableListOf<UIButton>()

    internal val menuButton =
        uiButton(label = "", size = Size(BUTTON_WIDTH, BUTTON_HEIGHT)) {
            bgColorOut = Colors.TRANSPARENT
            bgColorOver = HudStyle.BUTTON
            background.borderColor = Colors.TRANSPARENT
            repeat(3) { bar ->
                solidRect(Size(BAR_WIDTH, BAR_HEIGHT), Colors.WHITE) {
                    x = (BUTTON_WIDTH - BAR_WIDTH) / 2
                    y = (BUTTON_HEIGHT - 3 * BAR_HEIGHT - 2 * BAR_GAP) / 2 + bar * (BAR_HEIGHT + BAR_GAP)
                }
            }
            onClick { if (isOpen) close() else open() }
        }

    val isOpen: Boolean
        get() = dropdown != null

    open fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    open fun displayHistoryIcons(icons: Map<HistoryIcon, Bitmap>) {
        historyIcons = icons
        undoButton?.showIcon(HistoryIcon.UNDO)
        redoButton?.showIcon(HistoryIcon.REDO)
    }

    open fun displayHistory(
        canUndo: Boolean,
        canRedo: Boolean,
    ) {
        this.canUndo = canUndo
        this.canRedo = canRedo
        undoButton?.showEnabled(canUndo)
        redoButton?.showEnabled(canRedo)
    }

    open fun displayOptions(options: List<Option>) {
        this.options = options
        if (isOpen) {
            close()
            open()
        }
    }

    open fun open() {
        if (isOpen) return
        val origin = localToGlobal(Point(0, 0))
        outsideTapCatcher =
            solidRect(GAME_WINDOW_SIZE, Colors.TRANSPARENT) {
                x = -origin.x
                y = -origin.y
                name = FingerTap.PASS_THROUGH
                onClick { if (!fingerTap.isHoldingBack) close() }
            }
        menuButton.removeFromParent()
        addChild(menuButton)
        dropdown =
            container {
                x = BUTTON_WIDTH - OPTION_WIDTH
                y = BUTTON_HEIGHT + DROPDOWN_GAP
                uiVerticalStack(padding = 0.0) {
                    uiHorizontalStack(padding = 0.0) {
                        undoButton = historyButton(HistoryIcon.UNDO, isEnabled = canUndo) { delegate?.undoSelected() }
                        redoButton = historyButton(HistoryIcon.REDO, isEnabled = canRedo) { delegate?.redoSelected() }
                    }
                    options.forEach { option ->
                        optionButtons +=
                            uiButton(label = option.label, size = Size(OPTION_WIDTH, OPTION_HEIGHT)) {
                                bgColorOut = HudStyle.BUTTON
                                background.borderColor = HudStyle.BUTTON_BORDER
                                onClick {
                                    close()
                                    option.onSelected()
                                }
                            }
                    }
                }
            }
    }

    open fun close() {
        outsideTapCatcher?.removeFromParent()
        outsideTapCatcher = null
        dropdown?.removeFromParent()
        dropdown = null
        optionButtons.clear()
        undoButton = null
        redoButton = null
    }

    private fun Container.historyButton(
        icon: HistoryIcon,
        isEnabled: Boolean,
        onSelected: () -> Unit,
    ): GameButton =
        gameButton(label = "", size = Size(OPTION_WIDTH / 2, OPTION_HEIGHT)) {
            image(Bitmaps.transparent) {
                name = HISTORY_ICON
                scale = ICON_SCALE
                smoothing = false
            }
            showIcon(icon)
            showEnabled(isEnabled)
            onClick { if (enabled) onSelected() }
        }

    private fun GameButton.showIcon(icon: HistoryIcon) {
        val image = findViewByName(HISTORY_ICON) as? Image ?: return
        val bitmap = historyIcons[icon]?.slice() ?: Bitmaps.transparent
        image.bitmap = bitmap
        image.xy((width - bitmap.width * ICON_SCALE) / 2, (height - bitmap.height * ICON_SCALE) / 2)
    }

    private fun GameButton.showEnabled(isEnabled: Boolean) {
        enabled = isEnabled
        (findViewByName(HISTORY_ICON) as? Image)?.alpha = if (isEnabled) 1.0 else DISABLED_ICON_ALPHA
    }
}
