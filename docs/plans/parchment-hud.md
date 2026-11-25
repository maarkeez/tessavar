# Plan: the parchment HUD

> **Status:** in progress: M1 built on `main`, its screenshots still to be approved.
>
> **Summary:** Today the HUD is dark: its panels are a dark navy (`HudStyle.PANEL`, `#171725`), the game's background a
> dark grey (`#2b2b2b`), and its text is KorGE's default sans-serif, which doesn't look like a game. Some text is too
> small to read on a phone: 15 sizes are under 12 px, down to the 9 px stack count on an effect badge
> (`EffectView.BADGE_TEXT_SIZE`). Instead, the HUD is light parchment with dark ink: cream panels, brown buttons, a deep
> red accent, Alegreya SC for titles, names and buttons, Alegreya for running text, and no text under 12 px. It was
> picked from a prototype of three directions (Tavern, Parchment and Arcade) on the branch `claude/project-thread-gn6k33`,
> compared on the screenshots of the
> [Tessavar theme prototypes](https://claude.ai/artifact/CAY2PPP8M3EQwc39VMtizc). It builds on the vector HUD chrome
> ([ADR 0007](../adr/0007-vector-hud-chrome-around-pixel-art.md)), which keeps the colours of the chrome in `HudStyle`.

## How the HUD looks today

- `HudStyle` holds the panel colours: `PANEL` `#171725`, `PANEL_RAISED` `#222236`, `STROKE` `#363650`, `MUTED`
  `#A4A4BE`, `FAINT` `#6C6C88` and `SELECTION` `#FFD84A`. Other chrome colours are constants of their own views, e.g.:
  `GameButton.GREY` (`Colors.DIMGRAY`), `InventorySlotView.EMPTY_COLOR` `#343434`, `FormationView.ROW_COLOR` `#343434`,
  `TalkView.BACKGROUND_COLOR` `#211640`, `ConfirmButton.COLOR` `#0088FF` and `VirtualPadView.BUTTON_COLOR` `#871646`.
- `Main.kt`, `PhoneFrameView.SCREEN_COLOR` and the four world builder forms (`WorldBuilderFormView`,
  `WorldBuilderStockView`, `WorldBuilderDetailsView`, `WorldBuilderResizeView`) paint the background `#2b2b2b`.
- About 60 texts on a panel are drawn `Colors.WHITE`, and the `uiText` views and `UIButton` labels are white by KorGE's
  default.
- Every text uses `DefaultTtfFontAsBitmap`, by default or by name (e.g.: `TalkView`, `ConsentView`,
  `PrivacyNoticeView`, `ConfirmDialogView`, `CommunityWorldsView`).
- Text sizes are constants of each view, from 9 px to 64 px. Under 12 px: `EffectView.BADGE_TEXT_SIZE` (9), and 10 or
  11 px in `BattleUnitInfoView`, `AbilityButtonView`, `RecipientPreviewView`, `QuestViewParts`, `QuestJournalView`,
  `QuestTrackerView`, `QuestMarkerView`, `CastPreviewView`, `RemainingSummonDurationView` and `PreviewBarView`.

## Decisions

| Topic        | Decision                                                                                                                                                                                                                                                                                                                                                                     |
|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope        | The chrome of every screen of the game: the menu, the dialogs, the HUD, the talk panel, the shop, the quests, the pause, the save slots, the virtual pad and the world builder. The Storybook's own chrome (sidebar, toolbar, controls) stays dark, as a developer tool.                                                                                                     |
| One look     | The parchment look replaces the dark one, with no theme setting. Chosen over keeping the prototype's `UiTheme` switch, as a second look doubles every screenshot to check and nobody asked to pick one in the game.                                                                                                                                                          |
| Palette      | The prototype's Parchment colours, in `HudStyle`: background `#D9C9A3`, panel `#F3E7C9`, raised `#FFF7E3`, stroke `#8B6A43`, track `#C9B58C`, text `#2E2216`, muted `#5C4833`, faint `#735E48`, selection `#8A5300`, button `#8B5E34` with a `#FFF7E3` label, and the accent (confirm, Finish turn, the pad's buttons, Human turn) `#A8322D`.                                |
| Colours kept | The pixel art, the health and mana bars (`HEALTH` `#FF375F`, `MANA` `#0091FF`), the ally and enemy rings, and `CPU_TURN_COLOR`. Text drawn on a coloured fill (a bar's value inside it, a solid pill, a cooldown over its overlay) stays white.                                                                                                                              |
| Contrast     | Every text colour has a contrast of at least 4.5:1 on the panel it is drawn on, e.g.: text `#2E2216` on panel `#F3E7C9` is 12.6:1, muted `#5C4833` 7.0:1 and faint `#735E48` 5.0:1. Highlights darken for paper: `SELECTION` from `#FFD84A` to `#8A5300` (5.2:1), and `GAIN` from `#5AD25A` to `#256B2A` (5.3:1). A disabled label is exempt, as it is not meant to be read. |
| Fonts        | Alegreya SC Bold for titles, names, captions and button labels, and Alegreya Medium (mixed case) for running text: the talk line, the consent and privacy text, quest descriptions, the effect and ability info. Chosen over Alegreya SC everywhere, as the prototype's all small caps made long text slow to read. Both are under the SIL Open Font License 1.1.            |
| Font loading | `HudFonts` loads both from `resources/ui/fonts/` through `ResourcesBundle` before the first scene, in `startApp`, `startStorybook` and the tests' `sharedViewsTest`, and turns each into a `BitmapFont`. Views name `HudFonts.TITLE` or `HudFonts.BODY`; `DefaultTtfFontAsBitmap` is no longer used by the game.                                                             |
| Text size    | A type scale in `HudStyle` replaces each view's own sizes: `CAPTION` 12, `BODY` 15, `NAME` 18, `TITLE` 22 and `DISPLAY` 40 (the menu's title). No text is under 12 px. Chosen over the prototype's font drawn 1.2× bigger than asked, which kept the layouts at the old sizes, so quest lines and long names got cramped.                                                    |
| Layout       | The panels with a fixed height grow to their text at the new sizes, e.g.: the battle unit info's header, the quest detail's description, the effect info and the shop's item line. A long name is cut with `QuestViewParts.fitted`, as today.                                                                                                                                |
| Architecture | `screen.adapters.presentation` (`HudStyle`, `HudFonts`, the views) and `shared.adapters.presentation` (`GameButton`, `BarView`, `PreviewBarView`). The fonts are resources under `resources/ui/fonts/`. No domain or use case changes.                                                                                                                                       |
| Storybook    | No story is added. Every story is checked on a screenshot at each milestone, with the [storybook skill](../../.claude/skills/storybook/SKILL.md).                                                                                                                                                                                                                            |
| Tests        | A `HudStyleTest` for the contrast of every text colour on its panel, a `HudFontsTest` for the fonts loading, and a `TextSizeArchitectureTest` that fails on a text size constant under 12 in `screen.adapters.presentation`.                                                                                                                                                 |
| Out of scope | The battlefield's void, drawn black by the terrain art ([ADR 0005](../adr/0005-famicube-palette-and-outline.md)), the portraits and icons, the Storybook's own chrome, a dark mode, and a setting for the text size.                                                                                                                                                         |
| Delivery     | 4 milestones, each green on CI. See [Milestones](#milestones).                                                                                                                                                                                                                                                                                                               |

For example, the battle unit info of the Mage, before and after:

| Piece        | Today                          | After                              |
|--------------|--------------------------------|------------------------------------|
| Panel        | `#171725`, stroke `#363650`    | `#F3E7C9`, stroke `#8B6A43`        |
| Name, "Mage" | white, default font, 18 px     | `#2E2216`, Alegreya SC Bold, 18 px |
| HP and MP    | `#6C6C88`, default font, 10 px | `#735E48`, Alegreya SC Bold, 12 px |
| "90/100"     | white, 12 px                   | `#2E2216`, 12 px                   |
| Finish turn  | `#0088FF`, white label         | `#A8322D`, `#FFF7E3` label         |
| Background   | `#2b2b2b`                      | `#D9C9A3`                          |

## Milestones

Each milestone is a few commits straight on `main`, green on CI.

| #  | Milestone  | Done when                                                                                                   | Status | Done in |
|----|------------|-------------------------------------------------------------------------------------------------------------|--------|---------|
| M1 | Palette    | Every screen is parchment with dark text, in the default font, and `HudStyleTest` checks the contrast.      | ✅      | 9a1559e |
| M2 | Fonts      | Every text of the game is in Alegreya SC Bold or Alegreya Medium, and the fonts are credited.               | ⬜      |         |
| M3 | Text sizes | Every text uses the type scale, none is under 12 px, and no story clips or overlaps text on its screenshot. | ⬜      |         |
| M4 | Docs       | The ADR is written, and the prototype branch is deleted.                                                    | ⬜      |         |

### M1 commits

1. Move every chrome colour into `HudStyle`, at today's values: `GameButton`'s, `ConfirmButton`'s,
   `InventorySlotView.EMPTY_COLOR`, `FormationView`'s rows, `AbilityInfoView.UNMARKED_COLOR`, `TalkView`'s,
   `VirtualPadView`'s, `BarView.TRACK` and `TRACK_STROKE`, and a new `HudStyle.BACKGROUND` for `Main.kt`,
   `PhoneFrameView` and the world builder forms. Nothing changes on screen.
2. Add `HudStyle.TEXT` and use it for every text drawn on a panel, in place of `Colors.WHITE`, and for the `uiText`
   views and button labels. Nothing changes on screen.
3. Change `HudStyle` to the parchment palette, with the darker `SELECTION` and `GAIN`, and add `HudStyleTest`.

### M2 commits

1. Add Alegreya SC Bold and Alegreya Medium under `resources/ui/fonts/` with their `OFL.txt`, credit them in
   `CREDITS.md`, and add `HudFonts`, loaded in `startApp`, `startStorybook` and `sharedViewsTest`, with `HudFontsTest`.
2. Use `HudFonts.TITLE` for the titles, names, captions and buttons (`GameButton`, `ConfirmButton`, `CancelButton` and
   the `uiButton`s), and `HudFonts.BODY` for the running text.

### M3 commits

1. Add the type scale to `HudStyle` and use it in every view of `screen.adapters.presentation` and
   `shared.adapters.presentation`, with `TextSizeArchitectureTest`.
2. Grow the panels with a fixed height to their text, story by story, checked on their screenshots.

### M4 commits

1. Write ADR 0024, the parchment HUD with Alegreya, and link it from ADR 0007.
2. Mark this plan as done, and delete the branch `claude/project-thread-gn6k33`.

## Reference

### HudStyle

- `BACKGROUND` `#D9C9A3`, `PANEL` `#F3E7C9`, `PANEL_RAISED` `#FFF7E3`, `STROKE` `#8B6A43`, `TRACK` `#C9B58C`.
- `TEXT` `#2E2216`, `MUTED` `#5C4833`, `FAINT` `#735E48`, `SELECTION` `#8A5300`, `GAIN` `#256B2A`.
- `BUTTON` `#8B5E34`, `BUTTON_PRESSED` `#A27040`, `BUTTON_BORDER` `#5A3B1E`, `BUTTON_LABEL` `#FFF7E3`, `DISABLED`
  `#CFC09E` with a `#8A7A62` label.
- `ACCENT` `#A8322D` and `ACCENT_PRESSED` `#8E2924`, for `ConfirmButton`, `FinishPlayerTurnView`, `VirtualPadView`'s
  buttons and `BannerView.HUMAN_TURN_COLOR`.
- `ROW` `#E6D6B2`, `SELECTED_ROW` `#F2CF7E`, `EMPTY_SLOT` `#DDCBA4`, `UNMARKED` `#D8C69E`, and the talk panel's name
  `#9A3B1E`.
- `CAPTION` 12, `BODY` 15, `NAME` 18, `TITLE` 22, `DISPLAY` 40.

### HudFonts

- `HudFonts.load(root: VfsFile)` reads `ui/fonts/alegreya-sc-bold.ttf` and `ui/fonts/alegreya-medium.ttf` with
  `readTtfFont()` and turns each into a `BitmapFont` at 48 px, so a text is drawn sharp up to the menu's 40 px title.
- `HudFonts.TITLE` and `HudFonts.BODY` are those fonts. Before `load`, both are `DefaultTtfFontAsBitmap`, so a view
  test that never loads them still draws its text.
- `UIButton.applyHudFont()` sets a button's label to `HudFonts.TITLE`, as KorGE's `UIButton` builds its own
  `RichTextData` with `DefaultTtfFontAsBitmap`.

### Tests

- `HudStyleTest`
  - `HudStyle should draw every text colour at a contrast of at least 4.5 to 1 when it is on a panel`
  - `HudStyle should draw every button label at a contrast of at least 4.5 to 1 when it is on its button`
- `HudFontsTest`
  - `HudFonts should load the title and body fonts when it reads the game resources`
  - `HudFonts should fall back to the default font when the fonts are not loaded`
- `TextSizeArchitectureTest` (in `shared.adapters.architecture`, like `StoryCoverageArchitectureTest`)
  - `Views should not draw text under 12 px when they set a text size constant`

## Known limitations

- The battlefield's void stays black around a light HUD, until its terrain art is redrawn.
- A light screen is brighter at night, and there is no dark mode.
- Alegreya SC and Alegreya add about 500 KB to `resources.zip`.
- The prototype's `UiTheme`, `UiFont` and `?theme=` link stay on the branch `claude/project-thread-gn6k33` until M4
  deletes it, and are not merged.

## Changes while building

- **M1, `HudStyle` in `shared.adapters.presentation`.** It moved there from `screen.adapters.presentation`, as
  `GameButton` and `BarView` (in `shared`) take their colours from it, and `shared` can't depend on `screen`.
  `GameButton.GREY`, `LABEL` and the others stay, as names for the `HudStyle` colours.
- **M1, inks for the texts in a light colour.** Some texts were drawn in a colour too light to read on parchment, so
  they take a darker ink, and `HudStyleTest` checks them too: the mana texts (the mana pills, "Not enough mana") in
  `MANA_INK` `#1F5C99` (5.6:1), the cooldown texts in `SELECTION`, a quest completed in `GAIN` instead of `ALLY`, the
  menu's title in `SELECTION` instead of `COIN_MIDDLE`, the Wishlist label in `TEXT`, and the world builder's error
  and info messages in `ACCENT_PRESSED` and `MUTED`. `PillView` takes an `ink` for its label and glyph.
- **M1, the gold of a coin.** `SELECTION` was also the gold of the coins and of the quest markers over the npcs, so a
  new `HudStyle.COIN` (`#FFD84A`, today's `SELECTION`) keeps them gold.
- **M1, the world builder's selected tool and the inventory's selected slot.** The selected tool was blue (`#006ACC`)
  and the selected slot's frame white, which don't fit parchment: they are `ACCENT` and `SELECTION`.
- **M1, the turn line of the banner.** It is drawn on the background, not on a panel: Human turn in `ACCENT` is 4.1:1
  and CPU turn in the kept `CPU_TURN_COLOR` 2.7:1 there. Both colours also tint the battlefield, so they stay.

