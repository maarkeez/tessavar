# Plan: undo and redo in the world builder with a two- and a three-finger tap

> **Status:** done, built on `main` in 330ebfaa and approved on screenshots.
>
> **Summary:** In the world builder, Undo and Redo are two icon buttons in the HUD, after Paint, Fill and Erase. On a
> phone, undoing the lava tile just painted means reaching down to a small button under the world, and the two buttons
> crowd the row of tools. Instead, a two-finger tap on the world undoes the last edit and a three-finger tap redoes it,
> as in drawing apps on a tablet, and the info line pops `Undo` or `Redo`. The two buttons leave the HUD for the first
> row of the hamburger menu, side by side as icons, for desktop, where there is no multi-touch. It builds on the undo of
> the [community worlds plan](community-worlds.md).

## How a tap with several fingers reaches the game today

KorGE 6 passes every finger of the browser's touch events to the game as a `TouchEvent`, and `View.touch { }` lists the
fingers down at once, as the Storybook's `ZoomView` does for its pinch. KorGE also turns the touch into mouse events
whenever exactly one finger is listed, so a two-finger tap also sends, e.g.:

| Finger    | Fingers listed | Mouse event sent                                         |
|-----------|----------------|----------------------------------------------------------|
| A touches | 1              | Down where A touched                                     |
| B touches | 2              | None                                                     |
| B lifts   | 2              | None                                                     |
| A lifts   | 1              | Up where A lifted: a click, under 20 px and 400 ms after |

That click taps the tile under A, painting it, and the down and up drag the world by however far A moved. With the
menu open, it lands on the menu's outside tap catcher and closes the menu.

## Decisions

| Topic        | Decision                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope        | A two-finger tap on the world undoes the last edit and a three-finger tap redoes the last edit undone. Undo and Redo move from the HUD to the first row of the hamburger menu, as two icon buttons.                                                                                                                                                                                                                                                                                                                                                                                                                         |
| Where        | Only a tap whose fingers all touch the world counts, never one on the HUD, a panel or the menu, so a finger on a button only presses that button.                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| Finger tap   | Every finger lifts within 300 ms of the first one touching and none moves more than 10 px. The fingers counted are the most down at once: 2 undoes, 3 redoes, 1 is a tile tap, 4 or more does nothing.                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Stray taps   | A tap with 2 or more fingers never taps a tile, never drags the world and never closes the menu, whatever mouse events KorGE sends with it.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Any state    | The finger tap also works with the menu or a panel open: the menu stays open, and an open form is displayed again, as an undo from the menu does today.                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Feedback     | The info line pops `Undo` or `Redo` after any undo or redo, from the menu or a finger tap, as the edit undone can be off screen. A finger tap with nothing to undo or redo pops `Nothing to undo` or `Nothing to redo`. Chosen over naming the edit undone (e.g. `Undo: paint`), as every edit would need a label and its translations.                                                                                                                                                                                                                                                                                     |
| Menu         | The first row of the menu holds Undo on the left and Redo on the right, each half the row (75x32) with its 16x16 icon drawn at 2x, without text. A disabled one is dimmed and inert, rather than hidden, so the menu doesn't shift. Pressing either keeps the menu open, so undoing several edits is one click each; every other option still closes it.                                                                                                                                                                                                                                                                    |
| Keyboard     | No Ctrl+Z or Cmd+Z: on desktop, undo and redo are in the menu only.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| Teaching     | Nothing in the game teaches the finger taps. `docs/contributing/share-a-world.md` mentions them in one line.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Architecture | All in `screen.adapters.presentation`. `BattlefieldView` recognises the finger tap, as it already owns the world's viewport, its tile taps and its drag, the only view that can hold them back. Its `Delegate` gains `fingersTapped(fingers)`, which `BattlefieldPresenter` ignores, so a two-finger tap in a battle no longer selects a tile either. Chosen over a flag turning it on in the world builder only, and over a separate view in front of the world, which would need a way to cancel `BattlefieldView`'s pending tap and drag. Nothing changes in `worldBuilder`: `UndoEdit` and `RedoEdit` stay as they are. |
| Storybook    | **Screen / World builder menu › Open** shows the history row; a new **Screen / World builder menu › Nothing to undo** story. The HUD's **Nothing to undo** story goes.                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Tests        | View tests fake the touches with a `TouchEvent`, like `ZoomViewExtension.pinch`. Presenter tests call `fingersTapped`. An end-to-end test undoes with two fingers.                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| Out of scope | Keyboard shortcuts, a hint in the game, naming the edit undone, and any other gesture (pinch to zoom on the world).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |

## FingerTap

A new class in `screen.adapters.presentation`, used by `BattlefieldView` and `WorldBuilderMenuView`.

- `FingerTap(view, area, onTapped)` follows `view.touch { }`, counting a tap only when its first finger touches inside
  `area()`, a global rectangle (`BattlefieldView`'s viewport). On the last finger lifting, it calls `onTapped(fingers)`
  when the tap lasted at most `FINGER_TAP_DURATION` (300 ms) and no finger moved more than `FINGER_TAP_DISTANCE`
  (10 px).
- `isHoldingBack` is true from the moment a second finger touches until the frame after the last finger lifts, so the
  mouse up KorGE sends with that last finger is still held back. E.g.: A touches the lava tile at row 2, column 2,
  B touches, both lift 120 ms later: `onTapped(2)`, and the click on the tile is ignored.
- A touch that KorGE ends without lifting a finger (it ignores `touchcancel`) is dropped at the next touch, so a
  stuck finger never blocks the next tap.

## BattlefieldView

- `Delegate.fingersTapped(fingers: Int)` is new, with an empty default, so `BattlefieldPresenter` and
  `BattlefieldViewStories` don't change.
- `BattlefieldView` keeps a `FingerTap` over its viewport and calls `delegate?.fingersTapped(fingers)` from it.
- `tileButton.onClick` doesn't call `tileSelected` while `FingerTap.isHoldingBack`.
- `viewport.onMouseDrag` stops while `FingerTap.isHoldingBack` and puts the world back at `dragStartX`, `dragStartY`,
  so the world doesn't move.

## WorldBuilderMenuView

- `Delegate { undoSelected(); redoSelected() }` and `setDelegate(delegate)` are new, moved from
  `WorldBuilderHudView.Delegate`.
- `open()` lays out the history row above the options: `undoButton` and `redoButton`, `GameButton`s of
  `OPTION_WIDTH / 2` by `OPTION_HEIGHT`. Clicking an enabled one calls the delegate and leaves the menu open.
- `displayHistory(canUndo, canRedo)` enables and dims them (`DISABLED_ICON_ALPHA`, 0.35), at once when the menu is
  open, else when it next opens. Both are disabled until it is called.
- `displayHistoryIcons(icons)` takes the bitmaps of `ui/tool-undo.png` and `ui/tool-redo.png`, loaded by
  `WorldBuilderMenuView.historyIcons(resourcesLoader)`, and `WorldBuilderScene.sceneInit` calls it.
- The outside tap catcher closes the menu only on a one-finger tap: its own `FingerTap` over the whole screen holds the
  click back after a tap with more fingers.

## WorldBuilderHudView

- `undoButton`, `redoButton`, `HISTORY_GAP`, `displayHistory`, `Delegate.undoSelected` and `Delegate.redoSelected` go.
  The row of tools ends with Erase.
- `ToolIcon` loses `UNDO` and `REDO`, and `toolIcons(resourcesLoader)` no longer loads their icons.

## WorldBuilderPresenter

- It sets itself as `menuView`'s delegate. `undoSelected()` and `redoSelected()` call `worldBuilderApi.undoEdit()` and
  `worldBuilderApi.redoEdit()`, as they do today from the HUD.
- `fingersTapped(2)` undoes when `searchWorld().canUndo`, else pops `Nothing to undo` with `infoView.displayInfo`.
  `fingersTapped(3)` does the same for redo. Other counts do nothing.
- `EditUndone` and `EditRedone` also pop `Undo` and `Redo`, after displaying the world, so the size shows again once
  the message fades.
- `displayWorld()` and `displayPaintedTile()` call `menuView.displayHistory(canUndo, canRedo)` instead of the HUD's.

## Localization

- `UiMessage` gains `WORLD_BUILDER_UNDONE` (`ui.worldBuilder.undone`, `Undo`), `WORLD_BUILDER_REDONE`
  (`ui.worldBuilder.redone`, `Redo`), `WORLD_BUILDER_NOTHING_TO_UNDO` (`ui.worldBuilder.nothingToUndo`,
  `Nothing to undo`) and `WORLD_BUILDER_NOTHING_TO_REDO` (`ui.worldBuilder.nothingToRedo`, `Nothing to redo`), in
  `messages.pot` and in `es.po` as `Deshacer`, `Rehacer`, `Nada que deshacer` and `Nada que rehacer`.

## Storybook

- `WorldBuilderMenuViewStories`: the `canUndo` and `canRedo` controls (`boolean`), and the menu gets its icons. The
  **Open** story (`screen-world-builder-menu--open`) has both enabled and its description names the history row first;
  the new **Nothing to undo** story (`screen-world-builder-menu--nothing-to-undo`) is open with both disabled.
- `WorldBuilderHudViewStories`: the `canUndo` and `canRedo` controls and the **Nothing to undo** story
  (`screen-world-builder-hud--nothing-to-undo`) go. **Default** no longer mentions undo.
- `StorySmokeTest` lists the new menu story and drops the HUD's.

## Docs

- [How to share a world](../contributing/share-a-world.md): under *2. Make the world playable*, one line: tap
  the world with two fingers to undo and with three to redo, or use the first row of the menu.

## Tests

- `FingerTapTest`
  - `FingerTap should tell two fingers when two fingers tap inside the area`
  - `FingerTap should tell three fingers when a third finger touches before the first two lift`
  - `FingerTap should tell nothing when the fingers lift after 300 ms`
  - `FingerTap should tell nothing when a finger moves more than 10 px`
  - `FingerTap should tell nothing when the first finger touches outside the area`
  - `FingerTap should hold back until the frame after the last finger lifts when a second finger touches`
- `BattlefieldViewTest`
  - `FingerTap should ask the delegate for two fingers when two fingers tap the world`
  - `FingerTap should not select the tile under the first finger when two fingers tap the world`
  - `FingerTap should not move the world when two fingers tap the world`
  - `Tile should still be selected when one finger taps it`
- `WorldBuilderMenuViewTest`
  - `MenuButton should open the menu with the history row above its options when it is clicked`
  - `Undo and Redo should ask to undo and redo and keep the menu open when they are clicked`
  - `Undo should ask nothing when there is nothing to undo`
  - `DisplayHistory should dim redo when there is only an edit to undo`
  - `WorldBuilderMenuView should stay open when the screen is tapped out of it with two fingers`
- `WorldBuilderHudViewTest`
  - `WorldBuilderHudView should display the paint, fill and erase tools as icons without a label when it is created`
    (replaces `WorldBuilderHudView should display the paint, fill and erase tools, then undo and redo, as icons without a label when it is created`)
  - `WorldBuilderHudView should disable undo and redo when it is created`, `DisplayHistory should enable undo and
    disable redo when there is only an edit to undo`, `Undo and Redo should ask to undo and redo when they are clicked`
    and `Undo should ask nothing when there is nothing to undo` move to `WorldBuilderMenuViewTest`.
- `WorldBuilderPresenterTest`
  - `FingersTapped should undo the last edit and display Undo when two fingers tap the world`
  - `FingersTapped should redo the edit undone and display Redo when three fingers tap the world`
  - `FingersTapped should display Nothing to undo when two fingers tap a world without edits`
  - `FingersTapped should display Nothing to redo when three fingers tap with nothing undone`
  - `EditUndone should display Undo when undo is selected in the menu`
  - The existing `EditUndone` and `EditRedone` tests call `menuView.delegate` instead of `hudView.delegate`.
- `WorldBuilderEndToEndTest`
  - `should bring the tile back when two fingers tap the world after a paint`
  - `should bring the tile back when the paint is undone` clicks Undo in the menu.

## Known limitations

- Not played on a real phone. Safari on an iPhone has its own three-finger gestures and KorGE ignores `touchcancel`:
  once on main, a three-finger tap is tried there. If Safari swallows it or shows its own undo bar, another redo gesture
  is picked in a new plan.
- On desktop, undoing several edits takes opening the menu, then one click each; there is no keyboard shortcut.

## Changes while building

- `FingerTap(view, onTapped)` reads KorGE's `TouchEvent`s itself instead of `view.touch { }`. When the browser cancels
  a touch, KorGE reports it as removed in the next `START`, and `TouchEvents` starts a finger for it that never lifts.
  Reading the fingers still down in each event drops a stuck finger at the next touch.
- A finger counts when the view under it, by hit test, is inside `view` or a view named `FingerTap.PASS_THROUGH`, instead
  of when it falls inside an `area()` rectangle. So a finger on the open menu, which hangs over the world, never counts,
  while the menu's outside tap catcher, named `PASS_THROUGH`, lets a finger tap on the world through. Every finger is
  checked as it touches, not only the first.
- `isHoldingBack` lasts until 100 ms (`HOLD_BACK_AFTER_LIFT`) after the last finger lifts, instead of until the next
  frame. KorGE's drag reads the emulated mouse up in the frame updates after it, in an order the view doesn't control.
- The menu has its own `WorldBuilderMenuView.HistoryIcon` (`UNDO`, `REDO`), loaded from the same `ui/tool-undo.png` and
  `ui/tool-redo.png`, rather than sharing the HUD's `ToolIcon`.
- The tests differ slightly from the list: `FingerTapTest` also checks one finger, a finger touching out of the view
  and the pass-through, and its hold back test is `FingerTap should hold back the mouse from the second finger touching
  until 100 ms after the last one lifts`. `BattlefieldViewTest` checks the stray tile tap with the two-finger tap, and
  checks that a two-finger tap whose finger slides doesn't move the world. `WorldBuilderMenuViewTest` also checks that a
  one-finger tap out of the menu still closes it. `WorldBuilderPresenterTest` also checks that four fingers do nothing.
- The tip in `share-a-world.md` calls Undo and Redo the arrows at the top of the menu, as the row shows icons only.
