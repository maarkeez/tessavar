# Plan: every effect readable: stacks at 0 through their last player turn, and equipped effect icons

> **Status:** built on `main`, the screenshots of M1 and M2 still to be approved.
>
> **Summary:** Today a stack ends the moment its remaining duration counts down to 0, at the start of its recipient's
> player turn, so the effect icon is gone for the whole last turn the effect shapes. E.g.: a CPU water bat soaks the hero
> with Splash and a storm bat casts Thunder on it, stun and stun-long. On the hero's first player turn the stun has
> already ended and the hero has no steps nor casts, with only stun-long (1) and soak (1) to show for it. On its second,
> stun-long and soak have ended too, and the hero stands stunned with no effect icon at all. Equipped effects, such as
> the rune hammer's +30 maximum health and +40 maximum mana, have no effect icon and no effect info either. Instead, a
> stack that reaches 0 in a battle stays ongoing, its effect icon reading 0 and its effect info "Ends this turn", until
> its recipient's player turn finishes, and every equipped effect has an effect icon, after the ongoing ones, and an
> effect info. It builds on the readable effects ([plan](readable-effects.md)) and the effects that interact
> ([plan](effect-interactions.md), [ADR 0023](../adr/0023-effect-interactions-composed-from-single-outcome-effects.md)).

## How it counts down today

- `BattleUnit.triggerOnPlayerTurnStartedEffects` triggers each stack timed on player turn started, and
  `Timing.OnPlayerTurnStarted.trigger()` returns null when the remaining duration reaches 0, which removes the stack.
  It then calls `OngoingEffects.countDownWhileOngoing()`, and `Timing.WhileOngoing.countDown()` removes a while ongoing
  stack the same way.
- `OngoingEffectsTrigger.trigger` runs it for the battle units of the player whose turn starts (`inBattle = true`), and
  each second while exploring (`inBattle = false`).
- `stun` and `stun-long` negate actions at that trigger, so the turn they shape is the one right after their last
  stack ended.

## Decisions

| Topic                 | Decision                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope                 | Every effect a battle unit has, ongoing or equipped, has an effect icon in the battle unit info and an effect info: `burn`, `soak`, `stun`, `stun-long`, `venom-damage` and `venom-on-defeat` read through their last player turn, and `maximum-health-20`, `-30`, `-40`, `maximum-mana-20` and `-40` get an icon. In battles and while exploring, and in the inventory's battle unit info.                                                                          |
| Stacks at 0           | Remaining durations still count down when the recipient's player turn starts. In a battle, a stack that reaches 0 stays ongoing until its recipient's player turn finishes, and then ends. Chosen over counting down when the player turn finishes, which reads 1 through the last turn, as 0 keeps one meaning (triggers still to come) and says it is spent; over lingering only for negate actions, which leaves soak vanishing; and over a separate "no actions" marker, which leaves the stun icon vanishing. |
| A stack at 0          | It still counts as ongoing, for apply if ongoing and resist, which changes no outcome today: it lives only through its recipient's own player turn, and no ability is cast on an ally, nor does burn trigger then. Its effect icon reads 0, not dimmed.                                                                                                                                                                                                              |
| Exploring and battles | While exploring, where there is no player turn, a stack ends at its last trigger as today. Any stack still at 0 ends when its battle finishes, e.g.: the hero's soak at 0 when it defeats the last enemy on its turn.                                                                                                                                                                                                                                                 |
| Effect info at 0      | A stack at 0 lists as "Ends this turn" ("Termina este turno"), not "0 turns left", e.g.: **Stun**, "Loses its actions at the start of each turn.", "Ends this turn". Stacked: "2 turns left · Ends this turn".                                                                                                                                                                                                                                                      |
| Water                 | A battle unit on water shows soak x2 during its own player turn: last round's stack at 0 and this round's at 1. It is true, so it stays.                                                                                                                                                                                                                                                                                                                             |
| Equipped icons        | One effect icon per equipped effect, grouped by effect id as the ongoing ones, with no number, after the ongoing effects, in the battle unit info only. The cast preview keeps ongoing effects only, as a cast never changes an equipped effect. Chosen over the picture of the item, as an outcome's picture reads the same on every item.                                                                                                                           |
| Equipped art          | Two new 16×16 drawings, a maximum health and a maximum mana picture, in `art/effect/maximum-health.aseprite` and `art/effect/maximum-mana.aseprite`, each exported as one PNG per effect id: `resources/effect/maximum-health-20.png`, `-30` and `-40`, and `maximum-mana-20.png` and `-40`. Chosen over one PNG per outcome, as the icon lookup and `EffectImagesTest` stay as they are.                                                                             |
| Equipped info         | The name "maximum health" or "maximum mana" ("salud máxima", "maná máximo"), and the sentence "+{amount} maximum health while equipped." ("+{amount} de salud máxima mientras está equipado."), with no time line. Chosen over naming the item.                                                                                                                                                                                                                       |
| Summon                | A summon's remaining summon duration stays the "Vanishes in N turns" text next to its name, as it is not an effect.                                                                                                                                                                                                                                                                                                                                                  |
| Architecture          | The stacks at 0 in `battleUnit.domain` (`BattleUnit`, `OngoingEffects`) and a new `battleUnit.usecases.commands.EndStacksAtZero`, called by `battleUnit.adapters.events.OnPlayerTurnFinished` and `OnBattleFinished`. The equipped icons in `screen.adapters.presentation`. No save change: the snapshot holds a remaining duration of 0 as any other.                                                                                                              |
| Storybook             | **Screen / Effect › Ending**, **Screen / Effect info › Stun ending** and **Maximum health**, and **Screen / Battle unit info › Equipped**. Approved on Storybook screenshots on a private Artifact page.                                                                                                                                                                                                                                                              |
| Tests                 | Domain tests for the stacks at 0, a use case test for `EndStacksAtZero`, an acceptance test for the hero stunned after Splash and Thunder, mapper and view tests for the 0 and the equipped icons, and `EffectImagesTest` and `EffectNamesTest` widened to equipped effects.                                                                                                                                                                                          |
| Out of scope          | Applying a terrain's effects when a battle unit walks onto it (today only at a round start), tapping the cast preview's icons, naming the item an equipped effect comes from, and an icon for a summon's remaining summon duration.                                                                                                                                                                                                                                  |
| Delivery              | 2 milestones, each green on CI. See [Milestones](#milestones).                                                                                                                                                                                                                                                                                                                                                                                                       |

For example, a CPU water bat casts Splash on the hero, and a storm bat casts Thunder on it, in the CPU's player turn:

| Moment                        | Effect icons today                | Effect icons                      | Effect info after tapping stun-long                               |
|-------------------------------|-----------------------------------|-----------------------------------|-------------------------------------------------------------------|
| CPU's player turn             | soak (2), stun (1), stun-long (2) | soak (2), stun (1), stun-long (2) | Stun. Loses its actions at the start of each turn. 2 turns left   |
| Hero's 1st player turn, stuck | soak (1), stun-long (1)           | soak (1), stun (0), stun-long (1) | Stun. Loses its actions at the start of each turn. 1 turn left    |
| Hero's 1st player turn ends   | soak (1), stun-long (1)           | soak (1), stun-long (1)           | Closed, as the next player turn start deselects                   |
| Hero's 2nd player turn, stuck | none                              | soak (0), stun-long (0)           | Stun. Loses its actions at the start of each turn. Ends this turn |
| Hero's 2nd player turn ends   | none                              | none                              | Closed                                                            |

## Milestones

Each milestone is a few commits straight on `main`, green on CI.

| #  | Milestone             | Done when                                                                                                                                          | Status | Done in  |
|----|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|--------|----------|
| M1 | Stacks at 0           | A stunned or soaked battle unit shows its effect icon at 0 and "Ends this turn" through its last player turn, approved on Storybook screenshots.   | ✅      | 5627f5a0 |
| M2 | Equipped effect icons | The battle unit info shows an effect icon and an effect info for every equipped effect, in English and Spanish, approved on Storybook screenshots. | ✅      | 49e0cdea |

### M1 commits

1. `BattleUnit.triggerOnPlayerTurnStartedEffects` keeps the stacks that reach 0 in a battle, `BattleUnit.endStacksAtZero`
   and `EndStacksAtZero` end them, called by `OnPlayerTurnFinished` and `OnBattleFinished`, with the acceptance test.
2. `ui.effectInfo.endsThisTurn` in `messages.pot` and `es.po`, and `EffectInfoView` reading a 0 as it.
3. **Screen / Effect › Ending** and **Screen / Effect info › Stun ending**, and the screenshots go on a private
   Artifact page.

### M2 commits

1. The maximum health and maximum mana drawings, with the `pixel-art` skill, and their five PNGs.
2. The equipped effect names and sentences in `messages.pot` and `es.po`, `EffectInfoMapper`'s two new sentences, and
   `EffectImagesTest` and `EffectNamesTest` widened to equipped effects.
3. `BattleUnitInfo.equippedEffects`, built by `BattleUnitInfoMapper` and drawn by `BattleUnitInfoView` after the
   ongoing effects, with their effect infos.
4. **Screen / Effect info › Maximum health** and **Screen / Battle unit info › Equipped**, and the screenshots go on a
   private Artifact page.

## Reference

### battleUnit

- `BattleUnit.triggerOnPlayerTurnStartedEffects(effects, currentRow, currentColumn, inBattle)` keeps a stack whose
  remaining duration reaches 0 when `inBattle`, as `OngoingEffectDto(effectId, remainingDuration = 0)`, and removes it
  as today while exploring. `Timing.OnPlayerTurnStarted.trigger()` and `Timing.WhileOngoing.countDown()` return a 0
  timing, and `OngoingEffects` drops it at once when not `inBattle`. The trigger loop moves past a stack at 0, as it
  does past any stack that stays. E.g.: a stun (1) triggers, negates the actions, and stays as stun (0).
- `BattleUnit.endStacksAtZero()` removes every stack timed on player turn started or while ongoing whose remaining
  duration is 0, and keeps the others. E.g.: soak (0) and stun-long (1) leave stun-long (1).
- `EndStacksAtZero.ofPlayer(playerId, battleUnitIds)` ends the stacks at 0 of the player's battle units in the battle,
  found as `ReduceRemainingSummonDurations` finds them, and `EndStacksAtZero.ofEveryBattleUnit()` those of every battle
  unit. `OnPlayerTurnFinished` calls `ofPlayer` next to `reduceRemainingSummonDurations`, and `OnBattleFinished` calls
  `ofEveryBattleUnit` before `vanishSummons`. It publishes no event: the next player turn start, or the battle
  finishing, deselects, and the battle unit info is drawn afresh.
- `hasOngoingEffect` counts a stack at 0, as `OngoingEffects.hasOngoingEffect` already counts every stack not timed
  immediately.

### Effect info at 0

- `EffectInfoView` words a 0 in `EffectInfo.remainingDurations` as `ui.effectInfo.endsThisTurn`, and every other as
  `turnsLeft` or `secondsLeft`, longest first, joined by " · ". E.g.: "2 turns left · Ends this turn".
- `EffectView.displayEffect` already draws a 0 as any other remaining duration.

### Equipped effect icons

- `BattleUnitInfo` gains `equippedEffects: List<EffectsView.Effect>`, defaulting to none. `BattleUnitInfoMapper`
  builds it from `battleUnit.equippedEffects` (each equipped item's effect ids), one icon per effect id in the order of
  the items, with its stack count and no remaining duration. E.g.: a rune hammer gives maximum-health-30 and
  maximum-mana-40.
- `BattleUnitInfoMapper` adds the equipped effects to `effectInfos`, with no remaining durations.
- `BattleUnitInfoView.display` and `refresh` draw `effects + equippedEffects` in `effectsView`, so a tap shows an
  equipped effect's info too. `RecipientPreviewView` keeps drawing `effects` only.
- `EffectInfoView.EffectInfo.Sentence` gains `IncreaseMaximumHealth(amount)` and `IncreaseMaximumMana(amount)`, which
  `EffectInfoMapper.effectInfo` picks for an increase maximum health or increase maximum mana outcome timed while
  equipped.
- `EffectImagesTest` and `EffectNamesTest` check the effects timed on `PreviewCastOngoingEffects.ONGOING_TIMINGS` and
  while equipped. `ONGOING_TIMINGS` stays as it is, as the cast preview reads it.

### Catalog

| Key                                           | English                                  | Spanish                                           |
|-----------------------------------------------|------------------------------------------|---------------------------------------------------|
| `ui.effectInfo.endsThisTurn`                  | Ends this turn                           | Termina este turno                                |
| `effect.maximum-health-20.name`, `-30`, `-40` | maximum health                           | salud máxima                                      |
| `effect.maximum-mana-20.name`, `-40`          | maximum mana                             | maná máximo                                       |
| `ui.effectInfo.increaseMaximumHealth`         | +{amount} maximum health while equipped. | +{amount} de salud máxima mientras está equipado. |
| `ui.effectInfo.increaseMaximumMana`           | +{amount} maximum mana while equipped.   | +{amount} de maná máximo mientras está equipado.  |

Each new key gets a `UiMessage` and a translator comment, as the effect info's others do.

## Storybook

- `EffectViewStories`: `Ending` (`screen-effect--ending`), a stun reading 0.
- `EffectInfoViewStories`: `Stun ending` (`screen-effect-info--stun-ending`), a stun at 0 reading "Ends this turn", and
  `Maximum health` (`screen-effect-info--maximum-health`), the rune hammer's +30 maximum health with no time line.
- `BattleUnitInfoViewStories`: `Equipped` (`screen-battle-unit-info--equipped`), the `With effects` battle unit with a
  rune hammer equipped: its ongoing effects, then maximum health and maximum mana.
- `StorySmokeTest` lists the new stories.

## Tests

- `BattleUnitTest`
  - `TriggerOnPlayerTurnStartedEffects should keep a stack at 0 when its remaining duration runs out in a battle`
  - `TriggerOnPlayerTurnStartedEffects should remove a stack when its remaining duration runs out while exploring`
  - `TriggerOnPlayerTurnStartedEffects should keep a while ongoing stack at 0 when it counts down to 0 in a battle`
  - `EndStacksAtZero should remove the stacks at 0 and keep the others when the battle unit has both`
  - `HasOngoingEffect should be true when the effect has a stack at 0`
- `TriggerOnPlayerTurnStartedEffectsTest`
  - `Trigger should count down the effects timed while ongoing, keeping the ones at 0, when the player turn starts`
    (replaces `Trigger should count down the effects timed while ongoing when the player turn starts`)
- `EndStacksAtZeroTest`
  - `OfPlayer should end the stacks at 0 of the player's battle units when the player turn finishes`
  - `OfPlayer should not end the stacks at 0 when the battle units are the other players'`
  - `OfEveryBattleUnit should end the stacks at 0 of every battle unit when the battle finishes`
- `OnPlayerTurnFinishedTest` (new)
  - `PlayerTurnFinished should end the stacks at 0 of the player's battle units`
- `OnBattleFinishedTest` (new)
  - `BattleFinished should end the stacks at 0 of every battle unit`
- `EffectInteractionsAcceptanceTest`
  - `The soaked battle unit should show stun-long and soak at 0 through its second stunned player turn when Thunder is cast after splash`
- `EffectInfoViewTest`
  - `Display should say ends this turn when a stack is at 0`
  - `Display should say ends this turn after the longer stacks when it is stacked`
  - `Display should not display a time line when the effect is equipped`
- `EffectInfoMapperTest`
  - `EffectInfo should say the maximum health while equipped when the effect increases the maximum health`
  - `EffectInfo should say the maximum mana while equipped when the effect increases the maximum mana`
- `BattleUnitInfoMapperTest`
  - `BattleUnitInfo should list one equipped icon per effect without remaining duration when items are equipped`
  - `BattleUnitInfo should group the equipped effects of an effect id when two items give it`
  - `BattleUnitInfo should give an effect info to each equipped effect`
- `BattleUnitInfoViewTest`
  - `Display should draw the equipped effects after the ongoing ones when the battle unit has both`
  - `TapEffect should display the effect info of an equipped effect when its icon is tapped`
- `RecipientPreviewViewTest`
  - `Display should not draw the equipped effects`
- `EffectImagesTest`, `EffectNamesTest`
  - the existing tests, now over the equipped effects too

## Known limitations

- A battle unit that walks onto water or lava in a battle still gets no effect until the next round starts, and none
  in the first round.
- An equipped effect's info does not say which item gives it; the inventory does.
- A stack at 0 counts as ongoing through its recipient's own player turn. Should an ability one day be cast on an ally
  with apply if ongoing, the spent stack would still meet it.

## Changes while building

- `EndStacksAtZero` is a command use case with a single `invoke()`, as `CommandUseCaseArchitectureTest` asks, not
  `ofPlayer` and `ofEveryBattleUnit`: it ends the stacks at 0 of every battle unit, both when a player turn finishes and
  when the battle finishes, which is the same, as only the battle units whose player turn is finishing hold any. Its
  tests are `should end the stacks at 0 of every battle unit` and `should keep the stacks with a remaining duration
  left`, and the fix is its own commit (5627f5a0).
- `OngoingEffects.nextOnPlayerTurnStartedIndex` skips a stack at 0, so a stack that is somehow still at 0 when a player
  turn starts is never triggered again, with `TriggerOnPlayerTurnStartedEffects should not trigger a stack at 0 when the
  next player turn starts before it ended` in `BattleUnitTest`.
- The `BattleUnitTest`s that play two player turns in a row on a battle unit now call `endStacksAtZero()` between them,
  as the player turn finishing does, and the ones that expected a stack gone after its last trigger in a battle expect
  it at 0: `TriggerOnPlayerTurnStartedEffects should reduce the health and keep the stack at 0 when the effect damages
  for its last player turn in a battle` and `TriggerOnPlayerTurnStartedEffects should remove the on player turn started
  effect when its player turn finishes after its last trigger` replace the two that said "remove".
- **Screen / Effect**'s `remainingDuration` control goes from -1, for none, as 0 now draws a 0. **Screen / Effect info**
  gains an `ending` control (`boolean`), which puts its shortest stack at 0, used by **Stun ending**.
- `EffectInfoViewTest` also checks "Termina este turno" in Spanish.
- The equipped story is **Screen / HUD › Equipped effect info** (`screen-hud--equipped-effect-info`), not **Screen /
  Battle unit info › Equipped**: an `equipped` control would make the battle unit info's controls cover its actions,
  which `StorySmokeTest` checks, as for the effect info of the readable effects plan. It shows the ongoing sample effects,
  then the rune hammer's maximum health and maximum mana, the maximum health tapped.
- The five equipped effect sources are `art/effect/maximum-health-20.aseprite`, `-30`, `-40`, `maximum-mana-20` and
  `-40`, copies of the two drawings, as the `pixel` tool exports a source to the PNG of its own name: a
  `maximum-health.aseprite` would export an unused `maximum-health.png`.
- `StoryContext.effects`, the sample effects of the stories, keeps to the ongoing effects (not `maximum-*`), as the new
  PNGs sort among them and would have turned **Screen / Effects › Some** into burn and two hearts. The art commit
  (4b7dfe5b) holds it, and its sample effect info tells an equipped effect with no time line.
- `BattleUnitInfoViewTest` also checks that a tap on an equipped icon selects it, and `EffectInfoViewTest` checks an
  equipped effect info in Spanish.
