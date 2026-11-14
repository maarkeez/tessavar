# Contributing

Tessavar is a free, open source game, and contributions are welcome: art, code, translations, and feedback from
playing it at https://maarkeez.github.io/tessavar/.

Open an [issue](https://github.com/maarkeez/tessavar/issues) to share an idea or a bug, or to say what you would like to
work on before you start, so we can agree on it. Then send a pull request.

## Licensing of contributions

What you contribute is released under the licenses of the repository: code under the GNU General Public License
version 3 or later ([LICENSE](LICENSE)), and art under CC BY-SA 4.0 ([LICENSE-ART.md](LICENSE-ART.md)). You keep the
copyright of your work, and there is no agreement to sign. By sending a pull request, you confirm the work is yours to
contribute under those licenses.

If your contribution includes art, add yourself to [CREDITS.md](CREDITS.md).

## Drawing for Tessavar

Read the [design brief](docs/art/design-brief.md) first: it describes the look of the game and what is drawn as pixel
art. In short:

- **Seen from above**: the battlefield, the battle units and the npcs are top-down fantasy pixel art, like *The Legend
  of Zelda: A Link to the Past*. Draw characters from above, never straight-on.
- **Palette**: only the 64 [FamiCube](https://lospec.com/palette-list/famicube) colours, fully opaque or fully
  transparent. See [ADR 0005](docs/adr/0005-famicube-palette-and-outline.md).
- **Outline**: battle units, npcs, portraits, lootable items, buildings and the chest are outlined with `#151515`.
- **Sizes**: battle units and npcs are 16x16 or 32x32, with a 32x32 portrait; items, abilities and effects are 16x16;
  buildings are 16 pixels per tile; terrain transition strips are 256x16.
- **Sources**: every image has a layered Aseprite source under `art/`, mirroring `resources/`: `art/unit/hero.aseprite`
  is exported to `resources/unit/hero.png`. Send both. See
  [ADR 0006](docs/adr/0006-aseprite-sources-drawn-with-the-pixel-tool.md).

The tests check the palette and the outline of every image, so a pull request tells you if one is off.

## Code

The game is written in Kotlin with [KorGE](https://korge.org). Run it with `./gradlew runJvm`, and test it with
`./gradlew jvmTest jsTest`. The [glossary](GLOSSARY.md) names every concept of the game, and the [ADRs](docs/adr)
explain the decisions behind the code. See [How to add a new test](docs/testing/how-to-add-a-new-test.md) and
[How to add a story](docs/testing/how-to-add-a-story.md).

## Translations

See [How to add a language](docs/localization/how-to-add-a-language.md).
