# World file reference

> A world file is a TOML file under `resources/world/` holding one world of Tessavar: its battlefield, buildings,
> battle units, npcs and quests.

## Overview

Every world of the game is a world file, the campaign `tessavar.toml` included
([ADR 0020](../adr/0020-the-campaign-is-a-world-like-any-other.md)). The world builder imports and exports world files,
and `WorldResourcesTest` checks every world file under `resources/world/`. A world file names what it uses from the
catalog, which is shared by every world: terrains, buildings, units, npcs, players and lootable items. Rows and columns
count from `0`, the row from the top and the column from the left. The texts are in English
([ADR 0022](../adr/0022-a-world-file-holds-its-english-text.md)).

## File name

`resources/world/<world id>.toml`. The world id is the world's `name` in lowercase words of letters and digits joined by
hyphens, e.g.: `The Sunken Keep` is `the-sunken-keep.toml`. `playtest` is never a world id, and `tessavar` is the
campaign's.

## Top-level keys

| Key       | Type     | Default | Required | Description                                                                                  |
|-----------|----------|---------|----------|----------------------------------------------------------------------------------------------|
| `version` | `int`    | none    | Yes      | The world version. Only `1` is accepted.                                                     |
| `name`    | `string` | none    | Yes      | The world's name, listed under *Community worlds*, e.g.: `"The Sunken Keep"`.                |
| `author`  | `string` | none    | Yes      | Who built the world, listed with its name, e.g.: `"maarkeez"`.                               |

## `[battlefield]`

| Key     | Type               | Default | Required | Description                                                                                                                         |
|---------|--------------------|---------|----------|-------------------------------------------------------------------------------------------------------------------------------------|
| `tiles` | `string[][]`       | none    | Yes      | The terrain of every tile, one list per row, every row as long. From 1 to 128 rows and columns. Accepted values: the terrain ids. |

Terrain ids, from `resources/terrain/`:

| Terrain id | Occupiable | Effect        |
|------------|------------|---------------|
| `void`     | No         | none          |
| `sand`     | Yes        | none          |
| `grass`    | Yes        | none          |
| `water`    | Yes        | `water-heal`  |
| `lava`     | Yes        | `lava-damage` |

Two tiles side by side hold the same terrain, or two terrains with a transition under `resources/terrain/transitions/`:
`sand` next to `grass`, `lava`, `void` or `water`.

## `[[buildings]]`

One table per building. A building stands at most once in a world.

| Key      | Type     | Default | Required | Description                                                                   |
|----------|----------|---------|----------|-------------------------------------------------------------------------------|
| `id`     | `string` | none    | Yes      | The building, from `resources/building/`. Accepted values: `fortress`.        |
| `row`    | `int`    | none    | Yes      | The row of its top left tile.                                                 |
| `column` | `int`    | none    | Yes      | The column of its top left tile.                                              |

A building covers the rows and columns its catalog file gives it: `fortress` is 4 × 4 tiles.

## `[[battleUnits]]`

One table per battle unit.

| Key        | Type     | Default | Required | Description                                                                                                                                     |
|------------|----------|---------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `id`       | `string` | none    | Yes      | The battle unit's id, unique in the world, e.g.: `rat-1`.                                                                                       |
| `unitId`   | `string` | none    | Yes      | Its unit, from `resources/unit/`. Accepted values: `bat`, `bee`, `goat-wizard`, `herbalist`, `hero`, `rat`, `viking-warrior`. |
| `playerId` | `string` | none    | Yes      | Its player. Accepted values: `player-one` (Human), `player-two` (CPU).                                                                         |
| `row`      | `int`    | none    | Yes      | The row of its tile.                                                                                                                            |
| `column`   | `int`    | none    | Yes      | The column of its tile.                                                                                                                         |

## `[[npcs]]`

One table per npc. An npc stands at most once in a world.

| Key      | Type       | Default | Required | Description                                                                                                                              |
|----------|------------|---------|----------|------------------------------------------------------------------------------------------------------------------------------------------|
| `id`     | `string`   | none    | Yes      | The npc, from `resources/npc/`. Accepted values: `alchemist`, `blacksmith`, `elder`, `elf`, `farmer`, `herbalist`, `princess`, `viking`. |
| `row`    | `int`      | none    | Yes      | The row of its tile.                                                                                                                     |
| `column` | `int`      | none    | Yes      | The column of its tile.                                                                                                                  |
| `stock`  | `string[]` | `[]`    | No       | The lootable items it sells, from `resources/lootableItem/`. An npc with a stock is a merchant.                                          |

`herbalist` and `viking` are recruitable: they join the party, as the units `herbalist` and `viking-warrior`. They are
never a quest giver nor the npc of a talk task.

Lootable item ids: `bee-essence`, `hammer`, `health-elixir`, `health-potion`, `iron-hammer`, `mana-elixir`,
`mana-potion`, `rune-hammer`, `teleport-essence`.

## Occupied tiles

Every tile of a building, a battle unit or an npc is inside the world, of an occupiable terrain, and holds nothing else.
A tile is not occupiable either when a terrain nothing can stand on, such as `void`, is both above or below it and to
its left or right: a corner. E.g.: in a world bordered with `void`, row `1`, column `1` is a corner, while row `1`,
column `2` is not.

## `[[quests]]`

One table per quest.

| Key               | Type      | Default | Required          | Description                                                                                                                  |
|-------------------|-----------|---------|-------------------|------------------------------------------------------------------------------------------------------------------------------|
| `id`              | `string`  | none    | Yes               | The quest's id, unique in the world, e.g.: `rats-in-the-fields`.                                                             |
| `type`            | `string`  | none    | Yes               | `MAIN` or `SIDE`. A world has exactly one main quest.                                                                       |
| `questGiverNpcId` | `string`  | none    | No                | The npc of the world giving a side quest. A main quest has none.                                                             |
| `repeatable`      | `bool`    | `false` | No                | Whether a side quest can be started again once completed. A main quest is never repeatable.                                 |
| `startTiles`      | `int[][]` | `[]`    | For a main quest  | The tiles, as `[row, column]`, starting the main quest when the party steps on one. A side quest has none.                   |
| `name`            | `string`  | `""`    | No                | The quest's name in the quest journal.                                                                                       |
| `description`     | `string`  | `""`    | No                | The quest's description in the quest journal.                                                                               |
| `lines`           | table     | `{}`    | For a side quest  | What the quest giver says, as an inline table, e.g.: `lines = { start = "...", reminder = "...", completed = "..." }`.       |

### `lines`

| Key          | Type     | Default | Required                                                       | Description                                                   |
|--------------|----------|---------|----------------------------------------------------------------|---------------------------------------------------------------|
| `start`      | `string` | none    | For a side quest                                               | Said when the quest giver gives the quest.                    |
| `reminder`   | `string` | none    | For a side quest with a task other than talking to its giver   | Said when talked to before the quest is completed.            |
| `completed`  | `string` | none    | For a side quest that is not repeatable                        | Said when talked to once the quest is completed.              |
| `startAgain` | `string` | none    | For a repeatable side quest                                    | Said when the quest giver gives the quest again.              |

### `[[quests.tasks]]`

One table per task, in the order they are done. Every quest has at least one task.

| Key               | Type      | Default | Required          | Description                                                                                   |
|-------------------|-----------|---------|-------------------|-----------------------------------------------------------------------------------------------|
| `type`            | `string`  | none    | Yes               | `DEFEAT`, `TALK` or `VISIT`.                                                                  |
| `unitId`          | `string`  | none    | For `DEFEAT`      | The unit whose battle units are defeated, e.g.: `rat`.                                        |
| `requiredDefeats` | `int`     | none    | For `DEFEAT`      | How many battle units of that unit are defeated.                                              |
| `npcId`           | `string`  | none    | For `TALK`        | The npc of the world talked to.                                                               |
| `tiles`           | `int[][]` | `[]`    | For `VISIT`       | The tiles, as `[row, column]`, of which the party steps on one. They are marked on the battlefield. |
| `text`            | `string`  | `""`    | No                | The task as the quest tracker lists it, e.g.: `"Defeat 2 Rats"`.                              |
| `line`            | `string`  | none    | For `TALK`        | What the npc says when talked to for this task.                                               |

### `[quests.reward]`

| Key               | Type       | Default | Required | Description                                                        |
|-------------------|------------|---------|----------|--------------------------------------------------------------------|
| `gold`            | `int`      | `0`     | No       | The gold the party receives when the quest is completed.           |
| `lootableItemIds` | `string[]` | `[]`    | No       | The lootable items the party receives when the quest is completed. |

## Playable

A world file is playable when it holds a battle unit of `player-one`, a battle unit of `player-two`, exactly one main
quest with start tiles, and a task in every quest. `WorldResourcesTest` rejects a world file under `resources/world/` that
is not playable.

## Example

```toml
version = 1
name = "The Sunken Keep"
author = "maarkeez"

[battlefield]
tiles = [
    ["void", "void", "void", "void"],
    ["void", "sand", "sand", "void"],
    ["void", "sand", "sand", "void"],
    ["void", "void", "void", "void"],
]

[[battleUnits]]
id = "hero-1"
unitId = "hero"
playerId = "player-one"
row = 1
column = 1

[[battleUnits]]
id = "rat-1"
unitId = "rat"
playerId = "player-two"
row = 2
column = 2

[[quests]]
id = "quest-1"
type = "MAIN"
startTiles = [[1, 2]]
name = "The Last Rat"
description = "A rat guards the keep."

[[quests.tasks]]
type = "DEFEAT"
unitId = "rat"
requiredDefeats = 1
text = "Defeat the Rat"

[quests.reward]
gold = 10
```
