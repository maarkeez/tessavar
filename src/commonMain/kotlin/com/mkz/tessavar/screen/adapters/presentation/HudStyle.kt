package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.BarView
import com.mkz.tessavar.shared.adapters.presentation.UiTheme
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.RoundRect
import korlibs.korge.view.roundRect
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Size

object HudStyle {
    val PANEL get() = UiTheme.current.panel

    val PANEL_RAISED get() = UiTheme.current.panelRaised

    val STROKE get() = UiTheme.current.stroke

    val TRACK get() = BarView.TRACK

    val MUTED get() = UiTheme.current.muted

    val FAINT get() = UiTheme.current.faint

    val SELECTION get() = UiTheme.current.selection

    val ALLY = RGBA(0x5A, 0xD2, 0x5A)
    val ENEMY = RGBA(0xFF, 0x37, 0x5F)
    val DEFEATED = RGBA(0x8A, 0x8A, 0xA0)

    val COIN_MIDDLE = RGBA(0xC7, 0x8A, 0x1E)
    val COIN_PRESSED = RGBA(0xA6, 0x73, 0x1A)

    val DANGER = RGBA(211, 47, 47)
    val DANGER_PRESSED = RGBA(183, 35, 35)

    val HEALTH = RGBA(255, 55, 95)
    val MANA = RGBA(0, 145, 255)

    val GAIN = ALLY

    const val PANEL_RADIUS = 10.0
    const val SLOT_RADIUS = 8.0

    fun Container.panel(
        size: Size,
        fill: RGBA = PANEL,
        radius: Double = PANEL_RADIUS,
        stroke: RGBA = STROKE,
    ): RoundRect = roundRect(size, RectCorners(radius), fill = fill, stroke = stroke, strokeThickness = 1.0)
}
