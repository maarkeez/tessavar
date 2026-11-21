package com.mkz.tessavar.screen.adapters.presentation

import korlibs.korge.tests.ViewsForTesting
import korlibs.math.geom.Point

fun PaletteRowView.isSlotInView(key: String): Boolean {
    val button = slotButtons.getValue(key)
    val left = button.x + scroll
    return left >= 0.0 && left + button.width <= width - 2 * PaletteRowView.BORDER
}

suspend fun ViewsForTesting.dragPaletteRow(
    rowView: PaletteRowView,
    distance: Double,
) {
    val start = rowView.localToGlobal(Point(PaletteRowView.BORDER + 2.5 * (PaletteRowView.SLOT_SIZE + PaletteRowView.SLOT_GAP), rowView.height / 2))
    mouseMoveTo(start.x, start.y)
    mouseDown()
    mouseMoveTo(start.x + distance / 2, start.y)
    mouseMoveTo(start.x + distance, start.y)
    mouseUp()
}
