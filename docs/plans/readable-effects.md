# Plan: effects the player can read: grouped icons with their remaining duration, and an effect info on tap

> **Status:** built on `main`, the screenshots of M1 and M2 still to be approved.
>
> **Summary:** Today the battle unit info shows each ongoing effect as a bare 20×20 icon, up to ten and then a "+",
> with no name, no description and no remaining duration, and tapping an icon does nothing. E.g.: a hero poisoned twice
> by venom-strike and soaked by Splash shows two identical green icons and a blue one, and the player can not tell what
> any of them does, nor that one venom ends in 2 player turns and the other in 4. While exploring, the icons and the
> health bar do not even change as venom ticks each second, as the battle unit info only redraws when a battle unit is
> selected again. Instead, the icons are grouped by effect, each with a ×2 count when stacked and its longest remaining
> duration, and they stay live while exploring. Tapping one shows an **effect info** in place of the ability buttons:
> the effect's name, what it does and how long each of its stacks has left. It builds on the effects that interact
> ([plan](effect-interactions.md), [ADR 0023](../adr/0023-effect-interactions-composed-from-single-outcome-effects.md))
> and the vector HUD chrome ([ADR 0007](../adr/0007-vector-hud-chrome-around-pixel-art.md)).

## Decisions

| Topic         | Decision                                                                                                                                                                                                                                                                                                                                                                                                                         |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope         | The six lasting effects, the ones with an icon: `burn`, `soak`, `stun`, `stun-long`, `venom-damage` and `venom-on-defeat`. Their icons wherever effects show (the battle unit info and the rows of the cast preview), and an effect info in the battle unit info only, in battles and while exploring.                                                                                                                           |
| Reading       | Tapping an effect icon in the battle unit info shows its effect info. Chosen over a line always shown next to each effect, as the HUD has no room for it, and over a popup over the icon, as the effect info reads like the ability info and phones have no hover.                                                                                                                                                               |
| Grouped icons | One icon per effect id, in the order the battle unit first received it. A stacked effect shows a ×2 (×3…) count and the longest remaining duration of its stacks. Chosen over one icon per ongoing effect, each with its own number, as two identical icons read as a mistake. `stun` and `stun-long` stay two icons, as they are two effects with two images.                                                                   |
| Badges        | The chip stays 20×20, as the effects row sits at y = 94 in the 120 px header. The remaining duration is a 9 px number in its bottom-right corner, the count a 9 px ×2 in its top-right corner. An on defeated effect (`venom-on-defeat`) shows no number. Built straight on `main` and approved on Storybook screenshots on a private Artifact page.                                                                             |
| Cast preview  | Its rows group the same way and stay plain icons, not tappable. A cast that adds a stack makes the grouped icon blink with its new count, e.g.: venom ×2 blinking on a hero already poisoned, instead of a second blinking venom. A new stack counts with the effect's own duration, e.g.: 5 for `venom-damage`.                                                                                                                 |
| Effect info   | The effect's icon, its name (with ×2 when stacked), one sentence of what it does each time it triggers, and a line with how long each stack has left, longest first. E.g.: **Venom ×2**, "3 damage at the start of each turn.", "4 turns left · 2 turns left". An on defeated effect has no time line. It names neither the source nor the abilities that react to it (e.g. Thunder on a soaked target).                         |
| Sentences     | Burn and venom: "{damage} damage at the start of each turn.". Stun: "Loses its actions at the start of each turn.". Soak: "{share}% less damage from {effect}.". Venom burst: the ability info's "When defeated: poisons its adjacent allies (…)". While exploring, burn and venom say "each second" and the time line says "seconds left"; stun keeps its battle sentence, as negate actions triggers nothing outside a battle. |
| Names         | Every lasting effect has a name, lowercase as burn's already is, capitalised as a title: Burn, Soak, Stun (both stuns), Venom and Venom burst. In Spanish: Quemado (replacing "quemadura", which the soak sentence also reads), Mojado, Aturdido, Veneno and Veneno explosivo.                                                                                                                                                   |
| Layout        | The effect info takes the place of the ability buttons, and the header (portrait, bars and effect icons) stays, so another effect can be tapped at once. Chosen over the ability info's layout, which shrinks the header to the strip and would hide the effect icons. The tapped icon is ringed in `HudStyle.SELECTION`, without blinking.                                                                                      |
| Closing       | Tapping the same icon again, or the effect info, closes it and brings the ability buttons back. Tapping another icon switches to it. Selecting another battle unit, deselecting, or a player turn start (which deselects) closes it. Displaying the same battle unit again keeps it open with fresh numbers, and closes it when its last stack ends.                                                                             |
| Selection     | The effect info is a state of `BattleUnitInfoView`, outside the `Selection`, as it shows nothing on the battlefield. The glossary's Selection stays "nothing, a battle unit, one of its abilities, or a cast to preview".                                                                                                                                                                                                        |
| Live refresh  | `battleUnit` publishes a new `OngoingEffectsTriggered` after a battle unit's ongoing effects trigger, and `BattlefieldPresenter` redraws the battle unit info when it shows that battle unit, while exploring. Chosen over refreshing on `EffectReceived` and `OnPlayerTurnStartedEffectTriggered` only, which misses a soak counting down, and over no refresh. It also keeps the health bar live while exploring.              |
| Architecture  | The remaining durations in `battleUnit.domain` (`BattleUnit.Dto.OngoingEffectsDto`), the event in `battleUnit.domain` and `battleUnit.usecases.services.OngoingEffectsTrigger`, and everything else in `screen.adapters.presentation`. No save change: the snapshot already holds the remaining durations.                                                                                                                       |
| Storybook     | **Screen / Effects** gains the stacked and new stack stories, **Screen / Effect info** is new, and **Screen / Battle unit info** gains an effect info story.                                                                                                                                                                                                                                                                     |
| Tests         | Domain tests for the remaining durations and the event, view tests for the chips and the effect info, mapper tests for the grouping and the sentences, a presenter test for the live refresh, and a resources test that every lasting effect has a name.                                                                                                                                                                         |
| Out of scope  | Equipped effects (e.g. a hammer's +20 maximum health), as they are not ongoing effects, have no icon and the health bar holds them. Tapping effects in the cast preview, a tap hint or first-time tip, naming who applied an effect, and listing the abilities that react to one.                                                                                                                                                |
| Delivery      | 2 milestones, each green on CI. See [Milestones](#milestones).                                                                                                                                                                                                                                                                                                                                                                   |

For example, a hero poisoned by two venom-strikes, the second one cast two player turns after the first, and then
soaked by the water bat's Splash:

| Moment                        | Effect icons           | Effect info after tapping venom                                           |
|-------------------------------|------------------------|---------------------------------------------------------------------------|
| Soaked                        | venom ×2 (5), soak (2) | Venom ×2. 3 damage at the start of each turn. 5 turns left · 3 turns left |
| Hero's next player turn       | venom ×2 (4), soak (1) | Closed, as the player turn start deselects                                |
| Battle ends, 1 s of exploring | venom ×2 (3)           | Venom ×2. 3 damage each second. 3 seconds left · 1 second left            |
| 1 s later                     | venom (2)              | Venom. 3 damage each second. 2 seconds left                               |
| 2 s later                     | none                   | Closed, as its last stack ended                                           |

## Milestones

Each milestone is a few commits straight on `main`, green on CI.

| #  | Milestone          | Done when                                                                                                                                                          | Status | Done in  |
|----|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|----------|
| M1 | Grouped live icons | The battle unit info and the cast preview show one icon per effect with its count and remaining duration, live while exploring, approved on Storybook screenshots. | ✅      | c0b19a8b |
| M2 | Effect info        | Tapping an effect icon shows its effect info, in English and Spanish, approved on Storybook screenshots.                                                           | ✅      | bb2eb376 |

### M1 commits

1. `BattleUnit.Dto.OngoingEffectsDto` gains `effects`, the ongoing effects with their remaining duration, and
   `BattleUnitInfoMapper` groups them into `EffectsView.Effect`s, drawn with their count and remaining duration.
2. The cast preview groups its new effects into the same icons, `CastPreviewView.Recipient.newEffectIds` and
   `casterNewEffectIds` replaced by the grouped effects of `BattleUnitInfo`.
3. `battleUnit` publishes `OngoingEffectsTriggered`, and `BattlefieldPresenter` redraws the battle unit info it shows
   while exploring.
4. **Screen / Effects** gains its stacked and new stack stories, and the screenshots go on a private Artifact page.

### M2 commits

1. The effect names and sentences in `messages.pot` and `es.po`, and `EffectInfoMapper`, which builds an
   `EffectInfoView.EffectInfo` from an effect and its stacks.
2. `EffectInfoView`, shown by `BattleUnitInfoView` in place of the ability buttons when an effect icon is tapped, and
   closed as the Closing decision says.
3. **Screen / Effect info** and the **Screen / Battle unit info › Effect info** story, and the screenshots go on a
   private Artifact page.

## Reference

### battleUnit

- `BattleUnit.Dto.OngoingEffectsDto.effects` lists every ongoing effect not timed immediately, in the order received,
  as an `OngoingEffectDto(effectId, remainingDuration)`, `remainingDuration` being null on defeated. E.g.: two venoms and
  a soak are `[venom-damage (4), venom-damage (2), soak (1)]`. The lists `onPlayerTurnStarted`, `onDefeatedEffects` and
  `whileOngoing` stay, as `ApplyEffect`, `OngoingEffectsTrigger`, `TriggerOnDefeatedEffects` and `WhatShouldCast` read
  them.
- `BattleUnitEvent.OngoingEffectsTriggered(battleUnitId)` is new. `BattleUnit.triggerOnPlayerTurnStartedEffects` adds
  it after the effects it triggered and counted down, so `OngoingEffectsTrigger.trigger` publishes it for each battle
  unit it triggers, in battles and while exploring.

### Effect icons

- `EffectsView.Effect(effectId, stacks, remainingDuration, isNew)` is one icon: `stacks` is how many ongoing effects of
  that id the battle unit has, `remainingDuration` the longest of them, null on defeated, and `isNew` that a cast
  previewed adds a stack.
- `EffectsView.display(effects)` replaces `display(effectIds, newEffectIds)`, and keeps its ten `effectViews` and the
  overflow "+".
- `EffectView.displayEffect(effect)` draws the icon, the remaining duration bottom-right and, from 2 stacks, the ×2
  top-right, both 9 px in white, and blinks when `isNew` as today. `select()` and `unselect()` ring it in
  `HudStyle.SELECTION` or `HudStyle.STROKE`.
- `BattleUnitInfo` gains `battleUnitId` and replaces `effectIds` with `effects: List<EffectsView.Effect>`.
- `BattleUnitInfoMapper.battleUnitInfo(battleUnit, isHumanPlayers, searchEffect, newEffectIds = emptyList())` groups
  `ongoingEffects.effects` by effect id, in the order first received, and adds each new effect id as a stack with its
  effect's own duration (`timing.onPlayerTurnStarted.duration` or `timing.whileOngoing.duration`), marking the icon new.
  E.g.: a venom-strike previewed on a hero with one venom with 2 player turns left gives venom ×2 (5), new.
- `RecipientPreviewView.display(battleUnit, remainingHealth)` draws `battleUnit.effects`, and `CastPreviewView` tells
  the caster changed when one of its effects is new.

### Live refresh

- `BattlefieldPresenter` handles `OngoingEffectsTriggered` while not battling: if `battleUnitInfoView` shows that
  battle unit, it displays it again with fresh data. `RefreshWalkingRange` does not do it, as it returns early while
  exploring when the tile has not changed.
- In battles nothing more is needed: the player turn start deselects and selects the leader again.

### Effect info

- `EffectInfoView.EffectInfo(effectId, stacks, sentence, remainingDurations, isExploring)`, `remainingDurations`
  longest first and empty on defeated.
- `EffectInfoMapper.effectInfo(effect, remainingDurations, isExploring)` picks the sentence from the effect's outcome
  and timing, as `AbilityInfoMapper` does: damage on player turn started, negate actions on player turn started, resist
  while ongoing, and apply effect to adjacent allies on defeated. Any other pair fails, naming the effect.
- `BattleUnitInfo` gains `effectInfos: Map<String, EffectInfoView.EffectInfo>`, built by `BattleUnitInfoMapper` with
  `isExploring` passed by `BattlefieldPresenter` (`!isBattling()`) and `InventoryPresenter` (no battle not finished).
- `EffectInfoView` is a `HudStyle.panel` `AbilityInfoView.WIDTH` (380 px) wide, its height following its text: the icon
  at 2x in a 40 px slot, the title at 14 px, the sentence wrapped as `AbilityInfoView.wrappedWords` does, and the time
  line in `HudStyle.MUTED`, the stacks joined by " · ".
- `BattleUnitInfoView` listens to taps on `effectsView`'s icons. A tap shows the effect info at the ability buttons'
  place in `body`, hides the ability buttons and selects the icon. Tapping it again, or the effect info, hides it and
  shows the ability buttons. `display(battleUnit, …)` keeps it open, refreshed, only when `battleUnit.battleUnitId` is
  the one shown and its effect is still ongoing, and closes it otherwise.

### Catalog

| Key                                         | English                                        | Spanish                                             |
|---------------------------------------------|------------------------------------------------|-----------------------------------------------------|
| `effect.burn.name`                          | burn (exists)                                  | quemado (was "quemadura")                           |
| `effect.soak.name`                          | soak                                           | mojado                                              |
| `effect.stun.name`, `effect.stun-long.name` | stun                                           | aturdido                                            |
| `effect.venom-damage.name`                  | venom                                          | veneno                                              |
| `effect.venom-on-defeat.name`               | venom burst                                    | veneno explosivo                                    |
| `ui.effectInfo.title`                       | {effect} ×{stacks}                             | {effect} ×{stacks}                                  |
| `ui.effectInfo.damageOnPlayerTurnStarted`   | {damage} damage at the start of each turn.     | {damage} de daño al empezar cada turno.             |
| `ui.effectInfo.damageEachSecond`            | {damage} damage each second.                   | {damage} de daño cada segundo.                      |
| `ui.effectInfo.negateActions`               | Loses its actions at the start of each turn.   | Pierde sus acciones al empezar cada turno.          |
| `ui.effectInfo.resist`                      | {share}% less damage from {effect}.            | {share}% menos de daño por {effect}.                |
| `ui.effectInfo.turnsLeft` (plural)          | {turns} turn left / {turns} turns left         | queda {turns} turno / quedan {turns} turnos         |
| `ui.effectInfo.secondsLeft` (plural)        | {seconds} second left / {seconds} seconds left | queda {seconds} segundo / quedan {seconds} segundos |

Venom burst reuses `ui.abilityInfo.effect.onDefeatedDamageAdjacentAllies`. Each new key gets a `UiMessage` and a
translator comment, as the ability info's do.

## Storybook

- `EffectsViewStories`: the `Some` story draws the remaining durations, and `venom-on-defeat` without one. New
  `Stacked` (`screen-effects--stacked`), a venom ×2 next to a soak, and `With new stack`
  (`screen-effects--with-new-stack`), a venom ×2 blinking. Its `stacks` control (`int`, 1–3).
- `EffectInfoViewStories`, **Screen / Effect info** (`screen-effect-info`): `Venom` (`screen-effect-info--venom`),
  `Venom stacked` (`screen-effect-info--venom-stacked`), `Soak` (`screen-effect-info--soak`), `Stun`
  (`screen-effect-info--stun`), `Venom burst` (`screen-effect-info--venom-burst`) and `Exploring`
  (`screen-effect-info--exploring`). Its `effect` control (`text`), `stacks` control (`int`, 1–3) and `exploring`
  control (`boolean`).
- `BattleUnitInfoViewStories`: `Effect info` (`screen-battle-unit-info--effect-info`), the `With effects` battle unit
  with its venom tapped.
- `StorySmokeTest` lists the new stories.

## Tests

- `BattleUnitTest`
  - `ToDto should list the ongoing effects with their remaining duration when the battle unit has some`
  - `ToDto should list an on defeated effect without remaining duration when the battle unit has one`
  - `TriggerOnPlayerTurnStartedEffects should publish ongoing effects triggered when it has ongoing effects`
- `OngoingEffectsTriggerTest`
  - `Trigger should publish ongoing effects triggered for each battle unit when exploring`
- `BattleUnitInfoMapperTest`
  - `BattleUnitInfo should group the ongoing effects of an effect id into one icon when it is stacked`
  - `BattleUnitInfo should keep the longest remaining duration when it is stacked`
  - `BattleUnitInfo should mark the icon new with one more stack when a cast adds a stack`
  - `BattleUnitInfo should add a new icon with the effect's duration when a cast adds a new effect`
- `EffectViewTest`
  - `DisplayEffect should display the remaining duration when the effect has one`
  - `DisplayEffect should not display a remaining duration when the effect is on defeated`
  - `DisplayEffect should display the count when the effect is stacked`
  - `Select should ring the icon when it is selected`
- `EffectsViewTest`
  - `Display should display one icon per effect when effects are displayed` (now with `EffectsView.Effect`s)
  - `Display should blink the effects marked new when new effects are displayed` (replaces `Display should display the
    new effects blinking after the current ones when new effects are displayed`)
- `RecipientPreviewViewTest`, `CastPreviewViewTest`
  - `Display should blink the stacked icon when the cast adds a stack`
- `BattlefieldPresenterTest`
  - `Ongoing effects triggered should display the battle unit info again when it shows that battle unit while exploring`
  - `Ongoing effects triggered should not display the battle unit info when it shows another battle unit`
- `EffectInfoMapperTest`
  - `EffectInfo should say the damage at the start of each turn when the effect deals damage on player turn started`
  - `EffectInfo should say the damage each second when exploring`
  - `EffectInfo should say the share and the resisted effect when the effect resists`
  - `EffectInfo should keep the battle sentence when the effect negates actions while exploring`
  - `EffectInfo should list the remaining durations longest first when it is stacked`
  - `EffectInfo should fail when the effect has an outcome and timing it can not describe`
- `EffectInfoViewTest`
  - `Display should display the name with its count when it is stacked`
  - `Display should display one remaining duration per stack when it is stacked`
  - `Display should not display a time line when the effect is on defeated`
- `BattleUnitInfoViewTest`
  - `TapEffect should display the effect info in place of the abilities when an effect icon is tapped`
  - `TapEffect should display the abilities again when the same effect icon is tapped again`
  - `TapEffect should switch the effect info when another effect icon is tapped`
  - `Display should keep the effect info open when the same battle unit is displayed again`
  - `Display should close the effect info when another battle unit is displayed`
  - `Display should close the effect info when its effect is no longer ongoing`
- `EffectNamesTest`
  - `should have a name when is an effect staying on the battle units`, next to `EffectImagesTest`

## Known limitations

- The cast preview's icons can not be tapped: the selected ability's info already says what its effects do.
- A battle unit with more than ten different effects still hides the rest behind the "+", unreadable. Today only six
  lasting effects exist.
- The effect info's sentences cover the outcome and timing pairs the game uses. A community world adding another pair
  fails in `EffectInfoMapper` until a sentence is written for it.

## Changes while building

- M1's commits 1, 2 and 4 are one commit (bbd677b7): replacing `BattleUnitInfo.effectIds` with `effects` changes the cast
  preview and every story that builds a `BattleUnitInfo`, so they could not compile apart.
- The live refresh redraws only the health, the mana and the effect icons, with a new `BattleUnitInfoView.refresh`, not
  `display` again: `display` closes the ability info, which the player can open while exploring, and would close it each
  second. `BattlefieldPresenter` remembers the battle unit its battle unit info shows, `displayedBattleUnitId`, set when
  it displays it and cleared with the selection.
- The use case test of the event is `Trigger should publish ongoing effects triggered for each battle unit when
  exploring` in `TriggerOnPlayerTurnStartedEffectsTest`, which already tests the trigger while exploring: there is no
  `OngoingEffectsTriggerTest`.
- **Screen / Effects › Stacked** and **With new stack** stack the first sample effect, burn, not venom: the stories take
  their effects from the effect images, in order. **Screen / Effect** also gains `Stacked` (`screen-effect--stacked`)
  and `No duration` (`screen-effect--no-duration`), with `remainingDuration` (`int`, 0–9) and `stacks` (`int`, 1–3)
  controls.
- M2's commits 1 and 2 are split otherwise: c78edd1a holds the catalog, `EffectInfoMapper` and `EffectInfoView`, as the
  mapper builds the view's `EffectInfo`, and f5d4d9ee the taps in `BattleUnitInfoView` with the stories. bb2eb376 gives
  the sample effect info of a story the remaining duration its icon shows.
- The stack count reads x2, not ×2, on the icon and in `ui.effectInfo.title` ("{effect} x{stacks}"): the game's bitmap
  font is built from `CharacterSet.LATIN_ALL`, which has no ×, as `LocalizationResourcesTest` checks.
- `EffectInfoView.EffectInfo.sentence` is a sealed `Sentence` (`DamageOnPlayerTurnStarted`, `NegateActions`, `Resist`,
  `OnDefeatedDamageAdjacentAllies`) that `EffectInfoView` words, as `AbilityInfoView` does, and
  `EffectInfoMapper.effectInfo` also takes `searchEffect`, for the venom venom burst spreads. The title is 16 px, as the
  ability info's name.
- `BattleUnitInfo.battleUnitId` and `effectInfos` default to "" and none, so the stories and tests that do not need them
  stay as they are. `BattleUnitInfoMapper` leaves out of `effectInfos` an effect it can not find.
- The tapped effect's story is **Screen / HUD › Effect info** (`screen-hud--effect-info`), not **Screen / Battle unit
  info › Effect info**: a `tappedEffect` control would make the battle unit info's controls cover its actions, which
  `StorySmokeTest` checks. Tapping an icon in **Screen / Battle unit info › With effects** opens it too.
- The effect info also closes when `BattleUnitInfoView.hide()` or `displayAbilityInfo` is called. Its view tests are
  `TapEffect…`, `TapEffectInfo…`, `Display…`, `Refresh…` and `Hide…` in `BattleUnitInfoViewTest`, and `EffectNamesTest`
  also checks that `EffectInfoMapper` describes every lasting effect of the game resources.
