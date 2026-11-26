# Plan: Tell the primary button from the destructive button

> **Status:** to do.
>
> **Summary:** Since the [parchment HUD](parchment-hud.md), the primary button is the deep red `HudStyle.ACCENT`,
> `#A8322D`, while the destructive button kept the old red `HudStyle.DANGER`, `#D32F2F`: two reds a little apart in
> lightness and saturation. E.g.: on the pause screen, Resume and the Quit of its confirmation are both red, and players
> can't tell the button that resumes from the one that loses what was played since the last save. Instead, the primary
> button keeps its deep red, and the destructive button is a filled light orange, `#F7A440`, with a dark ink label,
> picked from a Storybook prototype. Only the button that loses something is destructive, so Abandon on the quest
> detail, which only asks, becomes an ordinary brown button. Then the code calls them primary and destructive, as the
> [glossary](../../GLOSSARY.md) does. It builds on [ADR 0024](../adr/0024-parchment-hud-with-alegreya.md) and
> [ADR 0008](../adr/0008-in-game-storybook.md).

## How it looks today

| Button      | Colour             | Where                                                                                                            |
|-------------|--------------------|------------------------------------------------------------------------------------------------------------------|
| Primary     | `ACCENT` `#A8322D` | Confirm of a cast, Finish turn, Resume, Play new game, Buy, Give gold, the primary item buttons                  |
| Destructive | `DANGER` `#D32F2F` | The confirming Quit, Abandon, Delete and Overwrite, Discard of the discard question, Abandon of the quest detail |
| Ordinary    | `BUTTON` `#8B5E34` | Cancel, Back, Quit of the pause screen, Discard of the item buttons                                              |

## Decisions

| Topic             | Decision                                                                                                                                                                                                                                                                                                                                  |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope             | Every destructive button. The primary button, and the other uses of `ACCENT` (the virtual pad, the banner and tint of a player turn, the world builder's selected tool, the loss numbers), are not changed.                                                                                                                              |
| Primary colour    | Stays the deep red `#A8322D` of ADR 0024. Chosen in M1 over deep blue `#1F4E8C`, deep teal `#1E6B6B`, forest green `#2E6B3A` and dark ink `#2E2216`, which did not fit the game's palette.                                                                                                                                                |
| Destructive colour | A filled light orange, `#F7A440`, lighter when pressed, `#F8B666`, picked in M1. Chosen over gold, which sits near the gold coins and the gold Wishlist button, over purple, over darker oranges, close in hue to the deep red, and over an outlined button or a warning sign beside the label, as a filled button reads as a button like the others. |
| Label             | The destructive button's label is the dark ink of the HUD's text, `HudStyle.TEXT`, `#2E2216`, at 7.6:1 on it and 8.8:1 pressed, as the cream `BUTTON_LABEL` reaches only 2.3:1 on the light orange.                                                                                                                                      |
| Only the doing    | The destructive look is only for the button that loses something: the confirming Quit, Abandon, Delete and Overwrite, and Discard of the discard question. Abandon of the quest detail, which only asks, becomes brown, like Quit of the pause screen and Discard of the item buttons. Chosen over a destructive look on both steps, so it marks the point of no return. |
| Naming            | `ConfirmButton` becomes `PrimaryButton`, and `HudStyle.DANGER` becomes `HudStyle.DESTRUCTIVE`, as the glossary lists their words under _Avoid_. `HudStyle.ACCENT` keeps its name: it is a colour of the chrome, not a button.                                                                                                             |
| Architecture      | `shared.adapters.presentation` (`HudStyle`, `GameButton`), `screen.adapters.presentation` (`ButtonRowView`, `CallToActionView`, `DiscardQuestionView`, `QuestPresenter`, `ConfirmButton`) and `storybook.adapters.presentation` (the stories). No use case or domain changes.                                                             |
| Storybook         | No story is added. Before and after screenshots of **Screen / Call to action › Confirmation**, **Screen / Button row › Danger**, **Screen / Discard question › Question** and **Screen / Cancel and confirm › Default**, on a private Artifact page.                                                                                        |
| Tests             | `HudStyleTest` checks the destructive label at 4.5:1 through `HudStyle.BUTTON_LABELS`. `ButtonRowViewTest` checks an action's label colour. `QuestPresenterTest` checks Abandon of the quest detail is brown. The renames update the tests that name the old words.                                                                       |
| ADR               | None: the primary keeps the deep red accent of ADR 0024, and the destructive colour is cheap to change.                                                                                                                                                                                                                                  |
| Out of scope      | The primary button and every other use of `ACCENT`. The ordinary brown button, and the gold Wishlist button of the main menu. A setting to choose the colours. An icon on the destructive button.                                                                                                                                       |
| Delivery          | 3 milestones, each green on CI but M1. See [Milestones](#milestones).                                                                                                                                                                                                                                                                     |

E.g.: the buttons once built, each label on its button:

| Button      | Colour    | Pressed   | Label                  | Label contrast |
|-------------|-----------|-----------|------------------------|----------------|
| Primary     | `#A8322D` | `#8E2924` | `BUTTON_LABEL` `#FFF7E3` | 6.2:1        |
| Destructive | `#F7A440` | `#F8B666` | `TEXT` `#2E2216`         | 7.6:1        |
| Ordinary    | `#8B5E34` | `#A27040` | `BUTTON_LABEL` `#FFF7E3` | 5.2:1        |

## Milestones

Each milestone is a few commits straight on `main`, green on CI, but M1, which stays on its local branch.

| #  | Milestone               | Done when                                                                                                                                         | Status | Done in |
|----|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|--------|---------|
| M1 | Prototype               | The looks are Storybook globals on `prototype/button-colours`, their screenshots are on an Artifact page, and the user has picked the destructive button's look. | ⬜      |         |
| M2 | Light orange            | The game draws every destructive button light orange with a dark ink label, Abandon of the quest detail is brown, and the after screenshots are approved. | ⬜      |         |
| M3 | Renames                 | No name in the code calls a primary button a confirm button, or a destructive button a danger.                                                    | ⬜      |         |

### M1 commits

1. On `prototype/button-colours`, `ButtonLookPrototype`, in `shared.adapters.presentation`, holds the `primary` and the
   `destructive` look, and `HudStyle.ACCENT`, `ACCENT_PRESSED`, `DANGER` and `DANGER_PRESSED` read their colour from
   it.
2. `StorybookApi.init` adds the `primary` and `destructive` pick controls to its `globalControls`, and
   `StorybookPresenter.displayStory` sets `ButtonLookPrototype` from them before it renders the story.
3. `gameButton` draws the outlined and warning sign looks for a button whose colour is `HudStyle.DANGER`.
4. A second round replaces the destructive styles with gold, purple and orange, three filled shades each.
5. The screenshots of each round go on a private Artifact page.

### M2 commits

1. `HudStyle.DANGER` and `DANGER_PRESSED` become the light orange, `HudStyle.DANGER_LABEL` the dark ink,
   `ButtonRowView.Action` and `gameButton` take a label colour, and the destructive callers pass `DANGER_LABEL`, with
   `HudStyleTest` and `ButtonRowViewTest`.
2. `QuestPresenter.detailActions` gives Abandon the ordinary colours, and its test.
3. Take the before and after screenshots, and mark M2 as built, with its screenshots still to be approved.

### M3 commits

1. Rename `ConfirmButton` to `PrimaryButton`, `HudStyle.DANGER` to `HudStyle.DESTRUCTIVE`, and the danger story and
   control of the button row, and their tests.

## Reference

### HudStyle (M2)

- `DANGER` becomes `RGBA(0xF7, 0xA4, 0x40)` and `DANGER_PRESSED` `RGBA(0xF8, 0xB6, 0x66)`, lighter like
  `BUTTON_PRESSED`, as a darker orange would bring the dark label under 4.5:1.
- `DANGER_LABEL` is `TEXT`.
- `BUTTON_LABELS` pairs `DANGER` and `DANGER_PRESSED` with `DANGER_LABEL` instead of `DANGER` with `BUTTON_LABEL`, so
  `HudStyleTest` checks the label on both.

### Buttons (M2)

- `GameButton` and `gameButton` take a `labelColor`, `GameButton.LABEL` by default, which `GameButton` draws its label
  in while it is enabled; a disabled button keeps `DISABLED_LABEL`.
- `ButtonRowView.Action` gains `labelColor`, `GameButton.LABEL` by default, passed to `gameButton`.
- `CallToActionView.displayConfirmation` and `DiscardQuestionView` give their destructive action `HudStyle.DANGER_LABEL`.
  The confirming Abandon of `QuestPresenter.abandonSelected`, and the confirming Quit, Delete and Overwrite, go through
  `displayConfirmation`.
- `QuestPresenter.detailActions` builds Abandon as `ButtonRowView.Action(messages[UiMessage.QUEST_ABANDON])`, brown like
  Back.
- `ButtonRowViewStories` gives the danger story's last button `DANGER_LABEL`.

### Renames (M3)

| Today                                                                           | Once built                                                                      |
|---------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| `ConfirmButton`, `ConfirmButton.COLOR`, `PRESSED_COLOR`                         | `PrimaryButton`, `PrimaryButton.COLOR`, `PRESSED_COLOR`                         |
| `ConfirmButtonStories`, `screen-confirm-button--*`, **Screen / Confirm button** | `PrimaryButtonStories`, `screen-primary-button--*`, **Screen / Primary button** |
| `ConfirmButtonTest`, `ConfirmButtonExtension`                                   | `PrimaryButtonTest`, `PrimaryButtonExtension`                                   |
| `HudStyle.DANGER`, `DANGER_PRESSED`, `DANGER_LABEL`                             | `HudStyle.DESTRUCTIVE`, `DESTRUCTIVE_PRESSED`, `DESTRUCTIVE_LABEL`              |
| `screen-button-row--danger`, **Screen / Button row › Danger**                   | `screen-button-row--destructive`, **Screen / Button row › Destructive**         |
| The `lastIsDanger` control of `ButtonRowViewStories`                            | `lastIsDestructive`                                                             |

The story's description becomes "An action that loses something for good, e.g.: Abandon once asked."

### Tests

- `HudStyleTest`
  - `HudStyle should draw every button label at a contrast of at least four and a half to one when it is on its button`
    covers the light orange and its pressed shade with the dark ink.
- `ButtonRowViewTest`
  - `ButtonRowView should draw an action's label in its label colour when it is given one`
- `QuestPresenterTest`
  - `questJournalSelected should display Back and Abandon as ordinary buttons when it is an active side quest` (replaces
    `questJournalSelected should display Back and Abandon when it is an active side quest`)
- `CallToActionViewTest`, `QuestPresenterTest`, `ConfirmButtonTest`, `ShopPresenterTest`, `InventoryPresenterTest`,
  `InventoryPresenterGiveTest`, `ExplorationPresenterTest` and `StorybookEndToEndTest` follow the renames of M3.

## Known limitations

- The light orange is checked on screenshots and at 4.5:1, not with colour-blind players.

## Changes while building

- **M1 changed the plan.** The first round compared 5 primary colours with outlined and warning sign destructive
  buttons, on https://claude.ai/artifact/W6wN3Hv6jzMGs7U4XNFFJz. The user kept the primary deep red and asked for a
  second round of filled destructive colours without a sign: gold, purple and orange, three shades each, on
  https://claude.ai/artifact/95ZfEm1H2CmbN8a7eWkaHr, from which the light orange was picked. So the plan no longer
  changes the accent, the loss numbers or the world builder's error text, nor writes an ADR, and the destructive button
  takes a dark label, which `ButtonRowView.Action` and `gameButton` now carry.
- **Fewer screenshots.** The destructive stories show no primary button, so the first round's page shows their 3 styles
  once, not once per primary colour: 29 of the 65 screenshots taken.
