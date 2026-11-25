package com.mkz.tessavar.shared.adapters.presentation

import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.RoundRect
import korlibs.korge.view.roundRect
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Size

object HudStyle {
    val BACKGROUND = RGBA(0xD9, 0xC9, 0xA3)

    val PANEL = RGBA(0xF3, 0xE7, 0xC9)

    val PANEL_RAISED = RGBA(0xFF, 0xF7, 0xE3)

    val STROKE = RGBA(0x8B, 0x6A, 0x43)

    val TRACK = RGBA(0xC9, 0xB5, 0x8C)

    val TEXT = RGBA(0x2E, 0x22, 0x16)

    val MUTED = RGBA(0x5C, 0x48, 0x33)

    val FAINT = RGBA(0x73, 0x5E, 0x48)

    val SELECTION = RGBA(0x8A, 0x53, 0x00)

    val BUTTON = RGBA(0x8B, 0x5E, 0x34)
    val BUTTON_PRESSED = RGBA(0xA2, 0x70, 0x40)
    val BUTTON_BORDER = RGBA(0x5A, 0x3B, 0x1E)
    val BUTTON_LABEL = RGBA(0xFF, 0xF7, 0xE3)
    val DISABLED = RGBA(0xCF, 0xC0, 0x9E)
    val DISABLED_LABEL = RGBA(0x8A, 0x7A, 0x62)
    val DISABLED_BORDER = RGBA(0xB0, 0x9C, 0x7A)

    val ACCENT = RGBA(0xA8, 0x32, 0x2D)
    val ACCENT_PRESSED = RGBA(0x8E, 0x29, 0x24)

    val ROW = RGBA(0xE6, 0xD6, 0xB2)
    val SELECTED_ROW = RGBA(0xF2, 0xCF, 0x7E)
    val EMPTY_SLOT = RGBA(0xDD, 0xCB, 0xA4)
    val UNMARKED = RGBA(0xD8, 0xC6, 0x9E)

    val TALK_BACKGROUND = PANEL
    val TALK_NAME = RGBA(0x9A, 0x3B, 0x1E)

    val ALLY = RGBA(0x5A, 0xD2, 0x5A)
    val ENEMY = RGBA(0xFF, 0x37, 0x5F)
    val DEFEATED = RGBA(0x8A, 0x8A, 0xA0)

    val COIN = RGBA(0xFF, 0xD8, 0x4A)
    val COIN_MIDDLE = RGBA(0xC7, 0x8A, 0x1E)
    val COIN_PRESSED = RGBA(0xA6, 0x73, 0x1A)

    val DANGER = RGBA(211, 47, 47)
    val DANGER_PRESSED = RGBA(183, 35, 35)

    val HEALTH = RGBA(255, 55, 95)
    val MANA = RGBA(0, 145, 255)

    val MANA_INK = RGBA(0x1F, 0x5C, 0x99)

    val GAIN = RGBA(0x25, 0x6B, 0x2A)

    val LOSS = ACCENT

    val TEXT_COLORS = listOf(TEXT, MUTED, FAINT, SELECTION, GAIN, LOSS, MANA_INK, TALK_NAME)

    val PANELS = listOf(PANEL, PANEL_RAISED)

    val BUTTON_LABELS = listOf(BUTTON to BUTTON_LABEL, ACCENT to BUTTON_LABEL, DANGER to BUTTON_LABEL, COIN_MIDDLE to TEXT)

    const val CAPTION = 12.0
    const val BODY = 15.0
    const val NAME = 18.0
    const val TITLE = 22.0
    const val DISPLAY = 40.0

    const val PANEL_RADIUS = 10.0
    const val SLOT_RADIUS = 8.0

    fun Container.panel(
        size: Size,
        fill: RGBA = PANEL,
        radius: Double = PANEL_RADIUS,
        stroke: RGBA = STROKE,
    ): RoundRect = roundRect(size, RectCorners(radius), fill = fill, stroke = stroke, strokeThickness = 1.0)
}
