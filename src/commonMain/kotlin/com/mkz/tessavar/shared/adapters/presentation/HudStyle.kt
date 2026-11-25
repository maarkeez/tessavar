package com.mkz.tessavar.shared.adapters.presentation

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.RoundRect
import korlibs.korge.view.roundRect
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Size

/**
 * The colours of the chrome of the game: its background, panels, buttons and text (see ADR 0007). The pixel art keeps
 * its own palette.
 */
object HudStyle {
    val BACKGROUND = RGBA(0x2B, 0x2B, 0x2B)

    val PANEL = RGBA(0x17, 0x17, 0x25)

    val PANEL_RAISED = RGBA(0x22, 0x22, 0x36)

    val STROKE = RGBA(0x36, 0x36, 0x50)

    val TRACK = RGBA(0x0E, 0x0E, 0x18)

    val TEXT = RGBA(0xFF, 0xFF, 0xFF)

    val MUTED = RGBA(0xA4, 0xA4, 0xBE)

    val FAINT = RGBA(0x6C, 0x6C, 0x88)

    val SELECTION = RGBA(0xFF, 0xD8, 0x4A)

    val BUTTON = RGBA(0x69, 0x69, 0x69)
    val BUTTON_PRESSED = RGBA(85, 85, 85)
    val BUTTON_BORDER = RGBA(0xD3, 0xD3, 0xD3)
    val BUTTON_LABEL = RGBA(0xFF, 0xFF, 0xFF)
    val DISABLED = RGBA(58, 58, 58)
    val DISABLED_LABEL = RGBA(138, 138, 138)
    val DISABLED_BORDER = RGBA(85, 85, 85)

    val ACCENT = RGBA(0, 136, 255)
    val ACCENT_PRESSED = RGBA(30, 110, 244)

    val PAD_ARROW = RGBA(76, 76, 76)
    val PAD_ARROW_HELD = RGBA(110, 110, 110)
    val PAD_BUTTON = RGBA(135, 22, 70)
    val PAD_BUTTON_PRESSED = RGBA(207, 60, 113)
    val PAD_BUTTON_BORDER = RGBA(79, 21, 7)

    val ROW = RGBA(0x34, 0x34, 0x34)
    val SELECTED_ROW = RGBA(0x00, 0x52, 0x80)
    val EMPTY_SLOT = RGBA(0x34, 0x34, 0x34)
    val UNMARKED = RGBA(0x2A, 0x2A, 0x40)

    val TALK_BACKGROUND = RGBA(33, 22, 64)
    val TALK_NAME = RGBA(255, 231, 55)

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
