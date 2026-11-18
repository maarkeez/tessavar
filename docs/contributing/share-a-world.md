# How to share a world

> **Summary:** This guide takes a world built in Tessavar's world builder to every player, by adding its world file to
> the game in a pull request on GitHub. Once the pull request is merged, the world is listed under *Community worlds* on
> the main menu after the next deploy.

## Prerequisites

- A world built in *Build a world*, at https://maarkeez.github.io/tessavar/ or in the game run with `./gradlew runJvm`.
- A [GitHub account](https://github.com/signup). No git and no code are needed.
- Agreeing to release the world under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/), like the
  game's art: anyone may play, share and change it, crediting you.

## Steps

### 1. Name the world and sign it

In the world builder, open the menu at the top right and tap *Details*. Type the world's name, e.g.: `The Sunken Keep`,
and your name as its author, e.g.: your GitHub username. Tap *Done*.

The name gives the world file its name, `the-sunken-keep.toml`, which must not be taken by a world already in
[`resources/world/`](../../resources/world).

### 2. Make the world playable

Check the world holds:

- A battle unit of the human player and one of the CPU, placed from the *Units* tab.
- One main quest with its start tiles, from *Quests* in the menu: the main quest starts when the party steps on one of
  them.
- At least one task in every quest.
- A line for every talk task, and the start, reminder and completed lines (start again when repeatable) of every side
  quest's quest giver.

Tap *Playtest* in the menu. When something is missing, the info line says what, e.g.: `To playtest, add one main quest`.

Tip: tap the world with two fingers to undo the last edit, and with three to redo it, or use the arrows at the top of the
menu.

### 3. Playtest it

Play the world through: start the main quest, finish its tasks and the side quests. Open the pause screen and tap *Quit to
menu* to go back to the world builder, where the world is kept as you left it.

### 4. Export the world file

Tap *Export* in the menu. On the web, the browser downloads the world file, e.g.: `the-sunken-keep.toml`. On the JVM,
choose where to save it, named after the world.

When the world is not playable yet, the info line says what it lacks, e.g.:
`World exported. To be playable, place a battle unit of CPU`. Fix it, and export again.

### 5. Upload it to `resources/world/`

1. Open https://github.com/maarkeez/tessavar/tree/main/resources/world, signed in to GitHub.
2. Tap *Add file* › *Upload files*, and drop the world file, e.g.: `the-sunken-keep.toml`.
3. Under *Commit changes*, write `Add The Sunken Keep, a community world`, choose *Create a new branch for this commit
   and start a pull request*, and tap *Propose changes*.

GitHub forks the repository for you when you can't push to it.

### 6. Open the pull request

On the *Open a pull request* page, fill in the template: tick the box agreeing to CC BY-SA 4.0, and the one confirming
you playtested the world. Tap *Create pull request*.

## Verify

The pull request's *Testing* check passes: `WorldResourcesTest` reads every world file under `resources/world/` and
fails when one is invalid, unplayable, misnamed, or of another world version. The maintainer then reviews the world and
merges it. After the next deploy, *Community worlds* on the main menu lists it as `The Sunken Keep, by <author>`.

## Troubleshooting

| Symptom                                                | Cause                                                                                    | Fix                                                                                                                    |
|--------------------------------------------------------|------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| No check runs on the pull request                      | GitHub waits for the maintainer to approve the checks of a first-time contributor        | Wait: the maintainer approves them while reviewing.                                                                    |
| `Every world file should be playable` fails            | The world lacks a battle unit of a player, the main quest, its start tiles, or a task    | Import the file in the world builder, add what Playtest asks for, export it and upload it again to the same branch.    |
| `should be named after its world's name` fails         | The file was renamed, e.g.: `the-sunken-keep (1).toml` from a second download            | Rename it to the name Export gives it, e.g.: `the-sunken-keep.toml`.                                                   |
| `should have every line every quest needs` fails       | A talk task or a side quest's quest giver has no line                                     | Fill in the line in *Quests*, export and upload again.                                                                 |
| GitHub says the file replaces one already there        | Another world has the same name                                                          | Rename the world in *Details*, export and upload again.                                                                |

## Next steps

- [The world file reference](world-file.md): every table and key of a world file, to edit one by hand.
- [CONTRIBUTING.md](../../CONTRIBUTING.md): the other ways to contribute.
