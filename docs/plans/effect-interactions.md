# Plan: effects that interact: soak, burn, stun and venom

> **Status:** built on `main` up to M5, the screenshots of M5 still to be approved.
>
> **Summary:** Today an effect only does its own outcome, and no effect knows about another. E.g.: venom-strike's
> `venom-damage` deals 3 damage on each of the target's next 5 player turns, whatever else the target has, and water
> heals 5 and lava deals 5 at each round start, and that is all a terrain does. Nothing in the game rewards casting one
> ability to set up another. Instead, four effects interact. Soak resists half the damage of burn, Thunder stuns a soaked
> enemy for 2 player turns instead of 1, and Fireball spreads the venom of an enemy with an ongoing venom to its adjacent allies. Water
> soaks and lava burns. Every effect keeps one outcome: the interactions are extra effects the abilities carry, built
> from two new outcomes, apply if ongoing and resist, as [ADR 0023](../adr/0023-effect-interactions-composed-from-single-outcome-effects.md)
> records. Three bat recolours carry the abilities: the water bat (Splash), the fire bat (Fireball) and the storm bat
> (Thunder). The CPU battle brain learns to weigh them, and a second of exploring counts as a player turn start for
> ongoing effects, so they no longer freeze outside a battle. How the ability info and the cast preview show a
> conditional effect is picked from a prototype first.

## Decisions

| Topic            | Decision                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope            | Three new outcomes and one new timing in `effect`, a list of effects per terrain, ongoing effects counted down while exploring, three abilities, three units, the CPU battle brain's utility for them, and the display of the new effects. The three units stay out of the campaign: they are played in a community world later.                                                                                                                                                                                                                                                                             |
| Single outcome   | Every effect keeps one outcome and one timing, so effects stay building blocks and an ability composes them. Chosen over conditions (a "poisoned" state several effects give) and reactions on the ongoing effect, as [ADR 0023](../adr/0023-effect-interactions-composed-from-single-outcome-effects.md) records. The rules name effect ids: a second venom must be added to every rule that names `venom-damage`.                                                                                                                                                                                          |
| Negate actions   | A new outcome, `NEGATE_ACTIONS`, that sets the recipient's remaining steps and casts to 0. Only timed on player turn started, so it triggers after `ResetBattleUnitActionsAndReduceCooldowns` in `OnPlayerTurnStarted`. E.g.: `stun`, with a duration of 1, skips the recipient's next player turn. Two stuns stack as any effect does, so both trigger at the same player turn start, and a second one adds nothing unless it lasts longer.                                                                                                                                                                 |
| Apply if ongoing | A new outcome, `APPLY_IF_ONGOING`, with an `effectId` and an `ongoingEffectId`: it applies `effectId` to its recipient, from the same source, only if the recipient has `ongoingEffectId` ongoing. Only timed immediately. It sees the effects applied earlier in the same cast, as a cast's effects apply in order. Chosen over an outcome that changes an ongoing effect's duration or amount, which would need the amount stored on every ongoing effect.                                                                                                                                                 |
| Resist           | A new outcome, `RESIST`, with an `effectId` and a `share` from 1 to 100: the damage its recipient takes from `effectId` is lessened by `share` percent. The shares of every ongoing resist for the same effect add up, capped at 100%, and the damage taken rounds up. E.g.: one soak turns burn's 4 into 2, and two soaks make the recipient take none. A 5 damage burn on one soak deals 3. Chosen over "apply unless ongoing" and a damage halved by a condition, as every burn source would carry the rule.                                                                                              |
| While ongoing    | A new timing, `WHILE_ONGOING`, with a duration: the outcome lasts until the duration runs out, counted down at each of the recipient's player turn starts, like an effect timed on player turn started that triggers nothing. Only resist takes it.                                                                                                                                                                                                                                                                                                                                                          |
| Adjacent allies  | `APPLY_EFFECT_TO_ADJACENT_ALLIES` also works when it is timed immediately: it applies its effect to the recipient's living allies on adjacent tiles, not to the recipient. Today it only works on defeated (skull's `venom-on-defeat`).                                                                                                                                                                                                                                                                                                                                                                      |
| Unused stubs     | The `BEFORE_APPLYING_EFFECT` timing and the `NEGATE_INCREASE_HEALTH` outcome are deleted. No resource uses them, and `ApplyEffect` throws on the timing.                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Terrain effects  | A terrain holds a list of effect ids, `effectIds`, applied in order: water is `["soak"]` and lava is `["lava-damage", "burn"]`. `water-heal` is deleted, so no terrain heals any more. On lava, burns pile up as any effect stacks, at most two at once with a duration of 2.                                                                                                                                                                                                                                                                                                                                |
| Exploring second | Each second of exploring counts as a player turn start for the ongoing effects of every battle unit on the battlefield, moving or not. Burn and venom trigger, and every duration counts down. Negate actions triggers nothing outside a battle. Chosen over applying only a terrain's immediate effects while exploring, not applying an effect already ongoing, and clearing every ongoing effect at the end of a battle. Today a venom still ongoing when a battle ends freezes until the next one, and a battle unit idling on lava would pile up a burn every second.                                   |
| Effects          | `soak`: resist 50% of `burn`, while ongoing, 2 player turns. `burn`: 4 damage, on player turn started, 2 player turns. `stun`: negate actions, on player turn started, 1. `stun-long`: the same, 2. `stun-long-if-soaked`: apply if ongoing `stun-long` if `soak`. `venom-spread`: apply `venom-damage` to adjacent allies, immediately. `venom-spread-if-venom`: apply if ongoing `venom-spread` if `venom-damage`.                                                                                                                                                                                         |
| Abilities        | All three are aimed at an adjacent enemy and cost 10 mana. Splash: `soak`, cooldown 1. Fireball: `burn` and `venom-spread-if-venom`, cooldown 1. Thunder: `stun` and `stun-long-if-soaked`, cooldown 2.                                                                                                                                                                                                                                                                                                                                                                                                      |
| Units            | `water-bat`, `fire-bat` and `storm-bat`: the bat's 10 health and 5 steps, 20 mana, and only their own ability (Splash, Fireball, Thunder). Their sprites and portraits are the bat's, recoloured blue, red and yellow in the Famicube palette ([ADR 0005](../adr/0005-famicube-palette-and-outline.md)).                                                                                                                                                                                                                                                                                                     |
| CPU battle brain | An apply if ongoing counts toward a cast's utility only when the targeted battle unit already has the effect it looks for. A stun is worth the damage it prevents: what the stunned enemy's best ability would deal, for each player turn negated, discounted by `PLAYER_TURN_DISCOUNT` (0.9) as damage over time is. A soak is worth, as a setup, the extra utility a follow-up the caster knows (e.g. Thunder) would gain on a soaked target, discounted once, as it comes a player turn later.                                                                                                            |
| Display          | Picked from a prototype in M1, on the local branch `prototype/effect-interactions-display`, whose screenshots went on a private Artifact page. The ability info shows a conditional effect as a condition sentence: "If soaked: stuns for 2 turns.", chosen over a condition icon (the soak icon, an arrow, the stun icon and 2), as it reads like the other effects. The cast preview shows a conditional effect only on a target where it holds, chosen over showing it greyed where it won't, as it shows exactly what the cast does. Soak, burn, stun and stun-long get an icon each in the effects row. |
| Save             | `SAVE_VERSION` goes from 11 to 12, as an ongoing effect's snapshot gains the `WHILE_ONGOING` timing ([ADR 0011](../adr/0011-no-save-migrations.md)).                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| Architecture     | The outcomes and the timing in `effect.domain`. Their triggers in `battleUnit` (`BattleUnit`, `ApplyEffect`, `TriggerOnPlayerTurnStartedEffects`). The terrain list in `terrain` and `battlefield`. The exploring second in `party.usecases.commands.ApplyStandingTerrainEffects`, which publishes a `battleUnit` request event, since `party` depends on `battleUnit` and not the other way round ([ADR 0002](../adr/0002-acyclic-shrink-only-subdomain-dependencies.md)). The utility in `cpuBattleBrain.usecases.queries.WhatShouldCast`. The display in `screen.adapters.presentation`.                  |
| Storybook        | **Screen / Ability info** gains Splash, Fireball and Thunder stories, **Screen / Cast preview** a conditional story in the chosen variant, and **Screen / Effects** shows the new icons.                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Tests            | Domain and use case tests for each outcome, the timing and the terrain list. Acceptance tests for each interaction and for the exploring second. `WhatShouldCastTest` for the CPU. `SaveFixtureTest` with the version 12 golden save.                                                                                                                                                                                                                                                                                                                                                                        |
| Out of scope     | The three units in the campaign, a stronger venom or burn, conditions and reactions, lightning or other elements, a battlefield animation for a stunned battle unit, and tuning the numbers before the community world playtest.                                                                                                                                                                                                                                                                                                                                                                             |
| Delivery         | 6 milestones, each green on CI. See [Milestones](#milestones).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |

For example, the storm bat casts Thunder on a hero the water bat soaked last player turn:

| Moment                         | Hero's ongoing effects            | Hero's next player turns     |
|--------------------------------|-----------------------------------|------------------------------|
| Splash                         | soak (2)                          |                              |
| Thunder: `stun`                | soak (2), stun (1)                |                              |
| Thunder: `stun-long-if-soaked` | soak (2), stun (1), stun-long (2) |                              |
| Hero's 1st player turn starts  | soak (1), stun-long (1)           | no steps, no casts           |
| Hero's 2nd player turn starts  | none                              | no steps, no casts           |
| Hero's 3rd player turn starts  | none                              | 5 steps and 1 cast, as usual |

## Milestones

Each milestone is a few commits straight on `main`, green on CI, except M1, which stays on its local branch.

| #  | Milestone          | Done when                                                                                                                                                                                                     | Status | Done in  |
|----|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|----------|
| M1 | Display prototype  | The four variants are Storybook stories on `prototype/effect-interactions-display`, their screenshots are on an Artifact page, and the user has picked one for the ability info and one for the cast preview. | ✅      | 46248f53 |
| M2 | Effect blocks      | Negate actions, apply if ongoing, resist and while ongoing work through `ApplyEffect`, adjacent allies works immediately, the stubs are gone, and saves are at version 12.                                    | ✅      | 1dfd134c |
| M3 | Exploring second   | Every second of exploring triggers and counts down the ongoing effects of every battle unit on the battlefield: a venom left from a battle runs out while exploring.                                          | ✅      | 30318a5c |
| M4 | Terrain lists      | Water soaks and lava deals its damage and burns, in battles and while exploring.                                                                                                                              | ✅      | 36bcec74 |
| M5 | Bats and abilities | Splash, Fireball and Thunder, the water, fire and storm bats with their art, the new effect icons and the chosen display, in English and Spanish, can be placed in the world builder.                         | ✅      | af8014b4 |
| M6 | CPU battle brain   | A CPU storm bat stuns a soaked enemy over a dry one, a CPU water bat soaks an enemy when it knows Thunder, and a CPU fire bat targets an enemy with an ongoing venom.                                         | ⬜      |          |

### M1 commits

1. On `prototype/effect-interactions-display`, `AbilityInfoViewStories` gains **Screen / Ability info › Thunder, condition sentence** and **› Thunder, condition icon**, built from hand-written `AbilityInfo` data.
2. `CastPreviewViewStories` gains **Screen / Cast preview › Conditional, when it holds** and **› Conditional, greyed**, on two enemies, one soaked and one dry.
3. The screenshots of the four stories go on a private Artifact page, and the user's pick is written in this plan's Display decision.

### M2 commits

1. `Effect` gains `NEGATE_ACTIONS`, `APPLY_IF_ONGOING`, `RESIST` and `WHILE_ONGOING`, with their checks in `Effect.create`, and loses `BEFORE_APPLYING_EFFECT` and `NEGATE_INCREASE_HEALTH`. `ResourcesEffectLoader` reads them.
2. `BattleUnit` negates actions, receives effects timed while ongoing and counts them down at its player turn start, and lessens damage by its ongoing resists. `SAVE_VERSION` goes to 12, with its golden save.
3. `ApplyEffect` handles apply if ongoing, and adjacent allies timed immediately, through an `AdjacentAllies` service shared with `TriggerOnDefeatedEffects`.
4. `docs/testing/how-to-add-an-effect-outcome.md` lists the timings as they are now.

### M3 commits

1. `BattleUnitEvent.RequestTriggerOngoingEffects` and its handler `OnRequestTriggerOngoingEffects`, which triggers the ongoing effects of the battle units it lists, outside a battle.
2. `ApplyStandingTerrainEffects` publishes it every second for every battle unit on the battlefield, after the terrain effects. `GLOSSARY.md` mentions the exploring second in On player turn started and Duration.

### M4 commits

1. `Terrain` holds `effectIds`, `ResourcesTerrainLoader` reads `effectIds`, and `Battlefield.terrainEffectsOnOccupants` returns one entry per effect.
2. `water.toml` becomes `["soak"]` and `lava.toml` `["lava-damage", "burn"]`, with `soak.toml` and `burn.toml`. `water-heal.toml` is deleted. `docs/contributing/world-file.md`'s terrain table follows.

### M5 commits

1. The `stun`, `stun-long`, `stun-long-if-soaked`, `venom-spread` and `venom-spread-if-venom` effects, and the `splash`, `fireball` and `thunder` abilities.
2. Icons for the abilities, under `art/ability/`, and for soak, burn, stun and stun-long, under `art/effect/`, drawn with the `pixel` tool.
3. The `water-bat`, `fire-bat` and `storm-bat` units, their sprites and portraits recoloured from `art/unit/bat.aseprite` and `bat_portrait.aseprite`.
4. `AbilityInfoMapper`, `AbilityInfoView` and the cast preview show the new effects in the variants picked in M1, `BattleUnitInfoMapper` lists the effects timed while ongoing, and the stories follow.
5. The names and sentences in `messages.pot` and `es.po`.

### M6 commits

1. `WhatShouldCast` counts an apply if ongoing only when it holds on the target.
2. `WhatShouldCast` values a stun by the damage it prevents.
3. `WhatShouldCast` values a soak as a setup for a follow-up the caster knows.

## Reference

### effect

- `Effect.Dto.EffectOutcomeDto` gains `negateActions()`, `applyIfOngoing(effectId, ongoingEffectId)` and `resist(effectId, share)`, and `TimingDto` gains `WHILE_ONGOING` with a `whileOngoing` duration, from 0 to 99.
- `Effect.create` throws `InvalidEffectTiming` when negate actions isn't timed on player turn started, apply if ongoing isn't timed immediately, resist isn't timed while ongoing, a timing while ongoing has another outcome, or adjacent allies is timed other than immediately or on defeated.
- `Resist.share` throws `NegativeAmount` below 1 and `AmountAboveLimit` above 100.
- E.g.: `soak.toml`:

  ```toml
  [outcome]
  type = "RESIST"
  effectId = "burn"
  share = 50

  [timing]
  type = "WHILE_ONGOING"
  duration = 2
  ```

### battleUnit

- `BattleUnit.receiveWhileOngoingEffect(effectId, remainingDuration)` adds an ongoing effect timed while ongoing. `OngoingEffectsDto` gains `whileOngoing`, and `OngoingEffectSnapshotDto.TimingDto` gains `WHILE_ONGOING`.
- `BattleUnit.triggerOnPlayerTurnStartedEffects(effects, inBattle, ...)` also counts down the effects timed while ongoing, and triggers negate actions only when `inBattle`. E.g.: a stun received on the last player turn of a battle runs out on the first second of exploring and takes no action.
- `BattleUnit.applyDecreaseHealthEffect` lessens the damage by the share of the ongoing resists for the effect, which `ApplyEffect` and `TriggerOnPlayerTurnStartedEffects` look up and pass in. E.g.: `burn` (4) on a battle unit with two ongoing soaks deals 0.
- `BattleUnit.hasOngoingEffect(effectId)` tells whether an effect is ongoing, for apply if ongoing.
- `ApplyEffect` publishes `RequestApplyEffect` for an apply if ongoing whose effect is ongoing on the recipient, with the same source and recipient, and one per living adjacent ally for adjacent allies timed immediately. Both are handled after the cast's other effects already published, in publication order ([ADR 0014](../adr/0014-event-bus-handles-events-in-publication-order.md)).
- `AdjacentAllies`, a new service in `battleUnit.usecases.services`, holds what `TriggerOnDefeatedEffects.searchAdjacentAllies` does today.
- `BattleUnitEvent.RequestTriggerOngoingEffects(battleUnitIds)`, handled by `OnRequestTriggerOngoingEffects`, calls `TriggerOnPlayerTurnStartedEffects` outside a battle.

### terrain and battlefield

- `Terrain.Dto.effectIds: List<String>` replaces `effectId`, and `ResourcesTerrainLoader` reads `effectIds = [...]`.
- `ApplyTerrainEffects` maps every terrain to its list, and `Battlefield.terrainEffectsOnOccupants` returns one `TerrainEffectOnOccupantDto` per effect, in the terrain's order.

### party

- `ApplyStandingTerrainEffects` publishes `RequestTerrainEffects` for the battle units standing still, then `RequestTriggerOngoingEffects` for every battle unit on the battlefield. E.g.: a hero standing still on lava for 3 seconds takes 5 from lava each second, plus 4 from each burn ongoing: 9, 13 and 13.

### cpuBattleBrain

- `WhatShouldCast.expectedAmount` follows an apply if ongoing to the effect it applies when the targeted battle unit has its `ongoingEffectId` ongoing, and to each of the target's adjacent allies for adjacent allies.
- A cast whose effects negate actions is a `CastKind.DAMAGE` cast, worth the damage of the stunned enemy's best known ability, discounted by `PLAYER_TURN_DISCOUNT` for each player turn negated.
- A cast whose only effect is a resist on an enemy is worth, for each ability the caster knows with an apply if ongoing for that effect, the extra utility it would gain on that target, times `PLAYER_TURN_DISCOUNT`.

### screen

- `AbilityInfoMapper` gives a sentence or icon for negate actions, apply if ongoing, resist and adjacent allies timed immediately, in the variant picked in M1, instead of failing with "which has no sentence in AbilityInfoMapper".
- `BattleUnitInfoMapper.battleUnitInfo` lists `ongoingEffects.whileOngoing` in `effectIds`, so a soak shows its icon.
- `PreviewCastOngoingEffects.ONGOING_TIMINGS` gains `WHILE_ONGOING`, and the cast preview shows an apply if ongoing in the variant picked in M1.

### Resources

| File                                     | Holds                                                                 |
|------------------------------------------|-----------------------------------------------------------------------|
| `effect/soak.toml`, `soak.png`           | Resist 50% of `burn`, while ongoing, 2.                               |
| `effect/burn.toml`, `burn.png`           | 4 damage, on player turn started, 2.                                  |
| `effect/stun.toml`, `stun.png`           | Negate actions, on player turn started, 1.                            |
| `effect/stun-long.toml`, `stun-long.png` | Negate actions, on player turn started, 2. Its icon is stun's.        |
| `effect/stun-long-if-soaked.toml`        | Apply if ongoing `stun-long` if `soak`, immediately.                  |
| `effect/venom-spread.toml`               | Apply `venom-damage` to adjacent allies, immediately.                 |
| `effect/venom-spread-if-venom.toml`      | Apply if ongoing `venom-spread` if `venom-damage`, immediately.       |
| `ability/splash.toml`, `splash.png`      | Adjacent enemy, 10 mana, cooldown 1: `soak`.                          |
| `ability/fireball.toml`, `fireball.png`  | Adjacent enemy, 10 mana, cooldown 1: `burn`, `venom-spread-if-venom`. |
| `ability/thunder.toml`, `thunder.png`    | Adjacent enemy, 10 mana, cooldown 2: `stun`, `stun-long-if-soaked`.   |
| `unit/water-bat.toml`, `.png`, portrait  | 10 health, 20 mana, 5 steps, `splash`. Blue.                          |
| `unit/fire-bat.toml`, `.png`, portrait   | 10 health, 20 mana, 5 steps, `fireball`. Red.                         |
| `unit/storm-bat.toml`, `.png`, portrait  | 10 health, 20 mana, 5 steps, `thunder`. Yellow.                       |
| `terrain/water.toml`                     | `effectIds = ["soak"]`.                                               |
| `terrain/lava.toml`                      | `effectIds = ["lava-damage", "burn"]`.                                |

### Storybook

- `AbilityInfoViewStories`: the `Splash` (`screen-ability-info--splash`), `Fireball` (`screen-ability-info--fireball`) and `Thunder` (`screen-ability-info--thunder`) stories.
- `CastPreviewViewStories`: the `Conditional` story (`screen-cast-preview--conditional`), Thunder on a soaked and a dry enemy.
- `EffectsViewStories`: the `Some` story shows soak, burn and stun next to venom.
- `StorySmokeTest` lists the new stories.

### Docs

- `GLOSSARY.md`: On player turn started triggers "each time its recipient's player turn starts, or each second while exploring", and Duration counts "player turns, or seconds while exploring".
- `docs/testing/how-to-add-an-effect-outcome.md`: the timings are immediately, on player turn started, on defeated, while equipped and while ongoing.
- `docs/contributing/world-file.md`: the terrain table's Effect column lists `soak` for water and `lava-damage`, `burn` for lava.

### Tests

- `EffectTest`
  - `Effect should be created when the outcome negates actions on player turn started`
  - `Effect should fail to be created when negate actions is timed immediately`
  - `Effect should fail to be created when apply if ongoing is not timed immediately`
  - `Effect should fail to be created when resist is not timed while ongoing`
  - `Effect should fail to be created when the resist share is above 100`
- `ResourcesEffectLoaderTest`
  - `Loader should read a resist timed while ongoing when the effect file holds one`
  - `Loader should read an apply if ongoing when the effect file holds one`
- `BattleUnitTest`
  - `Battle unit should have no steps and no casts left when negate actions triggers at its player turn start`
  - `Battle unit should keep its actions when negate actions triggers outside a battle`
  - `Battle unit should take half the burn damage when it has an ongoing soak`
  - `Battle unit should take no burn damage when it has two ongoing soaks`
  - `Battle unit should take 3 when a 5 damage burn hits it with an ongoing soak`
  - `Battle unit should stop resisting when the soak's duration runs out`
- `ApplyEffectTest`
  - `Apply if ongoing should apply its effect when the recipient has the ongoing effect`
  - `Apply if ongoing should apply nothing when the recipient lacks the ongoing effect`
  - `Adjacent allies should apply the effect to the recipient's adjacent allies when it is timed immediately`
- `TriggerOnPlayerTurnStartedEffectsTest`
  - `Trigger should count down the effects timed while ongoing when the player turn starts`
- `ApplyStandingTerrainEffectsTest`
  - `Standing terrain effects should request the ongoing effects of every battle unit each second while exploring`
- `TerrainTest`, `ResourcesTerrainLoaderTest`
  - `Terrain should hold every effect id when it has several`
  - `Loader should read the effect ids of lava in order`
- `BattlefieldTest`
  - `Terrain effects on occupants should list one entry per effect of the terrain`
- `WhatShouldCastTest`
  - `should stun the soaked enemy when a dry enemy is also adjacent`
  - `should prefer the enemy with an ongoing venom when the fireball spreads it`
  - `should soak the enemy when the caster knows thunder`
  - `should not soak the enemy when the caster knows no follow up`
  - `should stun the enemy whose best ability deals the most damage`
- `EffectInteractionsAcceptanceTest` (new)
  - `Thunder should stun the soaked enemy for 2 player turns when it is cast after splash`
  - `Thunder should stun the dry enemy for 1 player turn`
  - `Fireball should spread the venom to the adjacent allies when the enemy has an ongoing venom`
  - `Lava should burn the battle unit standing on it for half when it is soaked`
- `TerrainEffectAcceptanceTest`
  - `should soak the occupants of water and burn the occupants of lava when a new round starts` (replaces `should apply the terrain effect to the occupants of the terrain tiles when a new round starts`)
- `ExploringTerrainEffectAcceptanceTest`
  - `should burn the battle units standing on lava every second while exploring`
  - `should finish the venom left from a battle when its duration runs out while exploring`
- `SaveFixtureTest`: the version 12 golden save, with an ongoing soak.

## Known limitations

- The numbers are a first guess until the community world playtest. Standing on lava while exploring deals about 13 per
  second instead of 5, and two soaks make a battle unit on water immune to burn.
- Water no longer heals, in the campaign too.
- A second venom or burn added later must be named in every apply if ongoing and resist that names the first.
- Durations mean player turns in a battle and seconds while exploring.

## Changes while building

- **M2**: the first three commits are one, as `Effect`, `BattleUnit` and `ApplyEffect` only compile together once the
  stubs are deleted. `BattleUnit.triggerImmediateEffect` takes the definitions of the recipient's ongoing effects timed
  while ongoing, `ongoingEffectDefinitions`, so it can lessen the damage by their resists. `ApplyEffect` handles an
  immediate apply if ongoing or adjacent allies without the battle unit receiving it: it only publishes the further
  `RequestApplyEffect`s.
- **M3**: `TriggerOnPlayerTurnStartedEffects` keeps one public `invoke`, as `CommandUseCaseArchitectureTest` requires, so
  `RequestTriggerOngoingEffects` is handled by a new use case, `TriggerOngoingEffectsWhileExploring`, and both share an
  `OngoingEffectsTrigger` service. As the event bus is first in, first out, the trigger runs before the effects the
  terrain applies that second: a burn received on lava first triggers the next second, as at a player turn start. A
  battle unit standing still on lava takes 5, 9 and 13 over its first 3 seconds, not 9, 13 and 13.
- **M4**: the soak and burn icons are drawn here rather than in M5, as a battle unit burnt by lava shows the burn icon
  and `GameSceneResourcesLoader.bitmap` fails on a missing one. `SearchTerrainHealthEffect` sums the health every effect
  of a terrain changes. `TerrainEffectAcceptanceTest` keeps its stub terrains and gives sand two effects, a damage and
  a burn; the real water and lava are covered by `ExploringTerrainEffectAcceptanceTest` and, in a battle, by
  `EffectInteractionsAcceptanceTest` in M5.
- **M5**: two commits instead of five, as the localization and ability info tests only pass with the effects, abilities,
  sentences and words together; the first blinks a battle unit alongside the timeline when one of its ongoing effects
  triggers while exploring, which M3 made happen every second. The sentences name each effect by a word in the
  catalogs, `effect.<id>.verb`, `.name` and `.condition` (e.g.: "Burns: 4 damage each turn for 2 turns.", "If
  soaked: stuns for 2 turns."), which `LocalizationResourcesTest` allows for every effect and `AbilityInfoResourcesTest`
  requires for the ones the game's abilities show; venom's sentence now reads "Poisons" from `effect.venom-damage.verb`.
  The **Screen / Cast preview › Conditional** story shows Thunder on one soaked enemy, as a cast preview has one row
  per target, and keys the soaked enemy on the Thunder ability, as one more control would cover the actions. The cast
  preview shows no row for the adjacent allies Fireball spreads the venom to. `EffectInteractionsAcceptanceTest` plays
  them on CPU battle units with no ability, so a stunned one is the one with no cast left after the CPU's turn.
