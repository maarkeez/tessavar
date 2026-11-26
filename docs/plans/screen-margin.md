# Plan: Keep the shadows of the inventory, the shop and the call to action off the screen edges

> **Status:** done.
>
> **Summary:** The inventory, the shop and every view of the call to action are as wide as the game window, 390 px, and
> start at its first column. So the window cuts off the shadow `UIButton` draws around each of their boxes and buttons
> on the outer side: the first and last of the six boxes of each row of the inventory and the shop, Close, `<` and `>`,
> Finish turn, Cancel and Confirm, and the item buttons. It was always so, but the light parchment of the
> [parchment HUD](parchment-hud.md) makes it plain to see. Instead, they keep their shadows and lay their content out
> 12 px in from each side of the screen, `HudStyle.SCREEN_MARGIN`, with the boxes 56 px instead of 60. Then the boxes,
> which the [glossary](../../GLOSSARY.md) calls storage spaces, are named so in the code.

## Cause

Six boxes of 60 px with five gaps of 6 px make 390 px, so `InventoryView` and `ShopView` put the first box at x 0 and
the last one's right side at x 390. `InventorySlotView` is a `UIButton` that keeps KorGE's default shadow, about 8 px
around it, so the window draws only the inner half of the shadow of the outer boxes. The views of the call to action
are 390 px wide too (`ButtonRowView.WIDTH`, `ItemButtonsView.WIDTH`, the 390 px button of `FinishPlayerTurnView`,
`CancelButton` and `ConfirmButton` at 190 px with 10 px between them), and `GameScene` places `callToActionView` at x 0.
A pixel check of the first story of every Storybook component finds no other view flush with the screen edges, but
the world builder's details, resize and stock.

## Decisions

| Topic        | Decision                                                                                                                                                                                                                                                                                                                |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope        | The inventory, the shop, and every view the call to action shows: Finish turn, Cancel and confirm, the button row, the item buttons, the discard question, give item, the give gold actions, the give gold refusal and inventory full.                                                                                    |
| Look         | The boxes and the buttons keep their shadow. Chosen over flat boxes with a 1 px outline, like the inventory button and the ability buttons, compared on a prototype: the outline is faint on the parchment, and the boxes read as less tappable.                                                                          |
| Margin       | `HudStyle.SCREEN_MARGIN`, 12 px, from each side of the screen, and `HudStyle.CONTENT_WIDTH`, 366 px, between them. Chosen over 9 px with 57 px boxes, which leaves the shadow less room, and over 10 px, which makes the boxes 56.7 px.                                                                                   |
| Boxes        | 56 px instead of 60, with the 6 px gap kept: 6 × 56 + 5 × 6 = 366. The item art, 16 px drawn 3×, is 48 px, and keeps 4 px around it.                                                                                                                                                                                      |
| One margin   | The call to action uses the same 12 px as the inventory, so the grid and the item buttons right under it line up. The gap between buttons stays 10 px.                                                                                                                                                                    |
| Where        | Inside each view: each keeps the screen's 390 px and lays its content out from `SCREEN_MARGIN` to 390 − `SCREEN_MARGIN`, so its own story shows what the game shows. Chosen over `CallToActionView` and the HUD placing 366 px views at x 12, which leaves the views' own stories flush left.                            |
| Not changed  | `GiveGoldView` and `TalkView` keep their own 12 px, as it is padding inside their panel, not a distance from the screen edge. The battle unit info and the cast preview stay 4 px in: they are outlined panels with no shadow.                                                                                           |
| Naming       | The boxes are storage spaces, as the glossary says: `InventorySlotView` becomes `StorageSpaceView`, and the slot names of the inventory and the shop follow. In the shop's Buy mode a storage space shows an item of the merchant's stock.                                                                                 |
| Architecture | Only `shared.adapters.presentation` (`HudStyle`), `screen.adapters.presentation` (the views) and `storybook.adapters.presentation` (the stories). No use case or domain changes.                                                                                                                                         |
| Storybook    | No story is added. Before and after screenshots of **Screen / Inventory › Full**, **Screen / Inventory › Exploring**, **Screen / Shop › Sell, inventory full**, **Screen / Item buttons › Consumable**, **Screen / Cancel and confirm › Default** and **Screen / Finish turn › Enabled** on a private page, for approval. |
| Tests        | Each view's test checks that its outer boxes or buttons sit 12 px from each side of the screen. The tests that expect x 0 and 390 px today are updated.                                                                                                                                                                 |
| ADR          | None: the margin is cheap to change, so it is not hard to reverse.                                                                                                                                                                                                                                                       |
| Out of scope | The world builder's details, resize and stock, which also touch the screen edges, as the world builder is its own full-screen HUD. The square selection and equipped frames over the rounded boxes. `HudStyle.SLOT_RADIUS` and the ability and icon slots, which are not storage spaces.                                 |
| Delivery     | 2 milestones, each green on CI. See [Milestones](#milestones).                                                                                                                                                                                                                                                         |

E.g.: where the boxes and buttons sit across the 390 px of the screen:

|                         | Today                     | Once built                 |
|-------------------------|---------------------------|----------------------------|
| First box of a row      | x 0 to 60                 | x 12 to 68                 |
| Last box of a row       | x 330 to 390              | x 322 to 378               |
| Close                   | x 310 to 390              | x 298 to 378               |
| Item buttons, three     | 123.3 px each, x 0 to 390 | 115.3 px each, x 12 to 378 |
| Cancel and Confirm      | 190 px each, x 0 to 390   | 178 px each, x 12 to 378   |
| Finish turn             | x 0 to 390                | x 12 to 378                |

## Milestones

Each milestone is a few commits straight on `main`, green on CI.

| #  | Milestone      | Done when                                                                                                                 | Status | Done in  |
|----|----------------|---------------------------------------------------------------------------------------------------------------------------|--------|----------|
| M1 | Screen margin  | The inventory, the shop and the call to action sit 12 px in from each side, their shadows whole, approved on screenshots. | ✅      | 3983f36c |
| M2 | Storage spaces | No name of the inventory, the shop or their stories calls a storage space a slot.                                         | ✅      | 48983087 |

### M1 commits

1. Add `HudStyle.SCREEN_MARGIN` and `HudStyle.CONTENT_WIDTH`, and lay the inventory and the shop out between them, with
   56 px boxes, and their tests.
2. Lay every view of the call to action out between them, and their tests.
3. Take the after screenshots, and mark M1 as built, with its screenshots still to be approved.

### M2 commits

1. Rename `InventorySlotView` to `StorageSpaceView`, its story and the slot names of the inventory and the shop, and
   their tests.

## Reference

### HudStyle

- `HudStyle.SCREEN_MARGIN` is 12.0: how far from each side of the screen a view lays out its boxes and buttons.
- `HudStyle.CONTENT_WIDTH` is `390.0 - 2 * SCREEN_MARGIN`, 366.0, the 390 px of `GAME_WINDOW_SIZE` between both
  margins. It is not read from `GAME_WINDOW_SIZE`, which lives in `Main.kt`, outside every subdomain.

### InventoryView

- `SLOT_SIZE` becomes 56.0, and `WIDTH` becomes `HudStyle.CONTENT_WIDTH`: 6 × 56 + 5 × 6.
- The view keeps its transparent `solidRect` 390 px wide, and its `uiVerticalStack` starts at x `SCREEN_MARGIN`. So
  the title line, Close and the arrows move in with the grid. E.g.: Close goes from x 310–390 to x 298–378.
- `HEIGHT` follows the boxes: 32 + 2 × 56 + 6 + 10, 160 px instead of 168. The HUD it sits in stays 300 px.

### ShopView

- `SLOT_SIZE` becomes 56.0, and `WIDTH` becomes `HudStyle.CONTENT_WIDTH`. The header, the boxes and the line of the
  item start at x `SCREEN_MARGIN`. E.g.: `>` goes from x 350–390 to x 338–378.
- `GAP` becomes 29.0, so the header, the boxes and the line of the item stay evenly apart in the 300 px panel:
  29 + 32 + 29 + 118 + 29 + 32 = 269, 31 px left under it. Today the gaps are 27 and the line of the item ends at 271.

### Call to action

Each view keeps the 390 px of the screen and lays its content out from `SCREEN_MARGIN`, `CONTENT_WIDTH` wide:

- `FinishPlayerTurnView`: its button is `CONTENT_WIDTH` wide, at x `SCREEN_MARGIN`.
- `CancelAndConfirmView`: Cancel and Confirm are `(CONTENT_WIDTH - 10) / 2`, 178 px, through the `buttonWidth` of
  `CancelButton` and `ConfirmButton`, its stack at x `SCREEN_MARGIN`.
- `ButtonRowView`: `WIDTH` becomes `CONTENT_WIDTH`, the first button at x `SCREEN_MARGIN`. E.g.: two buttons are 178 px,
  at x 12 and 200.
- `ItemButtonsView`, `GiveGoldActionsView`, `GiveGoldRefusalView`, `GiveItemView`, `InventoryFullView` and
  `DiscardQuestionView`: `WIDTH` becomes `CONTENT_WIDTH`, and their stack or row starts at x `SCREEN_MARGIN`. Their
  lines of text move in with their buttons. `DiscardQuestionView` gets it through its `ButtonRowView`.
- `CallToActionView` and `GameScene` are not changed.

### Renames (M2)

Only names of the inventory and the shop; the save slots are a glossary term of their own, and are left as they are.

| Today                                                   | Once built                                                     |
|---------------------------------------------------------|----------------------------------------------------------------|
| `InventorySlotView`, `InventorySlotViewStories`         | `StorageSpaceView`, `StorageSpaceViewStories`                  |
| `screen-inventory-slot--*`, **Screen / Inventory slot** | `screen-storage-space--*`, **Screen / Storage space**          |
| `InventoryView.Slot`, `ShopView.Slot`                   | `InventoryView.StorageSpace`, `ShopView.StorageSpace`          |
| `SLOTS`, `SLOTS_PER_ROW`, `SLOTS_Y`                     | `STORAGE_SPACES`, `STORAGE_SPACES_PER_ROW`, `STORAGE_SPACES_Y` |
| `SLOT_SIZE`, `SLOT_SPACING`                             | `STORAGE_SPACE_SIZE`, `STORAGE_SPACE_SPACING`                  |
| `slotViews`, `selectSlot`                               | `storageSpaceViews`, `selectStorageSpace`                      |
| `slotSelected`, `shopSlotSelected`                      | `storageSpaceSelected`, `shopStorageSpaceSelected`             |
| `selectedSlotIndex`, `displayedSlots` of the presenters | `selectedStorageSpaceIndex`, `displayedStorageSpaces`          |
| `slotsOf`, `clearSelectedSlot` of the presenters        | `storageSpacesOf`, `clearSelectedStorageSpace`                 |
| The `selectedSlot` and `equippedSlot` story controls    | `selectedStorageSpace` and `equippedStorageSpace`              |
| `HudStyle.EMPTY_SLOT`                                   | `HudStyle.EMPTY_STORAGE_SPACE`                                 |
| "The slot tapped last" and the other story descriptions | "The storage space tapped last"                                |

The test page objects and fakes follow, e.g.: `slotCount()` and `slotBitmaps()` in `InventoryViewExtension` and
`ShopViewExtension`, and `FakeInventoryView`. `StorySmokeTest` lists the new story ids.

### Storybook

- No story is added or changed in M1. The before screenshots of **Screen / Inventory › Full**,
  **Screen / Inventory › Exploring**, **Screen / Shop › Sell, inventory full**, **Screen / Item buttons › Consumable**,
  **Screen / Cancel and confirm › Default** and **Screen / Finish turn › Enabled** are taken with the
  [storybook skill](../../.claude/skills/storybook/SKILL.md) before the change, the after ones once built, and shown
  side by side on a private Artifact page for approval.
- The prototype the look was picked on, with a `look` control on the Inventory and Shop stories, is on the local branch
  `prototype/item-cells`. It is not merged.

### Tests

- `InventoryViewTest`
  - `Init should lay the boxes out 12 px in from each side of the screen, 56 px wide, when is initialized`
  - `Init should keep close 12 px in from the right side of the screen when is initialized`
- `ShopViewTest`
  - `Init should display 12 empty slots, under the header and above the line of the item, spread over the panel`:
    `footerBottom()` is 269.0, not 271.0.
  - `Init should lay the boxes and the arrows out 12 px in from each side of the screen`
- `ButtonRowViewTest`
  - `ButtonRowView should fill the row between the screen margins when it has one action` (replaces
    `ButtonRowView should fill the row when it has one action`): x 12, 366 px wide.
  - `ButtonRowView should keep the first button on the left when it has several actions`: x 12 and 200, 178 px wide.
  - `ButtonRowView should share the width in thirds when it has three actions`: the last button ends at 378.
- `FinishPlayerTurnViewTest`
  - `Init should lay finish turn out 12 px in from each side of the screen when is initialized`
- `CancelAndConfirmViewTest`
  - `Init should lay cancel and confirm out 12 px in from each side of the screen when is initialized`
- `GiveGoldActionsViewTest`
  - `Buttons should lay out 12 px in from each side of the screen`
- `CallToActionViewTest`
  - `DisplayItemButtons should lay the item buttons out 12 px in from each side of the screen when they are displayed`
  - `DisplayGiveItem should lay cancel out 12 px in from each side of the screen when it is displayed`
  - `DisplayGiveGoldRefusal should lay ok out 12 px in from each side of the screen when it is displayed`
  - `DisplayInventoryFull should lay its buttons out 12 px in from each side of the screen when it is displayed`
  - `DisplayDiscardQuestion should lay back and discard out 12 px in from each side of the screen when it is displayed`
- M2 renames the test names that say slot, e.g.: `SelectSlot should frame only the selected slot when another slot was
  selected` becomes `SelectStorageSpace should frame only the selected storage space when another one was selected`.

## Known limitations

- The world builder's details, resize and stock still touch the screen edges.
- The selection and equipped frames stay square over the rounded boxes.
- The inventory is 8 px shorter, which leaves 8 px more under it in the HUD.
- Like the rest of the parchment HUD, it is checked on Storybook screenshots, not on a real phone.

## Changes while building

- **M1, the inventory full message.** Beside the 32 px icon and its 8 px gap, the message of `InventoryFullView` gets
  326 px instead of 350, and `Tap an item to discard for: Health Potion (10 G)` needs 331 px, so it ended in `…`. The
  Spanish one already did, at 365 px. A line of its own does not fit, as the call to action has 82 px under the HUD. So
  the message is shorter in both languages, as agreed with the user: `Discard an item for: {item} ({sellPrice})` and
  `Descarta un objeto por: {item} ({sellPrice})`.
- **M1, the tests.** The views' tests read the sides of their boxes and buttons with `View.sidesIn`, a new helper of the
  shared `ViewExtension`, and `CallToActionView.buttonSides()` reads them over every button the call to action shows.
  `ShopViewTest` also checks the arrows, and `CancelAndConfirmViewTest` the 178 px of Cancel and Confirm.
- **M2, beyond the inventory and the shop.** The rename also reaches the names that quote them: the `shopSlotSelected`
  sample actions of `RecordActionTest`, `SearchActionsTest` and `ActionsViewTest`, the `screen-inventory-slot` sample
  ids of `StorybookTest`, the example of `how-to-add-a-story.md`, and the `selectedSlot` arg and the shop's click of the
  storybook skill, whose second storage space is now at (406, 409). `clickSlot` and `loadSlot` of the save slots, and
  `SLOT_RADIUS`, keep their names.
