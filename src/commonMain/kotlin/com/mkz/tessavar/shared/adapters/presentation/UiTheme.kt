package com.mkz.tessavar.shared.adapters.presentation

import korlibs.image.color.RGBA

/**
 * PROTOTYPE: a look for the chrome of the game (the panels, buttons, text), to compare a few directions side by side in
 * the Storybook (the `theme` global) and in the game (`?theme=<id>`). The colours read from [current] when a view is
 * built, so a view built after [select] takes the new look.
 */
class UiTheme(
    val id: String,
    val name: String,
    val background: RGBA,
    val panel: RGBA,
    val panelRaised: RGBA,
    val stroke: RGBA,
    val track: RGBA,
    val text: RGBA,
    val muted: RGBA,
    val faint: RGBA,
    val selection: RGBA,
    val button: RGBA,
    val buttonOver: RGBA,
    val buttonBorder: RGBA,
    val buttonLabel: RGBA,
    val disabled: RGBA,
    val disabledLabel: RGBA,
    val disabledBorder: RGBA,
    val row: RGBA,
    val selectedRow: RGBA,
    val emptySlot: RGBA,
    val unmarked: RGBA,
    val talkBackground: RGBA,
    val talkName: RGBA,
    val accent: RGBA,
    val accentPressed: RGBA,
    /** The font file under `resources/ui/fonts`, or null for KorGE's default font. */
    val fontFile: String?,
    /** How much bigger than the size a view asks for its text is drawn. */
    val textScale: Double,
) {
    companion object {
        val CURRENT =
            UiTheme(
                id = "current",
                name = "Current",
                background = RGBA(0x2B, 0x2B, 0x2B),
                panel = RGBA(0x17, 0x17, 0x25),
                panelRaised = RGBA(0x22, 0x22, 0x36),
                stroke = RGBA(0x36, 0x36, 0x50),
                track = RGBA(0x0E, 0x0E, 0x18),
                text = RGBA(0xFF, 0xFF, 0xFF),
                muted = RGBA(0xA4, 0xA4, 0xBE),
                faint = RGBA(0x6C, 0x6C, 0x88),
                selection = RGBA(0xFF, 0xD8, 0x4A),
                button = RGBA(0x69, 0x69, 0x69),
                buttonOver = RGBA(85, 85, 85),
                buttonBorder = RGBA(0xD3, 0xD3, 0xD3),
                buttonLabel = RGBA(0xFF, 0xFF, 0xFF),
                disabled = RGBA(58, 58, 58),
                disabledLabel = RGBA(138, 138, 138),
                disabledBorder = RGBA(85, 85, 85),
                row = RGBA(0x34, 0x34, 0x34),
                selectedRow = RGBA(0x00, 0x52, 0x80),
                emptySlot = RGBA(0x34, 0x34, 0x34),
                unmarked = RGBA(0x2A, 0x2A, 0x40),
                talkBackground = RGBA(33, 22, 64),
                talkName = RGBA(255, 231, 55),
                accent = RGBA(0, 136, 255),
                accentPressed = RGBA(30, 110, 244),
                fontFile = null,
                textScale = 1.0,
            )

        /** Warm, mid-tone wood and brass, a pixel font, text 15% bigger. */
        val TAVERN =
            UiTheme(
                id = "tavern",
                name = "Tavern",
                background = RGBA(0x4A, 0x36, 0x2A),
                panel = RGBA(0x6E, 0x4B, 0x33),
                panelRaised = RGBA(0x87, 0x5D, 0x3F),
                stroke = RGBA(0xD9, 0xA4, 0x5B),
                track = RGBA(0x3B, 0x27, 0x1A),
                text = RGBA(0xFF, 0xF6, 0xE2),
                muted = RGBA(0xF0, 0xD9, 0xB0),
                faint = RGBA(0xC9, 0xA5, 0x7A),
                selection = RGBA(0xFF, 0xD8, 0x4A),
                button = RGBA(0x9A, 0x63, 0x3A),
                buttonOver = RGBA(0xB3, 0x76, 0x46),
                buttonBorder = RGBA(0xF2, 0xC2, 0x7A),
                buttonLabel = RGBA(0xFF, 0xF6, 0xE2),
                disabled = RGBA(0x5E, 0x48, 0x3A),
                disabledLabel = RGBA(0xA8, 0x92, 0x7C),
                disabledBorder = RGBA(0x7A, 0x62, 0x50),
                row = RGBA(0x7E, 0x56, 0x3A),
                selectedRow = RGBA(0xB0, 0x6A, 0x2A),
                emptySlot = RGBA(0x55, 0x3A, 0x28),
                unmarked = RGBA(0x5A, 0x3E, 0x2B),
                talkBackground = RGBA(0x6E, 0x4B, 0x33),
                talkName = RGBA(0xFF, 0xD8, 0x4A),
                accent = RGBA(0x2E, 0x9E, 0x5B),
                accentPressed = RGBA(0x26, 0x8A, 0x4E),
                fontFile = "pixelify-sans.ttf",
                textScale = 1.15,
            )

        /** Light parchment and dark ink, a fantasy small-caps serif, text 20% bigger. */
        val PARCHMENT =
            UiTheme(
                id = "parchment",
                name = "Parchment",
                background = RGBA(0xD9, 0xC9, 0xA3),
                panel = RGBA(0xF3, 0xE7, 0xC9),
                panelRaised = RGBA(0xFF, 0xF7, 0xE3),
                stroke = RGBA(0x8B, 0x6A, 0x43),
                track = RGBA(0xC9, 0xB5, 0x8C),
                text = RGBA(0x2E, 0x22, 0x16),
                muted = RGBA(0x5C, 0x48, 0x33),
                faint = RGBA(0x8A, 0x74, 0x5C),
                selection = RGBA(0xB0, 0x6C, 0x00),
                button = RGBA(0x8B, 0x5E, 0x34),
                buttonOver = RGBA(0xA2, 0x70, 0x40),
                buttonBorder = RGBA(0x5A, 0x3B, 0x1E),
                buttonLabel = RGBA(0xFF, 0xF7, 0xE3),
                disabled = RGBA(0xCF, 0xC0, 0x9E),
                disabledLabel = RGBA(0x8A, 0x7A, 0x62),
                disabledBorder = RGBA(0xB0, 0x9C, 0x7A),
                row = RGBA(0xE6, 0xD6, 0xB2),
                selectedRow = RGBA(0xF2, 0xCF, 0x7E),
                emptySlot = RGBA(0xDD, 0xCB, 0xA4),
                unmarked = RGBA(0xD8, 0xC6, 0x9E),
                talkBackground = RGBA(0xF3, 0xE7, 0xC9),
                talkName = RGBA(0x9A, 0x3B, 0x1E),
                accent = RGBA(0xA8, 0x32, 0x2D),
                accentPressed = RGBA(0x8E, 0x29, 0x24),
                fontFile = "alegreya-sc-bold.ttf",
                textScale = 1.2,
            )

        /** Bright, saturated teal and violet, a chunky rounded display font, text 30% bigger. */
        val ARCADE =
            UiTheme(
                id = "arcade",
                name = "Arcade",
                background = RGBA(0x3E, 0x8E, 0x9E),
                panel = RGBA(0x5B, 0x4B, 0xB7),
                panelRaised = RGBA(0x70, 0x5F, 0xD1),
                stroke = RGBA(0xFF, 0xFF, 0xFF),
                track = RGBA(0x33, 0x29, 0x78),
                text = RGBA(0xFF, 0xFF, 0xFF),
                muted = RGBA(0xE4, 0xDD, 0xFF),
                faint = RGBA(0xB8, 0xAE, 0xEE),
                selection = RGBA(0xFF, 0xE0, 0x3D),
                button = RGBA(0x8A, 0x7B, 0xEE),
                buttonOver = RGBA(0x9D, 0x90, 0xF5),
                buttonBorder = RGBA(0xFF, 0xFF, 0xFF),
                buttonLabel = RGBA(0xFF, 0xFF, 0xFF),
                disabled = RGBA(0x7D, 0x76, 0xA8),
                disabledLabel = RGBA(0xC4, 0xBF, 0xE0),
                disabledBorder = RGBA(0x9C, 0x95, 0xC8),
                row = RGBA(0x70, 0x5F, 0xD1),
                selectedRow = RGBA(0x2B, 0xB6, 0xD6),
                emptySlot = RGBA(0x45, 0x38, 0x96),
                unmarked = RGBA(0x45, 0x38, 0x96),
                talkBackground = RGBA(0x5B, 0x4B, 0xB7),
                talkName = RGBA(0xFF, 0xE0, 0x3D),
                accent = RGBA(0xF2, 0x8C, 0x28),
                accentPressed = RGBA(0xFF, 0xA0, 0x40),
                fontFile = "lilita-one.ttf",
                textScale = 1.3,
            )

        val ALL = listOf(CURRENT, TAVERN, PARCHMENT, ARCADE)

        var current: UiTheme = CURRENT
            private set

        fun select(id: String) {
            current = ALL.firstOrNull { theme -> theme.id == id } ?: CURRENT
        }
    }
}
