# The campaign is a world like any other

A world is one world file under `resources/world/`: its battlefield, where its buildings stand, its battle units, where its
npcs stand with their stock, and its quests. The campaign is just the world a setting names, played by New game, and
every other world file there is a community world. The `world` subdomain reads world files and sets up a game in one,
absorbing `gameSetup`, whose only use case published the requests that set up the single, hard-wired world. We chose this
over keeping the campaign in its own resource files, one per subdomain, next to a separate format for community worlds,
so a world file made by the world builder can be played as the campaign or as a community world without changing it, and
the campaign and the community worlds never drift into allowing different things.

## Consequences

- The world builder has to read and write everything a world can hold, and keeps what it can not edit yet unchanged from
  import to export, so opening the campaign in it never loses anything.
- Making another world the campaign changes a setting, never a world file.
- What stays shared between worlds is the catalog: terrains, units, abilities, lootable items, loot tables, buildings
  and npcs, of which a world only says where they stand. A new one is still added by a change to the game, not by a
  world file.
- A building or an npc stands at most once in a world, as the quests name an npc by its id.
