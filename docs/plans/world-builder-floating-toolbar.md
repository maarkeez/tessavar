# Plan: a full-screen world builder with a floating toolbar

> **Status:** built on the local `main`, the screenshots of M1, M2 and M3 still to be approved.
>
> **Summary:** In the world builder, the world shows in the battlefield's 384x384 viewport, 6x6 tiles of 64 px, under
> the info line, and the HUD fills the rest of the 390x844 screen below it: four tab buttons, the row of tools and the
> palette. E.g.: painting the 16x16 world of a new world shows less than a sixth of it at a time, and the Units tab's
> two rows of ten units leave the HUD half empty on the Terrain tab. Instead, the world fills the whole screen, 6x13
> tiles, under a thin strip with the title and ☰, and the HUD becomes a pill floating at the bottom: the brush, then
> Paint, Fill and Erase. Tapping the brush opens a palette card above the pill, its tabs as sprites on top and the
> options in two rows that scroll sideways. Resize, Details, Stock and the quest forms open in one sheet rising from the
> bottom. It was picked from four prototypes compared on screenshots (layout B, the floating toolbar), and builds on the
> [community worlds plan](community-worlds.md), the [finger taps plan](world-builder-finger-taps.md) and
> [ADR 0007](../adr/0007-vector-hud-chrome-around-pixel-art.md).

## Decisions

| Topic         | Decision                                                                                                                                                                                                                                                                                                                                       |
|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope         | The world builder only. The battle screen keeps its 6x6 viewport, so the CPU's sight ([ADR 0013](../adr/0013-only-cpu-battle-units-in-sight-wander.md)) and the trailing scroll don't change. Tiles stay 64 px, with no zoom.                                                                                                                  |
| Layout        | The floating toolbar, chosen over a bottom sheet, edge rails and hiding today's HUD behind a button, as it leaves the most of the world in view while painting and keeps the palette one tap away.                                                                                                                                             |
| World         | `BattlefieldView` fills the window (390x844) at (0, 0). It scrolls past the world's edges by as much as the UI covers it: 52 px at the top (the strip) and 104 px at the bottom (the pill and its margin), so no tile stays hidden under the UI.                                                                                               |
| Top strip     | A 52 px strip across the top, `HudStyle.PANEL` at 90%, holds `WorldBuilderInfoView` (title, size and messages) at (12, 9), and ☰ at its right as today.                                                                                                                                                                                       |
| Pill          | 270x76, centred, 22 px above the bottom, rounded to a capsule: the brush (56x56), a divider, then Paint (or Place), Fill and Erase (56x56 each). The selected tool is filled `SELECTED_TOOL_COLOR` (#006ACC); Fill is dimmed off the Terrain tab, as today.                                                                                   |
| Stock         | On the Npcs tab, the pill's middle tool shows Stock instead of Fill, which is disabled there anyway, enabled only when the npc selected stands in the world. Chosen over a Stock button under the npc rows, which would make the card a row taller.                                                                                             |
| Palette card  | Tapping the brush opens a 366x270 card 8 px above the pill: four tabs on top (each its sprite, e.g. the grass, the fortress, the hero, the blacksmith, with its name under it, the selected one underlined in `HudStyle.SELECTION`), then the palette. Picking an option, tapping the brush again or opening ☰ closes it.                        |
| Two rows      | The palette is always two rows of 68 px slots, so the card keeps its height on every tab. On Units, one row per player, titled (e.g. Human, CPU), each scrolling sideways on its own. On the other tabs, the first row holds up to four options and the second the rest, scrolling when it holds more than four. Chosen over a grid scrolling up and down, which hides one player while showing the other. |
| Scrolling     | A row scrolls by dragging it sideways. A press that moves more than 10 px is a drag and selects nothing; under that it is a tap on the slot. The row stops at its first and last slot.                                                                                                                                                         |
| Tap outside   | With the card open, a one-finger tap on the world only closes it: nothing is painted and the world doesn't drag. Taps with two or three fingers do nothing while it is open, as with the menu.                                                                                                                                               |
| Panels        | Resize, Details, Stock and the quest forms open in a sheet rising 360 px from the bottom, with a grabber, over the world. While one is open, the pill and the card are hidden.                                                                                                                                                                  |
| Style         | Vector chrome through `HudStyle`, with pixel art only for the brushes and the tab sprites, at whole scales: a 16 px sprite at 4x and a 32 px one at 2x in a slot, and the 64 px fortress at 1x ([ADR 0007](../adr/0007-vector-hud-chrome-around-pixel-art.md)).                                                                                 |
| Architecture  | All in `screen.adapters.presentation`. `WorldBuilderHudView` keeps its name and its `Delegate`, so `WorldBuilderPresenter` changes only for Stock. A new `PaletteRowView` scrolls a row of slots. Nothing changes in `worldBuilder` or `battlefield`.                                                                                       |
| Storybook     | **Screen / World builder HUD** keeps its story ids and gains an `open` control; the tab stories show the card open. New **Screen / Palette row › Default** and **› Scrolled** stories.                                                                                                                                                         |
| Tests         | View tests for the pill, the card and the rows, `BattlefieldViewTest` for the margins, and the world builder's end-to-end tests through updated page objects.                                                                                                                                                                                   |
| Out of scope  | Zoom, a palette search, more than two players' rows, the battle screen, and re-recording the world builder clip for the posts.                                                                                                                                                                                                                 |
| Delivery      | 3 milestones, each green on CI, committed on the local `main` and pushed only when the user decides, after the community posts. See [Milestones](#milestones).                                                                                                                                                                                  |

## Milestones

Each milestone is a few commits on the local `main`, green on CI, pushed when the user decides.

| #  | Milestone          | Done when                                                                                                                             | Status | Done in |
|----|--------------------|---------------------------------------------------------------------------------------------------------------------------------------|--------|---------|
| M1 | Full-screen world  | The world fills the screen under the top strip, today's HUD floats over its bottom in a panel, and Details opens in the bottom sheet. | ✅      | 0b44f0e8 |
| M2 | Floating toolbar   | The pill and the palette card replace today's HUD, with Stock in the pill on the Npcs tab, and a tap on the world closes the card.   | ✅      | 9546e56b |
| M3 | Scrolling rows     | The card's rows scroll sideways: both players' ten units are reachable on the Units tab.                                             | ✅      | a6a40275 |

### M1 commits

1. `BattlefieldView` takes `viewportSize` and `scrollMargin`, defaulting to today's 384x384 and no margin.
2. `WorldBuilderScene` lays the world out full screen, with the top strip, today's HUD in a bottom panel and the panel
   sheet.

### M2 commits

1. `WorldBuilderHudView` draws the pill and the palette card, with the tab sprites and the tap-outside catcher.
2. Stock moves into the pill on the Npcs tab, and the presenter enables it as today.
3. The world scrolls past the bottom by the pill instead of the HUD panel, and the stories and page objects follow.

### M3 commits

1. `PaletteRowView` scrolls a row of slots sideways, telling a drag from a tap, with its stories.
2. The palette card lays its two rows out as `PaletteRowView`s: one per player on Units.

## Reference

### BattlefieldView

- `BattlefieldView(resourcesLoader, viewportSize = Size(VIEWPORT_WIDTH, VIEWPORT_HEIGHT), scrollMargin = Margin.ZERO)`
  sizes its viewport, its tile window and its tint overlay from `viewportSize`, not from the constants.
- The world scrolls between `scrollMargin.left` and `viewportWidth - battlefieldWidth - scrollMargin.right`, and the
  same vertically. E.g.: the 16x16 world (1024x1024 px) in the 390x844 viewport, with 52 px at the top and 104 px at
  the bottom, scrolls vertically from 52 down to -284.
- The world builder opens with the world's top-left tile just under the strip, at (0, 52).
- `followTile`, `trailLeader` and their deadzone keep the battle's constants: the world builder never trails.

### WorldBuilderScene

- `battlefieldView = BattlefieldView(resourcesLoader, GAME_WINDOW_SIZE, Margin(top = 52, bottom = 104))` at (0, 0).
- The top strip, then `menuView` at its right, as today (`MENU_MARGIN`, 6 px).
- `panelSheet`: a `HudStyle.panel` 390 wide rising 360 px from the bottom, with a 40x5 grabber, holding
  `resizeView`, `detailsView`, `stockView` and `formView` at (12, 26). It shows while any of them `isOpen`, and
  `hudView` hides meanwhile.
- In M1, `hudView` sits in a bottom panel 250 px tall at the bottom, and the bottom margin is 250 px; M2 replaces both.

### WorldBuilderHudView

- It draws the pill (`PILL_WIDTH` 270, `PILL_HEIGHT` 76, `PILL_BOTTOM` 22) and, while `isCardOpen`, the card above it
  (`CARD_WIDTH` 366, `CARD_HEIGHT` 270). `brushButton` shows the option selected on the displayed tab, e.g. the grass
  tile on Terrain, the hero on Units.
- `tabButtons`, `toolButtons`, `paletteButtons`, `stockButton` and `Delegate` keep their meaning, so the page objects
  keep working once they open the card first.
- `brushButton` toggles the card. Picking an option calls the delegate as today, then closes the card.
- `displayTab(Tab.NPCS)` swaps Fill for `stockButton` in the pill; any other tab swaps them back.
- While the card is open, `outsideTapCatcher`, a transparent rectangle over the whole screen under the card, closes it
  on a one-finger tap and holds back the taps of several fingers, with a `FingerTap` like `WorldBuilderMenuView`'s.
- `closeCard()` is public: `WorldBuilderScene` calls it when ☰ opens.
- The palette's rows: Units shows `displayUnits(rows)` one row per player with its title (e.g. `Human`, `CPU`); the
  other tabs put their first four options in the first row and the rest in the second.

### PaletteRowView

- `PaletteRowView(width, slotSize = 68, gap = 6)` holds a row of slots in a clipped container, `display(slots)` lays
  them out, and dragging it sideways scrolls them between its first and last slot.
- A press moving more than `DRAG_DISTANCE` (10 px) scrolls and taps nothing; a shorter one taps the slot under it.
- `scrollTo(index)` brings a slot into view, so the card opens with the selected option showing, e.g. the rat, the
  eighth CPU unit.

### WorldBuilderPresenter

- `displayStock()` still calls `hudView.displayStock(isEnabled)`: only where Stock is drawn changes.

### Storybook

- `WorldBuilderHudViewStories`: a new `open` control (`boolean`), off by default. **Default**, **Other terrain** and
  **Fill tool** show the pill with the card closed; **Buildings tab**, **Units tab** and **Npcs tab** show it open, and
  **Npcs tab** shows Stock in the pill. The ids stay, so the links shared keep working.
- `PaletteRowViewStories`: **Screen / Palette row › Default** (`screen-palette-row--default`), ten units from the
  first, and **› Scrolled** (`screen-palette-row--scrolled`), scrolled to the last; its `scrolledTo` control (`int`,
  0–9).
- `StorySmokeTest` lists the new stories.

### Docs

- [How to record a clip](../testing/how-to-record-a-clip.md): nothing changes in it, but `WorldBuilderClip` opens the
  card before each tab and pick through the page objects.

### Tests

- `BattlefieldViewTest`
  - `DisplayBattlefield should scroll the battlefield past its edges by the margins when it is dragged with scroll margins`
  - `DisplayBattlefield should size its viewport and the tiles it creates to the viewport size when it is given one`
- `WorldBuilderHudViewTest`
  - `WorldBuilderHudView should display the brush, paint, fill and erase in the pill with the card closed when it is created`
  - `Brush should open the card when it is clicked and close it when clicked again`
  - `PaletteTerrain should ask to select the terrain and close the card when it is clicked`
  - `DisplayTab should show Stock instead of Fill in the pill when the Npcs tab is displayed`
  - `OutsideTapCatcher should close the card without asking to select a tile when the world is tapped with one finger`
  - `DisplayUnits should display a row of units for each player, titled with its name, in the card` (replaces
    `DisplayUnits should display a row of units for each player, titled with its name, and a click ask to select the unit of that player`)
  - The others stay, opening the card first where they click a tab or a palette option.
- `PaletteRowViewTest`
  - `PaletteRowView should scroll the slots by the distance dragged when it is dragged sideways`
  - `PaletteRowView should not tap a slot when it is dragged more than 10 px`
  - `PaletteRowView should stop at its last slot when it is dragged past it`
  - `ScrollTo should bring the slot into view when it is out of the row`
- `WorldBuilderEndToEndTest`
  - `should place a battle unit of the CPU's last unit when its row is scrolled on the Units tab`
  - The others stay, through the page objects.

### Known limitations

- A world with more than two players would need a third row on Units; the catalog has two today.
- The tabs and the slots have no keyboard focus on desktop, as today.
- The clip in the world builder post shows today's HUD until it is recorded again.

## Changes while building

- **M2 in one commit.** The pill, the card, Stock in the pill, the scene and the page objects went in one commit instead of
  three, as the world builder's end-to-end tests pass only once the view, the scene and the page objects change together.
- **The tab sprites.** Each tab shows the option selected on it, the first until one is picked (e.g. the grass, the
  fortress), instead of a fixed option per tab, so no catalog id is written in the view.
- **`SlotButton`.** The brush, the tools, Stock, the tabs and the palette's slots are `WorldBuilderHudView.SlotButton`s, a
  `UIButton` drawn with `HudStyle` and filled `SELECTED_TOOL_COLOR` when highlighted, instead of `GameButton`s. The page
  objects read `isHighlighted` and the tab's underline instead of `bgColorOut`.
- **The HUD covers the window.** `WorldBuilderHudView` is 390x844 at (0, 0), and its own area is not hit by a tap, only
  the pill, the card and the tap catcher are, so the world's two- and three-finger taps still reach `BattlefieldView`.
- **The tab sprites and labels.** The sprites are fitted in 32 px instead of 28, so a 16 px unit shows at 2x, and the
  labels are 12 px.
- **The CPU's rat in M2.** Until the rows scroll in M3, `WorldBuilderEndToEndTest` placed the CPU's bee, the second unit,
  instead of the rat, the eighth, which only M3 brings into reach.
- **`PaletteRowView.display(slots, onTapped)`.** The row takes the callback of a tapped slot with its slots, and also
  has `scrollTo(key)` and `scroll`. `PaletteRowViewTest` gains `PaletteRowView should tap the slot under the press when it
  moves 10 px or less`, and `WorldBuilderHudViewTest` gains a test of the brush and the tab sprites and one of a
  two-finger tap keeping the card open.
- **The page objects scroll.** `clickPaletteTerrain`, `clickPaletteUnit` and `clickPaletteNpc` open the card, then drag
  the slot's row until the slot is in view, so `WorldBuilderEndToEndTest` places the CPU's rat again, and the new test
  drags the CPU's row to its last unit, the viking warrior.
- **The slots drawn as bitmaps.** KorGE draws a `RoundRect` on the GPU by default, which ignores a clip, so a scrolled
  row drew its slots over the card and the world. The slots' faces and the selection frame use
  `GraphicsRenderer.SYSTEM`, drawn as bitmaps the row clips.
