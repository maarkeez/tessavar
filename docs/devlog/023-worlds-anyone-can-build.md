---
date: 21st November 2026
commit: 6fd80fb0b46a7847e102062404dbf23a5a471b7d
---

# 023 — Worlds Anyone Can Build

## Hook

I paint a lake of water on a 24x32 world and tap Export.

The TOML lands on my clipboard.

Nobody else can ever play it.

**A world anyone can build is only worth something once everyone can play it.**

## Context

[The map builder](013-map-builder.md) painted terrain and exported a battlefield. Then
[the glossary](021-speaking-the-same-language.md) renamed it the world builder, because a game is played in a world, not
on a map.

But it still only painted terrain. The campaign was spread over a battlefield, the fortress, 14 battle units, 8 npcs and
4 quests, each in its own folder. The game could play that one world, and nothing else.

I was about to start posting every week. I wanted more worlds than I could ever build alone, and a reason for players to
come back and share.

So the world builder grew. It now places buildings, battle units and npcs, edits main and side quests, playtests the
world, and saves it as one world file.

Two things stayed out, on purpose:

- **No new art in a world.** A world only places what the game already has.
- **No online server.** No uploads, downloads, ratings or featured worlds.

## Main Story — From a File to a Community World

### Sharing without a server

> How does my world reach another player?

I looked at three answers:

- **An online service** where players upload and download worlds. A server to run, and every world to moderate.
- **Players passing files** to each other. No review, and no credit for the author.
- **A pull request** that adds the world file to the repository.

The pull request won. **The repository already had everything a server would need: review, checks and a history that
credits the author.**

The path of one world:

```text
World builder › Export      the-sunken-keep.toml is saved
World builder › How to share   opens the contributing guide
GitHub web upload           into resources/world/, no git needed
CI                          world version, playable, named after its world, a real game set up in it
Review                      I read it, and merge it
Next deploy                 Community worlds lists "The Sunken Keep, by <author>"
```

One surprise: CI only ran on pushes. A world uploaded from a fork would never have been checked. Now it runs on pull
requests too.

### Seeding the list

An empty Community worlds panel invites nobody.

So the first two are mine: Ashen Pass and The Drowned Mill. A script generated them, keeping the CPU's battle units
out of sight of the start, or the first battle would begin at once.

### The tradeoffs

- **A world reaches players only on a deploy**, never the moment its author finishes it.
- **A first pull request waits.** GitHub runs no CI for a first-time contributor until I approve it.

### Built for a phone

Then I opened the world builder on a phone.

The world showed in the battle's viewport, 6x6 tiles, less than a sixth of a new 16x16 world. Under it, the HUD filled
the rest of the screen, half empty on the Terrain tab.

And undoing a lava tile meant reaching down for a small button below the world.

**Undo first.** Drawing apps on a tablet already taught everyone the gesture: a two-finger tap undoes, a three-finger tap
redoes. The info line pops `Undo` or `Redo`, and the two buttons moved to the top of the menu, for desktop.

It wasn't free. KorGE turns a touch into mouse events whenever exactly one finger is down. So the first finger of a
two-finger tap also painted a tile. The battlefield now holds back that tap until it knows how many fingers came.

**Then the layout.** I prototyped four and compared them on screenshots: a bottom sheet, edge rails, today's HUD behind
a button, and a floating toolbar.

The floating toolbar won. The world fills the screen, 6x13 tiles, under a thin strip with the title and the menu. A pill
floats at the bottom:

```text
[ 🖌 | Paint  Fill  Erase ]
```

Tapping the brush opens a palette card above it, in two rows that scroll sideways. On Units, one row per player, Human
and CPU, ten units each.

The last bug: KorGE draws a rounded rectangle on the GPU, which ignores a clip. A scrolled row drew its slots right over
the card, until they were drawn as bitmaps.

### Still unfinished

Community worlds lists two worlds, and both are mine.

Nobody else has shared one yet.

## Key Takeaways

- **Let the repository be the server.** A pull request gives review, CI and credit, with nothing to run or moderate.
- **A world is only worth it once it's played.** Exporting a file isn't sharing it.
- **Give the world the screen.** Let the UI float over what the player builds, instead of sharing the screen with it.
- **Borrow the gestures players know.** Two fingers to undo, as in drawing apps, frees the row of tools.
- **Seed the list yourself.** An empty Community worlds panel invites nobody, so the first two worlds are mine.

## What's Next

The world builder clip is recorded with the floating toolbar. Next, it goes out with the weekly posts.

Then I wait for the first pull request from someone who isn't me.

A tool is finished when someone else uses it.
