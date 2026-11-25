package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.HudFonts
import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import korlibs.image.text.TextAlignment
import korlibs.korge.style.styles
import korlibs.korge.style.textAlignment
import korlibs.korge.style.textColor
import korlibs.korge.style.textFont
import korlibs.korge.style.textSize
import korlibs.korge.ui.UIContainer
import korlibs.korge.ui.uiText
import korlibs.korge.view.setText
import korlibs.math.geom.Size

class UnitNameView(
    size: Size,
) : UIContainer(size) {
    internal val unitNameLabel =
        uiText("", size = size) {
            styles.textColor = HudStyle.TEXT
            styles.textFont = HudFonts.TITLE
            styles.textSize = HudStyle.BODY
        }

    init {
        addChild(unitNameLabel)
    }

    fun display(unitName: String) {
        unitNameLabel.setText(QuestViewParts.fitted(unitName, HudStyle.BODY, width))
        visible = true
    }

    fun alignTextToRight() {
        unitNameLabel.styles.textAlignment = TextAlignment.RIGHT
    }

    fun hide() {
        visible = false
    }
}
