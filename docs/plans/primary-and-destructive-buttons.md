# Plan: Tell the primary button from the destructive button

> **Status:** to do.
>
> **Summary:** Since the [parchment HUD](parchment-hud.md), the primary button is the deep red `HudStyle.ACCENT`,
> `#A8322D`, while the destructive button kept the old red `HudStyle.DANGER`, `#D32F2F`: two reds a little apart in
> lightness and saturation. E.g.: on the pause screen, Resume and the Quit of its confirmation are both red, and players
> can't tell the button that resumes from the one that loses what was played since the last save. Instead, they look
> plainly different, in a look picked from a Storybook prototype of 5 primary colours and 3 destructive styles. Only
> the button that loses something is destructive, so Abandon on the quest detail, which only asks, becomes an ordinary
> brown button. Then the code calls them primary and destructive, as the [glossary](../../GLOSSARY.md) does. It builds
> on [ADR 0024](../adr/0024-parchment-hud-with-alegreya.md) and [ADR 0008](../adr/0008-in-game-storybook.md).

## How it looks today

| Button             | Colour            | Where                                                                                                   |
|--------------------|-------------------|---------------------------------------------------------------------------------------------------------|
| Primary            | `ACCENT` `#A8322D` | Confirm of a cast, Finish turn, Resume, Play new game, Buy, Give gold, the primary item buttons         |
| Destructive        | `DANGER` `#D32F2F` | The confirming Quit, Abandon, Delete and Overwrite, Discard of the discard question, Abandon of the quest detail |
| Ordinary           | `BUTTON` `#8B5E34` | Cancel, Back, Quit of the pause screen, Discard of the item buttons                                     |

`ACCENT` is also the virtual pad, the banner and the battlefield's tint on a player turn (`BannerView.HUMAN_TURN_COLOR`),
the world builder's selected tool, and, through `HudStyle.LOSS`, the loss numbers of the cast preview.
`WorldBuilderInfoView.ERROR_COLOR` is `ACCENT_PRESSED`, `#8E2924`.

## Decisions

| Topic             | Decision                                                                                                                                                                                                                                                                       |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope             | Every primary and destructive button, and the other uses of `ACCENT`: the virtual pad, the banner and tint of a player turn, and the world builder's selected tool.                                                                                                             |
| Primary colours   | The prototype compares today's deep red with deep blue `#1F4E8C`, deep teal `#1E6B6B`, forest green `#2E6B3A` and dark ink `#2E2216`. Green is tried though it looks like red to red–green colour-blind players, to see it beside blue and teal.                               |
| Destructive style | The prototype compares today's filled red with an outlined button, parchment with a `#C62828` border and label, and the filled red with a vector warning sign left of its label. `#C62828` is the closest to `#D32F2F` with a label at 4.5:1 on the panel, where `#D32F2F` reaches 4.05:1. |
| Combinations      | Two Storybook globals, `primary` and `destructive`, so any of the 15 pairs is one link away. Chosen over one list of 7 named looks, which can't mix e.g. a blue primary with an outlined destructive button.                                                                 |
| Globals in the URL | The prototype's globals are set only in the URL, e.g. `&globals=primary:blue;destructive:outlined`. Chosen over rows of buttons in the toolbar, as the screenshots page links every combination.                                                                             |
| Accent            | The virtual pad, the banner and tint of a player turn and the world builder's selected tool follow the primary colour. Chosen over keeping them deep red, so the chrome has one accent.                                                                                       |
| Red               | The loss numbers and the world builder's error text stay red, `#A8322D` and `#8E2924`, whatever the primary colour: red means loss, destruction and error.                                                                                                                     |
| Only the doing    | The destructive look is only for the button that loses something: the confirming Quit, Abandon, Delete and Overwrite, and Discard of the discard question. Abandon of the quest detail, which only asks, becomes brown, like Quit of the pause screen and Discard of the item buttons. Chosen over red on both steps, so red marks the point of no return. |
| Naming            | `ConfirmButton` becomes `PrimaryButton`, and `HudStyle.DANGER` becomes `HudStyle.DESTRUCTIVE`, as the glossary lists their words under _Avoid_. `HudStyle.ACCENT` keeps its name: it is a colour of the chrome, not a button.                                                  |
| Architecture      | `shared.adapters.presentation` (`HudStyle`, `gameButton`), `screen.adapters.presentation` (the buttons, views and `QuestPresenter`) and `storybook.adapters.presentation` (the stories, and the prototype's globals). No use case or domain changes.                            |
| Storybook         | No story is added. The prototype's screenshots, and the before and after screenshots of M2, are of **Screen / Call to action › Confirmation**, **Screen / Button row › Danger**, **Screen / Discard question › Question**, **Screen / Cancel and confirm › Default**, **Screen / Pause › Options**, **Screen / Shop › Buy, item selected** and **Screen / Menu › Options**, on a private Artifact page. |
| Tests             | `HudStyleTest` checks the new colours at 4.5:1 through `HudStyle.BUTTON_LABELS` and `TEXT_COLORS`. `QuestPresenterTest` checks Abandon of the quest detail is brown. The renames update the tests that name the old words.                                                     |
| ADR               | When the picked primary colour is not deep red, a new ADR records it, as it replaces the deep red accent of ADR 0024. None when deep red stays.                                                                                                                                |
| Out of scope      | The ordinary brown button, and the gold Wishlist button of the main menu. A setting to choose the colours. An icon per destructive action (a bin for Discard, a door for Quit). The health and mana bars, and the ally and enemy colours.                                    |
| Delivery          | 3 milestones, each green on CI but M1. See [Milestones](#milestones).                                                                                                                                                                                                         |

E.g.: the colours the prototype compares, each label `HudStyle.BUTTON_LABEL`, `#FFF7E3`, on its button:

| `primary`    | Colour    | Pressed   | Label contrast |
|--------------|-----------|-----------|----------------|
| `red`        | `#A8322D` | `#8E2924` | 6.2:1          |
| `blue`       | `#1F4E8C` | `#183F72` | 7.8:1          |
| `teal`       | `#1E6B6B` | `#175656` | 5.8:1          |
| `green`      | `#2E6B3A` | `#24552E` | 6.0:1          |
| `ink`        | `#2E2216` | `#4A3826` | 14.5:1         |

| `destructive` | Look                                                                                                    |
|---------------|---------------------------------------------------------------------------------------------------------|
| `filled`      | Today's: `#D32F2F`, pressed `#B72323`, label 4.7:1.                                                      |
| `outlined`    | Filled with `PANEL_RAISED`, `#FFF7E3`, a `#C62828` border and label at 5.3:1; pressed, filled `#C62828` with a `#FFF7E3` label. |
| `icon`        | Today's filled red, with a 14 px warning sign, a triangle holding "!", in the label's colour, left of the label. |

The pressed dark ink is lighter, not darker, as there is little darker than the ink.

## Milestones

Each milestone is a few commits straight on `main`, green on CI, but M1, which stays on its local branch.

| #  | Milestone              | Done when                                                                                                                                                       | Status | Done in |
|----|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|---------|
| M1 | Prototype              | The 15 looks are Storybook globals on `prototype/button-colours`, their screenshots are on an Artifact page, and the user has picked a primary colour and a destructive style. | ⬜      |         |
| M2 | Primary and destructive | The game draws the picked look, Abandon of the quest detail is brown, and the after screenshots are approved.                                                   | ⬜      |         |
| M3 | Renames                | No name in the code calls a primary button a confirm button, or a destructive button a danger.                                                                  | ⬜      |         |

### M1 commits

1. On `prototype/button-colours`, `ButtonLookPrototype`, in `shared.adapters.presentation`, holds the `primary` and the
   `destructive` look, and `HudStyle.ACCENT`, `ACCENT_PRESSED`, `DANGER` and `DANGER_PRESSED` read their colour from
   it. `HudStyle.LOSS` and `WorldBuilderInfoView.ERROR_COLOR` keep `#A8322D` and `#8E2924`. The colours kept once in a
   companion, `ConfirmButton.COLOR` and `PRESSED_COLOR`, `VirtualPadView.BUTTON_COLOR`, `BannerView.HUMAN_TURN_COLOR`
   and `WorldBuilderHudView.SELECTED_TOOL_COLOR`, read `HudStyle` each time.
2. `StorybookApi.init` adds the `primary` (`red`, `blue`, `teal`, `green`, `ink`) and `destructive` (`filled`,
   `outlined`, `icon`) pick controls to its `globalControls`, `red` and `filled` by default, and
   `StorybookPresenter.displayStory` sets `ButtonLookPrototype` from them before it renders the story.
3. `gameButton` draws the outlined and icon looks for a button whose colour is `HudStyle.DANGER`.
4. The screenshots go on a private Artifact page: each of the 15 pairs on **Call to action › Confirmation**, **Button
   row › Danger** and **Discard question › Question**, and each of the 5 primary colours on **Cancel and confirm ›
   Default**, **Pause › Options**, **Shop › Buy, item selected** and **Menu › Options**: 65 screenshots.

### M2 commits

1. `HudStyle` takes the look picked in M1, with `HudStyle.LOSS` and `WorldBuilderInfoView.ERROR_COLOR` given their own
   red when the primary colour is not deep red, and `HudStyleTest` checks its labels.
2. When the outlined or the icon style is picked, `ButtonRowView.Action` gains `isDestructive`, `gameButton` draws its
   look, and the destructive callers pass it in place of `DANGER` and `DANGER_PRESSED`.
3. `QuestPresenter.detailActions` gives Abandon the ordinary colours, and its test.
4. When the primary colour is not deep red, the new ADR, and ADR 0024 links to it.
5. Take the before and after screenshots, and mark M2 as built, with its screenshots still to be approved.

### M3 commits

1. Rename `ConfirmButton` to `PrimaryButton`, `HudStyle.DANGER` to `HudStyle.DESTRUCTIVE`, and the danger story and
   control of the button row, and their tests.

## Reference

### HudStyle (M2)

- `ACCENT` and `ACCENT_PRESSED` take the picked primary colour, e.g. `#1F4E8C` and `#183F72` for deep blue.
- `LOSS` becomes its own `#A8322D` instead of `ACCENT`, when `ACCENT` is no longer that red.
- `DANGER` and `DANGER_PRESSED` take the picked destructive colours: `#D32F2F` and `#B72323` for filled and icon,
  `#C62828` for outlined.
- `BUTTON_LABELS` gains `PANEL_RAISED to DANGER` when the outlined style is picked, so `HudStyleTest` checks its label
  at 4.5:1 on its parchment fill.

### Views (M2)

- `ConfirmButton`, `FinishPlayerTurnView`, `VirtualPadView`, `BannerView` and `WorldBuilderHudView` keep reading
  `ACCENT`, so they follow the picked colour with no change of their own.
- `WorldBuilderInfoView.ERROR_COLOR` becomes `#8E2924` instead of `ACCENT_PRESSED`, when the primary colour is not deep
  red.
- `CallToActionView.displayConfirmation` and `DiscardQuestionView` keep a destructive confirming button. The confirming
  Abandon of `QuestPresenter.abandonSelected` stays destructive, through `displayConfirmation`.
- `QuestPresenter.detailActions` builds Abandon as `ButtonRowView.Action(messages[UiMessage.QUEST_ABANDON])`, brown like
  Back.

### Renames (M3)

| Today                                                     | Once built                                                       |
|-----------------------------------------------------------|------------------------------------------------------------------|
| `ConfirmButton`, `ConfirmButton.COLOR`, `PRESSED_COLOR`   | `PrimaryButton`, `PrimaryButton.COLOR`, `PRESSED_COLOR`          |
| `ConfirmButtonStories`, `screen-confirm-button--*`, **Screen / Confirm button** | `PrimaryButtonStories`, `screen-primary-button--*`, **Screen / Primary button** |
| `ConfirmButtonTest`, `ConfirmButtonExtension`             | `PrimaryButtonTest`, `PrimaryButtonExtension`                    |
| `HudStyle.DANGER`, `HudStyle.DANGER_PRESSED`              | `HudStyle.DESTRUCTIVE`, `HudStyle.DESTRUCTIVE_PRESSED`           |
| `screen-button-row--danger`, **Screen / Button row › Danger** | `screen-button-row--destructive`, **Screen / Button row › Destructive** |
| The `lastIsDanger` control of `ButtonRowViewStories`      | `lastIsDestructive`                                              |

The story's description becomes "An action that loses something for good, e.g.: Abandon once asked."

### Tests

- `HudStyleTest`
  - `HudStyle should draw every button label at a contrast of at least four and a half to one when it is on its button`
    covers the picked colours, and the outlined label on its parchment.
- `QuestPresenterTest`
  - `questJournalSelected should display Back and Abandon as ordinary buttons when it is an active side quest` (replaces
    `questJournalSelected should display Back and Abandon when it is an active side quest`)
- `ButtonRowViewTest`
  - `ButtonRowView should draw an action in the destructive look when it is destructive`, when the outlined or the icon
    style is picked.
- `CallToActionViewTest`, `QuestPresenterTest`, `ConfirmButtonTest`, `ShopPresenterTest`, `InventoryPresenterTest`,
  `InventoryPresenterGiveTest`, `ExplorationPresenterTest` and `StorybookEndToEndTest` follow the renames of M3.

## Known limitations

- The colours are checked on screenshots and at 4.5:1, not with colour-blind players: blue next to red is chosen to stay
  apart for red–green colour blindness, from how it is known to look, not from a test.
- The banner and battlefield tint of a player turn follow the primary colour, and are seen only in the real game, not
  in the prototype's screenshots.
