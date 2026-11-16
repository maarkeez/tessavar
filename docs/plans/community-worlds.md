# Plan: anyone builds a world in the world builder and shares it by pull request, played by everyone as a community world

> **Status:** to do.
>
> **Summary:** Today the world builder only paints terrain. E.g.: a player paints a lake of water on a 24 × 32 world,
> taps Export, and gets the TOML of `resources/battlefield/battlefield.toml` on their clipboard, which nobody else can
> play. A playable world takes much more than that: the campaign is spread over 1 battlefield, the fortress, 14 battle
> units, 8 npcs and 4 quests, each in its own folder under `resources/`. The game plays only that one world.
> The rows and columns steppers take half the screen, and Import sits alone in the hamburger menu. Instead, a world is
> one world file under `resources/world/`, holding its battlefield, buildings, battle units, npcs and quests. The
> campaign is the world file `tessavar.toml`. The world builder edits every part of a world, playtests it and saves it
> as a world file, and an author opens a pull request that adds it. Once merged, it is listed under Community worlds on
> the main menu for every player. It builds on the glossary's **World**, **Campaign**, **Community world**, **World
> file**, **World version**, **Author**, **Playable**, **Playtest**, **Fill**, **Place** and **Erase**, and on
> [the campaign is a world like any other](../adr/0020-the-campaign-is-a-world-like-any-other.md),
> [community worlds arrive by pull request](../adr/0021-community-worlds-arrive-by-pull-request.md),
> [a world file holds its English text](../adr/0022-a-world-file-holds-its-english-text.md),
> [saves are not migrated](../adr/0011-no-save-migrations.md) and the [referral plan](referral.md).

## Decisions

| Topic                  | Decision                                                                                                                                                                                                                                                                                                                                             |
|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope                  | The world builder's UX, one world file format for the campaign and the community worlds, placing buildings, battle units and npcs, a quest editor, playtests, the Community worlds list, the world in telemetry and saves, CI checks of every world file, and the docs and templates to contribute one.                                              |
| One format             | The campaign and the community worlds share the world file format, and any world file can be played as either without changes. Chosen over keeping the campaign's resource files next to a separate community format, as the two would drift ([ADR 0020](../adr/0020-the-campaign-is-a-world-like-any-other.md)).                                          |
| World and catalog      | A world file says where things stand and what they say: tiles, buildings, battle units, npcs with their stock, and quests. The catalog stays shared: terrains, units, abilities, lootable items, loot tables, buildings with their size, and npcs with their art, own line and unit. A building or an npc stands at most once in a world.                                     |
| Campaign               | `World.CAMPAIGN_ID` is `"tessavar"`, the world file `resources/world/tessavar.toml`, named "Tessavar" by the author maarkeez. Chosen over a setting in a resource file, as a constant is checked by the compiler and the tests.                                                                                                                   |
| Reading a world        | Each subdomain's `Resources…Loader` is given the world file's text and decodes its own section, and its `Parse…` and `Export…` queries do the same for the world builder. `world` only picks which world file to play. Chosen over `world` decoding the whole file, as each subdomain keeps owning its data and no subdomain depends on `world` ([ADR 0002](../adr/0002-acyclic-shrink-only-subdomain-dependencies.md)). |
| World version          | Every world file starts with `version = 1`. A world file of another world version is refused on import and rejected by CI. Every bump updates the world files of the repository in the same commit, with no migrations.                                                                                                                          |
| Texts                  | A world file holds the English texts of its quests. Their translations sit in the PO catalogs under `world.<world id>.quest.<quest id>.…`, falling back to the English ([ADR 0022](../adr/0022-a-world-file-holds-its-english-text.md)). The campaign's Spanish moves from `quest.<quest id>` to `world.tessavar.quest.<quest id>` in `es.po`.               |
| Quests                 | The world builder configures main and side quests as they work today: ordered defeat, visit and talk tasks, start tiles, quest giver, repeatable, rewards and quest lines. There is no "goal". Finishing a world's main quest does not end it, as in the campaign.                                                                                    |
| Battle units           | A battle unit in a world names its player, `player-one` (human) or `player-two` (CPU), never a side. The world builder's two rows of units are titled with the players' names. Any unit can be deployed for either player.                                                                                                                       |
| Playable               | A world is playable with a battle unit of each player and a main quest with start tiles. Export always works, so a world can be moved between devices before it is finished, but warns when the world is not playable. CI rejects a world file that is not playable.                                                                                 |
| Builder layout         | On screen: the world, the palette (Terrain, Buildings, Units and Npcs tabs), the Paint, Fill and Erase tools, undo and redo, and the info line. In the hamburger menu: Resize, Details, Quests, Playtest, Import, Export, How to share and Back. Chosen over keeping Export and Back as a call to action, so building gets the whole screen.                   |
| Undo                   | Undo and redo cover every edit of the world, up to `WorldBuilder.UNDO_STEPS` = 50 steps back.                                                                                                                                                                                                                                                      |
| Fill                   | Fill paints the connected area of the tapped tile's terrain, and is rejected whole when any of its tiles would be adjacent to a terrain it can't be. E.g.: filling a sand area with water next to lava is rejected, with the same message as a paint.                                                                                                |
| Erase                  | Erasing a battle unit or an npc that a quest names also removes those tasks, or the side quest an erased quest giver gives, and one undo brings them back.                                                                                                                                                                                          |
| Autosave               | The world being built is kept in `views.storage` on every edit, so it survives a playtest, leaving the world builder and a reload.                                                                                                                                                                                                                 |
| Export                 | Export saves a world file named after the world's id, e.g.: "The Sunken Keep" gives `the-sunken-keep.toml`, as a download on the web and with a save dialog on the JVM. Chosen over the clipboard, as the pull request's web upload takes a file.                                                                                                  |
| Details                | The world's name and author are typed in the Details dialog, in a `UITextInput`. On the web, when the keyboard doesn't open on iOS Safari, the browser's `prompt()` takes them instead.                                                                                                                                                             |
| Playtest               | Playtest plays the world being built in the game screen, from its text and with the world id `playtest`. Quitting, or the game over, goes back to the world builder. Saving is not offered, as `playtest` is no world a save could be loaded from.                                                                                                  |
| Community worlds list  | A Community worlds button on the main menu opens a scrolling panel, like the save slots. It lists every world file under `resources/world/` other than the campaign, by name with its author, alphabetically, and a tap starts a new game in it. The game lists the folder inside the resources bundle, so no index is needed ([ADR 0003](../adr/0003-single-resources-bundle.md)). |
| Saves                  | A save records its world, and its summary shows the world's name. `SAVE_VERSION` goes from 10 to 11, so every earlier save becomes incompatible ([ADR 0011](../adr/0011-no-save-migrations.md)). A save of a world no longer shipped is incompatible too.                                                                                            |
| Telemetry              | `GAME_STARTED` carries the world id, e.g.: `tessavar`, `the-sunken-keep` or `playtest`, and the other product events reach it through their game id. The telemetry collector keeps it in a new `world_id` column. `PRIVACY_NOTICE_VERSION` goes from 3 to 4, so every player is asked again.                                                     |
| Contributing           | An author adds their world file to `resources/world/` with GitHub's web upload ([ADR 0021](../adr/0021-community-worlds-arrive-by-pull-request.md)). `CONTRIBUTING.md` points to a Diataxis how-to and a Diataxis reference under `docs/contributing/`. The pull request template asks them to agree to CC BY-SA 4.0, and the maintainer reviews every one.  |
| Curation               | CI rejects a world file that is invalid or not playable, or whose id is taken, isn't lowercase words joined by hyphens, or is `playtest`. Every merged community world gets a post through Buffer crediting its author.                                                                                                                        |
| Architecture           | A new `world` subdomain absorbs `gameSetup`: `world.domain` (`World`), `world.usecases` (`SetUpGame`, `SearchCommunityWorlds`, `SearchWorld`), `world.adapters` (`ResourcesWorldLoader`, `WorldApi`). `worldBuilder` uses the `Parse…` and `Export…` queries of `battlefield`, `building`, `battleUnit`, `npc` and `quest`.                                  |
| Storybook              | New stories for the resize dialog, the details dialog, the quest editor and the Community worlds panel; the world builder's HUD and menu stories change; the save slot stories show the world.                                                                                                                                                   |
| Tests                  | Domain and use case tests of `WorldBuilder` and `World`, a loader test per section, resource tests over every world file, view tests, `WorldBuilderEndToEndTest`, a playtest acceptance test, `SaveFixtureTest` version 11, telemetry tests and the collector's Vitest tests.                                                                    |
| Out of scope           | New buildings, units or npcs (a regular change to the game, with the [pixel-art skill](../../.claude/skills/pixel-art/SKILL.md)), the same building twice in a world, sharing worlds online without a pull request, ratings or a featured world, and translating community worlds.                                                    |
| Delivery               | 5 milestones, each green on CI. See [Milestones](#milestones).                                                                                                                                                                                                                                                                                       |

For example, an author builds "The Sunken Keep" and shares it:

| Step                                                         | Where                     | What happens                                                                                       |
|--------------------------------------------------------------|---------------------------|----------------------------------------------------------------------------------------------------|
| They paint a 20 × 20 world, place the fortress and 2 knights | World builder             | The knights are `player-one`'s, under the human player's row; every edit is kept in storage        |
| They place 4 rats and the goat wizard for `player-two`       | World builder             | The info line says nothing yet: export would warn, as there is no main quest                       |
| They add a main quest: start tiles, defeat 1 goat wizard     | World builder › Quests    | The world is playable                                                                              |
| They playtest, and quit from the pause screen                | Game screen               | `GAME_STARTED` with world id `playtest`; Save is not offered; quitting goes back to the world builder |
| They type the name and author in Details, then Export        | World builder › Details   | `the-sunken-keep.toml` is downloaded                                                               |
| They follow How to share and upload it to `resources/world/` | GitHub                    | CI checks it; the maintainer reviews and merges it                                                 |
| After the next deploy, a player taps Community worlds        | Main menu                 | "The Sunken Keep, by its author" is listed; a tap starts a new game with world id `the-sunken-keep` |

## Milestones

Each milestone is a few commits straight on `main`, green on CI.

| #  | Milestone            | Done when                                                                                                                                                                                         | Status | Done in |
|----|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|---------|
| M1 | Builder UX           | The world builder has the hamburger menu with Resize, Import, Export and Back; Paint and Fill; undo and redo; Export saves a file; the world survives a reload. Screenshots approved.              | ⬜      |         |
| M2 | One world format     | The campaign is played from `resources/world/tessavar.toml` as before; saves record their world; the world builder imports and exports it unchanged and edits its name and author in Details.  | ⬜      |         |
| M3 | Placing and playtest | The world builder places and erases buildings, battle units and npcs with their stock, and playtests the world. Screenshots approved.                                                             | ⬜      |         |
| M4 | Quest editor         | The world builder configures main and side quests, and the tiles of the current visit task are marked on the battlefield, in the campaign too. Screenshots approved.                              | ⬜      |         |
| M5 | Community worlds     | The main menu lists 2 community worlds by maarkeez; `GAME_STARTED` carries the world id under the privacy notice version 4; CI checks every world file; the contributing docs and templates are on `main`. | ⬜      |         |

### M1 commits

1. `WorldBuilderMenuView` offers Resize, Import, Export and Back. `WorldBuilderResizeView` holds the rows and columns steppers taken out of `WorldBuilderHudView`, and `WorldBuilderCallToActionView` keeps only the leave confirmation.
2. `WorldBuilder.fill` and the Paint and Fill tools in `WorldBuilderHudView`, through a new `FillTile` command.
3. `WorldBuilder.undo` and `redo` with `UNDO_STEPS`, the `UndoEdit` and `RedoEdit` commands, and the undo and redo buttons.
4. `WorldFileSaver`: Export saves a file on the web and the JVM, in place of `GameWindowClipboard`.
5. `StorageWorldBuilderRepository` keeps the world in `views.storage`, in place of `InMemoryWorldBuilderRepository`.
6. The stories and screenshots of the world builder.

### M2 commits

1. The `world` subdomain: `World`, `WorldVersion` 1, `ResourcesWorldLoader` and `WorldApi`. `SetUpGame` moves from `gameSetup` into it and sets up a game in the world it is given, and `gameSetup` is deleted.
2. `resources/world/tessavar.toml`, written from today's battlefield, the fortress, 14 battle units, 8 npcs and 4 quests with their English texts. The placements move out of `resources/building/`, `resources/battleUnit/` and `resources/npc/`, and `resources/quest/` and `resources/battlefield/battlefield.toml` are deleted.
3. Each `Resources…Loader` of `battlefield`, `building`, `battleUnit`, `npc` and `quest` decodes its section of the world file text, with `Parse…` and `Export…` queries. `GameScene` takes the world to play.
4. Quest texts come from the world file, with `world.<world id>.quest.<quest id>.…` translations. `es.po` and `messages.pot` are re-keyed, and `QuestResourcesTest` checks every world file.
5. The save records its world; `SAVE_VERSION` 11; `SaveSlotsView` shows the world's name; the golden save `11.json`.
6. `WorldBuilder` holds a whole world. `ImportWorld` and `ExportWorld` read and write every section, keeping the sections it can't edit yet unchanged.
7. `WorldBuilderDetailsView`, the `RenameWorld` command, and the `prompt()` fallback on the web.

### M3 commits

1. The palette's tabs in `WorldBuilderHudView`: Terrain, Buildings, Units by player, and Npcs.
2. `WorldBuilder.place` and `erase`, with the `PlaceOccupant` and `EraseOccupant` commands, rejected on a tile that isn't vacant and occupiable.
3. The stock of a placed merchant, chosen in `WorldBuilderStockView` from the lootable items.
4. Playtest: `WorldBuilderScene` opens `GameScene` with the world's text and the id `playtest`, which goes back to it on quit or game over, with `canSave` false.
5. The stories and screenshots.

### M4 commits

1. `WorldBuilderQuestsView` lists the world's quests, adds and removes them, and opens `WorldBuilderQuestView`.
2. `WorldBuilderQuestView` edits a quest's type, quest giver, repeatable, start tiles, tasks, reward and texts, with the `EditQuest` command.
3. `Erase` removes the tasks naming an erased battle unit or npc, and the side quests of an erased quest giver.
4. The quest marker on the tiles of the current visit task, from `SearchQuestMarkers`.
5. The stories and screenshots.

### M5 commits

1. `CommunityWorldsView` in `MenuScene`, its button in `MenuView`, and `SearchCommunityWorlds`.
2. `WorldResourcesTest` and the resource tests over every world file in `resources/world/`.
3. `0003_add_world_id_to_product_events.sql`, `parseBatch` with `world_id`, `Telemetry.startGame` with the world id, and the privacy notice version 4.
4. `CONTRIBUTING.md`, `docs/contributing/share-a-world.md`, `docs/contributing/world-file.md`, `.github/pull_request_template.md`, and `LICENSE-ART.md` covering `resources/world/`.
5. The 2 seed worlds by maarkeez.

## Reference

### World file

`resources/world/<world id>.toml`, where the world id is lowercase words joined by hyphens. E.g.: an excerpt of
`tessavar.toml`:

```toml
version = 1
name = "Tessavar"
author = "maarkeez"

[battlefield]
tiles = [
    ["void", "void", "void"],
    ["void", "sand", "sand"],
]

[[buildings]]
id = "fortress"
row = 17
column = 13

[[battleUnits]]
id = "hero"
unitId = "hero"
playerId = "player-one"
row = 2
column = 1

[[npcs]]
id = "alchemist"
row = 21
column = 18
stock = ["health-potion", "mana-potion", "health-elixir", "mana-elixir", "teleport-essence"]

[[quests]]
id = "rats-in-the-fields"
type = "SIDE"
questGiverNpcId = "elf"
repeatable = true
name = "Rats in the Fields"
description = "..."
lines = { start = "...", reminder = "...", startAgain = "..." }

[[quests.tasks]]
type = "DEFEAT"
unitId = "rat"
requiredDefeats = 2
text = "Defeat 2 rats"

[quests.reward]
gold = 20
```

What stays in the catalog: `resources/building/fortress.toml` keeps `rows` and `columns`, and
`resources/npc/<npc id>.toml` keeps `unitId` for a recruitable npc, next to their images.

### world

- `World` holds a world's id, name, author and world version. `World.CAMPAIGN_ID` is `"tessavar"`, and
  `World.PLAYTEST_ID` is `"playtest"`. `WorldVersion.CURRENT` is 1.
- `WorldLoader` is a port; `ResourcesWorldLoader` lists `resources/world/` with `listNames()`, reads each file's
  `version`, `name` and `author`, and gives a world file's text by its id.
- `SearchCommunityWorlds` returns every world other than the campaign, sorted by name. `SearchWorld(id)` returns one world
  with its text.
- `SetUpGame` moves from `gameSetup.usecases.commands`, and `GameSetupApi` becomes part of `WorldApi`.
  `SubdomainDependencyArchitectureTest` allows `world` to depend on what `gameSetup` did.

### battlefield, building, battleUnit, npc and quest

- `ResourcesBattlefieldLoader`, `ResourcesBuildingLoader`, `ResourcesBattleUnitLoader`, `ResourcesNpcLoader` and
  `ResourcesQuestLoader` take `worldFile: String`, the world file's text, and decode their section. The catalog parts
  (`rows` and `columns` of a building, `unitId` of an npc) are still read from `resources/building/` and `resources/npc/`.
- Each subdomain gains `Parse…` and `Export…` queries for its section, like `ParseBattlefield` and `ExportBattlefield`
  today, e.g.: `ParseQuests(text)` and `ExportQuests(quests)`.
- `Quest.Dto` gains its English texts, and `QuestMapper` reads `world.<world id>.quest.<quest id>.…` with them as the
  fallback. `npc.<npc id>.line` stays as it is.
- `SearchQuestMarkers` also marks the tiles of the current visit task (M4).

### worldBuilder

- `WorldBuilder` holds the tiles, buildings, battle units, npcs and quests of a world, its name and author, and up to
  `UNDO_STEPS` = 50 earlier states for undo, with the states undone for redo.
- `fill(row, column, canBeAdjacent)` paints the connected area of the tapped tile's terrain with the selected terrain,
  or publishes `TilePaintRejected` and changes nothing.
- `place(occupant, row, column, isOccupiable)` and `erase(row, column)` (M3). `erase` also removes the tasks naming the
  erased battle unit or npc, and the side quests of an erased quest giver (M4).
- `isPlayable()` checks for a battle unit of `player-one`, a battle unit of `player-two`, and a main quest with start
  tiles. `ExportWorld` returns the world file and the reasons it isn't playable, which `WorldBuilderInfoView` shows as a
  warning.
- `ImportWorld` refuses a world file of another world version with `WorldImportRejected`, reason `OTHER_WORLD_VERSION`.
- `StorageWorldBuilderRepository` writes the world file to `views.storage` under `worldBuilder.world` on every save, and
  reads it back when the world builder opens.

### screen

- `WorldBuilderMenuView`: Resize, Details, Quests, Playtest, Import, Export, How to share and Back. How to share opens
  `CONTRIBUTING.md` on GitHub.
- `WorldBuilderHudView`: the palette's tabs, the Paint, Fill and Erase tools, and undo and redo. Its steppers move to
  `WorldBuilderResizeView`.
- `GameScene` takes the world to play, its id and text: the campaign from New game, a community world from
  `CommunityWorldsView`, the world of a save from Load, or `playtest` from the world builder.
- `MenuView` gains `onCommunityWorldsSelected` and its button; `CommunityWorldsView` lists the community worlds, each by
  name with its author.
- `SaveSlotsView` shows the world's name of each save.

### save

- `Save.Dto.SummaryDto` gains `worldId`. `SAVE_VERSION` is 11, and `src/jvmTest/fixtures/save` gains its golden save.

### telemetry and telemetry-collector

- `Telemetry.startGame(gameId, worldId, occurredAt)` stamps `ProductEventRecorded.worldId` on `GAME_STARTED`;
  `PRIVACY_NOTICE_VERSION` is 4.
- The privacy notice's "With it:" paragraph adds "and the name of the world you play" in `messages.pot` and `es.po`.
- `migrations/0003_add_world_id_to_product_events.sql` adds a nullable `world_id`, and `parseBatch` accepts it, checked
  against `[a-z0-9-]{1,64}`.

### Docs

- `CONTRIBUTING.md`: what can be contributed, and a link to each guide.
- `docs/contributing/share-a-world.md`: a Diataxis how-to, from building a world to opening its pull request with
  GitHub's web upload, written with the `diataxis-how-to-writer` skill.
- `docs/contributing/world-file.md`: a Diataxis reference of the world file, written with the
  `diataxis-reference-writer` skill.
- `.github/pull_request_template.md`: a checkbox agreeing to CC BY-SA 4.0, and one confirming the world was playtested.
- `LICENSE-ART.md` covers the world files under `resources/world/`.

### Storybook

- **Screen / World builder resize › Default**, **› Smallest world** and **› Largest world**, with `rows` and `columns`
  controls (`int`, 1–128).
- **Screen / World builder HUD**: **› Terrain tab**, **› Buildings tab**, **› Units tab**, **› Npcs tab**, **› Fill
  tool**, **› Nothing to undo**.
- **Screen / World builder menu › Open** lists the 8 options.
- **Screen / World builder details › Default**, with `name` and `author` controls (`text`).
- **Screen / World builder stock › Alchemist**.
- **Screen / World builder quests › Campaign** and **Screen / World builder quest › Main quest**, **› Side quest**.
- **Screen / Community worlds › Two worlds**, **› Empty**.
- **Screen / Save slots** stories show the world's name.
- `StorySmokeTest` lists the new stories.

### Tests

- `WorldBuilderTest`
  - `WorldBuilder should fill the connected area of one terrain when no tile would be adjacent to a forbidden terrain`
  - `WorldBuilder should reject the fill when one tile would be adjacent to a forbidden terrain`
  - `WorldBuilder should undo the last edit when undoing`
  - `WorldBuilder should forget the oldest edit when more than 50 edits are made`
  - `WorldBuilder should place a battle unit when its tile is vacant and occupiable`
  - `WorldBuilder should reject placing a building when it would stand on void`
  - `WorldBuilder should remove the tasks naming a battle unit when it is erased`
  - `WorldBuilder should be playable when it has a battle unit of each player and a main quest with start tiles`
- `ImportWorldTest`
  - `ImportWorld should reject a world file when its world version is not 1`
  - `ImportWorld should keep every section when the world is imported and exported again`
- `SetUpGameTest`
  - `SetUpGame should set up a game in the world it is given`
- `ResourcesWorldLoaderTest`
  - `ResourcesWorldLoader should list every world file under the world folder`
- `ResourcesQuestLoaderTest`, `ResourcesNpcLoaderTest`, `ResourcesBuildingLoaderTest`, `ResourcesBattleUnitLoaderTest`
  and `ResourcesBattlefieldLoaderTest`
  - `Resources…Loader should decode its section when given a world file`
- `WorldResourcesTest`
  - `Every world file should be of world version 1`
  - `Every world file should be playable`
  - `Every world file should be named lowercase words joined by hyphens, other than playtest`
  - `Every world file's occupants should stand on vacant and occupiable tiles`
- `QuestResourcesTest`, `NpcUnitResourcesTest` and `LocalizationResourcesTest` run over every world file.
- `PlaytestAcceptanceTest`
  - `A playtest should go back to the world builder when the player quits`
  - `A playtest should not offer saving`
- `SaveFixtureTest` gains version 11.
- `TelemetryTest`
  - `Telemetry should stamp the world id on the game started when a game starts`
- `MenuViewTest`, `CommunityWorldsViewTest`, `WorldBuilderHudViewTest`, `WorldBuilderMenuViewTest`,
  `WorldBuilderPresenterTest` and `WorldBuilderEndToEndTest` cover the views.
- `collector.test.ts`
  - `the telemetry collector should keep the world id of a game started`

## Known limitations

- A building or an npc stands at most once in a world, so a world can't hold two fortresses until buildings get a kind
  and a piece, like units and battle units.
- A community world plays in English until someone translates it.
- A world reaches players only on a deploy, never the moment its author finishes it.
- `SAVE_VERSION` 11 makes every save of every player incompatible, and the privacy notice version 4 asks every player
  again.
- World files exported before M2, battlefield TOML only, are refused by the world builder.
- A first-time contributor's pull request needs the maintainer's approval before GitHub runs its CI.
