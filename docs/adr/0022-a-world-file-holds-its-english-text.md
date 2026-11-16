# A world file holds its English text

The texts of a world, the name, description and tasks of its quests and their quest lines, are written in English in its
world file, and their translations stay in the PO catalogs, keyed under `world.<world id>`. A text with no translation
falls back to the English of the world file, and its `messages.pot` entry is only added once someone translates the
world; a resource test then fails, in CI, when that entry's `msgid` differs from the world file, so editing the English
marks its translations as fuzzy as before. We chose this over keeping only keys in the world file, which would make every
author edit `messages.pot`, and over writing every language into the world file, which would leave translators without
their PO tools, so a world file is complete on its own and translating it works as it always did.

This supersedes the `quest.<quest id>` keys of [ADR 0016](0016-a-quest-owns-the-lines-its-npcs-say-about-it.md), whose
quest lines are otherwise unchanged, and amends [ADR 0009](0009-gettext-po-catalogs-with-own-parser.md):
`messages.pot` no longer holds every English text, only those of the game and of the translated worlds.

## Consequences

- A community world plays in English until someone translates it.
- An npc's own line stays in the catalog under `npc.<npc id>.line`, as the npc is shared by every world.
