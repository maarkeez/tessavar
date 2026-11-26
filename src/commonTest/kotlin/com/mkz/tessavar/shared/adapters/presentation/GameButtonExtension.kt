package com.mkz.tessavar.shared.adapters.presentation

import korlibs.image.text.place
import korlibs.math.geom.Rectangle

fun GameButton.isFaded(): Boolean = textColor == GameButton.DISABLED_LABEL && background.borderColor == GameButton.DISABLED_BORDER

fun GameButton.backgroundColorWhenDisabled() = bgColorDisabled

fun GameButton.labelBaseline(): Double {
    val placements = textView.text.place(Rectangle(0.0, 0.0, textView.width, textView.height), align = textView.align).placements
    return textView.y + placements.first().pos.y
}
