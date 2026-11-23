# Plan: five steps in the strip of the battle unit info

> **Status:** done.
>
> **Summary:** When a battle unit with 5 steps selects an ability, its Move points run past the strip that the battle
> unit info collapses to. E.g.: a bat selects Bat. The strip's points start at x 332, the panel ends at x 386, and 5 points
> are 82 px wide, so they end at x 414, past the panel and the 390 px view. The view is then wider than the screen
> expects. The full header, and every unit's single cast, fit. Instead, the strip always has room for 5 Move points at
> their full size, 6 px inside the panel, with shorter health and mana bars. A test over every unit in the game
> resources keeps it so. It was picked from a prototype of three options on the local branch `prototype/strip-move-pips`.
> It builds on the vector HUD chrome ([ADR 0007](../adr/0007-vector-hud-chrome-around-pixel-art.md)).

## How the strip lays out today

- `BattleUnitInfoView.focus()` hides the `header` and shows the `strip` when `displayAbilityInfo` is called.
- The strip's bars end at `STRIP_BARS_RIGHT` (`WIDTH - MARGIN - 112`, x 274). The Move and Cast captions start 14 px
  after them, at `STRIP_ACTIONS_X` (x 288), and `stripMovementPointsView` and `stripCastPointsView` start 44 px after
  that, at `STRIP_POINTS_X` (x 332).
- A `PointsView` draws each point 14×7 (`POINT_WIDTH`, `POINT_HEIGHT`) with a 3 px gap (`POINT_SPACING`), so 3
  points take 48 px and 5 take 82 px. The panel's right edge is at x 386 (`MARGIN + PANEL_WIDTH`), which leaves 54 px.
- `bat`, `fire-bat`, `storm-bat`, `water-bat` and `bee` have `maximumSteps = 5`. Every other unit has 2 or 3, and
  every battle unit has 1 cast (`CASTS_PER_PLAYER_TURN`).

## Decisions

| Topic        | Decision                                                                                                                                                                                                                                                                                                                                                                                                               |
|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope        | The strip of `BattleUnitInfoView` only, in battles, while exploring and in the inventory's battle unit info. The full header already fits 5 points.                                                                                                                                                                                                                                                                    |
| Room         | The strip keeps room for 5 points, the most steps any unit has, and lays out from the right: the points end 6 px inside the panel, as the portrait starts 6 px inside it on the left. Chosen over smaller 8×6 pips in the strip, which end almost on the panel's edge and shrink the single Cast point, and over "2/5" as text above 3 steps, which makes the strip look different for bats than for every other unit. |
| Bars         | The strip's health and mana bars shrink from 188 to 154 px for every unit, including 3-step units, so the strip looks the same for every battle unit.                                                                                                                                                                                                                                                                  |
| Points       | Points keep their size, 14×7 with a 3 px gap, in the header and the strip alike.                                                                                                                                                                                                                                                                                                                                       |
| Architecture | `screen.adapters.presentation` only: `BattleUnitInfoView`'s strip constants. No domain or use case change.                                                                                                                                                                                                                                                                                                             |
| Storybook    | The battle unit info stories show 5 steps, the widest strip and header in the game, and a new **Screen / Battle unit info › Ability selected, bat** story shows a bat with 2 of its 5 steps left.                                                                                                                                                                                                                      |
| Tests        | A view test for 5 points inside the strip, and a new `BattleUnitInfoResourcesTest` that displays every unit in the game resources focused and lists any whose Move points end past the panel.                                                                                                                                                                                                                          |
| Out of scope | The full header, `PointsView`, the HUD stories' 3 steps, and a strip for more than 5 steps.                                                                                                                                                                                                                                                                                                                            |

For example, the strip from left to right, before and after:

| Piece              | Today        | After        |
|--------------------|--------------|--------------|
| Bars               | x 86 to 274  | x 86 to 240  |
| Move and Cast      | x 288        | x 254        |
| Points, 3 steps    | x 332 to 380 | x 298 to 346 |
| Points, 5 steps    | x 332 to 414 | x 298 to 380 |
| Panel's right edge | x 386        | x 386        |

## BattleUnitInfoView

- `STRIP_FITTED_STEPS` (5) and `STRIP_PADDING` (6.0) are new. `STRIP_POINTS_X` is
  `MARGIN + PANEL_WIDTH - STRIP_PADDING - STRIP_FITTED_STEPS * PointsView.POINT_WIDTH - (STRIP_FITTED_STEPS - 1) * PointsView.POINT_SPACING`
  (x 298), and `STRIP_ACTIONS_X` and `STRIP_BARS_RIGHT` are worked out from it, 44 and 58 px before (x 254 and x 240).
  E.g.: a bat's 5 Move points end at x 380, and the hero's 3 end at x 346.
- `stripHealthBarView` and `stripManaBarView` are `STRIP_BARS_RIGHT - STRIP_CONTENT_X - CAPTION_WIDTH` wide, 154 px.

## Storybook

- `BattleUnitInfoViewStories`: `MAXIMUM_STEPS` goes from 3 to 5, so the `steps` control is an int from 0 to 5 and
  every story starts with 5 steps.
- `BattleUnitInfoViewStories`: the **Screen / Battle unit info › Ability selected, bat** story
  (`screen-battle-unit-info--ability-selected-bat`): `unit` `bat`, `steps` 2, `selectedAbility` 1. Its description is
  "A bat, with 5 steps, has selected an ability: its Move points fit in the strip, 2 of them left."
- `StorySmokeTest` lists the new story.

## Tests

- `BattleUnitInfoViewTest`
  - `BattleUnitInfoView should draw five steps inside the strip when it displays the ability info`
- `BattleUnitInfoResourcesTest` (new, in `screen.adapters.presentation`, like `AbilityInfoResourcesTest`)
  - `BattleUnitInfoView should draw the steps of every unit inside the strip when it reads the game resources`: it
    loads every unit with `ResourcesUnitLoader`, displays each one with its `maximumSteps` and an ability info, and
    lists the units whose `stripMovementPointsView` ends past `MARGIN + PANEL_WIDTH - STRIP_PADDING`.

## Known limitations

- A unit with 6 or more steps fails `BattleUnitInfoResourcesTest`. Then the strip needs another layout, e.g.: smaller
  points or text, which were prototyped and dropped.
- A 3-step unit leaves 34 px empty after its Move points in the strip.
- The prototype branch `prototype/strip-move-pips` stays local and is deleted once this plan is built.
