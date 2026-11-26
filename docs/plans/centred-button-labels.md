# Plan: Centre the labels of the buttons on their capitals

> **Status:** built on `main` in 2102233, its screenshots still to be approved.
>
> **Summary:** Since the HUD moved to Alegreya ([ADR 0024](../adr/0024-parchment-hud-with-alegreya.md), the
> [parchment HUD plan](parchment-hud.md)), every button draws its label low: on the main menu, Play new game, Load
> game and the five buttons under them show their label 3.5 px under the middle of their 32 px button. It looks
> careless on the first screen of the game. Instead, `applyHudFont` lifts the label by an amount worked out from the
> font, so the capitals of every button's label sit on the middle of the button, at any text size.

## Cause

`UIButton` lays its label out in a `TextBlock` aligned `TextAlignment.MIDDLE_CENTER`. KorGE's `RichTextData.place`
centres a box as tall as the font's ascent, and puts the baseline at the bottom of that box. Alegreya keeps room for
accents above its capitals: its ascent is 1016 units of 1000, its capitals 649 and its small caps 510. So the visible
letters fill only the bottom of the box:

| At `HudStyle.BODY` (15 px), in a 32 px button | Today                       | Once built        |
|-----------------------------------------------|-----------------------------|-------------------|
| Ascent box                                    | 8.4 to 23.6 px              | 5.6 to 20.9 px    |
| Capitals (9.7 px)                             | 13.9 to 23.6 px, 2.8 px low | centred           |
| Small caps (7.7 px)                           | 15.9 to 23.6 px, 3.8 px low | 1 px low          |

The labels drawn as a `Text` and centred by hand, as `(height - label.height) / 2` in `PillView` and `GoldButtonView`,
centre the whole line, ascent and descent, which puts their capitals within 0.2 px of the middle. They are left as they
are.

## Decisions

| Topic        | Decision                                                                                                                                                                                                                                                                             |
|--------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope        | Every button that calls `applyHudFont`: `GameButton`, `ConfirmButton`, `CancelButton`, and the buttons of `InventoryFullView`, `GiveItemView` and `WorldBuilderMenuView`. E.g.: the main menu, the pause screen, the save slots, the item buttons, the world builder menu.            |
| Centred on   | The capitals: the middle of a label's capitals is the middle of its button, and its small caps sit 1 px low. Chosen over centring the small caps, as the first capital of each label would stick up 2 px, and over halfway between, as the pills and the gold button already centre their capitals. |
| How          | `applyHudFont` lifts the label by half the font's ascent less its capital height, read from `HudFonts.TITLE` at the label's text size, so it holds at any size. Chosen over a fixed 3 px nudge, which is wrong at every other size, and over building the bitmap fonts with a smaller ascent, which also moves every other text and its line spacing. |
| Two lines    | A label on two lines, such as a save slot's, moves by the same amount, which centres the block from its first line's capitals to its last line's baseline. Nothing else is needed.                                                                                                      |
| Plain text   | The labels drawn as a `Text` (`PillView`, `GoldButtonView`, `BannerView`) are not changed, as they already centre their capitals.                                                                                                                                                     |
| Architecture | Only `shared.adapters.presentation`: `HudFonts.kt`. No caller changes.                                                                                                                                                                                                                 |
| Storybook    | No story is added. **Screen / Menu › Options**, **Screen / Pause › Options**, **Screen / Save slots › Load** and **Screen / Item buttons › Consumable** are compared before and after on screenshots, on a private page, for approval.                                                 |
| Tests        | One test per text size in `HudFontsTest`: the capitals of a label sit within 0.5 px of the middle of their button.                                                                                                                                                                       |
| Docs         | No ADR, as it is easy to undo and was no real trade-off: a comment over `applyHudFont` says why it lifts the label.                                                                                                                                                                     |
| Out of scope | Labels drawn as a `Text`, the font files and `HudFonts.ATLAS_SIZE`, and every text that is not centred in a box.                                                                                                                                                                      |

## HudFonts

- `HudFonts.labelLift(textSize)` returns half of the `HudFonts.TITLE` ascent less its capital height, both at
  `textSize`: the ascent from `getFontMetrics(textSize).ascent`, the capital height from the bounds of
  `getGlyphMetrics(textSize, 'H'.code)`. E.g.: about 2.8 px at `HudStyle.BODY` 15, about 4 px at `HudStyle.TITLE` 22.
- `UIButton.applyHudFont(textSize)` sets the font as today, then the bottom padding of the button's `textView` to
  `2 * labelLift(textSize)`. `RichTextData.place` centres in the height left above the padding, so the label moves up
  by `labelLift`. The padding stays when a caller then sets the button's `width` or `text`, as `ConfirmButton` and
  `CancelButton` do.
- A comment over `applyHudFont` says that Alegreya's ascent keeps room for accents above its capitals, which KorGE
  centres along with the letters.

## Storybook

- No story changes. The before screenshots of **Screen / Menu › Options**, **Screen / Pause › Options**,
  **Screen / Save slots › Load** and **Screen / Item buttons › Consumable** are taken with the
  [storybook skill](../../.claude/skills/storybook/SKILL.md) before the change, the after ones once built. Both are
  shown side by side on a private Artifact page for approval.

## Tests

- `HudFontsTest`
  - `applyHudFont should centre the capitals of a label on its button when the label is at the body size`
  - `applyHudFont should centre the capitals of a label on its button when the label is at the title size`

  Each lays a `GameButton` 32 px high out with `RichTextData.place`, over the `textView`'s size and padding, and checks
  that the first baseline less half the capital height is within 0.5 px of 16.

## Known limitations

- The small caps, most of each label, sit 1 px under the middle, as the capitals are centred.
- A text size set on a button after `applyHudFont`, through `UIButton.textSize`, keeps the lift of the earlier size.
  No caller does it today.
- Like the rest of the parchment HUD, it is checked on Storybook screenshots, not on a real phone.

## Changes while building

- **The ascent is the font's top.** KorGE's `BitmapFont` reports the font's top, 1065 units, as its ascent, not 1016.
  So the capitals were 3.1 px low at `HudStyle.BODY`, not 2.8, and `labelLift` is about 3.1 px at 15 and 4.6 px at 22.
  `labelLift` reads the ascent from `HudFonts.TITLE`, so it was already right.
- **The capital height comes from the TTF.** `HudFonts.load` reads it once from `alegreya-sc-bold.ttf`, as the top of
  `H` at 1000 units, 649. The `BitmapFont`'s and the TTF's glyph metrics at the label's size are rounded to whole
  pixels, which would be off by up to half a pixel.
- **The label moves by `textView.y`, not by padding.** Bottom padding shrinks the height `RichTextData.place` fits the
  lines in, which a two-line label, such as a save slot's in its 48 px row, needs. `applyHudFont` sets
  `textView.y = -labelLift(textSize)` instead, which `UIButton` never resets.
- **No comment over `applyHudFont`.** `CommentArchitectureTest` allows no comment in the source files. The reason is in
  `labelLift`'s name and in this plan.
- **The measurement is in the page object.** `ViewTestPageObjectArchitectureTest` forbids a views test to import view
  classes, so `GameButton.labelBaseline()` in `GameButtonExtension.kt` lays the label out for the tests.
