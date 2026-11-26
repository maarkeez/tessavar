# Plan: essences that teach the punches

> **Status:** to do.
>
> **Summary:** Burn, stun and soak, the effects that interact since the
> [effect interactions plan](effect-interactions.md), are only cast by CPU battle units: the fire bat's Fireball, the
> storm bat's Thunder and the water bat's Splash. No party member can learn them. E.g.: the hero knows Sword and Heal,
> and the only essences the Tessavar alchemist sells teach Teleport. Instead, three new consumables, Fire Punch Essence,
> Thunder Punch Essence and Water Punch Essence, teach a party member Fire Punch, Thunder Punch and Water Punch. Each
> punch deals 5 damage and applies the same effects as its bat's ability, which reference the existing effects without
> changing them ([ADR 0023](../adr/0023-effect-interactions-composed-from-single-outcome-effects.md)). The Tessavar
> alchemist sells them at 20 gold each. Their six 16x16 icons are drawn with the pixel tool
> ([ADR 0006](../adr/0006-aseprite-sources-drawn-with-the-pixel-tool.md)).

## Decisions

| Topic         | Decision                                                                                                                                                                                                                                                                                                                                                                                             |
|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope         | Three abilities (`fire-punch`, `thunder-punch`, `water-punch`), three consumables (`fire-punch-essence`, `thunder-punch-essence`, `water-punch-essence`), three learn ability effects, their names in English and Spanish, the Tessavar alchemist's stock and the alchemist's own line.                                                                                                              |
| Effects       | Each punch applies `light-physical-damage` (5 damage) and then its bat ability's effects, unchanged: Fire Punch `burn` and `venom-spread-if-venom`, Thunder Punch `stun` and `stun-long-if-soaked`, Water Punch `soak`. Chosen over the bat effects alone, as a punch hits, and over `low-physical-damage` (10), which would make Fire Punch outdo Sword on top of its burn. No effect file changes. |
| Cost          | 10 mana each, with the bats' cooldowns: 1 for Fire Punch, 2 for Thunder Punch, 1 for Water Punch. All three target an adjacent enemy (`ADJACENT_ENEMY`).                                                                                                                                                                                                                                             |
| Names         | The items are named after the ability they teach, like Teleport Essence and Bee Essence. Chosen over Fire Essence, Thunder Essence and Water Essence, which do not say what is learned.                                                                                                                                                                                                              |
| Learners      | Any party member with a free ability slot (6 per battle unit, `Unit.MAXIMUM_ABILITIES`), as for every ability today. No new rule.                                                                                                                                                                                                                                                                    |
| Where to get  | Only the Tessavar alchemist sells them, at 20 gold each, after Teleport Essence. The Drowned Mill's alchemist keeps its stock, and no loot table holds them. Chosen over each elemental bat dropping its own essence, to keep them a purchase.                                                                                                                                                       |
| World builder | World creators can stock them in any npc, as any other lootable item: the stock list grows from 9 to 12 items, 6 rows, which still fits in `WorldBuilderStockView`.                                                                                                                                                                                                                                  |
| Own line      | The alchemist's own line becomes "Potions, elixirs, and essences that bend space or wake a fist. What will it be?". In Tessavar the alchemist gives the side quest Storm in a Bottle, so it says that quest's lines instead, and the own line only shows in The Drowned Mill and in community worlds. Kept anyway, as the alchemist sells essences in every world.                                   |
| Art           | Six 16x16 icons with the pixel tool. The ability icons are a fist with a flame, a lightning bolt and a water drop. The essences keep the Teleport Essence's shape, recoloured, each with a small element symbol. Chosen over a new shape per essence, so every essence reads as one family.                                                                                                          |
| Architecture  | Resources only: `resources/ability`, `resources/effect`, `resources/lootableItem`, `resources/world/tessavar.toml` and the catalogs. No domain, use case or adapter code changes, besides the stories.                                                                                                                                                                                               |
| Storybook     | Three new stories, **Screen / Ability info › Fire Punch**, **› Thunder Punch** and **› Water Punch**. No new shop story: **Screen / Shop › Buy** with its `items` control at 12 shows the three essences.                                                                                                                                                                                            |
| Tests         | One new `PunchEssenceAcceptanceTest` for all three punches. The existing resource tests check the references, the names and the effect words. No new tests for the art.                                                                                                                                                                                                                              |
| Review        | Pushed to `main` first. Then the six icons and the stories' screenshots go on one private Artifact page, reviewed while CI/CD runs.                                                                                                                                                                                                                                                                  |
| Out of scope  | Changes to the bats, their abilities or any effect; loot drops for the essences; The Drowned Mill's stock; Storm in a Bottle's lines; and a rule limiting which unit can learn a punch.                                                                                                                                                                                                              |

For example, the three punches next to the bat abilities they mirror:

| Ability       | Mana | Cooldown | Effects                                                  | Mirrors  |
|---------------|------|----------|----------------------------------------------------------|----------|
| Fire Punch    | 10   | 1        | `light-physical-damage`, `burn`, `venom-spread-if-venom` | Fireball |
| Thunder Punch | 10   | 2        | `light-physical-damage`, `stun`, `stun-long-if-soaked`   | Thunder  |
| Water Punch   | 10   | 1        | `light-physical-damage`, `soak`                          | Splash   |

## Art

- Each icon follows the [pixel art skill](../../.claude/skills/pixel-art/SKILL.md): a layered source under `art/`, drawn
  with `tools/pixel-art/pixel`, saved with its PNG under `resources/`, in the Famicube palette with its outline
  ([ADR 0005](../adr/0005-famicube-palette-and-outline.md)).
- `art/ability/fire-punch.aseprite`, `art/ability/thunder-punch.aseprite` and `art/ability/water-punch.aseprite` are
  new 16x16 sources with `outline`, `shade` and `base` layers: one clenched fist, the same in all three, with a flame,
  a lightning bolt or a water drop. They are exported to `resources/ability/<id>.png`.
- `art/lootableItem/fire-punch-essence.aseprite`, `thunder-punch-essence.aseprite` and
  `water-punch-essence.aseprite` start from a copy of `art/lootableItem/teleport-essence.aseprite`. Each gets a
  fire, thunder or water colour from the palette and a small flame, bolt or drop. They are exported to
  `resources/lootableItem/<id>.png`.

## Resources

- `resources/ability/fire-punch.toml`: `manaCost = 10`, `cooldown = 1`, `targeting = "ADJACENT_ENEMY"`, with the
  ability effects `light-physical-damage`, `burn` and `venom-spread-if-venom`, all on `TARGETED_BATTLE_UNIT`.
- `resources/ability/thunder-punch.toml`: `manaCost = 10`, `cooldown = 2`, `targeting = "ADJACENT_ENEMY"`, with
  `light-physical-damage`, `stun` and `stun-long-if-soaked` on `TARGETED_BATTLE_UNIT`.
- `resources/ability/water-punch.toml`: `manaCost = 10`, `cooldown = 1`, `targeting = "ADJACENT_ENEMY"`, with
  `light-physical-damage` and `soak` on `TARGETED_BATTLE_UNIT`.
- `resources/effect/learn-fire-punch.toml`, `learn-thunder-punch.toml` and `learn-water-punch.toml`: a
  `LEARN_ABILITY` outcome with the punch's `abilityId`, timed `IMMEDIATELY`, as `learn-teleport.toml`.
- `resources/lootableItem/fire-punch-essence.toml`, `thunder-punch-essence.toml` and `water-punch-essence.toml`:
  `price = 20`, `type = "CONSUMABLE"`, with `[consumable] effectIds = ["learn-<punch>"]`.
- `resources/world/tessavar.toml`: the alchemist's `stock` becomes `["health-potion", "mana-potion", "health-elixir",
  "mana-elixir", "teleport-essence", "fire-punch-essence", "thunder-punch-essence", "water-punch-essence"]`, 8 of the
  shop's 12 slots (`ShopView.SLOTS`).

## Localization

- `resources/localization/messages.pot` and `resources/localization/es.po` gain:

| Key                                       | English               | Spanish                   |
|-------------------------------------------|-----------------------|---------------------------|
| `ability.fire-punch.name`                 | Fire Punch            | Puño de fuego             |
| `ability.thunder-punch.name`              | Thunder Punch         | Puño de trueno            |
| `ability.water-punch.name`                | Water Punch           | Puño de agua              |
| `lootableItem.fire-punch-essence.name`    | Fire Punch Essence    | Esencia de puño de fuego  |
| `lootableItem.thunder-punch-essence.name` | Thunder Punch Essence | Esencia de puño de trueno |
| `lootableItem.water-punch-essence.name`   | Water Punch Essence   | Esencia de puño de agua   |

- `npc.alchemist.line` becomes "Potions, elixirs, and essences that bend space or wake a fist. What will it be?", in
  Spanish "Pociones, elixires y esencias que doblan el espacio o despiertan un puño. ¿Qué va a ser?".

## Storybook

- `AbilityInfoViewStories`: `abilities` gains `fire-punch`, `thunder-punch` and `water-punch`, each with
  `Effect.Damage(5)` first and then the effects of Fireball, Thunder and Splash, so the `ability` control lists them.
- `AbilityInfoViewStories`: the **Screen / Ability info › Fire Punch** (`screen-ability-info--fire-punch`),
  **› Thunder Punch** (`screen-ability-info--thunder-punch`) and **› Water Punch**
  (`screen-ability-info--water-punch`) stories, with `ability` set to the punch. E.g.: Thunder Punch shows 10 mana, a
  cooldown of 2, 5 damage, a stun for 1 turn and a stun for 2 turns if soaked.
- `StorySmokeTest` lists the three new stories.
- **Screen / Shop › Buy** needs no change: `StoryContext.lootableItemIds()` reads every image under
  `resources/lootableItem`, so with `items` at 12 it shows the three essences next to the other nine items.

## Docs

- [docs/contributing/world-file.md](../contributing/world-file.md): the lootable item ids gain `fire-punch-essence`,
  `thunder-punch-essence` and `water-punch-essence`.

## Tests

- `PunchEssenceAcceptanceTest`, set up as `LearnTeleportAcceptanceTest`, with the viking of `AcceptanceUnitLoader`,
  who knows only Sword:
  - `The viking should learn each punch for good when the viking consumes its essence`
  - `Fire Punch should deal 5 damage to the adjacent enemy and burn it when the viking casts it`
  - `Thunder Punch should deal 5 damage to the adjacent enemy and stun it when the viking casts it`
  - `Water Punch should deal 5 damage to the adjacent enemy and soak it when the viking casts it`
- `AbilityInfoResourcesTest`, `LocalizationResourcesTest` and the resource loader tests run unchanged and cover the new
  abilities, effects, items and names.

## Known limitations

- The new own line never shows in Tessavar, the only world that sells the punches, as the alchemist gives a side quest
  there.
- The punches are untried in the real game: whether 10 mana is fair for a party member with 30 mana, like the hero, is
  left to play.
