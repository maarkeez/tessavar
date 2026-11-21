package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderHudView.Companion.fittedImage
import korlibs.image.bitmap.Bitmap
import korlibs.korge.input.onClick
import korlibs.korge.input.onDown
import korlibs.korge.input.onMouseDrag
import korlibs.korge.ui.UIContainer
import korlibs.korge.view.Container
import korlibs.korge.view.addTo
import korlibs.korge.view.clipContainer
import korlibs.korge.view.container
import korlibs.korge.view.xy
import korlibs.math.clamp
import korlibs.math.geom.Size
import kotlin.math.abs
import kotlin.math.min

open class PaletteRowView(
    private val rowWidth: Double,
    private val slotSize: Double = SLOT_SIZE,
    private val gap: Double = SLOT_GAP,
) : UIContainer(Size(rowWidth + 2 * BORDER, slotSize + 2 * BORDER)) {
    companion object {
        const val SLOT_SIZE = 68.0
        const val SLOT_GAP = 6.0
        const val BORDER = 3.0
        const val DRAG_DISTANCE = 10.0
        const val SPRITE_INSET = 4.0
    }

    data class Slot(
        val key: String,
        val bitmap: Bitmap,
        val imageName: String,
    )

    private val viewport = clipContainer(size)

    internal val strip: Container = viewport.container { xy(BORDER, BORDER) }

    internal val slotButtons = linkedMapOf<String, WorldBuilderHudView.SlotButton>()

    private var onTapped: (String) -> Unit = {}

    private var isDragged = false

    private val minScroll: Double
        get() = min(0.0, rowWidth - contentWidth)

    private val contentWidth: Double
        get() = if (slotButtons.isEmpty()) 0.0 else slotButtons.size * slotSize + (slotButtons.size - 1) * gap

    val scroll: Double
        get() = strip.x - BORDER

    init {
        var dragStartX = 0.0
        viewport.onDown { isDragged = false }
        viewport.onMouseDrag { event ->
            if (event.start) {
                isDragged = false
                dragStartX = scroll
            }
            val dragX = event.localDXY(viewport).x
            if (abs(dragX) > DRAG_DISTANCE) isDragged = true
            if (isDragged) scrollToOffset(dragStartX + dragX)
        }
    }

    open fun display(
        slots: List<Slot>,
        onTapped: (String) -> Unit,
    ) {
        this.onTapped = onTapped
        strip.removeChildren()
        slotButtons.clear()
        slots.forEachIndexed { index, slot ->
            slotButtons[slot.key] =
                WorldBuilderHudView.SlotButton(Size(slotSize, slotSize)).addTo(strip).apply {
                    xy(index * (slotSize + gap), 0.0)
                    fittedImage(slot.bitmap, slotSize, slotSize - SPRITE_INSET, slot.imageName)
                    onClick { if (!isDragged) this@PaletteRowView.onTapped(slot.key) }
                }
        }
        scrollToOffset(0.0)
    }

    open fun scrollTo(index: Int) {
        if (index !in 0 until slotButtons.size) return
        val left = index * (slotSize + gap)
        val right = left + slotSize
        when {
            left + scroll < 0 -> scrollToOffset(-left)
            right + scroll > rowWidth -> scrollToOffset(rowWidth - right)
        }
    }

    fun scrollTo(key: String) = scrollTo(slotButtons.keys.indexOf(key))

    private fun scrollToOffset(offset: Double) {
        strip.x = BORDER + offset.clamp(minScroll, 0.0)
    }
}
