package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.gameButton
import korlibs.image.font.DefaultTtfFontAsBitmap
import korlibs.image.text.RichTextData
import korlibs.image.text.TextAlignment
import korlibs.korge.input.onClick
import korlibs.korge.input.onMouseDrag
import korlibs.korge.style.styles
import korlibs.korge.style.textAlignment
import korlibs.korge.style.textColor
import korlibs.korge.ui.UIContainer
import korlibs.korge.ui.UIText
import korlibs.korge.ui.uiText
import korlibs.korge.view.Container
import korlibs.korge.view.TextBlock
import korlibs.korge.view.clipContainer
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.textBlock
import korlibs.math.clamp
import korlibs.math.geom.Size

open class CommunityWorldsView(
    private val messages: Messages,
) : UIContainer(Size(width = WIDTH, height = HEIGHT)) {
    companion object {
        const val WIDTH = 390.0
        const val HEIGHT = 300.0
        const val PADDING = 10.0
        const val TITLE_HEIGHT = 30.0
        const val ROW_HEIGHT = 48.0
        const val ROW_GAP = 12.0
        const val EMPTY_TEXT_SIZE = 16.0
    }

    data class Entry(
        val worldId: String,
        val name: String,
        val author: String,
    )

    interface Delegate {
        fun communityWorldSelected(worldId: String)
    }

    private var delegate: Delegate? = null

    internal var entries: List<Entry> = emptyList()
        private set
    internal val titleText: UIText =
        uiText(messages[UiMessage.COMMUNITY_WORLDS_TITLE], size = Size(WIDTH - 2 * PADDING, TITLE_HEIGHT)) {
            styles.textColor = HudStyle.TEXT
            styles.textAlignment = TextAlignment.MIDDLE_CENTER
            position(PADDING, 0.0)
        }
    internal val emptyText: TextBlock =
        textBlock(
            RichTextData(messages[UiMessage.COMMUNITY_WORLDS_EMPTY], font = DefaultTtfFontAsBitmap, textSize = EMPTY_TEXT_SIZE, color = HudStyle.TEXT),
            align = TextAlignment.MIDDLE_CENTER,
            size = Size(WIDTH - 2 * PADDING, ROW_HEIGHT * 2),
        ) {
            position(PADDING, TITLE_HEIGHT + PADDING)
            wordWrap = true
            visible = false
        }
    internal val worldButtons = mutableMapOf<String, GameButton>()
    private val viewport: Container = clipContainer(Size(WIDTH, HEIGHT - TITLE_HEIGHT - PADDING)) { y = TITLE_HEIGHT + PADDING }
    internal val list: Container = viewport.container()

    init {
        var dragStart = 0.0
        viewport.onMouseDrag { event ->
            if (event.start) dragStart = list.y
            scrollTo(dragStart + event.localDXY(viewport).y)
        }
    }

    open fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    open fun display(entries: List<Entry>) {
        this.entries = entries
        list.removeChildren()
        worldButtons.clear()
        entries.forEachIndexed { index, entry ->
            worldButtons[entry.worldId] =
                list.gameButton(messages[UiMessage.COMMUNITY_WORLDS_ENTRY, "name" to entry.name, "author" to entry.author], Size(WIDTH - 2 * PADDING, ROW_HEIGHT)) {
                    position(PADDING, index * (ROW_HEIGHT + ROW_GAP))
                    onClick { delegate?.communityWorldSelected(entry.worldId) }
                }
        }
        emptyText.visible = entries.isEmpty()
        scrollTo(0.0)
    }

    private fun scrollTo(y: Double) {
        val listHeight = entries.size * (ROW_HEIGHT + ROW_GAP) - ROW_GAP
        val minimum = minOf(0.0, viewport.height - listHeight)
        list.y = y.clamp(minimum, 0.0)
    }
}
